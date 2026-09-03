package unu_la_multi.company_management.CompanyManagement.employee.dtos;

import java.math.BigDecimal;

public record EmployeeUpdateResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        BigDecimal salary,
        String jobTitle,
        String accessCode
) {
}
