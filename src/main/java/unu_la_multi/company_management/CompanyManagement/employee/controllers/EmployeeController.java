package unu_la_multi.company_management.CompanyManagement.employee.controllers;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import unu_la_multi.company_management.CompanyManagement.employee.dtos.*;
import unu_la_multi.company_management.CompanyManagement.employee.services.interfaces.EmployeeCommandService;
import unu_la_multi.company_management.CompanyManagement.employee.services.interfaces.EmployeeQueryService;

import java.math.BigDecimal;
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
            @Valid @RequestBody EmployeeCreateRequest request) {
        return ResponseEntity.ok(employeeCommandService.createEmployeeWithDepartment(request, departmentid));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeUpdateResponse> updateEmployee(
            @PathVariable("id") Long employeeId,
            @Valid @RequestBody EmployeeUpdateRequest request
    ){
        return ResponseEntity.ok(employeeCommandService.updateEmployee(employeeId, request));
    }

    @PatchMapping("/{id}/salary")
    public ResponseEntity<ChangeSalaryResponse> changeSalary(
            @PathVariable("id") Long employeeId,
            @Valid @RequestBody ChangeSalaryRequest request
    ) {
        return ResponseEntity.ok(employeeCommandService.changeSalary(employeeId, request));
    }

    @PatchMapping("/{id}/{newSalary}")
    public ResponseEntity<ChangeSalaryResponse> changeSalaryThruUrl(
            @PathVariable("id") Long employeeId,
            @PathVariable("newSalary") BigDecimal newSalary
    ){
        return ResponseEntity.ok(employeeCommandService.changeSalaryThruUrl(employeeId, newSalary));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EmployeeDeleteResponse> deleteEmployee(
            @PathVariable("id") Long employeeId
    ){
        return ResponseEntity.ok(employeeCommandService.deleteEmployee(employeeId));
    }

}
