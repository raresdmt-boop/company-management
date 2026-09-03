package unu_la_multi.company_management.CompanyManagement.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import unu_la_multi.company_management.CompanyManagement.employee.models.Employee;

import java.math.BigDecimal;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmail(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Employee e set e.salary = :newSalary where e.email = :email")
    int updateSalaryByEmail(@Param("email") String email, @Param("newSalary") BigDecimal salary);
}
