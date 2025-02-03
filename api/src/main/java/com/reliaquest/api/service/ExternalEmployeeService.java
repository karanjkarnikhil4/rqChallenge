package com.reliaquest.api.service;

import com.reliaquest.api.exception.ExternalServiceException;
import com.reliaquest.api.model.*;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class ExternalEmployeeService {

    private final RestTemplate restTemplate;
    private final String externalApiUrl;

    public ExternalEmployeeService(@Value("${external.api.url}") String externalApiUrl, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.externalApiUrl = externalApiUrl;
    }

    public static final ParameterizedTypeReference<Response<List<Employee>>> EMPLOYEE_LIST_TYPE =
            new ParameterizedTypeReference<>() {};
    public static final ParameterizedTypeReference<Response<Employee>> EMPLOYEE_TYPE =
            new ParameterizedTypeReference<>() {};
    public static final ParameterizedTypeReference<Response<Boolean>> BOOLEAN_TYPE =
            new ParameterizedTypeReference<>() {};

    public List<Employee> fetchEmployees() {
        log.debug("Fetching all employees from {}", externalApiUrl);
        return makeApiCall(externalApiUrl, HttpMethod.GET, null, EMPLOYEE_LIST_TYPE)
                .map(response -> {
                    log.info("Fetched {} employees", response.size());
                    return response;
                })
                .orElseThrow(() ->
                        new ExternalServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "Fetching employees failed"));
    }

    public Optional<Employee> fetchEmployeeById(String id) {
        UUID uuid = UUID.fromString(id);
        String url = externalApiUrl + "/" + uuid;
        log.debug("Fetching employee by ID: {}", uuid);

        return makeApiCall(url, HttpMethod.GET, null, EMPLOYEE_TYPE);
    }

    public ResponseEntity<Integer> getHighestSalary() {
        List<Employee> employees = fetchEmployees();
        return employees.stream()
                .max(Comparator.comparingInt(Employee::getSalary))
                .map(Employee::getSalary)
                .map(salary -> {
                    log.info("Highest salary found: {}", salary);
                    return ResponseEntity.ok(salary);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        List<String> topTenEmployees = fetchEmployees().stream()
                .sorted(Comparator.comparingInt(Employee::getSalary).reversed())
                .limit(10)
                .map(Employee::getName)
                .toList();

        if (topTenEmployees.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        log.info("Top 10 highest earning employees fetched successfully");
        return ResponseEntity.ok(topTenEmployees);
    }

    public Employee createEmployee(CreateEmployeeRequest employeeInput) {
        log.debug("Creating new employee: {}", employeeInput);

        HttpEntity<CreateEmployeeRequest> requestEntity = createRequestEntity(employeeInput);

        return makeApiCall(externalApiUrl, HttpMethod.POST, requestEntity, EMPLOYEE_TYPE)
                .map(employee -> {
                    log.info("Employee created successfully: {}", employee.getName());
                    return employee;
                })
                .orElseThrow(() ->
                        new ExternalServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create employee"));
    }

    public ResponseEntity<String> deleteEmployeeById(String id) {
        UUID uuid = UUID.fromString(id);
        Optional<Employee> findEmployee = fetchEmployeeById(uuid.toString());

        if (findEmployee.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Employee employee = findEmployee.get();

        HttpEntity<DeleteEmployeeRequest> requestEntity =
                createRequestEntity(new DeleteEmployeeRequest(employee.getName()));

        return makeApiCall(externalApiUrl, HttpMethod.DELETE, requestEntity, BOOLEAN_TYPE)
                .map(response -> {
                    if (response) {
                        log.info("Employee {} deleted successfully", employee.getName());
                        return ResponseEntity.ok("Employee " + employee.getName() + " deleted successfully.");
                    } else {
                        log.error("Failed to delete Employee {}", employee.getName());
                        throw new ExternalServiceException(
                                HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete employee.");
                    }
                })
                .orElseThrow(
                        () -> new ExternalServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
    }

    private <T> Optional<T> makeApiCall(
            String url,
            HttpMethod method,
            HttpEntity<?> requestEntity,
            ParameterizedTypeReference<Response<T>> responseType) {
        try {
            ResponseEntity<Response<T>> response = restTemplate.exchange(url, method, requestEntity, responseType);
            return Optional.ofNullable(response.getBody()).map(Response::data);
        } catch (HttpStatusCodeException e) {
            throw new ExternalServiceException(e.getStatusCode(), e.getMessage());
        }
    }

    private <T> HttpEntity<T> createRequestEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
