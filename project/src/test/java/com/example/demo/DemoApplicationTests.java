package com.example.demo;

import com.example.demo.batch.BatchConfig;
import com.example.demo.model.Employee;
import com.example.demo.model.EmployeeRepository;
import com.example.demo.patterns.factory.NotificationFactory;
import com.example.demo.patterns.singleton.AppConfigManager;
import com.example.demo.patterns.strategy.EgyptianSalaryScaleStrategy;
import com.example.demo.patterns.strategy.SalaryGradeStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests — verify the whole pipeline works end to end.
 */
@SpringBootTest
@SpringBatchTest
class DemoApplicationTests {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importEmployeesJob;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private NotificationFactory notificationFactory;

    @Autowired
    private AppConfigManager configManager;

    // -------------------------------------------------------
    // Test 1: Spring Batch ETL Pipeline
    // -------------------------------------------------------
    @Test
    void testBatchJobCompletesSuccessfully() throws Exception {
        JobParameters params = new JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();

        JobExecution execution = jobLauncher.run(importEmployeesJob, params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // CSV has 20 rows, 2 have missing email → 18 should be written
        long count = employeeRepository.count();
        assertThat(count).isGreaterThan(0);
        System.out.println("✅ Batch Test: " + count + " employees imported");
    }

    // -------------------------------------------------------
    // Test 2: Strategy Pattern
    // -------------------------------------------------------
    @Test
    void testSalaryGradeStrategyCalculatesCorrectly() {
        SalaryGradeStrategy strategy = new EgyptianSalaryScaleStrategy();

        assertThat(strategy.calculateGrade(40000)).isEqualTo("Junior");
        assertThat(strategy.calculateGrade(50000)).isEqualTo("Mid");
        assertThat(strategy.calculateGrade(65000)).isEqualTo("Senior");
        assertThat(strategy.calculateGrade(75000)).isEqualTo("Lead");

        System.out.println("✅ Strategy Pattern Test: Salary grades calculated correctly");
    }

    // -------------------------------------------------------
    // Test 3: Factory Pattern
    // -------------------------------------------------------
    @Test
    void testFactoryCreatesCorrectSender() {
        var emailSender = notificationFactory.create("email");
        var smsSender   = notificationFactory.create("sms");
        var pushSender  = notificationFactory.create("push");

        assertThat(emailSender.getType()).isEqualTo("email");
        assertThat(smsSender.getType()).isEqualTo("sms");
        assertThat(pushSender.getType()).isEqualTo("push");

        // Should throw for unknown type
        try {
            notificationFactory.create("fax");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("fax");
        }

        System.out.println("✅ Factory Pattern Test: Correct senders created");
    }

    // -------------------------------------------------------
    // Test 4: Singleton Pattern
    // -------------------------------------------------------
    @Test
    void testSingletonReturnsConsistentConfig() {
        String chunkSize = configManager.get("batch.chunk-size");
        assertThat(chunkSize).isEqualTo("5");

        configManager.set("test.key", "test.value");
        assertThat(configManager.get("test.key")).isEqualTo("test.value");

        System.out.println("✅ Singleton Pattern Test: Config manager is consistent");
    }

    // -------------------------------------------------------
    // Test 5: Builder Pattern
    // -------------------------------------------------------
    @Test
    void testBuilderCreatesEmployeeCorrectly() {
        Employee emp = Employee.builder()
            .name("Test Employee")
            .email("test@example.com")
            .age(30)
            .salary(55000.0)
            .salaryGrade("Mid")
            .department("Operations")
            .build();

        assertThat(emp.getName()).isEqualTo("Test Employee");
        assertThat(emp.getSalaryGrade()).isEqualTo("Mid");
        assertThat(emp.getDepartment()).isEqualTo("Operations");

        System.out.println("✅ Builder Pattern Test: Employee built correctly");
    }
}
