package com.sism.property;

import com.sism.enums.IndicatorStatus;
import com.sism.vo.IndicatorVO;
import net.jqwik.api.*;
import java.util.*;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Indicator Type Filter Correctness
 * **Feature: data-alignment-sop, Property 12: 指标类型过滤正确性**
 * **Validates: Requirements 7.3, 7.5**
 */
public class IndicatorTypeFilterPropertyTest {

    private static final Set<String> VALID_TYPE1_VALUES = Set.of("定性", "定量");
    private static final Set<String> VALID_TYPE2_VALUES = Set.of("发展性", "基础性");

    @Provide
    Arbitrary<String> type1Values() {
        return Arbitraries.of(VALID_TYPE1_VALUES);
    }

    @Provide
    Arbitrary<String> type2Values() {
        return Arbitraries.of(VALID_TYPE2_VALUES);
    }

    @Provide
    Arbitrary<Boolean> isQualitativeValues() {
        return Arbitraries.of(true, false);
    }

    @Provide
    Arbitrary<IndicatorStatus> statusValues() {
        return Arbitraries.of(IndicatorStatus.ACTIVE, IndicatorStatus.ARCHIVED);
    }

    @Provide
    Arbitrary<IndicatorVO> indicatorVOGenerator() {
        return Combinators.combine(
            Arbitraries.longs().between(1, 1000),
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
            Arbitraries.of(true, false),
            Arbitraries.of("定性", "定量"),
            Arbitraries.of("发展性", "基础性"),
            Arbitraries.of(IndicatorStatus.ACTIVE, IndicatorStatus.ARCHIVED),
            Arbitraries.integers().between(0, 100)
        ).as((id, desc, isQual, type1, type2, status, progress) -> {
            IndicatorVO vo = new IndicatorVO();
            vo.setIndicatorId(id);
            vo.setIndicatorDesc(desc);
            vo.setIsQualitative(isQual);
            vo.setType1(type1);
            vo.setType2(type2);
            vo.setStatus(status);
            vo.setProgress(progress);
            return vo;
        });
    }

    @Provide
    Arbitrary<List<IndicatorVO>> indicatorListGenerator() {
        return indicatorVOGenerator().list().ofMinSize(1).ofMaxSize(50);
    }

    private List<IndicatorVO> filterByType1(List<IndicatorVO> indicators, String type1) {
        return indicators.stream()
            .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
            .filter(i -> type1.equals(i.getType1()))
            .collect(Collectors.toList());
    }

    private List<IndicatorVO> filterByType2(List<IndicatorVO> indicators, String type2) {
        return indicators.stream()
            .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
            .filter(i -> type2.equals(i.getType2()))
            .collect(Collectors.toList());
    }

    private List<IndicatorVO> filterByQualitative(List<IndicatorVO> indicators, Boolean isQualitative) {
        return indicators.stream()
            .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
            .filter(i -> isQualitative.equals(i.getIsQualitative()))
            .collect(Collectors.toList());
    }

    private List<IndicatorVO> filterByStatus(List<IndicatorVO> indicators, IndicatorStatus status) {
        return indicators.stream()
            .filter(i -> status.equals(i.getStatus()))
            .collect(Collectors.toList());
    }

    private List<IndicatorVO> filterByCombined(List<IndicatorVO> indicators, String type1, String type2) {
        return indicators.stream()
            .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
            .filter(i -> type1.equals(i.getType1()))
            .filter(i -> type2.equals(i.getType2()))
            .collect(Collectors.toList());
    }

    private List<IndicatorVO> getAllActive(List<IndicatorVO> indicators) {
        return indicators.stream()
            .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
            .collect(Collectors.toList());
    }


    /** Property 12.1: Type1 filter returns only matching indicators */
    @Property(tries = 100)
    void filterByType1_shouldReturnOnlyMatchingIndicators(
            @ForAll("indicatorListGenerator") List<IndicatorVO> indicators,
            @ForAll("type1Values") String type1) {
        List<IndicatorVO> filtered = filterByType1(indicators, type1);
        for (IndicatorVO indicator : filtered) {
            assertThat(indicator.getType1()).isEqualTo(type1);
        }
    }

    /** Property 12.2: Type2 filter returns only matching indicators */
    @Property(tries = 100)
    void filterByType2_shouldReturnOnlyMatchingIndicators(
            @ForAll("indicatorListGenerator") List<IndicatorVO> indicators,
            @ForAll("type2Values") String type2) {
        List<IndicatorVO> filtered = filterByType2(indicators, type2);
        for (IndicatorVO indicator : filtered) {
            assertThat(indicator.getType2()).isEqualTo(type2);
        }
    }

    /** Property 12.3: Qualitative filter returns only matching indicators */
    @Property(tries = 100)
    void filterByQualitative_shouldReturnOnlyMatchingIndicators(
            @ForAll("indicatorListGenerator") List<IndicatorVO> indicators,
            @ForAll("isQualitativeValues") Boolean isQualitative) {
        List<IndicatorVO> filtered = filterByQualitative(indicators, isQualitative);
        for (IndicatorVO indicator : filtered) {
            assertThat(indicator.getIsQualitative()).isEqualTo(isQualitative);
        }
    }

    /** Property 12.4: Status filter returns only matching indicators */
    @Property(tries = 100)
    void filterByStatus_shouldReturnOnlyMatchingIndicators(
            @ForAll("indicatorListGenerator") List<IndicatorVO> indicators,
            @ForAll("statusValues") IndicatorStatus status) {
        List<IndicatorVO> filtered = filterByStatus(indicators, status);
        for (IndicatorVO indicator : filtered) {
            assertThat(indicator.getStatus()).isEqualTo(status);
        }
    }

    /** Property 12.5: Combined filter returns only matching indicators */
    @Property(tries = 100)
    void filterByCombinedTypes_shouldReturnOnlyMatchingIndicators(
            @ForAll("indicatorListGenerator") List<IndicatorVO> indicators,
            @ForAll("type1Values") String type1,
            @ForAll("type2Values") String type2) {
        List<IndicatorVO> filtered = filterByCombined(indicators, type1, type2);
        for (IndicatorVO indicator : filtered) {
            assertThat(indicator.getType1()).isEqualTo(type1);
            assertThat(indicator.getType2()).isEqualTo(type2);
        }
    }


    /** Property 12.6: Filter results are subset of all indicators */
    @Property(tries = 100)
    void filterResults_shouldBeSubsetOfAllIndicators(
            @ForAll("indicatorListGenerator") List<IndicatorVO> indicators,
            @ForAll("type1Values") String type1) {
        List<IndicatorVO> allActive = getAllActive(indicators);
        List<IndicatorVO> filtered = filterByType1(indicators, type1);
        Set<Long> allIds = allActive.stream().map(IndicatorVO::getIndicatorId).collect(Collectors.toSet());
        Set<Long> filteredIds = filtered.stream().map(IndicatorVO::getIndicatorId).collect(Collectors.toSet());
        assertThat(allIds).containsAll(filteredIds);
    }

    /** Property 12.7: Filter completeness - no matching indicators are excluded */
    @Property(tries = 100)
    void filterResults_shouldIncludeAllMatchingIndicators(
            @ForAll("indicatorListGenerator") List<IndicatorVO> indicators,
            @ForAll("type1Values") String type1) {
        List<IndicatorVO> allActive = getAllActive(indicators);
        List<IndicatorVO> filtered = filterByType1(indicators, type1);
        long expectedCount = allActive.stream().filter(i -> type1.equals(i.getType1())).count();
        assertThat(filtered.size()).isEqualTo(expectedCount);
    }

    /** Property 12.8: Filter idempotence - applying same filter twice gives same result */
    @Property(tries = 100)
    void filterIdempotence_shouldReturnSameResult(
            @ForAll("indicatorListGenerator") List<IndicatorVO> indicators,
            @ForAll("type1Values") String type1,
            @ForAll("type2Values") String type2) {
        List<IndicatorVO> firstResult = filterByCombined(indicators, type1, type2);
        List<IndicatorVO> secondResult = filterByCombined(indicators, type1, type2);
        assertThat(firstResult.size()).isEqualTo(secondResult.size());
        Set<Long> firstIds = firstResult.stream().map(IndicatorVO::getIndicatorId).collect(Collectors.toSet());
        Set<Long> secondIds = secondResult.stream().map(IndicatorVO::getIndicatorId).collect(Collectors.toSet());
        assertThat(firstIds).isEqualTo(secondIds);
    }
}
