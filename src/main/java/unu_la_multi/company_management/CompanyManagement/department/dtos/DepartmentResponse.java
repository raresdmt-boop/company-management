package unu_la_multi.company_management.CompanyManagement.department.dtos;

import unu_la_multi.company_management.CompanyManagement.department.models.Department;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeResponse;

import java.util.List;

public record DepartmentResponse(
        Long id,
        String name,
        List<EmployeeResponse> employees
) {
    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getEmployees()
                        .stream()
                        .map(EmployeeResponse::from).
                        toList());
    }
}
