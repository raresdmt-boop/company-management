package unu_la_multi.company_management.CompanyManagement.employee.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeResponse;
import unu_la_multi.company_management.CompanyManagement.employee.services.interfaces.EmployeeQueryService;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@Validated
public class EmployeeController {

    private final EmployeeQueryService employeeQueryService;

    public EmployeeController(EmployeeQueryService employeeQueryService) {
        this.employeeQueryService = employeeQueryService;
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getEmployees() {
        return ResponseEntity.ok(employeeQueryService.getEmployees());
    }

}
