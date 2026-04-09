package com.sism.execution.application;

import com.sism.exception.ResourceNotFoundException;
import com.sism.execution.domain.model.milestone.Milestone;
import com.sism.execution.domain.model.milestone.MilestoneStatus;
import com.sism.execution.domain.repository.ExecutionMilestoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MilestoneApplicationServiceTest {

    @Mock
    private ExecutionMilestoneRepository milestoneRepository;

    private MilestoneApplicationService milestoneApplicationService;

    @BeforeEach
    void setUp() {
        milestoneApplicationService = new MilestoneApplicationService(milestoneRepository);
    }

    @Test
    void updateMilestone_shouldThrowResourceNotFoundWhenMissing() {
        when(milestoneRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> milestoneApplicationService.updateMilestone(
                99L,
                2001L,
                "里程碑",
                "说明",
                null,
                20,
                MilestoneStatus.PLANNED,
                1,
                true,
                null
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Milestone not found with id: 99");
    }

    @Test
    void createMilestone_shouldNormalizeStatus() {
        Milestone milestone = new Milestone();
        when(milestoneRepository.save(org.mockito.ArgumentMatchers.any(Milestone.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Milestone created = milestoneApplicationService.createMilestone(
                1L, "M1", "desc", null, 10, MilestoneStatus.COMPLETED, 1, true, null
        );

        assertEquals("COMPLETED", created.getStatus());
    }

    @Test
    void findMilestonesByStatus_shouldNormalizeStatusBeforeQuery() {
        when(milestoneRepository.findByStatus(MilestoneStatus.IN_PROGRESS)).thenReturn(List.of());

        milestoneApplicationService.findMilestonesByStatus(MilestoneStatus.IN_PROGRESS);

        verify(milestoneRepository).findByStatus(MilestoneStatus.IN_PROGRESS);
    }

    @Test
    void findMilestonesByStatusPage_shouldNormalizeStatusAndPageable() {
        var pageable = PageRequest.of(1, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(milestoneRepository.findByStatus(MilestoneStatus.DELAYED, pageable))
                .thenReturn(org.springframework.data.domain.Page.empty(pageable));

        milestoneApplicationService.findMilestonesByStatus(MilestoneStatus.DELAYED, 2, 20);

        verify(milestoneRepository).findByStatus(MilestoneStatus.DELAYED, pageable);
    }

    @Test
    void getStatusEnum_shouldExposeStronglyTypedModelStatus() {
        Milestone milestone = new Milestone();
        milestone.setStatus(MilestoneStatus.IN_PROGRESS);

        assertEquals(MilestoneStatus.IN_PROGRESS, milestone.getStatusEnum());
    }
}
