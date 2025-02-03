package com.reliaquest.api.controller;

import static org.mockito.Mockito.*;

import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.ExternalEmployeeService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalEmployeeService externalEmployeeService;

    private final List<Employee> employees = List.of(
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

    @BeforeEach
    void setUpEmployees() {

        when(externalEmployeeService.fetchEmployees()).thenReturn(employees);
    }

    @Test
    void getAllEmployees_returnsAllEmployees() throws Exception {

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpectAll(result -> {});

        assertEmployees(resultActions, employees);

        verify(externalEmployeeService, times(1)).fetchEmployees();
    }

    @Test
    void searchEmployeesByName_returnsAllEmployeesMatching() throws Exception {

        when(externalEmployeeService.fetchEmployees()).thenReturn(employees);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee/search").param("name", "Mary"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].employee_name").value("Mary Jane"));
        verify(externalEmployeeService, times(1)).fetchEmployees();
    }

    @Test
    void getHighestSalary_returnsHighestSalary() throws Exception {

        when(externalEmployeeService.getHighestSalary()).thenReturn(ResponseEntity.ok(458866));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee/highest-salary"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").value(458866));

        verify(externalEmployeeService, times(1)).getHighestSalary();
    }

    @Test
    void getTop10HighestSalaries_returnsNamesOfEmployees() throws Exception {

        when(externalEmployeeService.getTopTenHighestEarningEmployeeNames())
                .thenReturn(ResponseEntity.ok(
                        List.of("Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Hank", "Ivy", "Jack")));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee/top-ten-highest-earning-employees"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.size()").value(10)) // Ensure 10 names are returned
                .andExpect(MockMvcResultMatchers.jsonPath("$[0]").value("Alice")) // Verify the first element
                .andExpect(MockMvcResultMatchers.jsonPath("$[1]").value("Bob")) // Verify the second element
                .andExpect(MockMvcResultMatchers.jsonPath("$[9]").value("Jack")); // Verify the last element

        verify(externalEmployeeService, times(1)).getTopTenHighestEarningEmployeeNames();
    }

    @Test
    void getEmployeeById_returnsEmployee() throws Exception {
        String employeeId = "a06ee5d-b7ba-4853-ac0c-abac53243b51";

        when(externalEmployeeService.fetchEmployeeById(employeeId)).thenReturn((Optional.of(employees.get(0))));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee/{id}", employeeId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.employee_name").value("Mary Jane"));

        verify(externalEmployeeService, times(1)).fetchEmployeeById(employeeId);
    }

    @Test
    void testCreateEmployee() throws Exception {

        CreateEmployeeRequest createRequest = new CreateEmployeeRequest("Mary Jane", 234566, 20, "developer");

        when(externalEmployeeService.createEmployee(createRequest)).thenReturn(employees.get(0));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/employee")
                        .contentType("application/json")
                        .content(
                                "{\"name\": \"Mary Jane\", \"title\": \"developer\", \"salary\": 234566, \"age\": 20}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.employee_name").value("Mary Jane"));

        verify(externalEmployeeService, times(1)).createEmployee(createRequest);
    }

    @Test
    void deleteEmployeeById_deletesTheEmployee() throws Exception {
        String employeeId = "a06ee5d-b7ba-4853-ac0c-abac53243b51";

        when(externalEmployeeService.deleteEmployeeById(employeeId))
                .thenReturn(ResponseEntity.ok("Employee Mary Jane deleted successfully"));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/employee/{id}", employeeId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("Employee Mary Jane deleted successfully"));

        verify(externalEmployeeService, times(1)).deleteEmployeeById(employeeId);
    }

    private void assertEmployees(ResultActions resultActions, List<Employee> employees) throws Exception {
        for (int i = 0; i < employees.size(); i++) {
            Employee employee = employees.get(i);

            resultActions
                    .andExpect(MockMvcResultMatchers.jsonPath("$[" + i + "].id").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$[" + i + "].employee_name")
                            .value(employee.getName()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[" + i + "].employee_salary")
                            .value(employee.getSalary()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[" + i + "].employee_age")
                            .value(employee.getAge()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[" + i + "].employee_email")
                            .value(employee.getEmail()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[" + i + "].employee_title")
                            .value(employee.getTitle()));
        }
    }
}
