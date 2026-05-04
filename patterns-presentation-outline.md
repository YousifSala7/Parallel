# Presentation Outline: Architecture & Design Patterns

> This document served as the working outline for the Architecture Pattern & Design Pattern presentation.

---

## Slide Structure

---

### Slide 1 — Title

**Architecture & Design Patterns**
*A Practical Overview*

---

### Slide 2 — Agenda

1. Why Patterns Matter
2. Architecture Patterns (the big picture)
3. Design Patterns (the code level)
4. Real Examples from Our Project
5. Summary

---

### Slide 3 — Why Do Patterns Matter?

- Software problems tend to repeat themselves across different projects
- Patterns are named, proven solutions to recurring problems
- They give teams a **shared vocabulary** — saying "use the Strategy pattern here" is faster than explaining the whole concept
- They help you avoid reinventing the wheel (badly)

Key quote to anchor the slide:
> "Each pattern describes a problem which occurs over and over again in our environment, and then describes the core of the solution to that problem." — Christopher Alexander

---

### Slide 4 — Architecture vs. Design Patterns

| | Architecture Patterns | Design Patterns |
|---|---|---|
| Scope | Entire system | Classes & objects |
| Purpose | System organization | Code structure |
| Examples | Layered, MVC, Microservices | Singleton, Factory, Observer |
| When decided | Early system design | During implementation |

---

### Slide 5 — Architecture Patterns (1/2)

**Layered (N-Tier)**
- Divide the app into layers: Presentation → Business → Data
- Each layer only talks to the one below it
- Most common enterprise architecture — Spring MVC is built around this

**MVC (Model-View-Controller)**
- Model: data + business logic
- View: what the user sees
- Controller: handles requests, coordinates model and view
- Used natively in Spring MVC (`@Controller`, `@Service`, `@Repository`)

---

### Slide 6 — Architecture Patterns (2/2)

**Microservices**
- Break the system into small, independent services
- Each service does one thing, deployed independently
- Adds operational complexity but improves scalability and team autonomy

**Event-Driven**
- Components communicate by firing and listening to events
- Loose coupling between services
- Tools: Kafka, RabbitMQ, Spring Events

**ETL (Extract → Transform → Load)**
- Core pattern for data pipelines and batch processing
- Implemented via Spring Batch in our project

---

### Slide 7 — Design Patterns Overview

Organized into three families:

- **Creational** — how objects are created
  - Singleton, Factory Method, Builder, Abstract Factory, Prototype

- **Structural** — how objects are composed
  - Adapter, Decorator, Facade, Proxy, Composite

- **Behavioral** — how objects communicate
  - Observer, Strategy, Template Method, Command, Iterator

---

### Slide 8 — Creational: Singleton & Factory

**Singleton**
- One instance per application
- Spring beans are singletons by default
- Use case: configuration managers, connection pools

**Factory Method**
- Delegate object creation to a factory
- Decouples the caller from the specific class being created
- Use case: notification types (Email, SMS, Push)

---

### Slide 9 — Creational: Builder

- Useful for objects with many optional parameters
- Readable, chainable construction
- Lombok's `@Builder` generates this for you automatically

```java
User user = new User.Builder("email@example.com")
    .name("Ahmed")
    .role("admin")
    .build();
```

---

### Slide 10 — Structural: Adapter & Decorator

**Adapter**
- Makes two incompatible interfaces work together
- Like a plug adapter in the real world
- Use case: integrating a legacy API into a modern system

**Decorator**
- Adds behavior to an object without changing its class
- Java I/O streams are the classic example:
  `new BufferedInputStream(new FileInputStream(...))`

---

### Slide 11 — Behavioral: Observer & Strategy

**Observer**
- One object notifies many when something changes
- Spring's `ApplicationEvent` / `@EventListener` is built on this
- Use case: triggering side effects when an order is placed

**Strategy**
- Encapsulate a family of algorithms; swap them at runtime
- Use case: multiple export formats (CSV, Excel, PDF) with a shared interface

---

### Slide 12 — How These Apply to Our Project

| Where in the Project | Pattern Used |
|---|---|
| Spring MVC structure | Layered Architecture + MVC |
| Batch processing pipeline | ETL Architecture + Template Method |
| Notification sending | Factory Method + Strategy |
| Data transformation | Decorator / Builder |
| Event handling between components | Observer |

---

### Slide 13 — Summary

- Architecture patterns define **how the system is organized**
- Design patterns define **how the code is structured**
- Both exist to solve recurring problems with proven solutions
- Spring Framework itself is built on many of these patterns
- Understanding them makes reading, writing, and reviewing code much easier

---

### Slide 14 — Q&A

Open for questions.

---

## Talking Points / Notes

- Don't rush through the pattern names — give one concrete example for each
- The comparison table on slide 4 is usually what trips people up, spend a moment there
- For the project application slide, walk through actual code if there's time
- Refactoring Guru (refactoring.guru) is a great visual reference to recommend to the audience
