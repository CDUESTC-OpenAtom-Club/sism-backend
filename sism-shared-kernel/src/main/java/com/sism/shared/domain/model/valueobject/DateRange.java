package com.sism.shared.domain.model.valueobject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 日期范围值对象
 * 用于表示一个时间区间
 */
public class DateRange {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String displayFormat;

    private DateRange(LocalDate startDate, LocalDate endDate, String displayFormat) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        this.startDate = startDate;
        this.endDate = endDate;
        this.displayFormat = displayFormat;
    }

    public DateRange(LocalDate startDate, LocalDate endDate) {
        this(startDate, endDate, null);
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * 判断指定日期是否在范围内
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * 判断指定日期时间是否在范围内
     */
    public boolean contains(LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        return contains(date);
    }

    /**
     * 判断是否与另一个日期范围重叠
     */
    public boolean overlaps(DateRange other) {
        return !this.endDate.isBefore(other.startDate) && !this.startDate.isAfter(other.endDate);
    }

    /**
     * 获取范围天数
     */
    public long getDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * 判断是否与当前日期重叠
     */
    public boolean overlapsWithToday() {
        return contains(LocalDate.now());
    }

    /**
     * 格式化显示
     */
    public String format() {
        if (displayFormat != null) {
            return startDate.format(DateTimeFormatter.ofPattern(displayFormat)) + " - " +
                   endDate.format(DateTimeFormatter.ofPattern(displayFormat));
        }
        return startDate + " to " + endDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DateRange dateRange = (DateRange) o;
        return Objects.equals(startDate, dateRange.startDate) &&
               Objects.equals(endDate, dateRange.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate);
    }

    @Override
    public String toString() {
        return format();
    }
}
