package unu_la_multi.company_management.CompanyManagement.department.controllers;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import unu_la_multi.company_management.CompanyManagement.department.dtos.DepartmentCreateRequest;
import unu_la_multi.company_management.CompanyManagement.department.dtos.DepartmentResponse;
import unu_la_multi.company_management.CompanyManagement.department.services.interfaces.DepartmentCommandService;
import unu_la_multi.company_management.CompanyManagement.department.services.interfaces.DepartmentQueryService;

import java.util.List;

@RestController
@RequestMapping("/api/department")
@Validated
public class DepartmentController {

    private final DepartmentQueryService departmentQueryService;
    private final DepartmentCommandService departmentCommandService;

    public DepartmentController(DepartmentQueryService departmentQueryService, DepartmentCommandService departmentCommandService) {
        this.departmentQueryService = departmentQueryService;
        this.departmentCommandService = departmentCommandService;
    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getDepartmentsWithEmployees() {
        return ResponseEntity.ok(departmentQueryService.getDepartmentsWithEmployees());
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentCreateRequest departmentCreateRequest) {
        DepartmentResponse created = departmentCommandService.createDepartment(departmentCreateRequest);
        return ResponseEntity.ok(created);
    }

}
