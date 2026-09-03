package unu_la_multi.company_management.CompanyManagement.employee.exceptions;

public class EmployeeIdNotFound extends RuntimeException {
    public EmployeeIdNotFound() {
        super(ExceptionConstants.EMPLOYEE_ID_NOT_FOUND);
    }
}
