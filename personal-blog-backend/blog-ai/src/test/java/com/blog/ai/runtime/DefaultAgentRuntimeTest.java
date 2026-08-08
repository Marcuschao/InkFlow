package com.blog.ai.runtime;

import com.blog.ai.runtime.config.AgentRuntimeProperties;
import com.blog.ai.runtime.core.AgentCancellationRegistry;
import com.blog.ai.runtime.core.AgentStateMachine;
import com.blog.ai.runtime.event.AgentEventStore;
import com.blog.ai.runtime.handler.AgentTaskHandler;
import com.blog.ai.runtime.mapper.AgentRunMapper;
import com.blog.ai.runtime.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DefaultAgentRuntimeTest {
    @Test
    void persistsStrictlyIncreasingEventsForSuccessfulRun() {
        AgentTaskHandler handler = mock(AgentTaskHandler.class);
        when(handler.supports()).thenReturn(AgentTaskType.WRITING);
        AgentResult taskResult = new AgentResult(); taskResult.setAnswer("done");
        when(handler.execute(any(), any())).thenReturn(taskResult);
        AgentRunMapper mapper = mock(AgentRunMapper.class);
        when(mapper.transition(anyString(), anyString(), anyString(), anyInt())).thenReturn(1);
        when(mapper.finish(anyString(), anyString(), any(), any(), anyInt(), anyInt(), anyDouble(), any(), any())).thenReturn(1);
        AgentEventStore eventStore = mock(AgentEventStore.class);
        AgentCancellationRegistry cancellations = mock(AgentCancellationRegistry.class);
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(List.of(handler), mapper, new AgentStateMachine(),
                eventStore, cancellations, new AgentRuntimeProperties(), new ObjectMapper(),
                new SimpleMeterRegistry(), new TaskExecutorAdapter(new SyncTaskExecutor()));
        AgentRequest request = new AgentRequest(); request.setTaskType(AgentTaskType.WRITING); request.setOperation("outline");
        AgentResult result = runtime.run(request, new AgentExecutionContext(1L, null, null, "user", null));
        assertEquals(AgentRunStatus.COMPLETED, result.getStatus());
        ArgumentCaptor<AgentEvent> events = ArgumentCaptor.forClass(AgentEvent.class);
        verify(eventStore, atLeast(7)).append(events.capture());
        List<AgentEvent> values = events.getAllValues();
        for (int i = 1; i < values.size(); i++) assertTrue(values.get(i).getSequence() > values.get(i - 1).getSequence());
        assertTrue(values.stream().anyMatch(e -> e.getType() == AgentEventType.STATE_CHANGED));
        assertEquals(AgentEventType.RUN_COMPLETED, values.get(values.size() - 1).getType());
    }
}
