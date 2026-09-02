package unu_la_multi.company_management.CompanyManagement.employee.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import unu_la_multi.company_management.CompanyManagement.department.exceptions.DepartmentIdNotFound;
import unu_la_multi.company_management.CompanyManagement.department.models.Department;
import unu_la_multi.company_management.CompanyManagement.department.repository.DepartmentRepository;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeCreateRequest;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeResponse;
import unu_la_multi.company_management.CompanyManagement.employee.models.Employee;
import unu_la_multi.company_management.CompanyManagement.employee.repository.EmployeeRepository;
import unu_la_multi.company_management.CompanyManagement.employee.services.interfaces.EmployeeCommandService;

@Service
@Validated
@Transactional
public class EmployeeCommandServiceImpl implements EmployeeCommandService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeCommandServiceImpl(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }


    @Override
    public EmployeeResponse createEmployeeWithDepartment(EmployeeCreateRequest request, Long departmentId) {

        Department department = departmentRepository.findById(departmentId).orElse(null);

        Employee employee = new Employee(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.salary(),
                request.jobTitle(),
                request.accessCode()
        );

        if (department != null) {
            department.addEmployee(employee);
        }
        else throw new DepartmentIdNotFound();

        employeeRepository.save(employee);

        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName()
        );
    }
}
