package unu_la_multi.company_management.CompanyManagement.department.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import unu_la_multi.company_management.CompanyManagement.department.dtos.DepartmentEmployeeCount;
import unu_la_multi.company_management.CompanyManagement.department.dtos.DepartmentResponse;
import unu_la_multi.company_management.CompanyManagement.department.exceptions.NoDepartmentFound;
import unu_la_multi.company_management.CompanyManagement.department.models.Department;
import unu_la_multi.company_management.CompanyManagement.department.repository.DepartmentRepository;
import unu_la_multi.company_management.CompanyManagement.department.services.interfaces.DepartmentQueryService;

import java.util.ArrayList;
import java.util.List;

@Service
@Validated
@Transactional
public class DepartmentQueryServiceImpl implements DepartmentQueryService {

    private final DepartmentRepository departmentRepository;
    public DepartmentQueryServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public List<DepartmentResponse> getDepartmentsWithEmployees() {

        return departmentRepository.getDepartmentsWithEmployees()
                .stream()
                .map(DepartmentResponse::from)
                .toList();

    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
       return DepartmentResponse.from(departmentRepository.findById(id).orElseThrow(NoDepartmentFound::new));
    }

    @Override
    public long countEmployeesByDepartmentId(Long id) {
        return  departmentRepository.countEmployeesByDepartmentId(id);
    }

    @Override
    public DepartmentEmployeeCount getEmployeeCount(Long departmentId) {
        Long count =  departmentRepository.countEmployeesByDepartmentId(departmentId);
        Department department = departmentRepository.findById(departmentId).orElseThrow(NoDepartmentFound::new);
        return new DepartmentEmployeeCount(
                department.getName(),
                count
        );
    }

    @Override
    public List<DepartmentEmployeeCount> getAllEmployeeCounts() {
        List<Department>  departments = departmentRepository.findAll();
        List<DepartmentEmployeeCount> counts = new ArrayList<>();
        for(Department d : departments){
            counts.add(getEmployeeCount(d.getId()));
        }
        return counts;
    }


}
