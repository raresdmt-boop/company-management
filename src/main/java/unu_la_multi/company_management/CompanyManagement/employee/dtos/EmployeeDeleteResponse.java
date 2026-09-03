package unu_la_multi.company_management.CompanyManagement.employee.dtos;

public record EmployeeDeleteResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String departmentName
) {
}
