# Multithreading in Java

## What Is Multithreading?

When a program runs, it normally executes one instruction after another — that's a single thread of execution. Multithreading is about running multiple threads at the same time (or at least, giving the illusion of doing so) within the same process.

The main motivation is simple: modern machines have multiple CPU cores that can do work in parallel. If your program only uses one thread, those extra cores are sitting idle. Multithreading lets you take advantage of that.

But it's not without complications. Shared state, synchronization, and race conditions are real problems that come with it — which is why understanding the fundamentals before just throwing threads at a problem is important.

---

## Processes vs. Threads

Before diving in, a quick distinction:

- A **process** is an independent program running in its own memory space. Processes don't share memory.
- A **thread** lives inside a process and shares the same memory with other threads in that process.

Because threads share memory, communication between them is cheap — but it also means they can interfere with each other if not managed carefully.

---

## Creating Threads in Java

### Option 1: Extending `Thread`

```java
public class MyTask extends Thread {
    @Override
    public void run() {
        System.out.println("Running on thread: " + Thread.currentThread().getName());
    }
}

// Usage
MyTask t = new MyTask();
t.start(); // don't call run() directly — that runs on the current thread
```

### Option 2: Implementing `Runnable`

Usually preferred because Java doesn't support multiple inheritance — if your class already extends something, you can't also extend `Thread`.

```java
public class MyTask implements Runnable {
    @Override
    public void run() {
        System.out.println("Task running on: " + Thread.currentThread().getName());
    }
}

Thread t = new Thread(new MyTask());
t.start();
```

### Option 3: Lambda (most common in modern Java)

```java
Thread t = new Thread(() -> {
    System.out.println("Running: " + Thread.currentThread().getName());
});
t.start();
```

---

## Thread Lifecycle

A Java thread goes through the following states:

```
NEW → RUNNABLE → (BLOCKED / WAITING / TIMED_WAITING) → TERMINATED
```

- **NEW** — thread created but not started yet
- **RUNNABLE** — running or ready to run
- **BLOCKED** — waiting to acquire a lock
- **WAITING** — waiting indefinitely for another thread (e.g., `wait()`, `join()`)
- **TIMED_WAITING** — waiting for a specified time (e.g., `sleep(1000)`)
- **TERMINATED** — finished execution

---

## Thread Pools and ExecutorService

Creating a new thread for every task is expensive — threads take time and memory to create. A **thread pool** keeps a set of pre-created threads alive and reuses them for incoming tasks.

Java's `ExecutorService` makes this clean:

```java
ExecutorService executor = Executors.newFixedThreadPool(4); // 4 threads

for (int i = 0; i < 10; i++) {
    int taskId = i;
    executor.submit(() -> {
        System.out.println("Task " + taskId + " on " + Thread.currentThread().getName());
    });
}

executor.shutdown(); // waits for tasks to finish, then shuts down
```

Common pool types:
| Factory Method | Behavior |
|---|---|
| `newFixedThreadPool(n)` | Fixed number of threads |
| `newCachedThreadPool()` | Creates threads as needed, reuses idle ones |
| `newSingleThreadExecutor()` | Single thread, tasks execute sequentially |
| `newScheduledThreadPool(n)` | For scheduled/repeated tasks |

---

## Synchronization and Shared State

This is where most multithreading bugs live. When multiple threads read and write the same variable, you get a **race condition** — the result depends on which thread gets there first, which is unpredictable.

### The Problem

```java
public class Counter {
    private int count = 0;

    public void increment() {
        count++; // NOT atomic — this is actually read → add → write
    }
}
```

If two threads call `increment()` at the same time, they might both read `count = 5`, both add 1, and both write back `6` — so instead of `7`, you get `6`.

### Fix 1: `synchronized`

```java
public synchronized void increment() {
    count++;
}
```

Only one thread can execute a `synchronized` method at a time. Simple, but can become a bottleneck if many threads are competing.

### Fix 2: `AtomicInteger`

For simple numeric operations, Java's `java.util.concurrent.atomic` package provides atomic classes that handle this without explicit locking:

```java
private AtomicInteger count = new AtomicInteger(0);

public void increment() {
    count.incrementAndGet();
}
```

### Fix 3: Locks

More flexible than `synchronized`:

```java
private final ReentrantLock lock = new ReentrantLock();

public void increment() {
    lock.lock();
    try {
        count++;
    } finally {
        lock.unlock(); // always release in finally
    }
}
```

---

## The `volatile` Keyword

When a thread reads a variable, it might read from its local CPU cache rather than main memory. This means changes made by one thread might not be visible to another.

Marking a variable `volatile` tells the JVM to always read from and write to main memory:

```java
private volatile boolean running = true;

public void stop() {
    running = false;
}

public void run() {
    while (running) {
        // ...
    }
}
```

Note: `volatile` ensures visibility, but not atomicity. For compound operations (like `count++`), you still need synchronization or atomic classes.

---

## Common Problems

### Deadlock

Two threads are each waiting for a lock held by the other — neither can proceed.

```
Thread A holds Lock 1, waiting for Lock 2
Thread B holds Lock 2, waiting for Lock 1
→ Deadlock
```

Prevention: always acquire locks in the same order across all threads.

### Starvation

A thread is perpetually denied access to a resource because other threads keep getting priority. Low-priority threads can starve if high-priority threads never yield.

### Livelock

Similar to deadlock, but instead of being frozen, both threads keep responding to each other without making progress — like two people in a hallway both stepping aside in the same direction repeatedly.

---

## Multithreading with Large Files

One practical use case is processing large files. Instead of reading the file sequentially in one thread, you can split it into chunks and process them in parallel.

```java
ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
List<Future<?>> futures = new ArrayList<>();

long fileSize = file.length();
long chunkSize = fileSize / numThreads;

for (int i = 0; i < numThreads; i++) {
    long start = i * chunkSize;
    long end = (i == numThreads - 1) ? fileSize : start + chunkSize;

    futures.add(executor.submit(() -> processChunk(filePath, start, end)));
}

// Wait for all chunks to finish
for (Future<?> f : futures) {
    f.get();
}

executor.shutdown();
```

This approach is useful for log parsing, CSV processing, or any large file where each record is independent.

---

## Multithreading with Databases

When multiple threads are hitting a database simultaneously, a few things matter:

### Connection Pooling

Don't create a new connection per thread — use a connection pool like **HikariCP** (which Spring Boot uses by default). The pool maintains a set of reusable connections and hands them out to threads as needed.

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
```

### Thread Safety in Repositories

Spring's `@Repository` beans are singletons, but that's fine because they don't hold state — each method call creates its own local variables. The connection comes from the pool and is returned when the transaction completes.

### Transaction Management

In a multithreaded context, be careful with `@Transactional`. Each thread should have its own transaction — don't share transaction contexts across threads. Spring handles this via `ThreadLocal` storage, so each thread gets its own transaction automatically.

---

## `CompletableFuture` (Modern Java Async)

Java 8 introduced `CompletableFuture` as a cleaner alternative to raw threads for async operations:

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    // This runs in a separate thread
    return fetchDataFromApi();
});

future.thenApply(data -> processData(data))
      .thenAccept(result -> saveToDatabase(result))
      .exceptionally(ex -> {
          System.err.println("Error: " + ex.getMessage());
          return null;
      });
```

You can also combine multiple futures:

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> fetchUsers());
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> fetchOrders());

CompletableFuture.allOf(f1, f2).join(); // wait for both
```

---

## Quick Summary Table

| Concept | Purpose |
|---|---|
| `Thread` / `Runnable` | Basic thread creation |
| `ExecutorService` | Thread pool management |
| `synchronized` | Mutual exclusion on methods/blocks |
| `AtomicInteger` etc. | Lock-free atomic operations |
| `volatile` | Visibility guarantee across threads |
| `ReentrantLock` | More flexible locking |
| `CompletableFuture` | Composable async tasks |
| `HikariCP` | Database connection pooling |

---

## References

- Java Concurrency in Practice — Brian Goetz (the go-to book on this topic)
- Java SE Documentation: `java.util.concurrent` — https://docs.oracle.com/en/java/
- Baeldung — https://www.baeldung.com/java-concurrency (great practical tutorials)
