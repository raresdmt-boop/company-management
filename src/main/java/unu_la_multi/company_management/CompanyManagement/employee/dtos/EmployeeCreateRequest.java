package unu_la_multi.company_management.CompanyManagement.employee.dtos;

import jakarta.validation.constraints.*;
import unu_la_multi.company_management.CompanyManagement.employee.models.Employee;

import java.math.BigDecimal;

public record EmployeeCreateRequest(

        @NotBlank(message = "Employee first name required")
        @Size(message = "Name must be maximum 50 characters long", max = 50)
        String firstName,

        @NotBlank(message = "Employee last name required")
        @Size(message = "Last name must be maximum 50 characters long", max = 50)
        String lastName,

        @NotBlank(message = "Email required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 50)
        String email,

        @NotNull(message = "Salary cannot be null")
        @Positive(message = "Salary must be pozitive")
        BigDecimal salary,

        @Size(min = 2, max = 20)
        String jobTitle,

        @NotBlank(message = "Access code required")
        @Size(message ="Access code must be exactly 6 characters", min = 6, max = 6)
        String accessCode
) {

}