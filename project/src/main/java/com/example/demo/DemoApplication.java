package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ============================================================
 * Architecture & Design Patterns — Spring Boot Demo
 * ============================================================
 *
 * This project demonstrates:
 *
 * ARCHITECTURE PATTERNS:
 *  ✅ Layered Architecture     → Controller → Service → Repository
 *  ✅ MVC                      → Spring MVC (REST controllers)
 *  ✅ ETL Architecture         → Spring Batch (CSV → DB)
 *
 * DESIGN PATTERNS:
 *  ✅ Singleton                → AppConfigManager (Spring bean)
 *  ✅ Factory Method           → NotificationFactory
 *  ✅ Builder                  → Employee.builder() (Lombok)
 *  ✅ Strategy                 → SalaryGradeStrategy (2 implementations)
 *  ✅ Observer                 → ApplicationEvent + @EventListener
 *  ✅ Template Method          → Spring Batch ItemProcessor/ItemReader
 *
 * MULTITHREADING:
 *  ✅ ExecutorService + Thread Pool
 *  ✅ Callable + Future
 *  ✅ CompletableFuture (async chaining)
 *  ✅ AtomicInteger (thread-safe counter)
 *  ✅ Data partitioning across threads
 *  ✅ HikariCP connection pooling
 *
 * SPRING BATCH:
 *  ✅ FlatFileItemReader (CSV)
 *  ✅ ItemProcessor (validate + transform)
 *  ✅ RepositoryItemWriter (JPA)
 *  ✅ Chunk-oriented processing (chunk size = 5)
 *  ✅ Skip logic (invalid records skipped gracefully)
 *  ✅ Job metadata tracking (BATCH_JOB_EXECUTION table)
 *  ✅ Job lifecycle listener
 *
 * HOW TO RUN:
 *  mvn spring-boot:run
 *
 * THEN USE THE API:
 *  1. Import CSV data via batch:
 *     POST http://localhost:8080/api/batch/run
 *
 *  2. View imported employees:
 *     GET  http://localhost:8080/api/employees
 *
 *  3. Run parallel analytics:
 *     GET  http://localhost:8080/api/analytics/parallel
 *
 *  4. Async summary (CompletableFuture):
 *     GET  http://localhost:8080/api/analytics/async-summary
 *
 *  5. Factory Pattern — send notification:
 *     POST http://localhost:8080/api/notify?type=sms&recipient=01012345678
 *
 *  6. Singleton Pattern — view config:
 *     GET  http://localhost:8080/api/config
 *
 *  7. H2 Database Console:
 *     http://localhost:8080/h2-console
 *     JDBC URL: jdbc:h2:mem:testdb
 * ============================================================
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
