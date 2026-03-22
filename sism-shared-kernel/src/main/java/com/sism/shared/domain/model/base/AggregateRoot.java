package com.sism.shared.domain.model.base;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 聚合根基类
 * DDD中聚合根是领域模型的核心，包含业务规则和不变量
 *
 * @param <ID> 聚合根标识类型
 */
public abstract class AggregateRoot<ID> {

    protected ID id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public AggregateRoot() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public AggregateRoot(ID id) {
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
     * 获取领域事件列表
     */
    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    /**
     * 清除所有领域事件（在事件发布后调用）
     */
    public void clearEvents() {
        domainEvents.clear();
    }

    /**
     * 添加领域事件
     */
    protected void addEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    /**
     * 聚合根是否可以发布事件
     * 子类可重写此方法控制事件发布时机
     */
    public boolean canPublish() {
        return true;
    }

    /**
     * 验证聚合根的业务规则
     * 子类必须实现此方法进行业务验证
     */
    public abstract void validate();

    /**
     * 标记实体已更新
     */
    protected void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregateRoot<?> that = (AggregateRoot<?>) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + getId() + "}";
    }
}
