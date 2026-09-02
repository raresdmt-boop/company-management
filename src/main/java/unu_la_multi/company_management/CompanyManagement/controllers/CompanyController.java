package unu_la_multi.company_management.CompanyManagement.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unu_la_multi.company_management.CompanyManagement.department.dtos.DepartmentResponse;
import unu_la_multi.company_management.CompanyManagement.department.services.interfaces.DepartmentQueryService;

import java.util.List;

@RestController
@RequestMapping("/api/company")
@Validated
public class CompanyController {

    private final DepartmentQueryService departmentQueryService;
    public CompanyController(DepartmentQueryService departmentQueryService) {
        this.departmentQueryService = departmentQueryService;
    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getDepartmentsWithEmployees() {
        return ResponseEntity.ok(departmentQueryService.getDepartmentsWithEmployees());
    }

}
