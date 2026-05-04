# Batch Processing with Spring Batch

## What Is Batch Processing?

Batch processing is about handling large volumes of data in chunks, usually without user interaction and often on a schedule. Think of things like:

- Processing thousands of bank transactions overnight
- Generating monthly reports from a year's worth of data
- Importing a 500,000-row CSV file into a database
- Running ETL pipelines to move data between systems
- Sending out bulk emails or notifications

The key characteristics of batch work are:
- **Large data volumes** — you're not processing 10 records, you're processing millions
- **No real-time interaction** — it runs in the background, often on a schedule
- **Restart/retry capability** — if something fails halfway, you need to be able to resume
- **Trackability** — you need to know the status of each run

This is exactly what Spring Batch was designed for.

---

## Why Spring Batch?

You could technically write your own batch processing loop — read all records, process them, write them out. But that breaks down quickly:

- What happens if the job crashes halfway through?
- How do you avoid reprocessing records that were already handled?
- How do you track which runs succeeded or failed?
- How do you handle one bad record without stopping the whole job?
- How do you process 10 million records without running out of memory?

Spring Batch addresses all of these out of the box.

---

## Core Concepts

Spring Batch has a well-defined structure. Once you understand it, the rest clicks into place.

### Job

A `Job` is the top-level unit in Spring Batch. It represents the entire batch process — "process today's transactions", "generate monthly reports", etc.

A job is made up of one or more **Steps**.

### Step

A `Step` is a single phase within a job. For simple jobs, there might be one step. For complex ones, you might chain multiple steps where each does a different thing (e.g., Step 1: validate data, Step 2: transform it, Step 3: load it into the database).

### Chunk-Oriented Processing

The most common step type reads data in **chunks**. Instead of reading one record, processing it, and writing it — then moving on — Spring Batch reads N records, processes all N, then writes all N as a group. This is much more efficient, especially with database writes.

```
Read 100 records → Process all 100 → Write all 100 → Commit
Read next 100 → Process → Write → Commit
...
```

The chunk size is configurable. Larger chunks mean fewer transactions (faster), but also more memory usage and larger rollback units if something fails.

---

## The Three Interfaces

Every chunk-oriented step uses three components:

### ItemReader

Reads data from a source, one item at a time. Spring Batch calls `read()` repeatedly until it returns `null`, which signals the end of input.

```java
public class CustomerReader implements ItemReader<Customer> {
    private final CustomerRepository repo;
    private Iterator<Customer> iterator;

    @Override
    public Customer read() {
        if (iterator == null) {
            iterator = repo.findAll().iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}
```

Spring Batch also provides ready-made readers:
- `FlatFileItemReader` — for CSV and flat files
- `JdbcCursorItemReader` — reads directly from a database with a cursor
- `JpaPagingItemReader` — paginates through JPA results
- `StaxEventItemReader` — for XML files

### ItemProcessor

Transforms or validates each item. This is optional — if you don't need to transform anything, you can skip it.

```java
public class CustomerProcessor implements ItemProcessor<Customer, CustomerDTO> {

    @Override
    public CustomerDTO process(Customer customer) throws Exception {
        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            return null; // returning null filters out this record
        }
        return new CustomerDTO(
            customer.getId(),
            customer.getName().trim(),
            customer.getEmail().toLowerCase()
        );
    }
}
```

Returning `null` from a processor tells Spring Batch to skip that item (it won't be passed to the writer).

### ItemWriter

Writes the processed chunk to the destination.

```java
public class CustomerWriter implements ItemWriter<CustomerDTO> {

    private final CustomerDTORepository repo;

    @Override
    public void write(List<? extends CustomerDTO> items) throws Exception {
        repo.saveAll(items); // saves all items in the chunk as one batch insert
    }
}
```

Ready-made writers include:
- `FlatFileItemWriter` — write to CSV or text files
- `JdbcBatchItemWriter` — efficient batch inserts into a database
- `JpaItemWriter` — writes via JPA
- `CompositeItemWriter` — chains multiple writers together

---

## Setting Up Spring Batch

### Dependencies (Maven)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-batch</artifactId>
</dependency>

<!-- Spring Batch needs a database to store job metadata -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Basic Configuration

```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Bean
    public Job customerImportJob(JobBuilderFactory jobs, Step importStep) {
        return jobs.get("customerImportJob")
                   .start(importStep)
                   .build();
    }

    @Bean
    public Step importStep(
            StepBuilderFactory steps,
            ItemReader<Customer> reader,
            ItemProcessor<Customer, CustomerDTO> processor,
            ItemWriter<CustomerDTO> writer) {

        return steps.get("importStep")
                    .<Customer, CustomerDTO>chunk(100)  // process 100 at a time
                    .reader(reader)
                    .processor(processor)
                    .writer(writer)
                    .build();
    }
}
```

---

## Job Metadata and the Job Repository

Spring Batch automatically tracks every job run in a set of database tables. You don't need to manage this — it's done for you. But it's worth knowing what's being tracked:

| Table | What It Stores |
|---|---|
| `BATCH_JOB_INSTANCE` | Each unique job instance |
| `BATCH_JOB_EXECUTION` | Each run of a job instance (status, start time, end time) |
| `BATCH_STEP_EXECUTION` | Per-step details (read count, write count, skip count, etc.) |
| `BATCH_JOB_EXECUTION_PARAMS` | Parameters passed to the job |

This is powerful — if a job fails on step 3 of 5, Spring Batch can restart it from step 3 without re-running the first two steps.

---

## Skip and Retry Logic

Real data is messy. Some records might be invalid, and you don't want one bad record to kill the entire job.

### Skip

```java
return steps.get("importStep")
            .<Customer, CustomerDTO>chunk(100)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skip(InvalidDataException.class)
            .skipLimit(50) // allow up to 50 skips before failing the job
            .build();
```

### Retry

```java
.faultTolerant()
.retry(TransientDataAccessException.class)
.retryLimit(3) // retry up to 3 times before skipping or failing
```

---

## ETL with Spring Batch

Spring Batch is a natural fit for ETL (Extract, Transform, Load) pipelines:

```
[ Source DB / File / API ]
        ↓ Extract (ItemReader)
[ Raw records ]
        ↓ Transform (ItemProcessor)
[ Cleaned / reformatted records ]
        ↓ Load (ItemWriter)
[ Target DB / File / System ]
```

A real example might be:
1. Read raw order data from a legacy system's database
2. Validate and transform it into the format your new system expects
3. Write it into the new database, logging any skipped records

---

## Job Scheduling

You'll often want batch jobs to run on a schedule — nightly, weekly, etc. Spring's `@Scheduled` annotation combined with a `JobLauncher` makes this easy:

```java
@Component
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job customerImportJob;

    @Scheduled(cron = "0 0 2 * * ?") // every day at 2:00 AM
    public void runJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
            .addLong("timestamp", System.currentTimeMillis()) // ensures unique run
            .toJobParameters();

        jobLauncher.run(customerImportJob, params);
    }
}
```

Note: Spring Batch considers two job runs with identical parameters as the same run — adding a timestamp ensures each scheduled run is treated as a new instance.

---

## Monitoring a Job Run

You can check the status of a job programmatically:

```java
JobExecution execution = jobLauncher.run(job, params);

System.out.println("Status: " + execution.getStatus());
System.out.println("Start: " + execution.getStartTime());
System.out.println("End:   " + execution.getEndTime());

for (StepExecution step : execution.getStepExecutions()) {
    System.out.println("Step: " + step.getStepName());
    System.out.println("  Read:    " + step.getReadCount());
    System.out.println("  Written: " + step.getWriteCount());
    System.out.println("  Skipped: " + step.getSkipCount());
}
```

---

## Parallelism in Spring Batch

For very large datasets, you can add parallelism to Spring Batch without dealing with raw threads yourself.

### Partitioning

Splits the data into partitions and processes each partition in its own thread:

```java
@Bean
public Step partitionedStep(PartitionHandler partitionHandler) {
    return steps.get("partitionedStep")
                .partitioner("workerStep", partitioner())
                .partitionHandler(partitionHandler)
                .build();
}

@Bean
public PartitionHandler partitionHandler(Step workerStep) {
    TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
    handler.setTaskExecutor(new SimpleAsyncTaskExecutor());
    handler.setStep(workerStep);
    handler.setGridSize(4); // 4 parallel threads
    return handler;
}
```

---

## Summary

| Concept | Description |
|---|---|
| `Job` | The complete batch process |
| `Step` | A single phase within a job |
| `ItemReader` | Reads data from a source |
| `ItemProcessor` | Transforms/validates each record |
| `ItemWriter` | Writes the results to a destination |
| Chunk size | How many records per transaction |
| Job Repository | Auto-tracks all job executions in the DB |
| Skip / Retry | Handle bad records without stopping the job |
| Partitioning | Parallel processing within Spring Batch |

---

## Connecting Back to Architecture

Spring Batch fits naturally into several architectural patterns discussed in the architecture document:

- It follows a **layered architecture** internally (reader → processor → writer)
- It's an excellent tool for implementing **ETL pipelines**
- It can be one service in a **microservices** setup — a dedicated batch processing service
- It plays well with **event-driven** architectures — a job can be triggered by a Kafka message, for example

---

## References

- Spring Batch Official Documentation — https://docs.spring.io/spring-batch/docs/current/reference/html/
- Spring Batch GitHub — https://github.com/spring-projects/spring-batch
- Baeldung Spring Batch Guide — https://www.baeldung.com/spring-batch-tutorial
- *The Definitive Guide to Spring Batch* — Michael Minella
