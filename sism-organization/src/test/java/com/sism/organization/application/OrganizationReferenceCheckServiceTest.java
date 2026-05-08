package com.sism.organization.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationReferenceCheckService Tests")
class OrganizationReferenceCheckServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Should return true when any active reference exists")
    void shouldReturnTrueWhenAnyActiveReferenceExists() {
        OrganizationReferenceCheckService service = new OrganizationReferenceCheckService(jdbcTemplate);

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(9L), eq(9L)))
                .thenReturn(0L)
                .thenReturn(1L);

        assertTrue(service.hasActiveReferences(9L));
    }

    @Test
    @DisplayName("Should return false when no active references exist")
    void shouldReturnFalseWhenNoActiveReferencesExist() {
        OrganizationReferenceCheckService service = new OrganizationReferenceCheckService(jdbcTemplate);

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(9L), eq(9L)))
                .thenReturn(0L)
                .thenReturn(0L)
                .thenReturn(0L);

        assertFalse(service.hasActiveReferences(9L));
    }
}
