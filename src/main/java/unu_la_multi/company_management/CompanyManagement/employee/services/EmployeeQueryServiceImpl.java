package unu_la_multi.company_management.CompanyManagement.employee.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeResponse;
import unu_la_multi.company_management.CompanyManagement.employee.exceptions.NoEmployeesFound;
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



}
