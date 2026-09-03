package unu_la_multi.company_management.CompanyManagement.employee.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ChangeSalaryRequest(
        @NotNull(message = "Salary expected")
        @Positive(message = "Salary cannot be negative")
        BigDecimal salary
) {
}
