package com.sism.shared.domain.model.base;

import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 实体基类
 * DDD中实体是有唯一标识的领域对象
 *
 * @param <ID> 实体标识类型
 */
public abstract class Entity<ID> {

    @Transient
    protected ID id;
    @Transient
    protected LocalDateTime createdAt;
    @Transient
    protected LocalDateTime updatedAt;

    public Entity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Entity(ID id) {
        this();
        this.id = id;
    }

    public ID getId() {
        return id;
    }

    /**
     * Helper for subclasses that expose an ID setter backed by a dedicated field.
     * Shared-kernel aggregates use field access for JPA, so the setter guard lives
     * here while the actual assignment stays on the concrete aggregate.
     */
    protected final void assertIdUnchanged(ID currentId, ID newId) {
        if (currentId != null && !Objects.equals(currentId, newId)) {
            throw new IllegalStateException("ID is immutable once assigned");
        }
    }

    /**
     * Base ID setter kept non-public so JPA field access remains the primary write path.
     * Concrete aggregates may override this to synchronize their own identifier field.
     */
    protected void setId(ID id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 标记实体已更新
     */
    protected void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 实体是否相等（基于ID）
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> entity = (Entity<?>) o;
        if (getId() == null || entity.getId() == null) {
            return false;
        }
        return Objects.equals(getId(), entity.getId());
    }

    @Override
    public int hashCode() {
        if (getId() == null) {
            return System.identityHashCode(this);
        }
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + getId() + "}";
    }
}
