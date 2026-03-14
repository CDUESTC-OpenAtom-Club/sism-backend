package com.sism.shared.domain.model.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * 实体ID值对象
 * 提供类型安全的ID包装
 *
 * @param <T> 实体类型
 */
public class EntityId<T> {

    private final String value;
    private final Class<T> entityType;

    public EntityId(Class<T> entityType) {
        this.value = UUID.randomUUID().toString();
        this.entityType = entityType;
    }

    public EntityId(String value, Class<T> entityType) {
        this.value = value;
        this.entityType = entityType;
    }

    public EntityId(Long value, Class<T> entityType) {
        this.value = String.valueOf(value);
        this.entityType = entityType;
    }

    public String getValue() {
        return value;
    }

    public Long getLongValue() {
        return Long.parseLong(value);
    }

    public Class<T> getEntityType() {
        return entityType;
    }

    public boolean isValid() {
        return value != null && !value.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityId<?> entityId = (EntityId<?>) o;
        return Objects.equals(value, entityId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
