package com.reliaquest.api.service;

import static com.reliaquest.api.service.ExternalEmployeeService.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.reliaquest.api.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
class ExternalEmployeeServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Value("${external.api.url}")
    private String apiUrl;

    @InjectMocks
    private ExternalEmployeeService externalEmployeeService;

    private List<Employee> employees;

    @BeforeEach
    void setUp() {
        employees = List.of(
                new Employee(
                        UUID.fromString("a06ee5d-b7ba-4853-ac0c-abac53243b51"),
                        "Mary Jane",
                        234566,
                        20,
                        "developer",
                        "maryjane.reliaquest.com"),
                new Employee(
                        UUID.fromString("067d73e-ad81-499f-93d0-bfee268e5bc7"),
                        "Peter Parker",
                        458866,
                        20,
                        "developer",
                        "peterparker.reliaquest.com"));
        externalEmployeeService = new ExternalEmployeeService(apiUrl, restTemplate);
    }

    private ResponseEntity<Response<List<Employee>>> mockEmployeeListResponse(List<Employee> employees) {
        Response<List<Employee>> mockResponse = new Response<>(employees, Response.Status.HANDLED, "Success");
        return new ResponseEntity<>(mockResponse, HttpStatus.OK);
    }

    private ResponseEntity<Response<List<Employee>>> mockEmptyEmployeeListResponse() {
        Response<List<Employee>> mockResponse = new Response<>(new ArrayList<>(), Response.Status.HANDLED, "Success");
        return new ResponseEntity<>(mockResponse, HttpStatus.OK);
    }

    private ResponseEntity<Response<Employee>> mockEmployeeResponse(Employee employee) {
        Response<Employee> mockResponse = new Response<>(employee, Response.Status.HANDLED, "Success");
        return new ResponseEntity<>(mockResponse, HttpStatus.OK);
    }

    private ResponseEntity<Response<Employee>> mockEmptyEmployeeResponse() {
        Response<Employee> mockResponse = new Response<>(null, Response.Status.HANDLED, "Success");
        return new ResponseEntity<>(mockResponse, HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<Response<Boolean>> mockDeleteResponse() {
        Response<Boolean> mockResponse = new Response<>(true, Response.Status.HANDLED, "Success");
        return new ResponseEntity<>(mockResponse, HttpStatus.OK);
    }

    @Test
    void fetchEmployees_ShouldReturnEmployeeList() {
        when(restTemplate.exchange(eq(apiUrl), eq(HttpMethod.GET), isNull(), eq(EMPLOYEE_LIST_TYPE)))
                .thenReturn(mockEmployeeListResponse(employees));

        List<Employee> result = externalEmployeeService.fetchEmployees();

        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertEquals("Mary Jane", result.get(0).getName()),
                () -> assertEquals("Peter Parker", result.get(1).getName()));
    }

    @Test
    void fetchEmployeeById_ShouldReturnEmployee() {
        String id = UUID.randomUUID().toString();
        when(restTemplate.exchange(eq(apiUrl + "/" + id), eq(HttpMethod.GET), isNull(), eq(EMPLOYEE_TYPE)))
                .thenReturn(mockEmployeeResponse(employees.get(0)));

        Employee result = externalEmployeeService.fetchEmployeeById(id).orElse(null);

        assertAll(() -> {
            assert result != null;
            assertEquals("Mary Jane", result.getName());
            assertEquals(234566, result.getSalary());
        });
    }

    @Test
    void fetchEmployeeById_employeeNotFound_ShouldReturnNull() {
        String id = UUID.randomUUID().toString();
        when(restTemplate.exchange(eq(apiUrl + "/" + id), eq(HttpMethod.GET), isNull(), eq(EMPLOYEE_TYPE)))
                .thenReturn(mockEmptyEmployeeResponse());

        Employee result = externalEmployeeService.fetchEmployeeById(id).orElse(null);

        assertNull(result);
    }

    @Test
    void getHighestSalary_ShouldReturnHighestSalary() {
        when(restTemplate.exchange(eq(apiUrl), eq(HttpMethod.GET), isNull(), eq(EMPLOYEE_LIST_TYPE)))
                .thenReturn(mockEmployeeListResponse(employees));

        ResponseEntity<Integer> result = externalEmployeeService.getHighestSalary();
        assertEquals(458866, result.getBody());
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_ShouldReturnTop10Names() {
        when(restTemplate.exchange(eq(apiUrl), eq(HttpMethod.GET), isNull(), eq(EMPLOYEE_LIST_TYPE)))
                .thenReturn(mockEmployeeListResponse(employees));

        ResponseEntity<List<String>> result = externalEmployeeService.getTopTenHighestEarningEmployeeNames();

        List<String> employeeNames = Objects.requireNonNull(result.getBody());

        assertAll(
                () -> assertNotNull(result.getBody()),
                () -> assertEquals(2, employeeNames.size()),
                () -> assertEquals("Peter Parker", employeeNames.get(0)));
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_returnsEmptyList_ShouldReturnNotFoundStatus() {
        when(restTemplate.exchange(eq(apiUrl), eq(HttpMethod.GET), isNull(), eq(EMPLOYEE_LIST_TYPE)))
                .thenReturn(mockEmptyEmployeeListResponse());

        ResponseEntity<List<String>> result = externalEmployeeService.getTopTenHighestEarningEmployeeNames();

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void createEmployee_ShouldReturnCreatedEmployee() {
        CreateEmployeeRequest request = new CreateEmployeeRequest("Mary Jane", 234566, 20, "developer");
        when(restTemplate.exchange(eq(apiUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(EMPLOYEE_TYPE)))
                .thenReturn(mockEmployeeResponse(employees.get(0)));

        Employee result = externalEmployeeService.createEmployee(request);

        assertAll(() -> assertEquals("Mary Jane", result.getName()), () -> assertEquals(234566, result.getSalary()));
    }

    @Test
    void deleteEmployeeById_shouldReturnSuccessMessage() {
        String id = UUID.fromString("a06ee5d-b7ba-4853-ac0c-abac53243b51").toString();

        when(restTemplate.exchange(eq(apiUrl + "/" + id), eq(HttpMethod.GET), isNull(), eq(EMPLOYEE_TYPE)))
                .thenReturn(mockEmployeeResponse(employees.get(0)));

        when(restTemplate.exchange(eq(apiUrl), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(BOOLEAN_TYPE)))
                .thenReturn(mockDeleteResponse());

        ResponseEntity<String> message = externalEmployeeService.deleteEmployeeById(id);
        assertEquals("Employee Mary Jane deleted successfully.", message.getBody());
    }

    @Test
    void deleteEmployeeById_employeeNotPresent_shouldReturnNotFoundStatus() {
        String id = UUID.fromString("a06ee5d-b7ba-4853-ac0c-abac53243b51").toString();

        when(restTemplate.exchange(eq(apiUrl + "/" + id), eq(HttpMethod.GET), isNull(), eq(EMPLOYEE_TYPE)))
                .thenReturn(mockEmptyEmployeeResponse());

        ResponseEntity<String> result = externalEmployeeService.deleteEmployeeById(id);
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }
}
