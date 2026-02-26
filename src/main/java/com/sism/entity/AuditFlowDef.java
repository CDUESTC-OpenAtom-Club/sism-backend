package com.sism.entity;

import com.sism.enums.AuditEntityType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Audit Flow Definition entity
 * Defines audit workflow templates for different entity types
 */
@Getter
@Setter
@Entity
@Table(name = "audit_flow_def", uniqueConstraints = {
    @UniqueConstraint(name = "uk_flow_code", columnNames = "flow_code")
})
public class AuditFlowDef extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Flow name is required")
    @Size(max = 100, message = "Flow name must not exceed 100 characters")
    @Column(name = "flow_name", nullable = false, length = 100)
    private String flowName;

    @NotBlank(message = "Flow code is required")
    @Size(max = 50, message = "Flow code must not exceed 50 characters")
    @Column(name = "flow_code", nullable = false, unique = true, length = 50)
    private String flowCode;

    @NotNull(message = "Entity type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private AuditEntityType entityType;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(name = "description", length = 500)
    private String description;
}
