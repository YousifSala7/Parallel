package com.example.demo.patterns.observer;

import com.example.demo.patterns.factory.NotificationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer 2: Sends a notification when batch job completes.
 * Also uses the Factory Pattern to get the right notification sender.
 *
 * Factory Pattern + Observer Pattern working together!
 */
@Component
public class JobCompletionNotificationObserver {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationObserver.class);

    private final NotificationFactory notificationFactory;

    public JobCompletionNotificationObserver(NotificationFactory notificationFactory) {
        this.notificationFactory = notificationFactory;
    }

    @EventListener
    public void onJobCompleted(BatchJobCompletedEvent event) {
        String message = String.format(
            "Batch job '%s' finished. %d records processed, %d skipped.",
            event.getJobName(), event.getRecordsWritten(), event.getRecordsSkipped()
        );

        // Factory creates the right sender — here we use "email"
        notificationFactory.create("email").send("admin@company.com", message);
        log.info("[Observer] Admin notification sent for job: {}", event.getJobName());
    }
}
