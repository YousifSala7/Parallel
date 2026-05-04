package com.example.demo.patterns.factory;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Factory — creates (or returns) the right NotificationSender
 * based on the requested type.
 *
 * Spring auto-injects all implementations of NotificationSender.
 * The factory maps them by type string.
 */
@Component
public class NotificationFactory {

    private final Map<String, NotificationSender> senderMap;

    // Spring injects ALL beans that implement NotificationSender
    public NotificationFactory(List<NotificationSender> senders) {
        this.senderMap = senders.stream()
            .collect(Collectors.toMap(NotificationSender::getType, s -> s));
    }

    /**
     * Factory method — returns the correct sender for the given type.
     * @param type "email", "sms", or "push"
     */
    public NotificationSender create(String type) {
        NotificationSender sender = senderMap.get(type.toLowerCase());
        if (sender == null) {
            throw new IllegalArgumentException(
                "Unknown notification type: '" + type + "'. Available: " + senderMap.keySet()
            );
        }
        return sender;
    }
}
