# Architecture Patterns & Design Patterns

## Overview

When building any non-trivial software system, two of the most important things to get right early on are the **architecture** of the system and the **design patterns** used within it. These aren't just theoretical concepts — they're practical tools that help you avoid common mistakes, write code that's easier to maintain, and make decisions that won't come back to bite you later.

This document covers what I found through researching both topics, with a focus on what's actually useful when building real projects.

---

## Part 1: Architecture Patterns

An architecture pattern is a high-level strategy for organizing an entire system. It defines how different parts of the application communicate, where responsibilities live, and how the system scales or evolves over time.

### 1. Layered (N-Tier) Architecture

This is probably the most common pattern, especially in enterprise Java applications. The idea is simple — you split your application into layers, and each layer only talks to the one directly below it.

**Typical layers:**
- **Presentation Layer** — what the user sees (UI, REST controllers)
- **Business Logic Layer** — where the actual rules and workflows live
- **Data Access Layer** — anything related to reading/writing from the database
- **Database Layer** — the actual database

**Why it works:** It enforces separation of concerns. Your controller doesn't need to know how you're querying the database, and your database layer shouldn't care about HTTP requests.

**Where it struggles:** In very large systems, the strict layer dependency can become a bottleneck. Also, simple operations sometimes pass through too many layers unnecessarily.

---

### 2. MVC (Model-View-Controller)

MVC is technically a design pattern, but at scale it functions as an architectural approach — especially in web frameworks like Spring MVC.

- **Model** — holds the data and business logic
- **View** — what's rendered to the user (HTML, JSON response, etc.)
- **Controller** — receives requests, delegates to the model, returns the view

Spring MVC is a great example of this in action. The `@Controller` handles incoming HTTP requests, calls a `@Service` which contains the logic, and the `@Repository` handles the data layer underneath.

---

### 3. Microservices Architecture

Instead of one big application doing everything, you split functionality into small, independent services that each handle one thing and communicate over a network (usually HTTP/REST or message queues).

**Benefits:**
- Each service can be deployed, scaled, and updated independently
- Different services can use different tech stacks if needed
- Failure in one service doesn't necessarily bring down everything

**Challenges:**
- Much more complex to manage (you now have multiple deployments, databases, and communication channels to worry about)
- Debugging across services is harder
- Requires good tooling (Docker, Kubernetes, API gateways, etc.)

---

### 4. Event-Driven Architecture

In this pattern, components communicate by producing and consuming events rather than calling each other directly. A component fires an event ("order placed") and other components react to it ("send confirmation email", "update inventory", "notify warehouse").

This decouples the components nicely — the order service doesn't need to know anything about the email service or inventory service.

Common tools: **Apache Kafka**, **RabbitMQ**, Spring's `@EventListener`.

---

### 5. ETL Architecture (Extract, Transform, Load)

This one comes up a lot in data engineering and batch processing contexts. The idea is:

1. **Extract** data from one or more sources (databases, files, APIs)
2. **Transform** it — clean it, reformat it, enrich it
3. **Load** it into a destination (a data warehouse, another database, a file)

Spring Batch is built around this model, which is why it fits naturally into ETL pipelines.

---

## Part 2: Design Patterns

Design patterns are lower-level, reusable solutions to common problems that come up when writing code. They were popularized by the "Gang of Four" book (*Design Patterns: Elements of Reusable Object-Oriented Software*) and are categorized into three groups.

---

### Creational Patterns — how objects are created

#### Singleton
Ensures only one instance of a class exists throughout the application. Spring beans are singletons by default — when you inject a `@Service` into two different places, you get the same object.

```java
// Spring handles this for you, but the manual version:
public class ConfigManager {
    private static ConfigManager instance;

    private ConfigManager() {}

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
}
```

**Real use case:** Database connection pools, logging services, configuration managers.

---

#### Factory Method
Instead of calling `new SomeClass()` directly, you delegate the creation to a factory method. This is useful when the exact class to instantiate isn't known ahead of time or when you want to centralize object creation.

```java
public interface Notification {
    void send(String message);
}

public class EmailNotification implements Notification {
    public void send(String message) { /* send email */ }
}

public class SMSNotification implements Notification {
    public void send(String message) { /* send SMS */ }
}

public class NotificationFactory {
    public static Notification create(String type) {
        return switch (type) {
            case "email" -> new EmailNotification();
            case "sms"   -> new SMSNotification();
            default      -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}
```

---

#### Builder
Used when constructing a complex object requires many optional parameters. Instead of a constructor with ten arguments (half of which might be null), you chain method calls.

```java
User user = new User.Builder("ahmed@example.com")
    .name("Ahmed")
    .age(25)
    .role("admin")
    .build();
```

Lombok's `@Builder` annotation does this for you automatically in Spring projects.

---

### Structural Patterns — how objects are composed

#### Adapter
Lets two incompatible interfaces work together. Think of it like a plug adapter — you have a device with one plug type, and you want to use it with a socket that has a different type.

```java
// You have this legacy interface
public interface OldPaymentGateway {
    void makePayment(int amountInCents);
}

// You want to use this new one
public interface PaymentService {
    void pay(double amount);
}

// The adapter bridges them
public class PaymentAdapter implements PaymentService {
    private OldPaymentGateway gateway;

    public PaymentAdapter(OldPaymentGateway gateway) {
        this.gateway = gateway;
    }

    public void pay(double amount) {
        gateway.makePayment((int)(amount * 100));
    }
}
```

---

#### Decorator
Adds behavior to an object dynamically without modifying its class. Java's I/O streams are a classic example — you wrap a `FileInputStream` in a `BufferedInputStream` to add buffering, then wrap that in a `DataInputStream` to add typed reads.

```java
InputStream in = new DataInputStream(
    new BufferedInputStream(
        new FileInputStream("data.bin")
    )
);
```

---

#### Facade
Provides a simplified interface to a complex subsystem. Instead of exposing all the moving parts of a service (database, cache, external API calls), you create a facade that presents a clean, simple API to callers.

---

### Behavioral Patterns — how objects communicate

#### Observer
One object (the subject) maintains a list of dependents (observers) and notifies them automatically when its state changes. Spring's `ApplicationEvent` and `@EventListener` mechanism is built on this pattern.

---

#### Strategy
Defines a family of algorithms, encapsulates each one, and makes them interchangeable. Useful when you have a behavior that can vary — like different sorting algorithms, different pricing strategies, or different export formats.

```java
public interface ExportStrategy {
    void export(List<Record> data);
}

public class CsvExport implements ExportStrategy { ... }
public class ExcelExport implements ExportStrategy { ... }
public class PdfExport implements ExportStrategy { ... }

// The context uses whichever strategy is injected
public class ReportExporter {
    private ExportStrategy strategy;

    public ReportExporter(ExportStrategy strategy) {
        this.strategy = strategy;
    }

    public void export(List<Record> data) {
        strategy.export(data);
    }
}
```

---

#### Template Method
Defines the skeleton of an algorithm in a base class, letting subclasses fill in specific steps without changing the overall structure.

Spring Batch's `ItemProcessor` interface is a good example — the framework defines the overall job execution flow, and you implement the specific processing logic.

---

## Architecture vs. Design Patterns — What's the Difference?

It's easy to mix these up:

| | Architecture Patterns | Design Patterns |
|---|---|---|
| **Scope** | Entire system | Individual classes/objects |
| **Purpose** | How the system is organized | How code is structured internally |
| **Examples** | Microservices, Layered, MVC | Singleton, Factory, Observer |
| **Decisions made at** | Early system design | During implementation |

In practice, you use both. Your system might follow a layered architecture with Spring MVC, and within that, you'd use patterns like Repository, Factory, and Observer in your code.

---

## Applying This to the Project

In our project, we're looking at:

- **Layered architecture** as the main structural approach (Controller → Service → Repository)
- **MVC** via Spring MVC for the web layer
- **ETL pattern** for batch processing flows (covered in detail in the Spring Batch document)
- **Strategy pattern** potentially useful for switching between different processing modes
- **Template Method** is implicitly used when implementing Spring Batch's `ItemReader`, `ItemProcessor`, and `ItemWriter` interfaces

---

## References

- *Design Patterns: Elements of Reusable Object-Oriented Software* — Gang of Four (Gamma, Helm, Johnson, Vlissides)
- Spring Framework Documentation — https://docs.spring.io/spring-framework/reference/
- Refactoring Guru — https://refactoring.guru/design-patterns (very visual, highly recommended)
- Martin Fowler's *Patterns of Enterprise Application Architecture*
