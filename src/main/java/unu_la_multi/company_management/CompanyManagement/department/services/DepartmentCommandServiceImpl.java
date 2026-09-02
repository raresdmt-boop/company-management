package unu_la_multi.company_management.CompanyManagement.department.services;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import unu_la_multi.company_management.CompanyManagement.department.dtos.DepartmentCreateRequest;
import unu_la_multi.company_management.CompanyManagement.department.dtos.DepartmentResponse;
import unu_la_multi.company_management.CompanyManagement.department.models.Department;
import unu_la_multi.company_management.CompanyManagement.department.repository.DepartmentRepository;
import unu_la_multi.company_management.CompanyManagement.department.services.interfaces.DepartmentCommandService;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeResponse;
import unu_la_multi.company_management.CompanyManagement.employee.models.Employee;

@Service
@Validated
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

        if(request.employees() != null) {
            request.employees().forEach(employeeRequest -> {
                Employee employee = new Employee(
                        employeeRequest.firstName(),
                        employeeRequest.lastName(),
                        employeeRequest.email(),
                        employeeRequest.salary(),
                        employeeRequest.jobTitle(),
                        employeeRequest.accessCode()
                );
                department.addEmployee(employee);
            });
        }

        departmentRepository.save(department);

        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getEmployees()
                        .stream()
                        .map(EmployeeResponse::from)
                        .toList());

    }
}
