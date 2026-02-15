package com.sism.service;

import com.sism.dto.WarnLevelCreateRequest;
import com.sism.dto.WarnLevelUpdateRequest;
import com.sism.entity.WarnLevel;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.WarnLevelRepository;
import com.sism.vo.WarnLevelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for warning level management
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarnLevelService {

    private final WarnLevelRepository warnLevelRepository;

    /**
     * Get all warning levels
     */
    public List<WarnLevelVO> getAllWarnLevels() {
        return warnLevelRepository.findAll().stream()
                .map(this::toWarnLevelVO)
                .collect(Collectors.toList());
    }

    /**
     * Get warning level by ID
     */
    public WarnLevelVO getWarnLevelById(Long id) {
        WarnLevel warnLevel = findWarnLevelById(id);
        return toWarnLevelVO(warnLevel);
    }

    /**
     * Get warning level by code
     */
    public WarnLevelVO getWarnLevelByCode(String levelCode) {
        WarnLevel warnLevel = warnLevelRepository.findByLevelCode(levelCode)
                .orElseThrow(() -> new ResourceNotFoundException("WarnLevel with code: " + levelCode));
        return toWarnLevelVO(warnLevel);
    }

    /**
     * Get active warning levels
     */
    public List<WarnLevelVO> getActiveWarnLevels() {
        return warnLevelRepository.findByIsActive(true).stream()
                .map(this::toWarnLevelVO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new warning level
     */
    @Transactional
    public WarnLevelVO createWarnLevel(WarnLevelCreateRequest request) {
        log.info("Creating warning level: {}", request.getLevelCode());

        // Check if level code already exists
        if (warnLevelRepository.findByLevelCode(request.getLevelCode()).isPresent()) {
            throw new BusinessException("Warning level with code '" + request.getLevelCode() + "' already exists");
        }

        WarnLevel warnLevel = new WarnLevel();
        warnLevel.setLevelName(request.getLevelName());
        warnLevel.setLevelCode(request.getLevelCode());
        warnLevel.setThresholdValue(request.getThresholdValue());
        warnLevel.setSeverity(request.getSeverity());
        warnLevel.setDescription(request.getDescription());
        warnLevel.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        warnLevel.setCreatedAt(LocalDateTime.now());
        warnLevel.setUpdatedAt(LocalDateTime.now());

        WarnLevel savedWarnLevel = warnLevelRepository.save(warnLevel);
        log.info("Successfully created warning level with ID: {}", savedWarnLevel.getId());

        return toWarnLevelVO(savedWarnLevel);
    }

    /**
     * Update an existing warning level
     */
    @Transactional
    public WarnLevelVO updateWarnLevel(Long id, WarnLevelUpdateRequest request) {
        WarnLevel warnLevel = findWarnLevelById(id);

        if (request.getLevelName() != null) {
            warnLevel.setLevelName(request.getLevelName());
        }
        if (request.getThresholdValue() != null) {
            warnLevel.setThresholdValue(request.getThresholdValue());
        }
        if (request.getSeverity() != null) {
            warnLevel.setSeverity(request.getSeverity());
        }
        if (request.getDescription() != null) {
            warnLevel.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            warnLevel.setIsActive(request.getIsActive());
        }

        warnLevel.setUpdatedAt(LocalDateTime.now());
        WarnLevel updatedWarnLevel = warnLevelRepository.save(warnLevel);

        return toWarnLevelVO(updatedWarnLevel);
    }

    /**
     * Delete a warning level
     */
    @Transactional
    public void deleteWarnLevel(Long id) {
        WarnLevel warnLevel = findWarnLevelById(id);
        warnLevelRepository.delete(warnLevel);
        log.info("Deleted warning level with ID: {}", id);
    }

    /**
     * Find warning level entity by ID
     */
    private WarnLevel findWarnLevelById(Long id) {
        return warnLevelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WarnLevel", id));
    }

    /**
     * Convert WarnLevel entity to VO
     */
    private WarnLevelVO toWarnLevelVO(WarnLevel warnLevel) {
        WarnLevelVO vo = new WarnLevelVO();
        vo.setId(warnLevel.getId());
        vo.setLevelName(warnLevel.getLevelName());
        vo.setLevelCode(warnLevel.getLevelCode());
        vo.setThresholdValue(warnLevel.getThresholdValue());
        vo.setSeverity(warnLevel.getSeverity());
        vo.setDescription(warnLevel.getDescription());
        vo.setIsActive(warnLevel.getIsActive());
        vo.setCreatedAt(warnLevel.getCreatedAt());
        vo.setUpdatedAt(warnLevel.getUpdatedAt());
        return vo;
    }
}
