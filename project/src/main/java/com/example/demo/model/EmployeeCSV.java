package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Raw CSV row — used by the ItemReader before transformation.
 * Fields match the CSV column headers exactly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeCSV {
    private String id;
    private String name;
    private String email;
    private String age;
    private String salary;
}
