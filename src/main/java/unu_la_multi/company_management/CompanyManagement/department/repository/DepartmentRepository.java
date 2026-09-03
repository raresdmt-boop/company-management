package unu_la_multi.company_management.CompanyManagement.department.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import unu_la_multi.company_management.CompanyManagement.department.models.Department;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @Query("""
            select d from Department d
            left join fetch d.employees
            """)
    List<Department> getDepartmentsWithEmployees();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Department d set d.budget = :newBudget where d.id = :id")
    int updateBudgetById(@Param("id") Long id, @Param("newBudget") Double newBudget);

}
