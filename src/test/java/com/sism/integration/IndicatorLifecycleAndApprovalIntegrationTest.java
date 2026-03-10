package com.sism.integration;

import com.sism.dto.IndicatorUpdateRequest;
import com.sism.entity.Indicator;
import com.sism.entity.StrategicTask;
import com.sism.entity.SysOrg;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.OrgType;
import com.sism.enums.ProgressApprovalStatus;
import com.sism.enums.TaskType;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.TaskRepository;
import com.sism.repository.SysOrgRepository;
import com.sism.service.IndicatorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 指标生命周期和进度审批流程全栈集成测试
 * 
 * 验证文档 docs/indicator-status-bug-fix-plan.md 中定义的两个独立流程：
 * 1. 指标生命周期流程：草稿 → 待审核 → 已下发
 * 2. 进度审批流程：已下发 + 填写进度 → 待审批 → 已下发 + 更新进度
 * 
 * 关键原则：
 * - 状态独立：指标生命周期状态与进度审批状态完全独立
 * - 审批不改变生命周期：审批通过/驳回不改变指标的 status 字段
 * - 下发前检查：有待审批进度时不允许下发
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class IndicatorLifecycleAndApprovalIntegrationTest {

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SysOrgRepository orgRepository;

    private StrategicTask testTask;
    private SysOrg testOrg;
    private Indicator testIndicator;

    @BeforeEach
    void setUp() {
        // 创建测试组织
        testOrg = new SysOrg();
        testOrg.setName("测试部门");
        testOrg.setType(OrgType.FUNCTIONAL_DEPT);
        testOrg.setIsActive(true);
        testOrg.setSortOrder(0);
        testOrg.setCreatedAt(LocalDateTime.now());
        testOrg.setUpdatedAt(LocalDateTime.now());
        testOrg = orgRepository.save(testOrg);

        // 创建测试任务
        testTask = new StrategicTask();
        testTask.setTaskName("测试战略任务");
        testTask.setTaskType(TaskType.KEY);
        testTask.setPlanId(1L);
        testTask.setCycleId(1L);
        testTask.setOrg(testOrg);
        testTask.setCreatedByOrg(testOrg);
        testTask.setSortOrder(0);
        testTask.setIsDeleted(false);
        testTask.setCreatedAt(LocalDateTime.now());
        testTask.setUpdatedAt(LocalDateTime.now());
        testTask = taskRepository.save(testTask);

        // 创建测试指标（草稿状态）
        testIndicator = Indicator.builder()
                .taskId(testTask.getTaskId())
                .indicatorDesc("测试指标")
                .ownerOrg(testOrg)
                .targetOrg(testOrg)
                .level(IndicatorLevel.PRIMARY)
                .weightPercent(BigDecimal.TEN)
                .sortOrder(1)
                .type("quantitative")
                .status(IndicatorStatus.DRAFT)
                .progressApprovalStatus(ProgressApprovalStatus.NONE)
                .progress(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testIndicator = indicatorRepository.save(testIndicator);
    }

    @AfterEach
    void tearDown() {
        if (testIndicator != null) {
            indicatorRepository.deleteById(testIndicator.getIndicatorId());
        }
        if (testTask != null) {
            taskRepository.deleteById(testTask.getTaskId());
        }
        if (testOrg != null) {
            orgRepository.deleteById(testOrg.getId());
        }
    }

    /**
     * 场景 1：正常指标生命周期流程
     * 
     * 验证：草稿 → 待审核 → 已下发
     */
    @Test
    @DisplayName("场景1：指标生命周期流程 - 草稿 → 待审核 → 已下发")
    void testIndicatorLifecycleFlow() {
        // 1. 初始状态：草稿
        assertThat(testIndicator.getStatus()).isEqualTo(IndicatorStatus.DRAFT);
        assertThat(testIndicator.getProgressApprovalStatus()).isEqualTo(ProgressApprovalStatus.NONE);

        // 2. 提交审核：草稿 → 待审核
        testIndicator.setStatus(IndicatorStatus.PENDING);
        testIndicator = indicatorRepository.save(testIndicator);

        Indicator afterSubmit = indicatorRepository.findById(testIndicator.getIndicatorId()).orElseThrow();
        assertThat(afterSubmit.getStatus()).isEqualTo(IndicatorStatus.PENDING);
        assertThat(afterSubmit.getProgressApprovalStatus()).isEqualTo(ProgressApprovalStatus.NONE); // 进度审批状态不变

        // 3. 审核通过：待审核 → 已下发
        testIndicator.setStatus(IndicatorStatus.DISTRIBUTED);
        testIndicator = indicatorRepository.save(testIndicator);

        Indicator afterDistribute = indicatorRepository.findById(testIndicator.getIndicatorId()).orElseThrow();
        assertThat(afterDistribute.getStatus()).isEqualTo(IndicatorStatus.DISTRIBUTED);
        assertThat(afterDistribute.getProgressApprovalStatus()).isEqualTo(ProgressApprovalStatus.NONE); // 进度审批状态仍然不变
    }

    /**
     * 场景 2：进度提报流程
     * 
     * 验证：已下发 + 填写进度 → 待审批
     */
    @Test
    @DisplayName("场景2：进度提报流程 - 已下发 + 填写进度 → 待审批")
    void testProgressSubmissionFlow() {
        // 前置条件：指标已下发
        testIndicator.setStatus(IndicatorStatus.DISTRIBUTED);
        testIndicator = indicatorRepository.save(testIndicator);

        // 1. 职能部门填写进度并提交
        IndicatorUpdateRequest submitProgressRequest = new IndicatorUpdateRequest();
        submitProgressRequest.setPendingProgress(50);
        submitProgressRequest.setPendingRemark("完成了一半的工作");
        submitProgressRequest.setProgressApprovalStatus("PENDING");
        
        indicatorService.updateIndicator(testIndicator.getIndicatorId(), submitProgressRequest);

        Indicator afterSubmit = indicatorRepository.findById(testIndicator.getIndicatorId()).orElseThrow();
        
        // 验证：指标状态保持已下发
        assertThat(afterSubmit.getStatus()).isEqualTo(IndicatorStatus.DISTRIBUTED);
        
        // 验证：进度审批状态变为待审批
        assertThat(afterSubmit.getProgressApprovalStatus()).isEqualTo(ProgressApprovalStatus.PENDING);
        
        // 验证：待审批进度和备注已保存
        assertThat(afterSubmit.getPendingProgress()).isEqualTo(50);
        assertThat(afterSubmit.getPendingRemark()).isEqualTo("完成了一半的工作");
        
        // 验证：实际进度尚未更新（仍为0）
        assertThat(afterSubmit.getProgress()).isEqualTo(0);
    }

    /**
     * 场景 3：审批通过流程
     * 
     * 验证：待审批 → 已下发 + 更新进度
     */
    @Test
    @DisplayName("场景3：审批通过流程 - 待审批 → 已下发 + 更新进度")
    void testApprovalApprovedFlow() {
        // 前置条件：指标已下发且有待审批进度
        testIndicator.setStatus(IndicatorStatus.DISTRIBUTED);
        testIndicator.setProgressApprovalStatus(ProgressApprovalStatus.PENDING);
        testIndicator.setPendingProgress(50);
        testIndicator.setPendingRemark("完成了一半的工作");
        testIndicator.setProgress(0); // 当前进度为0
        testIndicator = indicatorRepository.save(testIndicator);

        // 1. 战略发展部审批通过
        IndicatorUpdateRequest approveRequest = new IndicatorUpdateRequest();
        approveRequest.setProgressApprovalStatus("APPROVED");
        approveRequest.setProgress(50); // 将待审批进度更新为实际进度
        
        indicatorService.updateIndicator(testIndicator.getIndicatorId(), approveRequest);

        Indicator afterApprove = indicatorRepository.findById(testIndicator.getIndicatorId()).orElseThrow();
        
        // 验证：指标状态保持已下发（不变）
        assertThat(afterApprove.getStatus()).isEqualTo(IndicatorStatus.DISTRIBUTED);
        
        // 验证：进度审批状态变为已批准
        assertThat(afterApprove.getProgressApprovalStatus()).isEqualTo(ProgressApprovalStatus.APPROVED);
        
        // 验证：实际进度已更新
        assertThat(afterApprove.getProgress()).isEqualTo(50);
        
        // 验证：待审批字段应该被清空（根据业务逻辑）
        // 注意：这取决于后端实现，可能需要在审批通过时显式清空
    }

    /**
     * 场景 4：审批驳回流程
     * 
     * 验证：待审批 → 已下发 + 保持原进度
     */
    @Test
    @DisplayName("场景4：审批驳回流程 - 待审批 → 已下发 + 保持原进度")
    void testApprovalRejectedFlow() {
        // 前置条件：指标已下发且有待审批进度
        testIndicator.setStatus(IndicatorStatus.DISTRIBUTED);
        testIndicator.setProgressApprovalStatus(ProgressApprovalStatus.PENDING);
        testIndicator.setPendingProgress(50);
        testIndicator.setPendingRemark("完成了一半的工作");
        testIndicator.setProgress(30); // 当前进度为30%
        testIndicator = indicatorRepository.save(testIndicator);

        // 1. 战略发展部驳回
        IndicatorUpdateRequest rejectRequest = new IndicatorUpdateRequest();
        rejectRequest.setProgressApprovalStatus("REJECTED");
        // 注意：驳回时不更新 progress 字段
        
        indicatorService.updateIndicator(testIndicator.getIndicatorId(), rejectRequest);

        Indicator afterReject = indicatorRepository.findById(testIndicator.getIndicatorId()).orElseThrow();
        
        // 验证：指标状态保持已下发（不变）
        assertThat(afterReject.getStatus()).isEqualTo(IndicatorStatus.DISTRIBUTED);
        
        // 验证：进度审批状态变为已驳回
        assertThat(afterReject.getProgressApprovalStatus()).isEqualTo(ProgressApprovalStatus.REJECTED);
        
        // 验证：实际进度保持原值（30%）
        assertThat(afterReject.getProgress()).isEqualTo(30);
    }

    /**
     * 场景 5：状态独立性验证
     * 
     * 验证：指标生命周期状态与进度审批状态完全独立
     */
    @Test
    @DisplayName("场景5：状态独立性 - 生命周期状态与审批状态互不影响")
    void testStatusIndependence() {
        // 1. 指标在草稿状态时，可以有进度审批状态（虽然不常见）
        testIndicator.setStatus(IndicatorStatus.DRAFT);
        testIndicator.setProgressApprovalStatus(ProgressApprovalStatus.PENDING);
        testIndicator = indicatorRepository.save(testIndicator);

        Indicator saved = indicatorRepository.findById(testIndicator.getIndicatorId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IndicatorStatus.DRAFT);
        assertThat(saved.getProgressApprovalStatus()).isEqualTo(ProgressApprovalStatus.PENDING);

        // 2. 更新生命周期状态不影响进度审批状态
        testIndicator.setStatus(IndicatorStatus.PENDING);
        testIndicator = indicatorRepository.save(testIndicator);

        Indicator afterLifecycleUpdate = indicatorRepository.findById(testIndicator.getIndicatorId()).orElseThrow();
        assertThat(afterLifecycleUpdate.getStatus()).isEqualTo(IndicatorStatus.PENDING);
        assertThat(afterLifecycleUpdate.getProgressApprovalStatus()).isEqualTo(ProgressApprovalStatus.PENDING); // 不变

        // 3. 更新进度审批状态不影响生命周期状态
        IndicatorUpdateRequest updateApproval = new IndicatorUpdateRequest();
        updateApproval.setProgressApprovalStatus("APPROVED");
        indicatorService.updateIndicator(testIndicator.getIndicatorId(), updateApproval);

        Indicator afterApprovalUpdate = indicatorRepository.findById(testIndicator.getIndicatorId()).orElseThrow();
        assertThat(afterApprovalUpdate.getStatus()).isEqualTo(IndicatorStatus.PENDING); // 不变
        assertThat(afterApprovalUpdate.getProgressApprovalStatus()).isEqualTo(ProgressApprovalStatus.APPROVED);
    }

    /**
     * 场景 6：完整的端到端流程
     * 
     * 验证：从创建到多次进度审批的完整流程
     */
    @Test
    @DisplayName("场景6：完整端到端流程 - 创建 → 下发 → 多次进度审批")
    void testCompleteEndToEndFlow() {
        // 1. 草稿 → 待审核 → 已下发
        testIndicator.setStatus(IndicatorStatus.DRAFT);
        testIndicator = indicatorRepository.save(testIndicator);

        testIndicator.setStatus(IndicatorStatus.PENDING);
        testIndicator = indicatorRepository.save(testIndicator);

        testIndicator.setStatus(IndicatorStatus.DISTRIBUTED);
        testIndicator = indicatorRepository.save(testIndicator);

        Indicator afterDistribute = indicatorRepository.findById(testIndicator.getIndicatorId()).orElseThrow();
        assertThat(afterDistribute.getStatus()).isEqualTo(IndicatorStatus.DISTRIBUTED);

        // 2. 第一次进度提报：0% → 30%
        IndicatorUpdateRequest submitProgress1 = new IndicatorUpdateRequest();
        submitProgress1.setPendingProgress(30);
        submitProgress1.setProgressApprovalStatus("PENDING");
        indicatorService.updateIndicator(testIndicator.getIndicatorId(), submitProgress1);

        // 3. 审批通过
        IndicatorUpdateRequest approve1 = new IndicatorUpdateRequest();
        approve1.setProgressApprovalStatus("APPROVED");
        approve1.setProgress(30);
        indicatorService.updateIndicator(testIndicator.getIndicatorId(), approve1);

        Indicator afterApprove1 = indicatorRepository.findById(testIndicator.getIndicatorId()).orElseThrow();
        assertThat(afterApprove1.getStatus()).isEqualTo(IndicatorStatus.DISTRIBUTED); // 仍然是已下发
        assertThat(afterApprove1.getProgress()).isEqualTo(30);

        // 4. 第二次进度提报：30% → 60%
        IndicatorUpdateRequest submitProgress2 = new IndicatorUpdateRequest();
        submitProgress2.setPendingProgress(60);
        submitProgress2.setProgressApprovalStatus("PENDING");
        indicatorService.updateIndicator(testIndicator.getIndicatorId(), submitProgress2);

        // 5. 审批驳回
        IndicatorUpdateRequest reject = new IndicatorUpdateRequest();
        reject.setProgressApprovalStatus("REJECTED");
        indicatorService.updateIndicator(testIndicator.getIndicatorId(), reject);

        Indicator afterReject = indicatorRepository.findById(testIndicator.getIndicatorId()).orElseThrow();
        assertThat(afterReject.getStatus()).isEqualTo(IndicatorStatus.DISTRIBUTED); // 仍然是已下发
        assertThat(afterReject.getProgress()).isEqualTo(30); // 保持原进度

        // 6. 第三次进度提报：30% → 50%
        IndicatorUpdateRequest submitProgress3 = new IndicatorUpdateRequest();
        submitProgress3.setPendingProgress(50);
        submitProgress3.setProgressApprovalStatus("PENDING");
        indicatorService.updateIndicator(testIndicator.getIndicatorId(), submitProgress3);

        // 7. 审批通过
        IndicatorUpdateRequest approve2 = new IndicatorUpdateRequest();
        approve2.setProgressApprovalStatus("APPROVED");
        approve2.setProgress(50);
        indicatorService.updateIndicator(testIndicator.getIndicatorId(), approve2);

        Indicator finalState = indicatorRepository.findById(testIndicator.getIndicatorId()).orElseThrow();
        assertThat(finalState.getStatus()).isEqualTo(IndicatorStatus.DISTRIBUTED); // 始终是已下发
        assertThat(finalState.getProgress()).isEqualTo(50); // 最终进度50%
        assertThat(finalState.getProgressApprovalStatus()).isEqualTo(ProgressApprovalStatus.APPROVED);
    }
}
