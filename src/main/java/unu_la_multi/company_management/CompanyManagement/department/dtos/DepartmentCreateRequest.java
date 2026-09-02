package unu_la_multi.company_management.CompanyManagement.department.dtos;

import jakarta.validation.constraints.*;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeCreateRequest;

import java.util.List;

public record DepartmentCreateRequest(
        @NotBlank(message = "Department name required")
        @Size(message = "Department name can be max 50 characters", max = 50)
        String name,

        @NotBlank(message = "Department location required")
        @Size(message = "Department location can be max 20 characters", max = 20)
        String location,

        @PositiveOrZero(message = "Department budget cannot be negative")
        Double budget,

        List<EmployeeCreateRequest> employees
) {
}
