package com.sism.service;

import com.sism.entity.Org;
import com.sism.enums.OrgType;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.OrgRepository;
import com.sism.vo.OrgTreeVO;
import com.sism.vo.OrgVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for OrgService
 * Tests organization hierarchy and query operations
 * 
 * Requirements: 4.2 - Service layer unit test coverage
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrgServiceTest {

    @Autowired
    private OrgService orgService;

    @Autowired
    private OrgRepository orgRepository;

    @Nested
    @DisplayName("getOrgById Tests")
    class GetOrgByIdTests {

        @Test
        @DisplayName("Should return organization when exists")
        void shouldReturnOrgWhenExists() {
            // Given
            Org existingOrg = orgRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow();

            // When
            Org result = orgService.getOrgById(existingOrg.getOrgId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrgId()).isEqualTo(existingOrg.getOrgId());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when org not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            Long nonExistentId = 999999L;

            // When/Then
            assertThatThrownBy(() -> orgService.getOrgById(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Organization");
        }
    }

    @Nested
    @DisplayName("getAllActiveOrgs Tests")
    class GetAllActiveOrgsTests {

        @Test
        @DisplayName("Should return only active organizations")
        void shouldReturnOnlyActiveOrgs() {
            // When
            List<OrgVO> result = orgService.getAllActiveOrgs();

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(OrgVO::getIsActive);
        }
    }

    @Nested
    @DisplayName("getOrgsByType Tests")
    class GetOrgsByTypeTests {

        @Test
        @DisplayName("Should return organizations of specified type")
        void shouldReturnOrgsOfSpecifiedType() {
            // Given
            OrgType targetType = OrgType.FUNCTIONAL_DEPT;

            // When
            List<OrgVO> result = orgService.getOrgsByType(targetType);

            // Then
            assertThat(result).allMatch(org -> org.getOrgType() == targetType);
        }

        @Test
        @DisplayName("Should return all active orgs when type is null")
        void shouldReturnAllActiveOrgsWhenTypeIsNull() {
            // When
            List<OrgVO> result = orgService.getOrgsByType(null);

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(OrgVO::getIsActive);
        }
    }

    @Nested
    @DisplayName("getOrgHierarchy Tests")
    class GetOrgHierarchyTests {

        @Test
        @DisplayName("Should return organization hierarchy tree")
        void shouldReturnOrgHierarchyTree() {
            // When
            List<OrgTreeVO> result = orgService.getOrgHierarchy();

            // Then
            assertThat(result).isNotNull();
            // Root orgs should have no parent
            assertThat(result).allMatch(org -> org.getParentOrgId() == null);
        }

        @Test
        @DisplayName("Should include children in hierarchy")
        void shouldIncludeChildrenInHierarchy() {
            // When
            List<OrgTreeVO> result = orgService.getOrgHierarchy();

            // Then
            assertThat(result).isNotNull();
            // Check that children list is not null for each root org
            assertThat(result).allMatch(org -> org.getChildren() != null);
        }
    }

    @Nested
    @DisplayName("getOrgHierarchyFrom Tests")
    class GetOrgHierarchyFromTests {

        @Test
        @DisplayName("Should return hierarchy starting from specified org")
        void shouldReturnHierarchyFromSpecifiedOrg() {
            // Given
            Org existingOrg = orgRepository.findAll().stream()
                    .filter(Org::getIsActive)
                    .findFirst()
                    .orElseThrow();

            // When
            OrgTreeVO result = orgService.getOrgHierarchyFrom(existingOrg.getOrgId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrgId()).isEqualTo(existingOrg.getOrgId());
            assertThat(result.getOrgName()).isEqualTo(existingOrg.getOrgName());
        }

        @Test
        @DisplayName("Should throw exception when org not found")
        void shouldThrowExceptionWhenOrgNotFound() {
            // Given
            Long nonExistentId = 999999L;

            // When/Then
            assertThatThrownBy(() -> orgService.getOrgHierarchyFrom(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getDescendantOrgIds Tests")
    class GetDescendantOrgIdsTests {

        @Test
        @DisplayName("Should include the org itself in descendants")
        void shouldIncludeOrgItselfInDescendants() {
            // Given
            Org existingOrg = orgRepository.findAll().stream()
                    .filter(Org::getIsActive)
                    .findFirst()
                    .orElseThrow();

            // When
            List<Long> result = orgService.getDescendantOrgIds(existingOrg.getOrgId());

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).contains(existingOrg.getOrgId());
        }

        @Test
        @DisplayName("Should return all descendant org IDs")
        void shouldReturnAllDescendantOrgIds() {
            // Given - Find an org that has children
            Org parentOrg = orgRepository.findAll().stream()
                    .filter(Org::getIsActive)
                    .filter(org -> !orgRepository.findByParentOrg_OrgId(org.getOrgId()).isEmpty())
                    .findFirst()
                    .orElse(null);

            if (parentOrg != null) {
                // When
                List<Long> result = orgService.getDescendantOrgIds(parentOrg.getOrgId());

                // Then
                assertThat(result).hasSizeGreaterThan(1);
                assertThat(result).contains(parentOrg.getOrgId());
            }
        }

        @Test
        @DisplayName("Should return only self for leaf org")
        void shouldReturnOnlySelfForLeafOrg() {
            // Given - Find a leaf org (no children)
            Org leafOrg = orgRepository.findAll().stream()
                    .filter(Org::getIsActive)
                    .filter(org -> orgRepository.findByParentOrg_OrgId(org.getOrgId()).isEmpty())
                    .findFirst()
                    .orElse(null);

            if (leafOrg != null) {
                // When
                List<Long> result = orgService.getDescendantOrgIds(leafOrg.getOrgId());

                // Then
                assertThat(result).hasSize(1);
                assertThat(result).containsExactly(leafOrg.getOrgId());
            }
        }
    }
}
