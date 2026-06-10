package com.blog.personalblogbackend.notification.inbox;

import com.blog.personalblogbackend.model.enums.NotificationType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class InboxNotificationAssembler {

    private final Map<NotificationType, InboxNotificationContentStrategy> strategies;

    public InboxNotificationAssembler(List<InboxNotificationContentStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(InboxNotificationContentStrategy::type, Function.identity()));
    }

    public Optional<InboxNotificationDraft> assemble(NotificationType type, InboxNotificationRequest request) {
        InboxNotificationContentStrategy strategy = strategies.get(type);
        if (strategy == null) {
            return Optional.empty();
        }
        return strategy.build(request);
    }
}
