package com.example.demo.batch;

import com.example.demo.patterns.observer.BatchJobCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Listens to batch job lifecycle events.
 * Fires a Spring ApplicationEvent when the job completes —
 * this is where Observer Pattern kicks in.
 */
@Component
public class BatchJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(BatchJobListener.class);

    // Used to publish events to all observers
    private final ApplicationEventPublisher eventPublisher;

    public BatchJobListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(">>> Starting batch job: {}", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {

            // Collect stats from all step executions
            int totalRead = 0, totalWritten = 0, totalSkipped = 0;
            for (var step : jobExecution.getStepExecutions()) {
                totalRead    += step.getReadCount();
                totalWritten += step.getWriteCount();
                totalSkipped += step.getSkipCount();
            }

            log.info("<<< Batch job finished: status={}", jobExecution.getStatus());

            // OBSERVER PATTERN: publish event — all observers will react
            eventPublisher.publishEvent(new BatchJobCompletedEvent(
                this,
                jobExecution.getJobInstance().getJobName(),
                totalRead, totalWritten, totalSkipped
            ));

        } else {
            log.error("<<< Batch job FAILED: status={}", jobExecution.getStatus());
            jobExecution.getAllFailureExceptions()
                        .forEach(ex -> log.error("  Cause: {}", ex.getMessage()));
        }
    }
}
