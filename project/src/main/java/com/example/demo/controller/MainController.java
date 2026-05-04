package com.example.demo.controller;

import com.example.demo.batch.BatchConfig;
import com.example.demo.model.Employee;
import com.example.demo.multithreading.ParallelEmployeeAnalyticsService;
import com.example.demo.patterns.singleton.AppConfigManager;
import com.example.demo.service.EmployeeService;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ============================================================
 * REST Controller — Presentation Layer
 * ============================================================
 * This is the top layer of the Layered Architecture.
 * It handles HTTP requests and delegates to the Service layer.
 * It knows nothing about the database or batch internals.
 *
 * Available Endpoints:
 *   GET  /api/employees              → list all employees
 *   GET  /api/employees/high-earners → employees above salary threshold
 *   GET  /api/employees/department/{dept} → by department
 *   GET  /api/stats/grades           → salary grade distribution
 *   GET  /api/stats/departments      → department distribution
 *   POST /api/batch/run              → trigger batch import job
 *   GET  /api/analytics/parallel     → run multithreaded analytics
 *   GET  /api/analytics/async-summary → CompletableFuture demo
 *   POST /api/notify                 → Factory Pattern demo
 *   GET  /api/config                 → Singleton Pattern demo
 * ============================================================
 */
@RestController
@RequestMapping("/api")
public class MainController {

    private final EmployeeService employeeService;
    private final JobLauncher jobLauncher;
    private final Job importEmployeesJob;
    private final ParallelEmployeeAnalyticsService analyticsService;
    private final AppConfigManager configManager;

    public MainController(EmployeeService employeeService,
                          JobLauncher jobLauncher,
                          Job importEmployeesJob,
                          ParallelEmployeeAnalyticsService analyticsService,
                          AppConfigManager configManager) {
        this.employeeService = employeeService;
        this.jobLauncher = jobLauncher;
        this.importEmployeesJob = importEmployeesJob;
        this.analyticsService = analyticsService;
        this.configManager = configManager;
    }

    // -------------------------------------------------------
    // Employee CRUD endpoints
    // -------------------------------------------------------

    @GetMapping("/employees")
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/employees/high-earners")
    public ResponseEntity<List<Employee>> getHighEarners(
            @RequestParam(defaultValue = "60000") double minSalary) {
        return ResponseEntity.ok(employeeService.getHighEarners(minSalary));
    }

    @GetMapping("/employees/department/{dept}")
    public ResponseEntity<List<Employee>> getByDepartment(@PathVariable String dept) {
        return ResponseEntity.ok(employeeService.getByDepartment(dept));
    }

    // -------------------------------------------------------
    // Statistics
    // -------------------------------------------------------

    @GetMapping("/stats/grades")
    public ResponseEntity<Map<String, Long>> getGradeStats() {
        return ResponseEntity.ok(employeeService.getSalaryGradeDistribution());
    }

    @GetMapping("/stats/departments")
    public ResponseEntity<Map<String, Long>> getDeptStats() {
        return ResponseEntity.ok(employeeService.getDepartmentDistribution());
    }

    // -------------------------------------------------------
    // Spring Batch — ETL job trigger
    // -------------------------------------------------------

    /**
     * POST /api/batch/run
     * Triggers the Spring Batch ETL job:
     *   CSV → Validate/Transform → H2 Database
     */
    @PostMapping("/batch/run")
    public ResponseEntity<Map<String, Object>> runBatchJob() {
        Map<String, Object> result = new HashMap<>();
        try {
            JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis()) // unique each run
                .toJobParameters();

            JobExecution execution = jobLauncher.run(importEmployeesJob, params);

            result.put("jobName", execution.getJobInstance().getJobName());
            result.put("status", execution.getStatus().toString());
            result.put("startTime", execution.getStartTime());
            result.put("endTime", execution.getEndTime());

            // Collect step stats
            for (StepExecution step : execution.getStepExecutions()) {
                result.put("recordsRead", step.getReadCount());
                result.put("recordsWritten", step.getWriteCount());
                result.put("recordsSkipped", step.getSkipCount());
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // -------------------------------------------------------
    // Multithreading — parallel analytics
    // -------------------------------------------------------

    /**
     * GET /api/analytics/parallel
     * Splits employees across threads, processes in parallel,
     * then aggregates results.
     */
    @GetMapping("/analytics/parallel")
    public ResponseEntity<Map<String, Object>> runParallelAnalytics() {
        Map<String, Object> result = new HashMap<>();
        try {
            var analytics = analyticsService.runParallelAnalytics();

            result.put("totalEmployees", analytics.totalEmployees());
            result.put("averageSalary", String.format("%.2f", analytics.averageSalary()));
            result.put("maxSalary", analytics.maxSalary());
            result.put("byDepartment", analytics.byDepartment());
            result.put("processingMode", "parallel-multithreaded");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * GET /api/analytics/async-summary
     * Two CompletableFuture tasks run in parallel, result combined.
     */
    @GetMapping("/analytics/async-summary")
    public CompletableFuture<ResponseEntity<String>> getAsyncSummary() {
        return analyticsService.getCombinedSummaryAsync()
            .thenApply(ResponseEntity::ok);
    }

    // -------------------------------------------------------
    // Design Patterns demo endpoints
    // -------------------------------------------------------

    /**
     * POST /api/notify?type=email&recipient=test@example.com
     * Demonstrates Factory Pattern: pick the right sender at runtime
     */
    @PostMapping("/notify")
    public ResponseEntity<String> sendNotification(
            @RequestParam(defaultValue = "email") String type,
            @RequestParam(defaultValue = "demo@example.com") String recipient) {
        return ResponseEntity.ok(employeeService.sendTestNotification(type, recipient));
    }

    /**
     * GET /api/config
     * Shows Singleton Pattern: one shared config instance
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        return ResponseEntity.ok(configManager.getAll());
    }

    /**
     * GET /api/health
     * Quick status check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("totalEmployees", employeeService.getTotalCount());
        status.put("message", "Architecture Demo is running!");
        return ResponseEntity.ok(status);
    }
}
