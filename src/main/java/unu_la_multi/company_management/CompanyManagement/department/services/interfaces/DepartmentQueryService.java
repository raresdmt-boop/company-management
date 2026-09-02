package unu_la_multi.company_management.CompanyManagement.department.services.interfaces;

import org.springframework.stereotype.Service;
import unu_la_multi.company_management.CompanyManagement.department.dtos.DepartmentResponse;

import java.util.List;


public interface DepartmentQueryService {

    List<DepartmentResponse> getDepartmentsWithEmployees();

}
