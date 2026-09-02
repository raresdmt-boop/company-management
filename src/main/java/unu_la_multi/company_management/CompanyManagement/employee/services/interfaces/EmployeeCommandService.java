package unu_la_multi.company_management.CompanyManagement.employee.services.interfaces;

import jakarta.validation.Valid;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeCreateRequest;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeResponse;
import unu_la_multi.company_management.CompanyManagement.employee.models.Employee;

public interface EmployeeComandService {

    EmployeeResponse createEmployeeWithDepartment(@Valid EmployeeCreateRequest request, Long departmentId);

}
