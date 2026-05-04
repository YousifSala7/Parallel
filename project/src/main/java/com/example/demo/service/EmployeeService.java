package com.example.demo.service;

import com.example.demo.model.Employee;
import com.example.demo.model.EmployeeRepository;
import com.example.demo.patterns.factory.NotificationFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================
 * Service Layer — part of Layered Architecture
 * ============================================================
 * Controller → Service → Repository
 *
 * This layer contains business logic.
 * It does NOT deal with HTTP requests/responses (that's the controller's job)
 * It does NOT deal with SQL/database directly (that's the repository's job)
 * ============================================================
 */
@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final NotificationFactory notificationFactory;

    public EmployeeService(EmployeeRepository employeeRepository,
                           NotificationFactory notificationFactory) {
        this.employeeRepository = employeeRepository;
        this.notificationFactory = notificationFactory;
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public List<Employee> getHighEarners(double minSalary) {
        return employeeRepository.findHighEarners(minSalary);
    }

    public List<Employee> getByDepartment(String department) {
        return employeeRepository.findByDepartment(department);
    }

    /** Returns count of employees per salary grade */
    public Map<String, Long> getSalaryGradeDistribution() {
        return employeeRepository.findAll().stream()
            .collect(Collectors.groupingBy(
                e -> e.getSalaryGrade() != null ? e.getSalaryGrade() : "Unknown",
                Collectors.counting()
            ));
    }

    /** Returns count of employees per department */
    public Map<String, Long> getDepartmentDistribution() {
        return employeeRepository.findAll().stream()
            .collect(Collectors.groupingBy(
                e -> e.getDepartment() != null ? e.getDepartment() : "Unknown",
                Collectors.counting()
            ));
    }

    /** Demo of Factory Pattern: send notification using chosen type */
    public String sendTestNotification(String type, String recipient) {
        notificationFactory.create(type).send(recipient, "Hello from the Architecture Demo!");
        return "Notification sent via: " + type;
    }

    public long getTotalCount() {
        return employeeRepository.count();
    }
}
