package unu_la_multi.company_management.CompanyManagement.employee.services.interfaces;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.*;

import java.math.BigDecimal;

public interface EmployeeCommandService {

    EmployeeResponse createEmployeeWithDepartment(@Valid EmployeeCreateRequest request, Long departmentId);
    EmployeeUpdateResponse updateEmployee(Long id, @Valid EmployeeUpdateRequest request);
    ChangeSalaryResponse changeSalary(Long id, @Valid ChangeSalaryRequest request);
    ChangeSalaryResponse changeSalaryThruUrl(Long id, BigDecimal newSalary);
    EmployeeDeleteResponse deleteEmployee(Long id);
}
