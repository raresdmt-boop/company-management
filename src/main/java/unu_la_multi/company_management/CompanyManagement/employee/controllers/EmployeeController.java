package unu_la_multi.company_management.CompanyManagement.employee.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeCreateRequest;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.EmployeeResponse;
import unu_la_multi.company_management.CompanyManagement.employee.services.interfaces.EmployeeCommandService;
import unu_la_multi.company_management.CompanyManagement.employee.services.interfaces.EmployeeQueryService;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@Validated
public class EmployeeController {

    private final EmployeeCommandService employeeCommandService;
    private final EmployeeQueryService employeeQueryService;

    public EmployeeController(EmployeeCommandService employeeCommandService, EmployeeQueryService employeeQueryService) {
        this.employeeCommandService = employeeCommandService;
        this.employeeQueryService = employeeQueryService;
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getEmployees() {
        return ResponseEntity.ok(employeeQueryService.getEmployees());
    }

    @PostMapping("/{departmentid}")
    public ResponseEntity<EmployeeResponse> createEmployee(@PathVariable("departmentid") Long departmentid,
            EmployeeCreateRequest request) {
        return ResponseEntity.ok(employeeCommandService.createEmployeeWithDepartment(request, departmentid));
    }

}
