package unu_la_multi.company_management.CompanyManagement.department.exceptions;

public class DepartmentNotFound extends RuntimeException {
    public DepartmentNotFound() {
        super(ExceptionConstants.DEPARTMENT_NOT_FOUND);
    }
}
