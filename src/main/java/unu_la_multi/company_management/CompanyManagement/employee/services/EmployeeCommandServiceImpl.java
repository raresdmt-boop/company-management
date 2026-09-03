package unu_la_multi.company_management.CompanyManagement.employee.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import unu_la_multi.company_management.CompanyManagement.department.exceptions.DepartmentIdNotFound;
import unu_la_multi.company_management.CompanyManagement.department.models.Department;
import unu_la_multi.company_management.CompanyManagement.department.repository.DepartmentRepository;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.*;
import unu_la_multi.company_management.CompanyManagement.employee.exceptions.EmployeeIdNotFound;
import unu_la_multi.company_management.CompanyManagement.employee.models.Employee;
import unu_la_multi.company_management.CompanyManagement.employee.repository.EmployeeRepository;
import unu_la_multi.company_management.CompanyManagement.employee.services.interfaces.EmployeeCommandService;

import java.math.BigDecimal;

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

        Department department = departmentRepository.findById(departmentId).orElseThrow();

        Employee employee = new Employee(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.salary(),
                request.jobTitle(),
                request.accessCode()
        );

        department.addEmployee(employee);

        employeeRepository.save(employee);

        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getDepartment().getName()
        );
    }

    @Override
    public EmployeeUpdateResponse updateEmployee(Long id, EmployeeUpdateRequest request){

        Employee employee = employeeRepository.findById(id).orElseThrow();

        if(request.firstName()!=null && !request.firstName().isBlank()){
            employee.setFirstName(request.firstName());
        }
        if(request.lastName()!=null && !request.lastName().isBlank()){
            employee.setLastName(request.lastName());
        }
        if(request.email()!=null && !request.email().isBlank() && !employeeRepository.existsByEmail(request.email())){
            employee.setEmail(request.email());
        }
        if(request.salary()!=null){
            employee.setSalary(request.salary());
        }
        if(request.jobTitle()!=null && !request.jobTitle().isBlank()){
            employee.setJobTitle(request.jobTitle());
        }
        if(request.accessCode()!=null && !request.accessCode().isBlank()){
            employee.setAccessCode(request.accessCode());
        }

        employeeRepository.save(employee);

        return new EmployeeUpdateResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getSalary(),
                employee.getJobTitle(),
                employee.getAccessCode()
        );
    }

    @Override
    public ChangeSalaryResponse changeSalary(Long id, ChangeSalaryRequest request) {
        Employee employee =  employeeRepository.findById(id).orElseThrow(EmployeeIdNotFound::new);

        int updatedRows = employeeRepository.updateSalaryByEmail(employee.getEmail(), request.salary());

        return new ChangeSalaryResponse(
                employee.getId(),
                employee.getSalary(),
                updatedRows
        );
    }

    @Override
    public ChangeSalaryResponse changeSalaryThruUrl(Long id, BigDecimal newSalary) {
        Employee employee =  employeeRepository.findById(id).orElseThrow(EmployeeIdNotFound::new);

        int updatedRows = employeeRepository.updateSalaryByEmail(employee.getEmail(), newSalary);

        return new ChangeSalaryResponse(
                employee.getId(),
                employee.getSalary(),
                updatedRows
        );
    }

    @Override
    public EmployeeDeleteResponse deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id).orElseThrow(EmployeeIdNotFound::new);
        employeeRepository.delete(employee);
        return new EmployeeDeleteResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getDepartment().getName()
        );
    }
}
