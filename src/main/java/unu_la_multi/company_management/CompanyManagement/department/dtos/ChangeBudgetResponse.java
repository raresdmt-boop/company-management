package unu_la_multi.company_management.CompanyManagement.department.dtos;

public record ChangeBudgetResponse(
        Long id,
        String name,
        Double budget,
        int updatedRow
) {
}
