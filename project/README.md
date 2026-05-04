# Architecture & Design Patterns — Spring Boot Demo

A practical implementation of Architecture Patterns, Design Patterns, Spring Batch, and Multithreading in Java/Spring Boot.

---

## Project Structure

```
src/
├── main/java/com/example/demo/
│   ├── DemoApplication.java              ← Spring Boot entry point
│   ├── model/
│   │   ├── Employee.java                 ← JPA Entity (Builder Pattern via Lombok)
│   │   ├── EmployeeCSV.java              ← Raw CSV row DTO
│   │   └── EmployeeRepository.java       ← Data Access Layer (Layered Architecture)
│   ├── service/
│   │   └── EmployeeService.java          ← Business Logic Layer
│   ├── controller/
│   │   └── MainController.java           ← REST API (Presentation Layer / MVC)
│   ├── batch/
│   │   ├── BatchConfig.java              ← Spring Batch ETL Job (Reader→Processor→Writer)
│   │   ├── EmployeeItemProcessor.java    ← Transform step (Strategy + Template Method)
│   │   └── BatchJobListener.java         ← Job lifecycle (Observer publisher)
│   ├── multithreading/
│   │   └── ParallelEmployeeAnalyticsService.java  ← ExecutorService, Future, CompletableFuture
│   └── patterns/
│       ├── factory/
│       │   ├── NotificationSender.java   ← Factory interface
│       │   ├── NotificationSenderImpl.java ← Email, SMS, Push implementations
│       │   └── NotificationFactory.java  ← The Factory
│       ├── strategy/
│       │   ├── SalaryGradeStrategy.java  ← Strategy interface
│       │   ├── EgyptianSalaryScaleStrategy.java
│       │   └── ExperienceSalaryScaleStrategy.java
│       ├── observer/
│       │   ├── BatchJobCompletedEvent.java ← The Event
│       │   ├── StatisticsLogObserver.java  ← Observer 1
│       │   └── JobCompletionNotificationObserver.java ← Observer 2
│       ├── singleton/
│       │   └── AppConfigManager.java     ← Singleton (Spring + manual impl)
│       └── builder/
│           └── (see Employee.java — Lombok @Builder)
└── resources/
    ├── application.properties
    └── employees.csv                     ← Sample data for batch import
```

---

## Patterns Implemented

### Architecture Patterns
| Pattern | Where |
|---|---|
| **Layered Architecture** | Controller → Service → Repository |
| **MVC** | Spring MVC REST controllers |
| **ETL** | Spring Batch: CSV → Transform → H2 DB |

### Design Patterns
| Pattern | Where |
|---|---|
| **Singleton** | `AppConfigManager` (Spring bean + manual double-checked locking) |
| **Factory Method** | `NotificationFactory` → creates Email/SMS/Push senders |
| **Builder** | `Employee.builder()` via Lombok `@Builder` |
| **Strategy** | `SalaryGradeStrategy` — 2 interchangeable grading algorithms |
| **Observer** | `BatchJobCompletedEvent` + `@EventListener` on 2 observers |
| **Template Method** | Spring Batch `ItemProcessor` interface |
| **Adapter** | Documented in `architecture-design-patterns.md` |
| **Facade** | `EmployeeService` wraps repository + factory complexity |

---

## How to Run

### Prerequisites
- Java 17+
- Maven 3.8+

### Steps
```bash
# 1. Clone or download the project

# 2. Build
mvn clean install

# 3. Run
mvn spring-boot:run
```

The app starts on **http://localhost:8080**

---

## API Endpoints

### Step 1 — Import data via Spring Batch (ETL)
```
POST http://localhost:8080/api/batch/run
```
This runs the full ETL pipeline:
- **Extract**: reads `employees.csv` (20 rows)
- **Transform**: validates email, calculates salary grade (Strategy Pattern), assigns department
- **Load**: saves to H2 database (records with missing email are skipped gracefully)

Expected response:
```json
{
  "jobName": "importEmployeesJob",
  "status": "COMPLETED",
  "recordsRead": 20,
  "recordsWritten": 18,
  "recordsSkipped": 2
}
```

### Step 2 — Query employees
```
GET /api/employees                          → all employees
GET /api/employees/high-earners?minSalary=65000
GET /api/employees/department/Engineering
GET /api/stats/grades                       → count by salary grade
GET /api/stats/departments                  → count by department
```

### Step 3 — Run parallel analytics (Multithreading)
```
GET /api/analytics/parallel
```
Splits the employee list into partitions and processes each on a separate thread.

### Step 4 — Async summary (CompletableFuture)
```
GET /api/analytics/async-summary
```
Two CompletableFuture tasks run in parallel and combine results.

### Step 5 — Factory Pattern demo
```
POST /api/notify?type=email&recipient=test@example.com
POST /api/notify?type=sms&recipient=01012345678
POST /api/notify?type=push&recipient=user123
```

### Step 6 — Singleton Pattern demo
```
GET /api/config
```

### H2 Database Console
```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:testdb
Username: sa  |  Password: (empty)
```
After running the batch job, check the `EMPLOYEES` table and the Spring Batch metadata tables (`BATCH_JOB_EXECUTION`, `BATCH_STEP_EXECUTION`).

---

## Running Tests
```bash
mvn test
```
Tests cover: Batch ETL pipeline, Strategy Pattern, Factory Pattern, Singleton Pattern, Builder Pattern.

---

## Key Concepts Demonstrated

### Spring Batch — Chunk Processing
```
Read 5 records → Process all 5 → Write all 5 → Commit to DB
Read next 5   → Process      → Write       → Commit
...
```

### Multithreading — Data Partitioning
```
All Employees (e.g. 20)
       ↓
┌──────┬──────┬──────┬──────┐
│  P1  │  P2  │  P3  │  P4  │  ← 4 partitions
│  5   │  5   │  5   │  5   │
└──────┴──────┴──────┴──────┘
   ↓      ↓      ↓      ↓
Thread1 Thread2 Thread3 Thread4  ← run in parallel
   ↓      ↓      ↓      ↓
        Aggregate results
```

### Observer Pattern — Event Flow
```
BatchJob finishes
    ↓
BatchJobListener.afterJob()
    ↓
eventPublisher.publishEvent(BatchJobCompletedEvent)
    ↓
┌───────────────────────┬──────────────────────────────┐
│ StatisticsLogObserver │ JobCompletionNotificationObs  │
│ → logs stats to console│ → uses Factory to send email │
└───────────────────────┴──────────────────────────────┘
```

---

## References
- Spring Batch Docs: https://docs.spring.io/spring-batch/
- Spring Framework Docs: https://docs.spring.io/spring-framework/
- Refactoring Guru (Patterns): https://refactoring.guru/design-patterns
- Java Concurrency: https://docs.oracle.com/en/java/
