package com.sism.strategy.application;

import com.sism.strategy.domain.cycle.Cycle;
import com.sism.strategy.domain.repository.CycleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cycle Application Service Tests")
class CycleApplicationServiceTest {

    @Mock
    private CycleRepository cycleRepository;

    @Test
    @DisplayName("Should throw when cycle does not exist")
    void shouldThrowWhenCycleMissing() {
        CycleApplicationService service = new CycleApplicationService(cycleRepository);
        when(cycleRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getCycleById(404L));
    }
}
