package unu_la_multi.company_management.CompanyManagement.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unu_la_multi.company_management.CompanyManagement.employee.models.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}
