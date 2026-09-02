package unu_la_multi.company_management;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import unu_la_multi.company_management.CompanyManagement.department.dtos.DepartmentCreateRequest;
import unu_la_multi.company_management.CompanyManagement.department.dtos.DepartmentResponse;
import unu_la_multi.company_management.CompanyManagement.department.models.Department;
import unu_la_multi.company_management.CompanyManagement.department.repository.DepartmentRepository;
import unu_la_multi.company_management.CompanyManagement.department.services.interfaces.DepartmentCommandService;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeCreateRequest;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeResponse;
import unu_la_multi.company_management.CompanyManagement.employee.models.Employee;
import unu_la_multi.company_management.CompanyManagement.employee.services.interfaces.EmployeeCommandService;

import java.math.BigDecimal;
import java.util.List;

@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final DepartmentCommandService departmentCommandService;
    private final DepartmentRepository departmentRepository;
    private final EmployeeCommandService employeeCommandService;

    public DataSeeder(DepartmentCommandService departmentCommandService, DepartmentRepository departmentRepository, EmployeeCommandService employeeCommandService) {
        this.departmentCommandService = departmentCommandService;
        this.departmentRepository = departmentRepository;
        this.employeeCommandService = employeeCommandService;
    }

    @Override
    public void run(String... args) throws Exception {

        if(departmentRepository.count()>0) return;

        DepartmentCreateRequest departmentIT =
                new DepartmentCreateRequest(
                        "IT",
                        "Bacau",
                        250000.0
                );

        DepartmentCreateRequest departmentHR =
                new DepartmentCreateRequest(
                        "HR",
                        "Bucharest",
                        120000.0
                );

        DepartmentCreateRequest departmentFinance =
                new DepartmentCreateRequest(
                        "Finance",
                        "Iasi",
                        180000.0
                );

        DepartmentResponse responseIT = departmentCommandService.createDepartment(departmentIT);
        DepartmentResponse responseHR = departmentCommandService.createDepartment(departmentHR);
        DepartmentResponse responseFinance = departmentCommandService.createDepartment(departmentFinance);

        List<EmployeeCreateRequest> employeesIT =
                List.of(
                        new EmployeeCreateRequest(
                                "Rares",
                                "Dumitru",
                                "rares@gmail.com",
                                new BigDecimal("7500"),
                                "Java Developer",
                                "123456"
                        ),
                        new EmployeeCreateRequest(
                                "Andrei",
                                "Popescu",
                                "andrei@gmail.com",
                                new BigDecimal("9500"),
                                "Senior Developer",
                                "234567"
                        ),
                        new EmployeeCreateRequest(
                                "Maria",
                                "Ionescu",
                                "maria@gmail.com",
                                new BigDecimal("6800"),
                                "QA Engineer",
                                "345678"
                        )
        );

        List<EmployeeCreateRequest> employeesFinance =
                List.of(
                        new EmployeeCreateRequest(
                                "Daniel",
                                "Popa",
                                "daniel@gmail.com",
                                new BigDecimal("8200"),
                                "Financial Analyst",
                                "678901"
                        ),
                        new EmployeeCreateRequest(
                                "Elena",
                                "Dobre",
                                "elena@gmail.com",
                                new BigDecimal("11000"),
                                "Finance Manager",
                                "789012"
                        )
                );

        List<EmployeeCreateRequest> employeesHR =
                List.of(
                        new EmployeeCreateRequest(
                                "Ioana",
                                "Marin",
                                "ioana@gmail.com",
                                new BigDecimal("6000"),
                                "HR Specialist",
                                "456789"
                        ),
                        new EmployeeCreateRequest(
                                "Alex",
                                "Stan",
                                "alex@gmail.com",
                                new BigDecimal("7200"),
                                "Recruiter",
                                "567890"
                        )
                );

        for (EmployeeCreateRequest employee : employeesIT) {
            employeeCommandService.createEmployeeWithDepartment(employee, responseIT.id());
        }
        for (EmployeeCreateRequest employee : employeesHR) {
            employeeCommandService.createEmployeeWithDepartment(employee, responseHR.id());
        }
        for (EmployeeCreateRequest employee : employeesFinance) {
            employeeCommandService.createEmployeeWithDepartment(employee, responseFinance.id());
        }
    }
}
