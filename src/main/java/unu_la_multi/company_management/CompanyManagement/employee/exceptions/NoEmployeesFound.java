package unu_la_multi.company_management.CompanyManagement.employee.exceptions;

public class NoEmployeesFound extends RuntimeException {
    public NoEmployeesFound() {
        super(ExceptionConstants.NO_EMPLOYEES_FOUND);
    }
}
