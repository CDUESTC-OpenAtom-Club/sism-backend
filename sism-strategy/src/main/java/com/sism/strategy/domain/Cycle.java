package com.sism.strategy.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @Column(name = "cycle_name", nullable = false)
    private String name;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "description")
    private String description;

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
        cycle.createdAt = LocalDateTime.now();
        cycle.updatedAt = LocalDateTime.now();
        return cycle;
    }

    public void activate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void touch() {
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
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PostLoad
    protected void onLoad() {
    }

    @JsonProperty("cycleId")
    public Long getCycleId() {
        return id;
    }

    @JsonProperty("cycleName")
    public String getCycleName() {
        return name;
    }

    public String deriveStatus() {
        LocalDate today = LocalDate.now();
        if (endDate != null && endDate.isBefore(today)) {
            return "COMPLETED";
        }
        if (startDate != null && startDate.isAfter(today)) {
            return "UPCOMING";
        }
        return "ACTIVE";
    }
}
