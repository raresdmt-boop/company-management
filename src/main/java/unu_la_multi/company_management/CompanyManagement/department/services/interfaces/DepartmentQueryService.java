package unu_la_multi.company_management.CompanyManagement.department.services.interfaces;

import org.springframework.web.bind.annotation.RequestParam;
import unu_la_multi.company_management.CompanyManagement.department.dtos.DepartmentEmployeeCount;
import unu_la_multi.company_management.CompanyManagement.department.dtos.DepartmentResponse;

import java.util.List;


public interface DepartmentQueryService {

    List<DepartmentResponse> getDepartmentsWithEmployees();
    DepartmentResponse getDepartmentById(Long id);
    long countEmployeesByDepartmentId(Long id);
    DepartmentEmployeeCount getEmployeeCount(Long departmentId);
    List<DepartmentEmployeeCount> getAllEmployeeCounts();
}
