package com.sism.execution.infrastructure.persistence;

import com.sism.execution.domain.model.plan.Plan;
import com.sism.execution.domain.model.plan.PlanLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Jpa Plan Repository Tests")
class JpaPlanRepositoryTest {

    @Mock
    private JpaPlanRepositoryInternal jpaRepository;

    @Test
    @DisplayName("Should use generic page query when status is blank")
    void shouldUseGenericPageQueryWhenStatusIsBlank() {
        JpaPlanRepository repository = new JpaPlanRepository(jpaRepository);
        PageRequest pageable = PageRequest.of(0, 20);
        Plan plan = Plan.create(2026L, 35L, 35L, PlanLevel.STRATEGIC);

        when(jpaRepository.findPage(eq(List.of(2026L)), eq(false), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(plan), pageable, 1));

        var result = repository.findPage(List.of(2026L), List.of(), pageable);

        assertEquals(1, result.getTotalElements());
        verify(jpaRepository).findPage(List.of(2026L), false, pageable);
        verify(jpaRepository, never()).findPageByStatus(any(), anyBoolean(), any(), any());
    }

    @Test
    @DisplayName("Should use status-specific page query when status is provided")
    void shouldUseStatusSpecificPageQueryWhenStatusIsProvided() {
        JpaPlanRepository repository = new JpaPlanRepository(jpaRepository);
        PageRequest pageable = PageRequest.of(0, 20);
        Plan plan = Plan.create(2026L, 35L, 35L, PlanLevel.STRATEGIC);

        when(jpaRepository.findPageByStatus(eq(List.of(2026L)), eq(false), eq(List.of("DISTRIBUTED", "APPROVED")), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(plan), pageable, 1));

        var result = repository.findPage(List.of(2026L), List.of("DISTRIBUTED", "APPROVED"), pageable);

        assertEquals(1, result.getTotalElements());
        verify(jpaRepository).findPageByStatus(List.of(2026L), false, List.of("DISTRIBUTED", "APPROVED"), pageable);
        verify(jpaRepository, never()).findPage(any(), anyBoolean(), any());
    }

    @Test
    @DisplayName("Should skip cycle filter when cycle ids are empty")
    void shouldSkipCycleFilterWhenCycleIdsAreEmpty() {
        JpaPlanRepository repository = new JpaPlanRepository(jpaRepository);
        PageRequest pageable = PageRequest.of(0, 20);

        when(jpaRepository.findPage(eq(List.of()), eq(true), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        var result = repository.findPage(List.of(), List.of(), pageable);

        assertEquals(0, result.getTotalElements());
        verify(jpaRepository).findPage(List.of(), true, pageable);
    }
}
