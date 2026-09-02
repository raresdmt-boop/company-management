package unu_la_multi.company_management.CompanyManagement.department.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import unu_la_multi.company_management.CompanyManagement.department.dtos.DepartmentResponse;
import unu_la_multi.company_management.CompanyManagement.department.exceptions.NoDepartmentFound;
import unu_la_multi.company_management.CompanyManagement.department.repository.DepartmentRepository;
import unu_la_multi.company_management.CompanyManagement.department.services.interfaces.DepartmentQueryService;

import java.util.ArrayList;
import java.util.List;

@Service
@Validated
public class DepartmentQueryServiceImpl implements DepartmentQueryService {

    private final DepartmentRepository departmentRepository;
    public DepartmentQueryServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public List<DepartmentResponse> getDepartmentsWithEmployees() {
        if(departmentRepository.getDepartmentsWithEmployees().isEmpty())
            throw new NoDepartmentFound();

        return departmentRepository.getDepartmentsWithEmployees()
                .stream()
                .map(DepartmentResponse::from)
                .toList();

    }
}
