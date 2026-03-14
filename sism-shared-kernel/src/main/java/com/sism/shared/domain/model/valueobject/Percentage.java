package com.sism.shared.domain.model.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 百分比值对象
 * 提供类型安全的百分比计算和表示
 */
public class Percentage {

    private final BigDecimal value;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public Percentage(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Percentage value cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }
        this.value = value;
    }

    public Percentage(Integer value) {
        this(new BigDecimal(value));
    }

    public Percentage(Long value) {
        this(new BigDecimal(value));
    }

    public Percentage(Double value) {
        this(BigDecimal.valueOf(value));
    }

    /**
     * 从小数创建（如0.5表示50%）
     */
    public static Percentage fromDecimal(BigDecimal decimal) {
        return new Percentage(decimal.multiply(HUNDRED));
    }

    /**
     * 从小数创建（如0.5表示50%）
     */
    public static Percentage fromDecimal(Double decimal) {
        return fromDecimal(BigDecimal.valueOf(decimal));
    }

    /**
     * 获取百分比值（如50表示50%）
     */
    public BigDecimal getValue() {
        return value;
    }

    /**
     * 获取小数形式（如0.5表示50%）
     */
    public BigDecimal toDecimal() {
        return value.divide(HUNDRED, 4, RoundingMode.HALF_UP);
    }

    /**
     * 获取整数值
     */
    public int toInt() {
        return value.intValue();
    }

    /**
     * 相加两个百分比
     */
    public Percentage add(Percentage other) {
        return new Percentage(this.value.add(other.value));
    }

    /**
     * 相减两个百分比
     */
    public Percentage subtract(Percentage other) {
        return new Percentage(this.value.subtract(other.value));
    }

    /**
     * 乘以一个数
     */
    public Percentage multiply(BigDecimal multiplier) {
        return new Percentage(this.value.multiply(multiplier));
    }

    /**
     * 是否大于另一个百分比
     */
    public boolean isGreaterThan(Percentage other) {
        return this.value.compareTo(other.value) > 0;
    }

    /**
     * 是否小于另一个百分比
     */
    public boolean isLessThan(Percentage other) {
        return this.value.compareTo(other.value) < 0;
    }

    /**
     * 格式化显示
     */
    public String format() {
        return value.setScale(2, RoundingMode.HALF_UP) + "%";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Percentage that = (Percentage) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return format();
    }
}
