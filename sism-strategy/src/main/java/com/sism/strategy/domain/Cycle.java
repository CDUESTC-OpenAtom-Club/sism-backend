package com.sism.strategy.domain;

import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cycle - 考核周期实体
 */
@Getter
@Setter
@Entity
@Table(name = "cycle", schema = "public")
@Access(AccessType.FIELD)
public class Cycle extends AggregateRoot<Long> {

    @Id
    @SequenceGenerator(name = "Cycle_IdSeq", sequenceName = "public.cycle_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Cycle_IdSeq")
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Cycle create(String name, Integer year, LocalDate startDate, LocalDate endDate) {
        Cycle cycle = new Cycle();
        cycle.name = name;
        cycle.year = year;
        cycle.startDate = startDate;
        cycle.endDate = endDate;
        cycle.status = "ACTIVE";
        cycle.isDeleted = false;
        cycle.createdAt = LocalDateTime.now();
        cycle.updatedAt = LocalDateTime.now();
        return cycle;
    }

    public void activate() {
        this.status = "ACTIVE";
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = "INACTIVE";
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Cycle name is required");
        }
        if (year == null || year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 2000 and 2100");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date is required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }

    @PrePersist
    protected void onCreate() {
        if (isDeleted == null) {
            isDeleted = false;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
}
