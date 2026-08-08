package com.blog.ai.runtime.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.ai.runtime.config.AgentRuntimeProperties;
import com.blog.ai.runtime.mapper.AgentEventArchiveMapper;
import com.blog.ai.runtime.model.AgentEvent;
import com.blog.ai.runtime.model.AgentEventArchive;
import com.blog.ai.runtime.model.AgentEventType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RedisAgentEventStore implements AgentEventStore {
    private static final String PREFIX = "ai:agent:events:";
    private final StringRedisTemplate redisTemplate;
    private final AgentEventArchiveMapper archiveMapper;
    private final ObjectMapper objectMapper;
    private final AgentRuntimeProperties properties;

    public RedisAgentEventStore(StringRedisTemplate redisTemplate, AgentEventArchiveMapper archiveMapper,
                                ObjectMapper objectMapper, AgentRuntimeProperties properties) {
        this.redisTemplate = redisTemplate;
        this.archiveMapper = archiveMapper;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void append(AgentEvent event) {
        String payload = write(event.getData());
        try {
            Map<String, String> value = new LinkedHashMap<>();
            value.put("type", event.getType().name());
            value.put("traceId", event.getTraceId());
            value.put("timestamp", event.getTimestamp().toString());
            value.put("payload", payload);
            MapRecord<String, String, String> record = StreamRecords.newRecord()
                    .ofMap(value).withStreamKey(PREFIX + event.getRunId())
                    .withId(RecordId.of(event.getSequence() + "-0"));
            redisTemplate.opsForStream().add(record);
            redisTemplate.expire(PREFIX + event.getRunId(), properties.getEventRetention());
        } catch (RuntimeException ex) {
            log.warn("[agent-runtime] redis event append failed runId={} sequence={}: {}",
                    event.getRunId(), event.getSequence(), ex.getMessage());
        }
        AgentEventArchive row = new AgentEventArchive();
        row.setRunId(event.getRunId());
        row.setSequence(event.getSequence());
        row.setEventType(event.getType().name());
        row.setPayloadJson(payload);
        row.setCreatedAt(event.getTimestamp());
        try { archiveMapper.insert(row); }
        catch (DuplicateKeyException ignored) { }
    }

    @Override
    public List<AgentEvent> replay(String runId, long afterSequence, int limit) {
        int capped = Math.min(Math.max(limit, 1), properties.getEventReplayLimit());
        try {
            Range<String> range = Range.open(afterSequence + "-0", "+");
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().range(PREFIX + runId, range);
            if (records != null && !records.isEmpty()) {
                return records.stream().limit(capped).map(r -> fromRedis(runId, r)).toList();
            }
        } catch (RuntimeException ex) {
            log.warn("[agent-runtime] redis replay failed runId={}: {}", runId, ex.getMessage());
        }
        List<AgentEventArchive> rows = archiveMapper.selectList(new LambdaQueryWrapper<AgentEventArchive>()
                .eq(AgentEventArchive::getRunId, runId).gt(AgentEventArchive::getSequence, afterSequence)
                .orderByAsc(AgentEventArchive::getSequence).last("LIMIT " + capped));
        return rows.stream().map(this::fromArchive).toList();
    }

    private AgentEvent fromRedis(String runId, MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();
        long sequence = Long.parseLong(record.getId().getValue().split("-")[0]);
        return AgentEvent.builder().eventId(record.getId().getValue()).sequence(sequence).runId(runId)
                .traceId(String.valueOf(value.get("traceId")))
                .type(AgentEventType.valueOf(String.valueOf(value.get("type"))))
                .data(read(String.valueOf(value.get("payload"))))
                .timestamp(LocalDateTime.parse(String.valueOf(value.get("timestamp")))).build();
    }

    private AgentEvent fromArchive(AgentEventArchive row) {
        return AgentEvent.builder().eventId(row.getSequence() + "-0").sequence(row.getSequence())
                .runId(row.getRunId()).type(AgentEventType.valueOf(row.getEventType()))
                .data(read(row.getPayloadJson())).timestamp(row.getCreatedAt()).build();
    }

    private String write(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception ex) { throw new IllegalStateException("Cannot serialize agent event", ex); }
    }

    private Map<String, Object> read(String value) {
        try { return objectMapper.readValue(value, new TypeReference<>() {}); }
        catch (Exception ex) { return Map.of("raw", value); }
    }
}
