package unu_la_multi.company_management.CompanyManagement.department.exceptions;

public class NoDepartmentFound extends RuntimeException {
    public NoDepartmentFound() {
        super(ExceptionConstants.NO_DEPARTMENT_FOUND);
    }
}
