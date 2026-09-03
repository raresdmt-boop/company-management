package unu_la_multi.company_management.CompanyManagement.department.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record DepartmentUpdateRequest(
        @NotBlank(message = "Updating all rows is necessary for PUT mapping")
        @Size(message = "Department name can be max 50 characters", max = 50)
        String name,

        @NotBlank(message = "Updating all rows is necessary for PUT mapping")
        @Size(message = "Department location can be max 20 characters", max = 20)
        String location,

        @NotNull(message = "Updating all rows is necessary for PUT mapping")
        @PositiveOrZero(message = "Department budget cannot be negative")
        Double budget
) {
}
