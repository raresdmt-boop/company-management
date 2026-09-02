package unu_la_multi.company_management.CompanyManagement.employee.dtos;

import unu_la_multi.company_management.CompanyManagement.employee.models.Employee;

public record EmployeeResponse(
        Long id,
        String firstName,
        String lastName
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName()
        );
    }
}
