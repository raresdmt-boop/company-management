package unu_la_multi.company_management.CompanyManagement.department.services.interfaces;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import unu_la_multi.company_management.CompanyManagement.department.dtos.*;


public interface DepartmentCommandService {

    DepartmentResponse createDepartment(@Valid DepartmentCreateRequest departmentCreateRequest);
    DepartmentUpdateResponse updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentUpdateRequest departmentUpdateRequest);
    ChangeBudgetResponse changeBudget(Long id, @Valid @RequestBody ChangeBudgetRequest changeBudgetRequest);
    DepartmentDeleteResponse deleteDepartment(Long id);

}
