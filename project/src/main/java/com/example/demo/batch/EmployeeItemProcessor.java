package com.example.demo.batch;

import com.example.demo.model.Employee;
import com.example.demo.model.EmployeeCSV;
import com.example.demo.patterns.strategy.SalaryGradeStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * ============================================================
 * Spring Batch: ItemProcessor — the "Transform" step in ETL
 * ============================================================
 * Reads a raw EmployeeCSV, validates it, transforms it into
 * a proper Employee entity ready for the database.
 *
 * Design Patterns used here:
 *  - Strategy: SalaryGradeStrategy injected to calculate grade
 *  - Template Method: Spring Batch defines the job skeleton,
 *    we implement only the process() method
 * ============================================================
 */
@Component
public class EmployeeItemProcessor implements ItemProcessor<EmployeeCSV, Employee> {

    private static final Logger log = LoggerFactory.getLogger(EmployeeItemProcessor.class);

    // Strategy Pattern: injected strategy for salary grade calculation
    private final SalaryGradeStrategy salaryGradeStrategy;

    public EmployeeItemProcessor(@Qualifier("egyptianScale") SalaryGradeStrategy strategy) {
        this.salaryGradeStrategy = strategy;
    }

    @Override
    public Employee process(EmployeeCSV raw) throws Exception {

        // --- Validation (returning null = skip this record) ---
        if (raw.getName() == null || raw.getName().isBlank()) {
            log.warn("Skipping record with missing name: {}", raw);
            return null; // filtered out by Spring Batch
        }

        if (raw.getEmail() == null || raw.getEmail().isBlank()) {
            log.warn("Skipping record with missing email: id={}, name={}", raw.getId(), raw.getName());
            return null; // filtered out — won't reach the writer
        }

        // --- Transformation ---
        double salary = 0;
        try {
            salary = Double.parseDouble(raw.getSalary());
        } catch (NumberFormatException e) {
            log.warn("Invalid salary for {}: '{}'", raw.getName(), raw.getSalary());
            return null;
        }

        int age = 0;
        try {
            age = Integer.parseInt(raw.getAge());
        } catch (NumberFormatException e) {
            age = 0; // default, not a reason to skip
        }

        // Strategy Pattern: calculate grade using injected strategy
        String grade = salaryGradeStrategy.calculateGrade(salary);

        // Assign department based on salary grade (simple rule)
        String department = switch (grade) {
            case "Lead"   -> "Executive";
            case "Senior" -> "Engineering";
            case "Mid"    -> "Operations";
            default       -> "Internship";
        };

        // Builder Pattern: construct the Employee object
        return Employee.builder()
                .name(raw.getName().trim())
                .email(raw.getEmail().trim().toLowerCase())
                .age(age)
                .salary(salary)
                .salaryGrade(grade)
                .department(department)
                .build();
    }
}
