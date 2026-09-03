package unu_la_multi.company_management.CompanyManagement.employee.services.interfaces;

import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeResponse;
import unu_la_multi.company_management.CompanyManagement.employee.models.Employee;

import java.util.List;

public interface EmployeeQueryService {

    List<EmployeeResponse> getEmployees();
    EmployeeResponse getEmployee(Long id);
}
