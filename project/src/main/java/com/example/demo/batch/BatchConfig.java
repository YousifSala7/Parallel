package com.example.demo.batch;

import com.example.demo.model.Employee;
import com.example.demo.model.EmployeeCSV;
import com.example.demo.model.EmployeeRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * ============================================================
 * Spring Batch Configuration
 * ============================================================
 * ETL Pipeline:
 *   EXTRACT  → FlatFileItemReader reads employees.csv
 *   TRANSFORM → EmployeeItemProcessor validates & enriches
 *   LOAD     → RepositoryItemWriter saves to H2 database
 *
 * Architecture: ETL + Layered + Template Method pattern
 * ============================================================
 */
@Configuration
public class BatchConfig {

    // -------------------------------------------------------
    // EXTRACT: ItemReader — reads CSV file line by line
    // -------------------------------------------------------
    @Bean
    public FlatFileItemReader<EmployeeCSV> csvReader() {
        return new FlatFileItemReaderBuilder<EmployeeCSV>()
            .name("employeeCsvReader")
            .resource(new ClassPathResource("employees.csv"))
            .delimited()
            .names("id", "name", "email", "age", "salary") // CSV column names
            .linesToSkip(1)  // skip header row
            .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                setTargetType(EmployeeCSV.class);
            }})
            .build();
    }

    // -------------------------------------------------------
    // LOAD: ItemWriter — writes processed records to DB
    // -------------------------------------------------------
    @Bean
    public RepositoryItemWriter<Employee> dbWriter(EmployeeRepository repository) {
        return new RepositoryItemWriterBuilder<Employee>()
            .repository(repository)
            .methodName("save")
            .build();
    }

    // -------------------------------------------------------
    // STEP: ties reader → processor → writer together
    // chunk(5) = read 5 records, process 5, write 5, commit
    // -------------------------------------------------------
    @Bean
    public Step importEmployeesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<EmployeeCSV> reader,
            EmployeeItemProcessor processor,
            RepositoryItemWriter<Employee> writer) {

        return new StepBuilder("importEmployeesStep", jobRepository)
            .<EmployeeCSV, Employee>chunk(5, transactionManager) // chunk size = 5
            .reader(reader)
            .processor(processor)
            .writer(writer)
            // Skip bad records instead of failing the whole job
            .faultTolerant()
            .skip(Exception.class)
            .skipLimit(5) // allow up to 5 skips total
            .build();
    }

    // -------------------------------------------------------
    // JOB: the complete batch process
    // -------------------------------------------------------
    @Bean
    public Job importEmployeesJob(
            JobRepository jobRepository,
            Step importEmployeesStep,
            BatchJobListener listener) {

        return new JobBuilder("importEmployeesJob", jobRepository)
            .incrementer(new RunIdIncrementer()) // unique run ID each time
            .listener(listener)                  // Observer pattern hook
            .start(importEmployeesStep)
            .build();
    }
}
