package unu_la_multi.company_management.CompanyManagement.employee.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeResponse;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.PageResponse;
import unu_la_multi.company_management.CompanyManagement.employee.models.Employee;

import java.util.List;

public interface EmployeeQueryService {

    List<EmployeeResponse> getEmployees();
    EmployeeResponse getEmployee(Long id);
    List<EmployeeResponse> getEmployeesByJobTitle(String jobTitle);
    PageResponse<EmployeeResponse> getAllPageable(Pageable pageable);
}
