package com.sism.workflow.domain.runtime.repository;

import com.sism.workflow.domain.AuditStatus;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AuditInstanceRepository {

    Optional<AuditInstance> findById(Long id);

    Optional<AuditInstance> findByStepInstanceId(Long stepInstanceId);

    List<AuditInstance> findAll();

    List<AuditInstance> findByBusinessTypeAndBusinessId(String businessType, Long businessId);

    List<AuditInstance> findByBusinessId(Long businessId);

    List<AuditInstance> findByStatus(AuditStatus status);

    List<AuditInstance> findByInitiatorId(Long initiatorId);

    AuditInstance save(AuditInstance auditInstance);

    void delete(AuditInstance auditInstance);

    boolean existsById(Long id);

    boolean hasActiveInstance(Long businessEntityId, String entityType);

    Page<AuditInstance> findByFlowDefId(Long flowDefId, Pageable pageable);

    long countAll();

    long countPending();

    long countApproved();

    long countRejected();
}
