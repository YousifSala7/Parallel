package com.example.demo.patterns.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer 1: Logs job statistics when batch job completes.
 * Completely decoupled from the batch job itself.
 */
@Component
public class StatisticsLogObserver {

    private static final Logger log = LoggerFactory.getLogger(StatisticsLogObserver.class);

    @EventListener
    public void onJobCompleted(BatchJobCompletedEvent event) {
        log.info("==============================================");
        log.info("  BATCH JOB COMPLETED: {}", event.getJobName());
        log.info("  Records Read:    {}", event.getRecordsRead());
        log.info("  Records Written: {}", event.getRecordsWritten());
        log.info("  Records Skipped: {}", event.getRecordsSkipped());
        log.info("  Success Rate:    {}%",
            event.getRecordsRead() > 0
                ? (event.getRecordsWritten() * 100 / event.getRecordsRead())
                : 0);
        log.info("==============================================");
    }
}
