package com.example.demo.patterns.observer;

import org.springframework.context.ApplicationEvent;

/**
 * ============================================================
 * DESIGN PATTERN: Observer (via Spring Events)
 * ============================================================
 * Intent: Define a one-to-many dependency so that when one
 * object changes state, all its dependents are notified
 * automatically.
 *
 * Real Use Here:
 * When the batch job completes, multiple components need to
 * react — send a notification, log statistics, update cache.
 * None of them need to know about each other.
 * ============================================================
 */
public class BatchJobCompletedEvent extends ApplicationEvent {

    private final int recordsRead;
    private final int recordsWritten;
    private final int recordsSkipped;
    private final String jobName;

    public BatchJobCompletedEvent(Object source, String jobName,
                                   int read, int written, int skipped) {
        super(source);
        this.jobName = jobName;
        this.recordsRead = read;
        this.recordsWritten = written;
        this.recordsSkipped = skipped;
    }

    public int getRecordsRead()    { return recordsRead; }
    public int getRecordsWritten() { return recordsWritten; }
    public int getRecordsSkipped() { return recordsSkipped; }
    public String getJobName()     { return jobName; }
}
