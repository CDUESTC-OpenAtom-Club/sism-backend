package com.sism.service;

import com.sism.entity.SysOrg;
import com.sism.enums.OrgType;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.SysOrgRepository;
import com.sism.vo.OrgTreeVO;
import com.sism.vo.SysOrgVO;
import org.junit.jupiter.api.BeforeEach;
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
    private SysOrgRepository orgRepository;

    @BeforeEach
    void setUp() {
        // Create test data for each test
        SysOrg strategicDept = new SysOrg();
        strategicDept.setName("战略发展部");
        strategicDept.setType(OrgType.STRATEGY_DEPT);
        strategicDept.setIsActive(true);
        strategicDept.setSortOrder(1);
        orgRepository.save(strategicDept);

        SysOrg functionalDept = new SysOrg();
        functionalDept.setName("职能部门");
        functionalDept.setType(OrgType.FUNCTIONAL_DEPT);
        functionalDept.setIsActive(true);
        functionalDept.setSortOrder(2);
        orgRepository.save(functionalDept);

        SysOrg college = new SysOrg();
        college.setName("二级学院");
        college.setType(OrgType.COLLEGE);
        college.setIsActive(true);
        college.setSortOrder(3);
        orgRepository.save(college);
    }

    @Nested
    @DisplayName("getOrgById Tests")
    class GetOrgByIdTests {

        @Test
        @DisplayName("Should return organization when exists")
        void shouldReturnOrgWhenExists() {
            // Given
            SysOrg existingOrg = orgRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow();

            // When
            SysOrg result = orgService.getOrgById(existingOrg.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(existingOrg.getId());
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
            List<SysOrgVO> result = orgService.getAllActiveOrgs();

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(SysOrgVO::getIsActive);
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
            List<SysOrgVO> result = orgService.getOrgsByType(targetType);

            // Then
            assertThat(result).allMatch(org -> org.getType() == targetType);
        }

        @Test
        @DisplayName("Should return all active orgs when type is null")
        void shouldReturnAllActiveOrgsWhenTypeIsNull() {
            // When
            List<SysOrgVO> result = orgService.getOrgsByType(null);

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(SysOrgVO::getIsActive);
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
            SysOrg existingOrg = orgRepository.findAll().stream()
                    .filter(SysOrg::getIsActive)
                    .findFirst()
                    .orElseThrow();

            // When
            OrgTreeVO result = orgService.getOrgHierarchyFrom(existingOrg.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrgId()).isEqualTo(existingOrg.getId());
            assertThat(result.getOrgName()).isEqualTo(existingOrg.getName());
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
        @DisplayName("Should return empty list for flat structure (no descendants)")
        void shouldReturnEmptyListForFlatStructure() {
            // Given - In flat structure, no org has descendants
            SysOrg existingOrg = orgRepository.findAll().stream()
                    .filter(SysOrg::getIsActive)
                    .findFirst()
                    .orElseThrow();

            // When
            List<Long> result = orgService.getDescendantOrgIds(existingOrg.getId());

            // Then - Flat structure means no descendants
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should not throw exception for valid org ID")
        void shouldNotThrowExceptionForValidOrgId() {
            // Given
            SysOrg existingOrg = orgRepository.findAll().stream()
                    .filter(SysOrg::getIsActive)
                    .findFirst()
                    .orElseThrow();

            // When/Then - Should not throw exception
            assertThatCode(() -> orgService.getDescendantOrgIds(existingOrg.getId()))
                    .doesNotThrowAnyException();
        }
    }
}
