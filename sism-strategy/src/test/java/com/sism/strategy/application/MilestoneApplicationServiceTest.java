package com.sism.strategy.application;

import com.sism.strategy.domain.model.milestone.Milestone;
import com.sism.strategy.domain.repository.MilestoneRepository;
import com.sism.strategy.interfaces.dto.CreateMilestoneRequest;
import com.sism.strategy.interfaces.dto.BatchSaveMilestonesRequest;
import com.sism.strategy.interfaces.dto.MilestoneResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
@DisplayName("Milestone Application Service Tests")
class MilestoneApplicationServiceTest {

    @Mock
    private MilestoneRepository milestoneRepository;

    private MilestoneApplicationService service;

    @BeforeEach
    void setUp() {
        service = new MilestoneApplicationService(milestoneRepository);
    }

    @Test
    @DisplayName("Should batch save milestones with create update and delete in one call")
    void shouldBatchSaveMilestones() {
        Milestone existing = new Milestone();
        existing.setId(11L);
        existing.setIndicatorId(2002L);
        existing.setMilestoneName("旧里程碑");
        existing.setSortOrder(1);

        Milestone deleted = new Milestone();
        deleted.setId(12L);
        deleted.setIndicatorId(2002L);
        deleted.setMilestoneName("待删除里程碑");
        deleted.setSortOrder(2);

        BatchSaveMilestonesRequest.Item updateItem = new BatchSaveMilestonesRequest.Item();
        updateItem.setId(11L);
        updateItem.setMilestoneName("已更新里程碑");
        updateItem.setTargetProgress(40);
        updateItem.setDueDate(LocalDateTime.of(2026, 3, 31, 0, 0));
        updateItem.setStatus("NOT_STARTED");
        updateItem.setSortOrder(1);

        BatchSaveMilestonesRequest.Item createItem = new BatchSaveMilestonesRequest.Item();
        createItem.setMilestoneName("新增里程碑");
        createItem.setTargetProgress(80);
        createItem.setDueDate(LocalDateTime.of(2026, 6, 30, 0, 0));
        createItem.setStatus("NOT_STARTED");
        createItem.setSortOrder(2);

        Milestone savedUpdated = new Milestone();
        savedUpdated.setId(11L);
        savedUpdated.setIndicatorId(2002L);
        savedUpdated.setMilestoneName("已更新里程碑");
        savedUpdated.setProgress(40);
        savedUpdated.setSortOrder(1);

        Milestone savedCreated = new Milestone();
        savedCreated.setId(13L);
        savedCreated.setIndicatorId(2002L);
        savedCreated.setMilestoneName("新增里程碑");
        savedCreated.setProgress(80);
        savedCreated.setSortOrder(2);

        when(milestoneRepository.findByIndicatorId(2002L))
                .thenReturn(List.of(existing, deleted))
                .thenReturn(List.of(savedUpdated, savedCreated));
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<MilestoneResponse> responses = service.saveMilestones(2002L, List.of(updateItem, createItem));

        assertEquals(2, responses.size());
        assertEquals("已更新里程碑", responses.get(0).getMilestoneName());
        assertEquals("新增里程碑", responses.get(1).getMilestoneName());
        verify(milestoneRepository).delete(deleted);
        verify(milestoneRepository, atLeastOnce()).save(existing);
    }

    @Test
    @DisplayName("Should reject batch save when milestone id does not belong to indicator")
    void shouldRejectInvalidMilestoneOwnership() {
        BatchSaveMilestonesRequest.Item invalidItem = new BatchSaveMilestonesRequest.Item();
        invalidItem.setId(99L);
        invalidItem.setMilestoneName("非法里程碑");
        invalidItem.setTargetProgress(50);

        when(milestoneRepository.findByIndicatorId(2002L)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () ->
                service.saveMilestones(2002L, List.of(invalidItem))
        );

        verify(milestoneRepository, never()).save(any(Milestone.class));
    }

    @Test
    @DisplayName("Should query milestones with repository pagination instead of loading all rows")
    void shouldPageMilestonesAtRepositoryLevel() {
        Milestone milestone = new Milestone();
        milestone.setId(1L);
        milestone.setIndicatorId(2002L);
        milestone.setMilestoneName("阶段一");

        when(milestoneRepository.findByIndicatorIdAndStatus(eq(2002L), eq("PLANNED"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(milestone), PageRequest.of(0, 10), 1));

        var page = service.getMilestones(0, 10, 2002L, "PLANNED");

        assertEquals(1, page.getTotalElements());
        verify(milestoneRepository).findByIndicatorIdAndStatus(eq(2002L), eq("PLANNED"), any(Pageable.class));
        verify(milestoneRepository, never()).findAll();
    }

    @Test
    @DisplayName("Should reject create request with invalid target progress")
    void shouldRejectInvalidCreateRequest() {
        CreateMilestoneRequest request = new CreateMilestoneRequest();
        request.setIndicatorId(2002L);
        request.setMilestoneName("阶段一");
        request.setStatus("PLANNED");
        request.setTargetProgress(101);

        assertThrows(IllegalArgumentException.class, () -> service.createMilestone(request));
        verify(milestoneRepository, never()).save(any(Milestone.class));
    }
}
