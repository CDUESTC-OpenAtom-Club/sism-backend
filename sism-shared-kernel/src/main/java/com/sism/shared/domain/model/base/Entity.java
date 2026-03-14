package com.sism.shared.domain.model.base;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 实体基类
 * DDD中实体是有唯一标识的领域对象
 *
 * @param <ID> 实体标识类型
 */
public abstract class Entity<ID> {

    protected ID id;
    protected LocalDateTime createdAt;
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

    public void setId(ID id) {
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
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + "}";
    }
}
