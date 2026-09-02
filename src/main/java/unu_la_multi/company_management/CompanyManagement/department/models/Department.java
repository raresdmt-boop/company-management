package unu_la_multi.company_management.CompanyManagement.department.models;


import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import unu_la_multi.company_management.CompanyManagement.employee.models.Employee;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "Department")
@Table(name = "department")
public class Department {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Setter
    @Getter
    @NotBlank(message = "Department name required")
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Setter
    @Getter
    @NotBlank(message = "Department location required")
    @Column(name = "location", nullable = false, length = 20)
    private String location;

    @Setter
    @Getter
    @Nullable
    @PositiveOrZero(message = "Department budget cannot be negative")
    private Double budget;

    @Getter
    @OneToMany(
            mappedBy = "department",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Employee> employees = new ArrayList<>();

    protected Department() {
    }

    public Department(String name, String location, Double budget) {
        this.name = name;
        this.location = location;
        this.budget = budget;
    }

    public void addEmployee(Employee employee) {
        employees.add(employee);
        employee.setDepartment(this);
    }

    public void removeEmployee(Employee employee) {
        employees.remove(employee);
        employee.setDepartment(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Department other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Department{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", budget=" + budget +
                '}';
    }
}
