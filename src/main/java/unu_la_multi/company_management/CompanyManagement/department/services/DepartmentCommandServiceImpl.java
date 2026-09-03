package unu_la_multi.company_management.CompanyManagement.department.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import unu_la_multi.company_management.CompanyManagement.department.dtos.*;
import unu_la_multi.company_management.CompanyManagement.department.exceptions.DepartmentIdNotFound;
import unu_la_multi.company_management.CompanyManagement.department.exceptions.DepartmentNotFound;
import unu_la_multi.company_management.CompanyManagement.department.models.Department;
import unu_la_multi.company_management.CompanyManagement.department.repository.DepartmentRepository;
import unu_la_multi.company_management.CompanyManagement.department.services.interfaces.DepartmentCommandService;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeResponse;
import unu_la_multi.company_management.CompanyManagement.employee.models.Employee;

@Service
@Validated
@Transactional
public class DepartmentCommandServiceImpl implements DepartmentCommandService {

    private final DepartmentRepository departmentRepository;

    public DepartmentCommandServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public DepartmentResponse createDepartment(DepartmentCreateRequest request) {

        Department department = new Department(
                request.name(),
                request.location(),
                request.budget()
        );


        departmentRepository.save(department);

        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getEmployees()
                        .stream()
                        .map(EmployeeResponse::from)
                        .toList());

    }

    @Override
    public DepartmentUpdateResponse updateDepartment(Long id, DepartmentUpdateRequest request) {

        Department department = departmentRepository.findById(id).orElseThrow(DepartmentIdNotFound::new);

        if(request.name() != null && !request.name().isEmpty()) {
            department.setName(request.name());
        }
        if(request.location() != null && !request.location().isEmpty()) {
            department.setLocation(request.location());
        }
        if(request.budget() != null){
            department.setBudget(request.budget());
        }
        departmentRepository.save(department);
        return new DepartmentUpdateResponse(
                department.getId(),
                department.getName(),
                department.getLocation(),
                department.getBudget()
        );
    }

    @Override
    public ChangeBudgetResponse changeBudget(Long id, ChangeBudgetRequest changeBudgetRequest) {
        Department  department = departmentRepository.findById(id).orElseThrow(DepartmentIdNotFound::new);

        int updatedRows = departmentRepository.updateBudgetById(department.getId(), changeBudgetRequest.budget());
        return new ChangeBudgetResponse(
                department.getId(),
                department.getName(),
                changeBudgetRequest.budget(),
                updatedRows);
    }

    @Override
    public DepartmentDeleteResponse deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id).orElseThrow(DepartmentIdNotFound::new);

        departmentRepository.delete(department);

        return new DepartmentDeleteResponse(
                department.getId(),
                department.getName()
        );
    }

}
