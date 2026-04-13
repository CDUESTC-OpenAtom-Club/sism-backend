package com.sism.workflow.interfaces.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageResultTest {

    @Test
    void ofShouldClampInvalidPageSizeToOne() {
        PageResult<String> result = PageResult.of(List.of("a"), 3, 1, 0);

        assertEquals(1, result.getPageSize());
        assertEquals(3, result.getTotal());
        assertEquals(3, result.getTotalPages());
    }

    @Test
    void ofShouldReturnZeroTotalPagesForEmptyResult() {
        PageResult<String> result = PageResult.of(List.of(), 0, 1, 10);

        assertEquals(0, result.getTotalPages());
        assertEquals(10, result.getPageSize());
    }
}
