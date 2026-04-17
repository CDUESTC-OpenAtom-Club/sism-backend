package com.sism.main.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.iam.application.dto.CurrentUser;
import com.sism.main.application.MessageCenterApplicationService;
import com.sism.main.interfaces.dto.MessageCenterModels;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Message Center Controller Tests")
class MessageCenterControllerTest {

    @Mock
    private MessageCenterApplicationService messageCenterApplicationService;

    @InjectMocks
    private MessageCenterController messageCenterController;

    @Test
    @DisplayName("Should require login when requesting summary")
    void shouldRequireLoginWhenRequestingSummary() {
        ResponseEntity<ApiResponse<MessageCenterModels.Summary>> response = messageCenterController.getSummary(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(2000, response.getBody().getCode());
    }

    @Test
    @DisplayName("Should delegate summary query for current user")
    void shouldDelegateSummaryQueryForCurrentUser() {
        CurrentUser currentUser = currentUser(18L);
        MessageCenterModels.Summary summary = new MessageCenterModels.Summary(
                5,
                2,
                3,
                1,
                2,
                0,
                new MessageCenterModels.Capabilities(false, true, true),
                LocalDateTime.of(2026, 4, 17, 10, 0),
                false,
                List.of()
        );
        when(messageCenterApplicationService.getSummary(18L)).thenReturn(summary);

        ResponseEntity<ApiResponse<MessageCenterModels.Summary>> response = messageCenterController.getSummary(currentUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5, response.getBody().getData().totalCount());
        verify(messageCenterApplicationService).getSummary(18L);
    }

    @Test
    @DisplayName("Should reject marking workflow todo as read")
    void shouldRejectMarkingWorkflowTodoAsRead() {
        CurrentUser currentUser = currentUser(22L);
        when(messageCenterApplicationService.markMessageAsRead(22L, "workflow:12:501"))
                .thenThrow(new IllegalArgumentException("仅普通通知支持标记已读"));

        ResponseEntity<ApiResponse<MessageCenterModels.ReadResult>> response =
                messageCenterController.markMessageAsRead(currentUser, "workflow:12:501");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getCode());
        assertEquals("仅普通通知支持标记已读", response.getBody().getMessage());
    }

    private CurrentUser currentUser(Long id) {
        return new CurrentUser(
                id,
                "user-" + id,
                "User " + id,
                null,
                100L,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
