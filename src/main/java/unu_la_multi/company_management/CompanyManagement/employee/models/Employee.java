package unu_la_multi.company_management.CompanyManagement.employee.models;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import unu_la_multi.company_management.CompanyManagement.department.models.Department;

import java.math.BigDecimal;

@Entity(name = "Employee")
@Table(name = "employee",
uniqueConstraints = @UniqueConstraint(name = "uk_employee_email", columnNames = "email"))
public class Employee {


    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Getter
    @NotBlank(message = "Employee first name required")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Setter
    @Getter
    @NotBlank(message = "Employee last name required")
    @Column(name = "last_name",  nullable = false, length = 50)
    private String lastName;

    @Setter
    @Getter
    @NotBlank(message = "Employee email required")
    @Column(name = "email", nullable = false,unique = true, length = 50)
    @Size(min = 1, max = 50)
    @Email(message = "Email must be a valid address")
    private String email;

    @Setter
    @Getter
    @NotNull(message = "Salary cannot be null")
    @Positive(message = "Salary must be positive")
    @Column(name = "salary", nullable = false)
    private BigDecimal salary;

    @Setter
    @Getter
    @Column(name = "job_title", length = 20)
    private String jobTitle;

    @Setter
    @Getter
    @Size(min = 6, max = 6, message = "Access code must have exactly 6 characters")
    @NotBlank(message = "Access code required")
    @Column(name = "access_code", nullable = false, unique = true, length = 6)
    private String accessCode;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    protected Employee() {
    }

    public Employee(String firstName, String lastName, String email, BigDecimal salary, String jobTitle, String accessCode) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.salary = salary;
        this.jobTitle = jobTitle;
        this.accessCode = accessCode;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(!(o instanceof Employee other)) return false;
        return id!=null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", accessCode='" + accessCode + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", salary=" + salary +
                ", jobTitle='" + jobTitle + '\'' +
                '}';
    }
}
