package unu_la_multi.company_management.CompanyManagement.employee.services;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeResponse;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.PageResponse;
import unu_la_multi.company_management.CompanyManagement.employee.exceptions.EmployeeIdNotFound;
import unu_la_multi.company_management.CompanyManagement.employee.exceptions.NoEmployeesFound;
import unu_la_multi.company_management.CompanyManagement.employee.models.Employee;
import unu_la_multi.company_management.CompanyManagement.employee.repository.EmployeeRepository;
import unu_la_multi.company_management.CompanyManagement.employee.services.interfaces.EmployeeQueryService;

import java.util.List;

@Service
@Validated
@Transactional
public class EmployeeQueryServiceImpl implements EmployeeQueryService {

    private final EmployeeRepository employeeRepository;

    public EmployeeQueryServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<EmployeeResponse> getEmployees() {
        if(employeeRepository.findAll().isEmpty()) {
            throw new NoEmployeesFound();
        }
        return employeeRepository.findAll()
                .stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    @Override
    public EmployeeResponse getEmployee(Long id) {
        return EmployeeResponse.from(employeeRepository.findById(id).orElseThrow(EmployeeIdNotFound::new));
    }

    @Override
    public List<EmployeeResponse> getEmployeesByJobTitle(String jobTitle) {
        if(jobTitle == null || jobTitle.isEmpty()) {
            return employeeRepository.findAll()
                    .stream()
                    .map(EmployeeResponse::from)
                    .toList();
        }
        return employeeRepository.findByJobTitle(jobTitle)
                .stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    @Override
    public PageResponse<EmployeeResponse> getAllPageable(Pageable pageable) {
        Page<Employee> page =  employeeRepository.findAll(pageable);
        return PageResponse.from(page, EmployeeResponse::from);
    }


}
