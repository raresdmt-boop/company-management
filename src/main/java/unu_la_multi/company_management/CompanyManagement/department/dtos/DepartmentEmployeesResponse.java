package unu_la_multi.company_management.CompanyManagement.department.dtos;

import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeResponse;

import java.util.List;

public record DepartmentEmployeesResponse(
        String departmentName,
        List<EmployeeResponse> employees
) {
}
