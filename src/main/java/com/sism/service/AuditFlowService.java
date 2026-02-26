package com.sism.service;

import com.sism.dto.AuditFlowCreateRequest;
import com.sism.dto.AuditFlowUpdateRequest;
import com.sism.dto.AuditStepCreateRequest;
import com.sism.entity.AuditFlowDef;
import com.sism.entity.AuditStepDef;
import com.sism.enums.AuditEntityType;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.AuditFlowDefRepository;
import com.sism.repository.AuditStepDefRepository;
import com.sism.vo.AuditFlowVO;
import com.sism.vo.AuditStepVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for audit flow management
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditFlowService {

    private final AuditFlowDefRepository auditFlowDefRepository;
    private final AuditStepDefRepository auditStepDefRepository;

    /**
     * Get all audit flows
     */
    public List<AuditFlowVO> getAllAuditFlows() {
        return auditFlowDefRepository.findAll().stream()
                .map(this::toAuditFlowVO)
                .collect(Collectors.toList());
    }

    /**
     * Get audit flow by ID
     */
    public AuditFlowVO getAuditFlowById(Long id) {
        AuditFlowDef auditFlow = findAuditFlowById(id);
        return toAuditFlowVO(auditFlow);
    }

    /**
     * Get audit flow by code
     */
    public AuditFlowVO getAuditFlowByCode(String flowCode) {
        AuditFlowDef auditFlow = auditFlowDefRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new ResourceNotFoundException("AuditFlow with code: " + flowCode));
        return toAuditFlowVO(auditFlow);
    }

    /**
     * Get audit flows by entity type
     */
    public List<AuditFlowVO> getAuditFlowsByEntityType(AuditEntityType entityType) {
        return auditFlowDefRepository.findByEntityType(entityType).stream()
                .map(this::toAuditFlowVO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new audit flow
     */
    @Transactional
    public AuditFlowVO createAuditFlow(AuditFlowCreateRequest request) {
        log.info("Creating audit flow: {}", request.getFlowCode());

        // Check if flow code already exists
        if (auditFlowDefRepository.findByFlowCode(request.getFlowCode()).isPresent()) {
            throw new BusinessException("Audit flow with code '" + request.getFlowCode() + "' already exists");
        }

        AuditFlowDef auditFlow = new AuditFlowDef();
        auditFlow.setFlowName(request.getFlowName());
        auditFlow.setFlowCode(request.getFlowCode());
        auditFlow.setEntityType(request.getEntityType());
        auditFlow.setDescription(request.getDescription());
        auditFlow.setCreatedAt(LocalDateTime.now());
        auditFlow.setUpdatedAt(LocalDateTime.now());

        AuditFlowDef savedAuditFlow = auditFlowDefRepository.save(auditFlow);
        log.info("Successfully created audit flow with ID: {}", savedAuditFlow.getId());

        return toAuditFlowVO(savedAuditFlow);
    }

    /**
     * Update an existing audit flow
     */
    @Transactional
    public AuditFlowVO updateAuditFlow(Long id, AuditFlowUpdateRequest request) {
        AuditFlowDef auditFlow = findAuditFlowById(id);

        if (request.getFlowName() != null) {
            auditFlow.setFlowName(request.getFlowName());
        }
        if (request.getEntityType() != null) {
            auditFlow.setEntityType(request.getEntityType());
        }
        if (request.getDescription() != null) {
            auditFlow.setDescription(request.getDescription());
        }

        auditFlow.setUpdatedAt(LocalDateTime.now());
        AuditFlowDef updatedAuditFlow = auditFlowDefRepository.save(auditFlow);

        return toAuditFlowVO(updatedAuditFlow);
    }

    /**
     * Delete an audit flow
     */
    @Transactional
    public void deleteAuditFlow(Long id) {
        AuditFlowDef auditFlow = findAuditFlowById(id);
        
        // Delete all associated steps first
        List<AuditStepDef> steps = auditStepDefRepository.findByFlowIdOrderByStepOrderAsc(id);
        auditStepDefRepository.deleteAll(steps);
        
        auditFlowDefRepository.delete(auditFlow);
        log.info("Deleted audit flow with ID: {} and {} steps", id, steps.size());
    }

    /**
     * Add a step to an audit flow
     */
    @Transactional
    public AuditStepVO addAuditStep(AuditStepCreateRequest request) {
        log.info("Adding audit step to flow: {}", request.getFlowId());

        // Validate flow exists
        findAuditFlowById(request.getFlowId());

        // Check if step order already exists
        List<AuditStepDef> existingSteps = auditStepDefRepository.findByFlowIdOrderByStepOrderAsc(request.getFlowId());
        boolean orderExists = existingSteps.stream()
                .anyMatch(step -> step.getStepOrder().equals(request.getStepOrder()));
        
        if (orderExists) {
            throw new BusinessException("Step order " + request.getStepOrder() + " already exists in this flow");
        }

        AuditStepDef step = new AuditStepDef();
        step.setFlowId(request.getFlowId());
        step.setStepOrder(request.getStepOrder());
        step.setStepName(request.getStepName());
        step.setApproverRole(request.getApproverRoleId().toString());
        step.setIsRequired(true);
        step.setCreatedAt(LocalDateTime.now());
        step.setUpdatedAt(LocalDateTime.now());

        AuditStepDef savedStep = auditStepDefRepository.save(step);
        log.info("Successfully added audit step with ID: {}", savedStep.getId());

        return toAuditStepVO(savedStep);
    }

    /**
     * Get all steps for an audit flow
     */
    public List<AuditStepVO> getAuditStepsByFlowId(Long flowId) {
        return auditStepDefRepository.findByFlowIdOrderByStepOrderAsc(flowId).stream()
                .map(this::toAuditStepVO)
                .collect(Collectors.toList());
    }

    /**
     * Find audit flow entity by ID
     */
    private AuditFlowDef findAuditFlowById(Long id) {
        return auditFlowDefRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditFlow", id));
    }

    /**
     * Convert AuditFlowDef entity to VO
     */
    private AuditFlowVO toAuditFlowVO(AuditFlowDef auditFlow) {
        AuditFlowVO vo = new AuditFlowVO();
        vo.setId(auditFlow.getId());
        vo.setFlowName(auditFlow.getFlowName());
        vo.setFlowCode(auditFlow.getFlowCode());
        vo.setEntityType(auditFlow.getEntityType());
        vo.setDescription(auditFlow.getDescription());
        vo.setCreatedAt(auditFlow.getCreatedAt());
        vo.setUpdatedAt(auditFlow.getUpdatedAt());

        // Load steps
        List<AuditStepVO> steps = getAuditStepsByFlowId(auditFlow.getId());
        vo.setSteps(steps);

        return vo;
    }

    /**
     * Convert AuditStepDef entity to VO
     */
    private AuditStepVO toAuditStepVO(AuditStepDef step) {
        AuditStepVO vo = new AuditStepVO();
        vo.setId(step.getId());
        vo.setFlowId(step.getFlowId());
        vo.setStepOrder(step.getStepOrder());
        vo.setStepName(step.getStepName());
        vo.setApproverRoleId(Long.parseLong(step.getApproverRole()));
        vo.setApproverRoleName(step.getApproverRole());
        vo.setCreatedAt(step.getCreatedAt());
        vo.setUpdatedAt(step.getUpdatedAt());
        return vo;
    }
}
