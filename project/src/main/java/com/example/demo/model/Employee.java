package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Employee Entity - used by both JPA and Spring Batch
 *
 * Design Pattern: Builder (via Lombok @Builder)
 * Architecture: Layered - this is the Domain/Model layer
 */
@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    private Integer age;

    private Double salary;

    @Column(name = "department")
    private String department; // assigned during processing (Transform step)

    @Column(name = "salary_grade")
    private String salaryGrade; // calculated during processing

    // -----------------------------------------------
    // Example of Builder pattern usage (without Lombok):
    //
    // Employee emp = new Employee.Builder()
    //     .name("Ahmed")
    //     .email("ahmed@example.com")
    //     .salary(50000.0)
    //     .build();
    // -----------------------------------------------
}
