package com.reliaquest.api.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonNaming(Employee.PrefixNamingStrategy.class)
public class Employee {

    private UUID id;
    private String name;
    private Integer Salary;
    private Integer Age;
    private String Title;
    private String Email;

    static class PrefixNamingStrategy extends PropertyNamingStrategies.NamingBase {

        @Override
        public String translate(String propertyName) {
            if ("id".equals(propertyName)) {
                return propertyName;
            }
            return "employee_" + propertyName;
        }
    }
}
