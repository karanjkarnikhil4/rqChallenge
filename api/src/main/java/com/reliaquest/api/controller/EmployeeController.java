package com.reliaquest.api.controller;

import com.reliaquest.api.exception.BadRequestException;
import com.reliaquest.api.model.*;
import com.reliaquest.api.service.ExternalEmployeeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("api/v1/employee")
@RequiredArgsConstructor
public class EmployeeController implements IEmployeeController<Employee, CreateEmployeeRequest> {

    private final ExternalEmployeeService externalEmployeeService;

    @Override
    @GetMapping()
    public ResponseEntity<List<Employee>> getAllEmployees() {
        log.info("Handling GET request for all employees");
        return ResponseEntity.ok(externalEmployeeService.fetchEmployees());
    }

    @Override
    @GetMapping("/search")
    public ResponseEntity<List<Employee>> getEmployeesByNameSearch(@RequestParam(value = "name") String searchString) {
        log.info("Handling search request for employees with name containing '{}'", searchString);
        List<Employee> filteredEmployees = externalEmployeeService.fetchEmployees().stream()
                .filter(employee -> employee.getName().toLowerCase().contains(searchString.toLowerCase()))
                .toList();
        if (filteredEmployees.isEmpty()) {
            log.info("No matching employees for {}", searchString);
            return ResponseEntity.notFound().build();
        }
        log.info("Found {} matching employees", filteredEmployees.size());
        return ResponseEntity.ok(filteredEmployees);
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable("id") String id) {
        log.info("Handling GET request for employee ID: {}", id);
        try {
            return externalEmployeeService
                    .fetchEmployeeById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format: {}", id);
            throw new BadRequestException("Invalid UUID format: " + id);
        }
    }

    @Override
    @GetMapping("/highest-salary")
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        log.info("Handling GET request for highest salary");
        return externalEmployeeService.getHighestSalary();
    }

    @Override
    @GetMapping("/top-ten-highest-earning-employees")
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        log.info("Handling GET request for top 10 highest earning employees");
        return externalEmployeeService.getTopTenHighestEarningEmployeeNames();
    }

    @Override
    @PostMapping
    public ResponseEntity<Employee> createEmployee(@RequestBody CreateEmployeeRequest employeeInput) {
        log.info("Handling POST request to create employee: {}", employeeInput);
        return ResponseEntity.ok(externalEmployeeService.createEmployee(employeeInput));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployeeById(@PathVariable("id") String id) {
        log.info("Handling DELETE request for employee ID: {}", id);
        return externalEmployeeService.deleteEmployeeById(id);
    }
}
