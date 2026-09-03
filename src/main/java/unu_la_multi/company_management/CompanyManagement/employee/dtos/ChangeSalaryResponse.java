package unu_la_multi.company_management.CompanyManagement.employee.dtos;

import java.math.BigDecimal;

public record ChangeSalaryResponse(
        Long id,
        BigDecimal salary,
        int updatedRows
) {
}
