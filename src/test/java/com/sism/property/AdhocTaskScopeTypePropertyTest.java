package com.sism.property;

import com.sism.dto.AdhocTaskCreateRequest;
import com.sism.entity.*;
import com.sism.enums.AdhocScopeType;
import com.sism.enums.AdhocTaskStatus;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.repository.*;
import com.sism.service.AdhocTaskService;
import com.sism.vo.AdhocTaskVO;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Property-based tests for Adhoc Task Scope Type Handling
 * 
 * **Feature: sism-fullstack-integration, Property 13: Adhoc Task Scope Type Handling**
 * 
 * For any adhoc task creation:
 * - If scope_type is ALL_ORGS, target organizations SHALL be populated in adhoc_task_target table
 * - If scope_type is BY_DEPT_ISSUED_INDICATORS, the system SHALL automatically populate 
 *   adhoc_task_indicator_map with indicators where owner_org_id matches creator_org_id
 * - If scope_type is CUSTOM, the user-selected organizations/indicators SHALL be stored
 * 
 * **Validates: Requirements 10.1, 10.2, 10.3**
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class AdhocTaskScopeTypePropertyTest {

    @Autowired
    private AdhocTaskService adhocTaskService;

    @Autowired
    private AdhocTaskRepository adhocTaskRepository;

    @Autowired
    private AdhocTaskTargetRepository adhocTaskTargetRepository;

    @Autowired
    private AdhocTaskIndicatorMapRepository adhocTaskIndicatorMapRepository;

    @Autowired
    private AssessmentCycleRepository assessmentCycleRepository;

    @Autowired
    private SysOrgRepository orgRepository;

    @Autowired
    private IndicatorRepository indicatorRepository;

    // ==================== Helper Methods ====================

    /**
     * Get existing assessment cycles from the database.
     */
    private List<AssessmentCycle> getExistingCycles(int limit) {
        return assessmentCycleRepository.findAll().stream()
                .limit(limit)
                .toList();
    }

    /**
     * Get existing active organizations from the database.
     */
    private List<SysOrg> getExistingActiveOrgs(int limit) {
        return orgRepository.findByIsActiveTrue().stream()
                .limit(limit)
                .toList();
    }

    /**
     * Get existing active indicators from the database.
     */
    private List<Indicator> getExistingActiveIndicators(int limit) {
        return indicatorRepository.findByStatus(IndicatorStatus.ACTIVE).stream()
                .limit(limit)
                .toList();
    }

    /**
     * Get indicators issued by a specific organization.
     */
    private List<Indicator> getIndicatorsIssuedByOrg(Long ownerOrgId) {
        return indicatorRepository.findByOwnerOrgAndStatus(ownerOrgId, IndicatorStatus.ACTIVE);
    }

    /**
     * Create a basic adhoc task create request.
     */
    private AdhocTaskCreateRequest createBasicRequest(Long cycleId, Long creatorOrgId, 
                                                       AdhocScopeType scopeType) {
        AdhocTaskCreateRequest request = new AdhocTaskCreateRequest();
        request.setCycleId(cycleId);
        request.setCreatorOrgId(creatorOrgId);
        request.setScopeType(scopeType);
        request.setTaskTitle("Property Test Task " + System.currentTimeMillis());
        request.setTaskDesc("Property test adhoc task for scope type: " + scopeType);
        request.setOpenAt(LocalDate.now());
        request.setDueAt(LocalDate.now().plusDays(30));
        request.setIncludeInAlert(false);
        request.setRequireIndicatorReport(false);
        return request;
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<Integer> cycleIndices() {
        return Arbitraries.integers().between(0, 4);
    }

    @Provide
    Arbitrary<Integer> orgIndices() {
        return Arbitraries.integers().between(0, 9);
    }

    @Provide
    Arbitrary<List<Integer>> customOrgIndices() {
        return Arbitraries.integers().between(0, 9)
                .list().ofMinSize(1).ofMaxSize(5);
    }

    @Provide
    Arbitrary<List<Integer>> customIndicatorIndices() {
        return Arbitraries.integers().between(0, 9)
                .list().ofMinSize(1).ofMaxSize(5);
    }

    // ==================== Property Tests ====================


    /**
     * Property 13.1: ALL_ORGS scope type populates all active organizations
     * 
     * **Feature: sism-fullstack-integration, Property 13: Adhoc Task Scope Type Handling**
     * 
     * For any adhoc task with scope_type ALL_ORGS, the adhoc_task_target table SHALL
     * contain entries for all active organizations in the system.
     * 
     * **Validates: Requirements 10.2**
     */
    @Property(tries = 50)
    @Transactional
    void allOrgsScopeType_shouldPopulateAllActiveOrganizations(
            @ForAll("cycleIndices") Integer cycleIndex,
            @ForAll("orgIndices") Integer orgIndex) {

        // Get existing cycles and organizations
        List<AssessmentCycle> cycles = getExistingCycles(5);
        List<SysOrg> activeOrgs = getExistingActiveOrgs(10);

        // Skip if no cycles or organizations exist
        assumeThat(cycles).isNotEmpty();
        assumeThat(activeOrgs).isNotEmpty();

        // Select cycle and creator org based on indices
        AssessmentCycle cycle = cycles.get(cycleIndex % cycles.size());
        SysOrg creatorOrg = activeOrgs.get(orgIndex % activeOrgs.size());

        // Create adhoc task with ALL_ORGS scope type
        AdhocTaskCreateRequest request = createBasicRequest(
                cycle.getCycleId(), creatorOrg.getId(), AdhocScopeType.ALL_ORGS);

        // Create the adhoc task
        AdhocTaskVO createdTask = adhocTaskService.createAdhocTask(request);

        // Verify the task was created with correct scope type
        assertThat(createdTask).isNotNull();
        assertThat(createdTask.getScopeType()).isEqualTo(AdhocScopeType.ALL_ORGS);

        // Get all active organizations from the database
        List<SysOrg> allActiveOrgs = orgRepository.findByIsActiveTrue();

        // Get target organizations for the created task
        List<AdhocTaskTarget> targets = adhocTaskTargetRepository
                .findByAdhocTask_AdhocTaskId(createdTask.getAdhocTaskId());

        // Assert: Number of targets should equal number of active organizations
        assertThat(targets).hasSize(allActiveOrgs.size());

        // Assert: All active organizations should be in the targets
        List<Long> targetOrgIds = targets.stream()
                .map(t -> t.getTargetOrg().getId())
                .collect(Collectors.toList());
        List<Long> allActiveOrgIds = allActiveOrgs.stream()
                .map(SysOrg::getId)
                .collect(Collectors.toList());

        assertThat(targetOrgIds).containsExactlyInAnyOrderElementsOf(allActiveOrgIds);

        // Rollback will restore the original state due to @Transactional
    }

    /**
     * Property 13.2: BY_DEPT_ISSUED_INDICATORS scope type populates indicators by owner org
     * 
     * **Feature: sism-fullstack-integration, Property 13: Adhoc Task Scope Type Handling**
     * 
     * For any adhoc task with scope_type BY_DEPT_ISSUED_INDICATORS, the adhoc_task_indicator_map
     * table SHALL contain entries for indicators where owner_org_id matches creator_org_id.
     * 
     * **Validates: Requirements 10.3**
     */
    @Property(tries = 50)
    @Transactional
    void byDeptIssuedIndicatorsScopeType_shouldPopulateIndicatorsByOwnerOrg(
            @ForAll("cycleIndices") Integer cycleIndex,
            @ForAll("orgIndices") Integer orgIndex) {

        // Get existing cycles and organizations
        List<AssessmentCycle> cycles = getExistingCycles(5);
        List<SysOrg> activeOrgs = getExistingActiveOrgs(10);

        // Skip if no cycles or organizations exist
        assumeThat(cycles).isNotEmpty();
        assumeThat(activeOrgs).isNotEmpty();

        // Select cycle and creator org based on indices
        AssessmentCycle cycle = cycles.get(cycleIndex % cycles.size());
        SysOrg creatorOrg = activeOrgs.get(orgIndex % activeOrgs.size());

        // Create adhoc task with BY_DEPT_ISSUED_INDICATORS scope type
        AdhocTaskCreateRequest request = createBasicRequest(
                cycle.getCycleId(), creatorOrg.getId(), AdhocScopeType.BY_DEPT_ISSUED_INDICATORS);

        // Create the adhoc task
        AdhocTaskVO createdTask = adhocTaskService.createAdhocTask(request);

        // Verify the task was created with correct scope type
        assertThat(createdTask).isNotNull();
        assertThat(createdTask.getScopeType()).isEqualTo(AdhocScopeType.BY_DEPT_ISSUED_INDICATORS);

        // Get indicators issued by the creator organization
        List<Indicator> issuedIndicators = getIndicatorsIssuedByOrg(creatorOrg.getId());

        // Filter to secondary level indicators (FUNC_TO_COLLEGE) if any exist
        List<Indicator> secondaryIndicators = issuedIndicators.stream()
                .filter(ind -> ind.getLevel() == IndicatorLevel.FUNC_TO_COLLEGE)
                .collect(Collectors.toList());

        // Expected indicators: secondary if available, otherwise all issued
        List<Indicator> expectedIndicators = secondaryIndicators.isEmpty() 
                ? issuedIndicators : secondaryIndicators;

        // Get indicator mappings for the created task
        List<AdhocTaskIndicatorMap> mappings = adhocTaskIndicatorMapRepository
                .findByAdhocTask_AdhocTaskId(createdTask.getAdhocTaskId());

        // Assert: Number of mappings should equal expected indicators
        assertThat(mappings).hasSize(expectedIndicators.size());

        // Assert: All expected indicators should be in the mappings
        if (!expectedIndicators.isEmpty()) {
            List<Long> mappedIndicatorIds = mappings.stream()
                    .map(m -> m.getIndicator().getIndicatorId())
                    .collect(Collectors.toList());
            List<Long> expectedIndicatorIds = expectedIndicators.stream()
                    .map(Indicator::getIndicatorId)
                    .collect(Collectors.toList());

            assertThat(mappedIndicatorIds).containsExactlyInAnyOrderElementsOf(expectedIndicatorIds);
        }

        // Rollback will restore the original state due to @Transactional
    }

    /**
     * Property 13.3: CUSTOM scope type stores user-selected organizations
     * 
     * **Feature: sism-fullstack-integration, Property 13: Adhoc Task Scope Type Handling**
     * 
     * For any adhoc task with scope_type CUSTOM and user-selected target organizations,
     * the adhoc_task_target table SHALL contain exactly the selected organizations.
     * 
     * **Validates: Requirements 10.1, 10.4**
     */
    @Property(tries = 50)
    @Transactional
    void customScopeType_shouldStoreSelectedOrganizations(
            @ForAll("cycleIndices") Integer cycleIndex,
            @ForAll("orgIndices") Integer creatorOrgIndex,
            @ForAll("customOrgIndices") List<Integer> targetOrgIndices) {

        // Get existing cycles and organizations
        List<AssessmentCycle> cycles = getExistingCycles(5);
        List<SysOrg> activeOrgs = getExistingActiveOrgs(10);

        // Skip if no cycles or organizations exist
        assumeThat(cycles).isNotEmpty();
        assumeThat(activeOrgs).size().isGreaterThanOrEqualTo(2);

        // Select cycle and creator org based on indices
        AssessmentCycle cycle = cycles.get(cycleIndex % cycles.size());
        SysOrg creatorOrg = activeOrgs.get(creatorOrgIndex % activeOrgs.size());

        // Select target organizations based on indices (ensure uniqueness)
        List<Long> selectedOrgIds = targetOrgIndices.stream()
                .map(idx -> activeOrgs.get(idx % activeOrgs.size()).getId())
                .distinct()
                .collect(Collectors.toList());

        // Skip if no target organizations selected
        assumeThat(selectedOrgIds).isNotEmpty();

        // Create adhoc task with CUSTOM scope type
        AdhocTaskCreateRequest request = createBasicRequest(
                cycle.getCycleId(), creatorOrg.getId(), AdhocScopeType.CUSTOM);
        request.setTargetOrgIds(selectedOrgIds);

        // Create the adhoc task
        AdhocTaskVO createdTask = adhocTaskService.createAdhocTask(request);

        // Verify the task was created with correct scope type
        assertThat(createdTask).isNotNull();
        assertThat(createdTask.getScopeType()).isEqualTo(AdhocScopeType.CUSTOM);

        // Get target organizations for the created task
        List<AdhocTaskTarget> targets = adhocTaskTargetRepository
                .findByAdhocTask_AdhocTaskId(createdTask.getAdhocTaskId());

        // Assert: Number of targets should equal selected organizations
        assertThat(targets).hasSize(selectedOrgIds.size());

        // Assert: All selected organizations should be in the targets
        List<Long> targetOrgIds = targets.stream()
                .map(t -> t.getTargetOrg().getId())
                .collect(Collectors.toList());

        assertThat(targetOrgIds).containsExactlyInAnyOrderElementsOf(selectedOrgIds);

        // Rollback will restore the original state due to @Transactional
    }

    /**
     * Property 13.4: CUSTOM scope type stores user-selected indicators
     * 
     * **Feature: sism-fullstack-integration, Property 13: Adhoc Task Scope Type Handling**
     * 
     * For any adhoc task with scope_type CUSTOM and user-selected target indicators,
     * the adhoc_task_indicator_map table SHALL contain exactly the selected indicators.
     * 
     * **Validates: Requirements 10.1, 10.4**
     */
    @Property(tries = 50)
    @Transactional
    void customScopeType_shouldStoreSelectedIndicators(
            @ForAll("cycleIndices") Integer cycleIndex,
            @ForAll("orgIndices") Integer creatorOrgIndex,
            @ForAll("customIndicatorIndices") List<Integer> targetIndicatorIndices) {

        // Get existing cycles, organizations, and indicators
        List<AssessmentCycle> cycles = getExistingCycles(5);
        List<SysOrg> activeOrgs = getExistingActiveOrgs(10);
        List<Indicator> activeIndicators = getExistingActiveIndicators(10);

        // Skip if no cycles, organizations, or indicators exist
        assumeThat(cycles).isNotEmpty();
        assumeThat(activeOrgs).isNotEmpty();
        assumeThat(activeIndicators).isNotEmpty();

        // Select cycle and creator org based on indices
        AssessmentCycle cycle = cycles.get(cycleIndex % cycles.size());
        SysOrg creatorOrg = activeOrgs.get(creatorOrgIndex % activeOrgs.size());

        // Select target indicators based on indices (ensure uniqueness)
        List<Long> selectedIndicatorIds = targetIndicatorIndices.stream()
                .map(idx -> activeIndicators.get(idx % activeIndicators.size()).getIndicatorId())
                .distinct()
                .collect(Collectors.toList());

        // Skip if no target indicators selected
        assumeThat(selectedIndicatorIds).isNotEmpty();

        // Create adhoc task with CUSTOM scope type
        AdhocTaskCreateRequest request = createBasicRequest(
                cycle.getCycleId(), creatorOrg.getId(), AdhocScopeType.CUSTOM);
        request.setTargetIndicatorIds(selectedIndicatorIds);

        // Create the adhoc task
        AdhocTaskVO createdTask = adhocTaskService.createAdhocTask(request);

        // Verify the task was created with correct scope type
        assertThat(createdTask).isNotNull();
        assertThat(createdTask.getScopeType()).isEqualTo(AdhocScopeType.CUSTOM);

        // Get indicator mappings for the created task
        List<AdhocTaskIndicatorMap> mappings = adhocTaskIndicatorMapRepository
                .findByAdhocTask_AdhocTaskId(createdTask.getAdhocTaskId());

        // Assert: Number of mappings should equal selected indicators
        assertThat(mappings).hasSize(selectedIndicatorIds.size());

        // Assert: All selected indicators should be in the mappings
        List<Long> mappedIndicatorIds = mappings.stream()
                .map(m -> m.getIndicator().getIndicatorId())
                .collect(Collectors.toList());

        assertThat(mappedIndicatorIds).containsExactlyInAnyOrderElementsOf(selectedIndicatorIds);

        // Rollback will restore the original state due to @Transactional
    }

    /**
     * Property 13.5: Scope type determines which table is populated
     * 
     * **Feature: sism-fullstack-integration, Property 13: Adhoc Task Scope Type Handling**
     * 
     * For any adhoc task, the scope_type SHALL determine which mapping table is populated:
     * - ALL_ORGS: adhoc_task_target only
     * - BY_DEPT_ISSUED_INDICATORS: adhoc_task_indicator_map only
     * - CUSTOM: either or both based on user selection
     * 
     * **Validates: Requirements 10.1, 10.2, 10.3**
     */
    @Property(tries = 50)
    @Transactional
    void scopeType_shouldDetermineWhichTableIsPopulated(
            @ForAll("cycleIndices") Integer cycleIndex,
            @ForAll("orgIndices") Integer orgIndex) {

        // Get existing cycles and organizations
        List<AssessmentCycle> cycles = getExistingCycles(5);
        List<SysOrg> activeOrgs = getExistingActiveOrgs(10);

        // Skip if no cycles or organizations exist
        assumeThat(cycles).isNotEmpty();
        assumeThat(activeOrgs).isNotEmpty();

        // Select cycle and creator org based on indices
        AssessmentCycle cycle = cycles.get(cycleIndex % cycles.size());
        SysOrg creatorOrg = activeOrgs.get(orgIndex % activeOrgs.size());

        // Test ALL_ORGS scope type
        AdhocTaskCreateRequest allOrgsRequest = createBasicRequest(
                cycle.getCycleId(), creatorOrg.getId(), AdhocScopeType.ALL_ORGS);
        AdhocTaskVO allOrgsTask = adhocTaskService.createAdhocTask(allOrgsRequest);

        List<AdhocTaskTarget> allOrgsTargets = adhocTaskTargetRepository
                .findByAdhocTask_AdhocTaskId(allOrgsTask.getAdhocTaskId());
        List<AdhocTaskIndicatorMap> allOrgsMappings = adhocTaskIndicatorMapRepository
                .findByAdhocTask_AdhocTaskId(allOrgsTask.getAdhocTaskId());

        // Assert: ALL_ORGS should populate targets, not indicator mappings
        assertThat(allOrgsTargets).isNotEmpty();
        assertThat(allOrgsMappings).isEmpty();

        // Test BY_DEPT_ISSUED_INDICATORS scope type
        AdhocTaskCreateRequest byDeptRequest = createBasicRequest(
                cycle.getCycleId(), creatorOrg.getId(), AdhocScopeType.BY_DEPT_ISSUED_INDICATORS);
        AdhocTaskVO byDeptTask = adhocTaskService.createAdhocTask(byDeptRequest);

        List<AdhocTaskTarget> byDeptTargets = adhocTaskTargetRepository
                .findByAdhocTask_AdhocTaskId(byDeptTask.getAdhocTaskId());
        List<AdhocTaskIndicatorMap> byDeptMappings = adhocTaskIndicatorMapRepository
                .findByAdhocTask_AdhocTaskId(byDeptTask.getAdhocTaskId());

        // Assert: BY_DEPT_ISSUED_INDICATORS should populate indicator mappings, not targets
        assertThat(byDeptTargets).isEmpty();
        // Note: byDeptMappings may be empty if no indicators are issued by the creator org

        // Rollback will restore the original state due to @Transactional
    }

    /**
     * Property 13.6: Created task has correct status
     * 
     * **Feature: sism-fullstack-integration, Property 13: Adhoc Task Scope Type Handling**
     * 
     * For any newly created adhoc task regardless of scope type, the initial status
     * SHALL be DRAFT.
     * 
     * **Validates: Requirements 10.1**
     */
    @Property(tries = 50)
    @Transactional
    void createdTask_shouldHaveDraftStatus(
            @ForAll("cycleIndices") Integer cycleIndex,
            @ForAll("orgIndices") Integer orgIndex,
            @ForAll AdhocScopeType scopeType) {

        // Get existing cycles and organizations
        List<AssessmentCycle> cycles = getExistingCycles(5);
        List<SysOrg> activeOrgs = getExistingActiveOrgs(10);
        List<Indicator> activeIndicators = getExistingActiveIndicators(10);

        // Skip if no cycles or organizations exist
        assumeThat(cycles).isNotEmpty();
        assumeThat(activeOrgs).isNotEmpty();

        // Select cycle and creator org based on indices
        AssessmentCycle cycle = cycles.get(cycleIndex % cycles.size());
        SysOrg creatorOrg = activeOrgs.get(orgIndex % activeOrgs.size());

        // Create adhoc task with the given scope type
        AdhocTaskCreateRequest request = createBasicRequest(
                cycle.getCycleId(), creatorOrg.getId(), scopeType);

        // For CUSTOM scope type, add some targets
        if (scopeType == AdhocScopeType.CUSTOM && !activeOrgs.isEmpty()) {
            request.setTargetOrgIds(List.of(activeOrgs.get(0).getId()));
        }

        // Create the adhoc task
        AdhocTaskVO createdTask = adhocTaskService.createAdhocTask(request);

        // Assert: Status should be DRAFT
        assertThat(createdTask).isNotNull();
        assertThat(createdTask.getStatus()).isEqualTo(AdhocTaskStatus.DRAFT);

        // Rollback will restore the original state due to @Transactional
    }
}
