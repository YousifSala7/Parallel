package com.example.demo.multithreading;

import com.example.demo.model.Employee;
import com.example.demo.model.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * ============================================================
 * MULTITHREADING: Parallel Employee Processing
 * ============================================================
 * Demonstrates:
 *  1. ExecutorService & Thread Pool
 *  2. Callable + Future for results
 *  3. CompletableFuture for async chaining
 *  4. AtomicInteger for thread-safe counting
 *  5. Partitioning a dataset and processing each part in parallel
 *
 * Real Use Case:
 * After importing employees via batch, run parallel analytics
 * (salary stats, department grouping) on the dataset.
 * ============================================================
 */
@Service
public class ParallelEmployeeAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(ParallelEmployeeAnalyticsService.class);

    private final EmployeeRepository employeeRepository;

    // Thread pool — size = number of available CPU cores
    private final ExecutorService threadPool = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    );

    // Thread-safe counter to track processed records
    private final AtomicInteger processedCount = new AtomicInteger(0);

    public ParallelEmployeeAnalyticsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // -------------------------------------------------------
    // METHOD 1: Parallel Processing with Futures
    // Split employees into chunks, process each chunk in parallel
    // -------------------------------------------------------
    public AnalyticsResult runParallelAnalytics() throws InterruptedException, ExecutionException {
        List<Employee> allEmployees = employeeRepository.findAll();

        if (allEmployees.isEmpty()) {
            return new AnalyticsResult(0, 0.0, 0.0, Map.of());
        }

        log.info("[Multithreading] Starting parallel analytics on {} employees using {} threads",
            allEmployees.size(), Runtime.getRuntime().availableProcessors());

        // Partition: split list into chunks of ~5 employees each
        int chunkSize = Math.max(1, allEmployees.size() / 4);
        List<List<Employee>> partitions = partition(allEmployees, chunkSize);

        log.info("[Multithreading] Split into {} partitions of ~{} each",
            partitions.size(), chunkSize);

        // Submit each partition as a separate task to the thread pool
        List<Future<PartitionResult>> futures = new ArrayList<>();

        for (int i = 0; i < partitions.size(); i++) {
            final int partitionIndex = i;
            final List<Employee> partition = partitions.get(i);

            // Callable returns a result (unlike Runnable)
            Callable<PartitionResult> task = () -> {
                String threadName = Thread.currentThread().getName();
                log.info("[Thread: {}] Processing partition {} ({} employees)",
                    threadName, partitionIndex, partition.size());

                double totalSalary = partition.stream()
                    .mapToDouble(Employee::getSalary)
                    .sum();

                double maxSalary = partition.stream()
                    .mapToDouble(Employee::getSalary)
                    .max().orElse(0);

                processedCount.addAndGet(partition.size()); // thread-safe increment
                return new PartitionResult(partition.size(), totalSalary, maxSalary);
            };

            futures.add(threadPool.submit(task));
        }

        // Collect results from all threads
        int totalCount = 0;
        double totalSalary = 0;
        double overallMax = 0;

        for (Future<PartitionResult> future : futures) {
            PartitionResult result = future.get(); // blocks until this partition finishes
            totalCount  += result.count();
            totalSalary += result.totalSalary();
            overallMax   = Math.max(overallMax, result.maxSalary());
        }

        double avgSalary = totalCount > 0 ? totalSalary / totalCount : 0;

        // Department breakdown (done on main thread after parallel work)
        Map<String, Long> byDepartment = allEmployees.stream()
            .collect(Collectors.groupingBy(
                e -> e.getDepartment() != null ? e.getDepartment() : "Unknown",
                Collectors.counting()
            ));

        log.info("[Multithreading] Done. Total processed: {}", processedCount.get());
        return new AnalyticsResult(totalCount, avgSalary, overallMax, byDepartment);
    }

    // -------------------------------------------------------
    // METHOD 2: CompletableFuture for async, non-blocking calls
    // -------------------------------------------------------
    public CompletableFuture<Map<String, Long>> getGradeDistributionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("[CompletableFuture] Fetching grade distribution on thread: {}",
                Thread.currentThread().getName());

            return employeeRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                    e -> e.getSalaryGrade() != null ? e.getSalaryGrade() : "Unknown",
                    Collectors.counting()
                ));
        }, threadPool);
    }

    // -------------------------------------------------------
    // METHOD 3: Run two async tasks in parallel and combine
    // -------------------------------------------------------
    public CompletableFuture<String> getCombinedSummaryAsync() {
        CompletableFuture<Double> avgSalaryFuture = CompletableFuture.supplyAsync(() -> {
            log.info("[CF Thread 1] Calculating avg salary on: {}", Thread.currentThread().getName());
            return employeeRepository.findAll().stream()
                .mapToDouble(Employee::getSalary)
                .average().orElse(0.0);
        }, threadPool);

        CompletableFuture<Long> totalCountFuture = CompletableFuture.supplyAsync(() -> {
            log.info("[CF Thread 2] Counting employees on: {}", Thread.currentThread().getName());
            return employeeRepository.count();
        }, threadPool);

        // Combine both results when both are ready
        return avgSalaryFuture.thenCombine(totalCountFuture, (avg, count) ->
            String.format("Total: %d employees | Average Salary: %.2f EGP", count, avg)
        );
    }

    // --- Utilities ---

    /** Splits a list into sub-lists of at most chunkSize elements */
    private <T> List<List<T>> partition(List<T> list, int chunkSize) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            result.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return result;
    }

    // --- Inner records for result types ---
    public record PartitionResult(int count, double totalSalary, double maxSalary) {}

    public record AnalyticsResult(
        int totalEmployees,
        double averageSalary,
        double maxSalary,
        Map<String, Long> byDepartment
    ) {}
}
