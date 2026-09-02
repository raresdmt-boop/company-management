package unu_la_multi.company_management.CompanyManagement.department.exceptions;

public class DepartmentIdNotFound extends RuntimeException {
    public DepartmentIdNotFound() {
        super(ExceptionConstants.DEPARTMENT_ID_NOT_FOUND);
    }
}
