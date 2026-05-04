package com.example.demo.patterns.strategy;

import org.springframework.stereotype.Component;

/**
 * Strategy B: Grade by age + salary combination
 */
@Component("experienceScale")
public class ExperienceSalaryScaleStrategy implements SalaryGradeStrategy {

    @Override
    public String calculateGrade(double salary) {
        // Higher thresholds — for a more competitive market
        if (salary < 50000)      return "Junior";
        else if (salary < 65000) return "Mid";
        else if (salary < 75000) return "Senior";
        else                     return "Lead";
    }
}
