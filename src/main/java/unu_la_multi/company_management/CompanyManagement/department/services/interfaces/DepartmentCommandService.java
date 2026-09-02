package unu_la_multi.company_management.CompanyManagement.department.services.interfaces;

import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import unu_la_multi.company_management.CompanyManagement.department.dtos.DepartmentCreateRequest;
import unu_la_multi.company_management.CompanyManagement.department.dtos.DepartmentResponse;


public interface DepartmentCommandService {

    DepartmentResponse createDepartment(@Valid DepartmentCreateRequest departmentCreateRequest);

}
