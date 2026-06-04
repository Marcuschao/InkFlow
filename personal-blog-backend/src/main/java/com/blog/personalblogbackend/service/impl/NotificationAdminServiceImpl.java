package com.blog.personalblogbackend.service.impl;

import com.blog.personalblogbackend.config.properties.NotificationRabbitProperties;
import com.blog.personalblogbackend.model.dto.notification.NotificationMqStatusDto;
import com.blog.personalblogbackend.model.dto.notification.NotificationQueueStatDto;
import com.blog.personalblogbackend.service.NotificationAdminService;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationAdminServiceImpl implements NotificationAdminService {

    private final NotificationRabbitProperties props;
    private final ConnectionFactory connectionFactory;
    private final AmqpAdmin amqpAdmin;

    public NotificationAdminServiceImpl(NotificationRabbitProperties props,
                                      ConnectionFactory connectionFactory,
                                      AmqpAdmin amqpAdmin) {
        this.props = props;
        this.connectionFactory = connectionFactory;
        this.amqpAdmin = amqpAdmin;
    }

    @Override
    public NotificationMqStatusDto status() {
        NotificationMqStatusDto dto = new NotificationMqStatusDto();
        dto.setEnabled(props.isEnabled());
        dto.setExchange(props.getExchange());
        List<String> names = List.of(
                props.getInboxQueue(),
                props.getPushQueue(),
                props.getMailQueue(),
                props.getAuditQueue());
        dto.setQueues(names);
        dto.setConnected(isConnected());
        dto.setQueueStats(buildQueueStats(names));
        return dto;
    }

    private List<NotificationQueueStatDto> buildQueueStats(List<String> names) {
        List<NotificationQueueStatDto> stats = new ArrayList<>();
        if (!props.isEnabled() || amqpAdmin == null) {
            return stats;
        }
        for (String name : names) {
            NotificationQueueStatDto stat = new NotificationQueueStatDto();
            stat.setName(name);
            QueueInformation info = amqpAdmin.getQueueInfo(name);
            if (info != null) {
                stat.setMessageCount(info.getMessageCount());
                stat.setConsumerCount(info.getConsumerCount());
            }
            stat.setDlqMessageCount(queueMessageCount(name + ".dlq"));
            stats.add(stat);
        }
        return stats;
    }

    private long queueMessageCount(String queueName) {
        QueueInformation dlq = amqpAdmin.getQueueInfo(queueName);
        return dlq != null ? dlq.getMessageCount() : 0L;
    }

    private boolean isConnected() {
        if (!props.isEnabled()) {
            return false;
        }
        try {
            connectionFactory.createConnection().close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
