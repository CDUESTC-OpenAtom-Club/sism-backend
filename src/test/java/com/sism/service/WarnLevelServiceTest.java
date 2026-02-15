package com.sism.service;

import com.sism.dto.WarnLevelCreateRequest;
import com.sism.dto.WarnLevelUpdateRequest;
import com.sism.entity.WarnLevel;
import com.sism.enums.AlertSeverity;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.WarnLevelRepository;
import com.sism.vo.WarnLevelVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WarnLevelService
 */
@ExtendWith(MockitoExtension.class)
class WarnLevelServiceTest {

    @Mock
    private WarnLevelRepository warnLevelRepository;

    @InjectMocks
    private WarnLevelService warnLevelService;

    private WarnLevel testWarnLevel;

    @BeforeEach
    void setUp() {
        testWarnLevel = new WarnLevel();
        testWarnLevel.setId(1L);
        testWarnLevel.setLevelName("Critical Alert");
        testWarnLevel.setLevelCode("CRITICAL");
        testWarnLevel.setThresholdValue(80);
        testWarnLevel.setSeverity(AlertSeverity.CRITICAL);
        testWarnLevel.setDescription("Critical severity alert");
        testWarnLevel.setIsActive(true);
        testWarnLevel.setCreatedAt(LocalDateTime.now());
        testWarnLevel.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getAllWarnLevels_ShouldReturnAllLevels() {
        // Arrange
        WarnLevel level2 = new WarnLevel();
        level2.setId(2L);
        level2.setLevelName("Warning Alert");
        level2.setLevelCode("WARNING");
        level2.setThresholdValue(50);
        level2.setSeverity(AlertSeverity.WARNING);
        level2.setIsActive(true);
        level2.setCreatedAt(LocalDateTime.now());
        level2.setUpdatedAt(LocalDateTime.now());

        when(warnLevelRepository.findAll()).thenReturn(Arrays.asList(testWarnLevel, level2));

        // Act
        List<WarnLevelVO> result = warnLevelService.getAllWarnLevels();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLevelCode()).isEqualTo("CRITICAL");
        assertThat(result.get(1).getLevelCode()).isEqualTo("WARNING");
        verify(warnLevelRepository).findAll();
    }

    @Test
    void getWarnLevelById_WhenExists_ShouldReturnLevel() {
        // Arrange
        when(warnLevelRepository.findById(1L)).thenReturn(Optional.of(testWarnLevel));

        // Act
        WarnLevelVO result = warnLevelService.getWarnLevelById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getLevelCode()).isEqualTo("CRITICAL");
        assertThat(result.getThresholdValue()).isEqualTo(80);
        verify(warnLevelRepository).findById(1L);
    }

    @Test
    void getWarnLevelById_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(warnLevelRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> warnLevelService.getWarnLevelById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(warnLevelRepository).findById(999L);
    }

    @Test
    void getWarnLevelByCode_WhenExists_ShouldReturnLevel() {
        // Arrange
        when(warnLevelRepository.findByLevelCode("CRITICAL")).thenReturn(Optional.of(testWarnLevel));

        // Act
        WarnLevelVO result = warnLevelService.getWarnLevelByCode("CRITICAL");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLevelCode()).isEqualTo("CRITICAL");
        verify(warnLevelRepository).findByLevelCode("CRITICAL");
    }

    @Test
    void getWarnLevelByCode_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(warnLevelRepository.findByLevelCode("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> warnLevelService.getWarnLevelByCode("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(warnLevelRepository).findByLevelCode("INVALID");
    }

    @Test
    void getActiveWarnLevels_ShouldReturnOnlyActiveLevels() {
        // Arrange
        when(warnLevelRepository.findByIsActive(true)).thenReturn(Arrays.asList(testWarnLevel));

        // Act
        List<WarnLevelVO> result = warnLevelService.getActiveWarnLevels();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isTrue();
        verify(warnLevelRepository).findByIsActive(true);
    }

    @Test
    void createWarnLevel_WithValidData_ShouldCreateSuccessfully() {
        // Arrange
        WarnLevelCreateRequest request = new WarnLevelCreateRequest(
                "Info Alert",
                "INFO",
                90,
                AlertSeverity.INFO,
                "Info severity alert",
                true
        );

        when(warnLevelRepository.findByLevelCode("INFO")).thenReturn(Optional.empty());
        when(warnLevelRepository.save(any(WarnLevel.class))).thenReturn(testWarnLevel);

        // Act
        WarnLevelVO result = warnLevelService.createWarnLevel(request);

        // Assert
        assertThat(result).isNotNull();
        verify(warnLevelRepository).findByLevelCode("INFO");
        verify(warnLevelRepository).save(any(WarnLevel.class));
    }

    @Test
    void createWarnLevel_WithDuplicateCode_ShouldThrowException() {
        // Arrange
        WarnLevelCreateRequest request = new WarnLevelCreateRequest(
                "Critical Alert",
                "CRITICAL",
                80,
                AlertSeverity.CRITICAL,
                "Duplicate code",
                true
        );

        when(warnLevelRepository.findByLevelCode("CRITICAL")).thenReturn(Optional.of(testWarnLevel));

        // Act & Assert
        assertThatThrownBy(() -> warnLevelService.createWarnLevel(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
        verify(warnLevelRepository).findByLevelCode("CRITICAL");
        verify(warnLevelRepository, never()).save(any());
    }

    @Test
    void updateWarnLevel_WithValidData_ShouldUpdateSuccessfully() {
        // Arrange
        WarnLevelUpdateRequest request = new WarnLevelUpdateRequest(
                "Updated Critical Alert",
                85,
                AlertSeverity.WARNING,
                "Updated description",
                false
        );

        when(warnLevelRepository.findById(1L)).thenReturn(Optional.of(testWarnLevel));
        when(warnLevelRepository.save(any(WarnLevel.class))).thenReturn(testWarnLevel);

        // Act
        WarnLevelVO result = warnLevelService.updateWarnLevel(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(warnLevelRepository).findById(1L);
        verify(warnLevelRepository).save(any(WarnLevel.class));
    }

    @Test
    void updateWarnLevel_WhenNotExists_ShouldThrowException() {
        // Arrange
        WarnLevelUpdateRequest request = new WarnLevelUpdateRequest();
        when(warnLevelRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> warnLevelService.updateWarnLevel(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(warnLevelRepository).findById(999L);
        verify(warnLevelRepository, never()).save(any());
    }

    @Test
    void updateWarnLevel_WithPartialData_ShouldUpdateOnlyProvidedFields() {
        // Arrange
        WarnLevelUpdateRequest request = new WarnLevelUpdateRequest();
        request.setLevelName("Partially Updated");

        when(warnLevelRepository.findById(1L)).thenReturn(Optional.of(testWarnLevel));
        when(warnLevelRepository.save(any(WarnLevel.class))).thenReturn(testWarnLevel);

        // Act
        WarnLevelVO result = warnLevelService.updateWarnLevel(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(warnLevelRepository).findById(1L);
        verify(warnLevelRepository).save(any(WarnLevel.class));
    }

    @Test
    void deleteWarnLevel_WhenExists_ShouldDeleteSuccessfully() {
        // Arrange
        when(warnLevelRepository.findById(1L)).thenReturn(Optional.of(testWarnLevel));
        doNothing().when(warnLevelRepository).delete(testWarnLevel);

        // Act
        warnLevelService.deleteWarnLevel(1L);

        // Assert
        verify(warnLevelRepository).findById(1L);
        verify(warnLevelRepository).delete(testWarnLevel);
    }

    @Test
    void deleteWarnLevel_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(warnLevelRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> warnLevelService.deleteWarnLevel(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(warnLevelRepository).findById(999L);
        verify(warnLevelRepository, never()).delete(any());
    }
}
