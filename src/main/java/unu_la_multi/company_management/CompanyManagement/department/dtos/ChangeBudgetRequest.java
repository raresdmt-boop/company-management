package unu_la_multi.company_management.CompanyManagement.department.dtos;

import jakarta.validation.constraints.NotNull;

public record ChangeBudgetRequest(
        @NotNull(message = "Budget cannot be null")
        Double budget
) {
}
