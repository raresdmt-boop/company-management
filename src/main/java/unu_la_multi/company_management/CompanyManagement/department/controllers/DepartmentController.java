package unu_la_multi.company_management.CompanyManagement.department.controllers;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import unu_la_multi.company_management.CompanyManagement.department.dtos.*;
import unu_la_multi.company_management.CompanyManagement.department.services.interfaces.DepartmentCommandService;
import unu_la_multi.company_management.CompanyManagement.department.services.interfaces.DepartmentQueryService;

import java.util.List;
import java.util.Map;

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

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentUpdateResponse> updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentUpdateRequest departmentUpdateRequest) {
        DepartmentUpdateResponse updated = departmentCommandService.updateDepartment(id, departmentUpdateRequest);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/budget")
    public ResponseEntity<ChangeBudgetResponse> changeBudget(
            @PathVariable Long id,
            @Valid @RequestBody ChangeBudgetRequest changeBudgetRequest) {
        return ResponseEntity.ok(departmentCommandService.changeBudget(id, changeBudgetRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DepartmentDeleteResponse> deleteDepartment(@PathVariable Long id) {
        return ResponseEntity.ok(departmentCommandService.deleteDepartment(id));
    }

    @GetMapping("/count-employees-per-department")
    public ResponseEntity<Map<String, Object>> getCountEmployeesPerDepartment(@RequestParam Long departmentId) {
        return ResponseEntity.ok(Map.of(
                "department", departmentQueryService.getDepartmentById(departmentId).name(),
                "emmployee count", departmentQueryService.countEmployeesByDepartmentId(departmentId)));
    }

    @GetMapping("/employee-count")
    public ResponseEntity<List<DepartmentEmployeeCount>> getAllEmployeeCount(){
        return ResponseEntity.ok(departmentQueryService.getAllEmployeeCounts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id){
        return ResponseEntity.ok(departmentQueryService.getDepartmentById(id));
    }

}
