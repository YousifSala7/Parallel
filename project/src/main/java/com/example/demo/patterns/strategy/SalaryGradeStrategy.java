package com.example.demo.patterns.strategy;

/**
 * ============================================================
 * DESIGN PATTERN: Strategy
 * ============================================================
 * Intent: Define a family of algorithms, encapsulate each one,
 * and make them interchangeable. The algorithm varies independently
 * from clients that use it.
 *
 * Real Use Here:
 * Different ways to calculate an employee's salary grade.
 * We can swap the strategy without changing the service code.
 * ============================================================
 */
public interface SalaryGradeStrategy {

    /**
     * Calculates the salary grade for a given salary.
     * @param salary the employee's annual salary
     * @return grade string: "Junior", "Mid", "Senior", "Lead"
     */
    String calculateGrade(double salary);
}
