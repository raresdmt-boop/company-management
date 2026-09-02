package unu_la_multi.company_management.CompanyManagement.department.dtos;

public record DepartmentUpdateResponse(
        Long id,
        String name,
        String location,
        Double budget
){
}
