package com.sism.strategy.infrastructure.persistence;

import com.sism.strategy.domain.indicator.Indicator;
import com.sism.strategy.domain.indicator.IndicatorStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageRequest;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Jpa Indicator Repository Tests")
class JpaIndicatorRepositoryTest {

    @Mock
    private JpaIndicatorRepositoryInternal jpaRepository;

    @Test
    @DisplayName("Should delegate first level query to internal repository")
    void shouldDelegateFirstLevelQuery() {
        Indicator indicator = new Indicator();
        when(jpaRepository.findFirstLevelIndicators(com.sism.organization.domain.OrgType.functional))
                .thenReturn(List.of(indicator));

        JpaIndicatorRepository repository = new JpaIndicatorRepository(jpaRepository);

        assertEquals(1, repository.findFirstLevelIndicators().size());
        verify(jpaRepository).findFirstLevelIndicators(com.sism.organization.domain.OrgType.functional);
        verify(jpaRepository, never()).findAllByIsDeletedFalse();
    }

    @Test
    @DisplayName("Should delegate keyword search to internal repository")
    void shouldDelegateKeywordSearch() {
        Indicator indicator = new Indicator();
        when(jpaRepository.findByIndicatorDescContainingIgnoreCaseAndIsDeletedFalse("关键字"))
                .thenReturn(List.of(indicator));

        JpaIndicatorRepository repository = new JpaIndicatorRepository(jpaRepository);

        assertEquals(1, repository.findByKeyword("关键字").size());
        verify(jpaRepository).findByIndicatorDescContainingIgnoreCaseAndIsDeletedFalse("关键字");
        verify(jpaRepository, never()).findAllByIsDeletedFalse();
    }

    @Test
    @DisplayName("Should normalize status string before delegating status query")
    void shouldNormalizeStatusBeforeDelegating() {
        Indicator indicator = new Indicator();
        when(jpaRepository.findByStatusAndIsDeletedFalse(IndicatorStatus.DISTRIBUTED))
                .thenReturn(List.of(indicator));

        JpaIndicatorRepository repository = new JpaIndicatorRepository(jpaRepository);

        assertEquals(1, repository.findByStatus("distributed").size());
        verify(jpaRepository).findByStatusAndIsDeletedFalse(IndicatorStatus.DISTRIBUTED);
    }

    @Test
    @DisplayName("Should return empty page when status string is invalid")
    void shouldReturnEmptyPageWhenStatusInvalid() {
        JpaIndicatorRepository repository = new JpaIndicatorRepository(jpaRepository);

        assertTrue(repository.findByStatus("bad-status", PageRequest.of(0, 10)).isEmpty());
        verify(jpaRepository, never()).findByStatusAndIsDeletedFalse(any(IndicatorStatus.class), any());
    }
}
