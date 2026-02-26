package com.sism.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Adhoc task target entity
 * Maps adhoc tasks to target organizations
 */
@Getter
@Setter
@Entity
@Table(name = "adhoc_task_target")
@IdClass(AdhocTaskTarget.AdhocTaskTargetId.class)
public class AdhocTaskTarget {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adhoc_task_id", nullable = false)
    private AdhocTask adhocTask;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_org_id", nullable = false)
    private SysOrg targetOrg;

    /**
     * Composite primary key class
     */
    @Getter
    @Setter
    public static class AdhocTaskTargetId implements Serializable {
        private Long adhocTask;
        private Long targetOrg;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AdhocTaskTargetId that = (AdhocTaskTargetId) o;
            return adhocTask.equals(that.adhocTask) && targetOrg.equals(that.targetOrg);
        }

        @Override
        public int hashCode() {
            return adhocTask.hashCode() + targetOrg.hashCode();
        }
    }
}
