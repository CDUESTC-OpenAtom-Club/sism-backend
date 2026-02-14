package com.sism.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Adhoc task indicator map entity
 * Maps adhoc tasks to indicators
 */
@Getter
@Setter
@Entity
@Table(name = "adhoc_task_indicator_map")
@IdClass(AdhocTaskIndicatorMap.AdhocTaskIndicatorMapId.class)
public class AdhocTaskIndicatorMap {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adhoc_task_id", nullable = false)
    private AdhocTask adhocTask;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indicator_id", nullable = false)
    private Indicator indicator;

    /**
     * Composite primary key class
     */
    @Getter
    @Setter
    public static class AdhocTaskIndicatorMapId implements Serializable {
        private Long adhocTask;
        private Long indicator;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AdhocTaskIndicatorMapId that = (AdhocTaskIndicatorMapId) o;
            return adhocTask.equals(that.adhocTask) && indicator.equals(that.indicator);
        }

        @Override
        public int hashCode() {
            return adhocTask.hashCode() + indicator.hashCode();
        }
    }
}
