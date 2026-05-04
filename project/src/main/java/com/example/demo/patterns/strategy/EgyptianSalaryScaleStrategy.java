package com.example.demo.patterns.strategy;

import org.springframework.stereotype.Component;

/**
 * Strategy A: Grade by salary range (Egyptian market scale)
 */
@Component("egyptianScale")
public class EgyptianSalaryScaleStrategy implements SalaryGradeStrategy {

    @Override
    public String calculateGrade(double salary) {
        if (salary < 45000)      return "Junior";
        else if (salary < 60000) return "Mid";
        else if (salary < 70000) return "Senior";
        else                     return "Lead";
    }
}
