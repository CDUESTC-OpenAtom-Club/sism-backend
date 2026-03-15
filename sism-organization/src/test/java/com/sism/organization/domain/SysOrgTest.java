package com.sism.organization.domain;

import com.sism.enums.OrgType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SysOrg Aggregate Root Tests")
class SysOrgTest {

    @Test
    @DisplayName("Should create SysOrg with valid parameters")
    void shouldCreateSysOrgWithValidParameters() {
        SysOrg org = SysOrg.create("测试部门", OrgType.ACADEMIC);

        assertNotNull(org);
        assertEquals("测试部门", org.getName());
        assertEquals(OrgType.ACADEMIC, org.getType());
        assertTrue(org.getIsActive());
        assertFalse(org.getIsDeleted());
        assertNotNull(org.getCreatedAt());
        assertNotNull(org.getUpdatedAt());
    }

    @Test
    @DisplayName("Should throw exception when creating SysOrg with null name")
    void shouldThrowExceptionWhenCreatingSysOrgWithNullName() {
        assertThrows(IllegalArgumentException.class, () ->
            SysOrg.create(null, OrgType.ACADEMIC)
        );
    }

    @Test
    @DisplayName("Should throw exception when creating SysOrg with empty name")
    void shouldThrowExceptionWhenCreatingSysOrgWithEmptyName() {
        assertThrows(IllegalArgumentException.class, () ->
            SysOrg.create("", OrgType.ACADEMIC)
        );
    }

    @Test
    @DisplayName("Should activate SysOrg successfully")
    void shouldActivateSysOrgSuccessfully() {
        SysOrg org = SysOrg.create("测试部门", OrgType.ACADEMIC);
        org.setIsActive(false);

        org.activate();

        assertTrue(org.getIsActive());
        assertNotNull(org.getUpdatedAt());
    }

    @Test
    @DisplayName("Should deactivate SysOrg successfully")
    void shouldDeactivateSysOrgSuccessfully() {
        SysOrg org = SysOrg.create("测试部门", OrgType.ACADEMIC);

        org.deactivate();

        assertFalse(org.getIsActive());
        assertNotNull(org.getUpdatedAt());
    }

    @Test
    @DisplayName("Should update organization name successfully")
    void shouldUpdateOrganizationNameSuccessfully() {
        SysOrg org = SysOrg.create("旧部门名称", OrgType.ACADEMIC);

        org.updateName("新部门名称");

        assertEquals("新部门名称", org.getName());
        assertNotNull(org.getUpdatedAt());
    }

    @Test
    @DisplayName("Should update organization description successfully")
    void shouldUpdateOrganizationDescriptionSuccessfully() {
        SysOrg org = SysOrg.create("测试部门", OrgType.ACADEMIC);
        LocalDateTime initialUpdatedAt = org.getUpdatedAt();

        org.updateDescription("这是一个测试部门的描述");

        assertNotNull(org.getUpdatedAt());
        assertNotEquals(initialUpdatedAt, org.getUpdatedAt());
    }

    @Test
    @DisplayName("Should update organization parent successfully")
    void shouldUpdateOrganizationParentSuccessfully() {
        SysOrg parentOrg = SysOrg.create("父部门", OrgType.FUNCTIONAL);
        SysOrg org = SysOrg.create("子部门", OrgType.ACADEMIC);

        org.updateParent(parentOrg);

        assertEquals(parentOrg.getId(), org.getParentOrgId());
    }

    @Test
    @DisplayName("Should delete SysOrg successfully")
    void shouldDeleteSysOrgSuccessfully() {
        SysOrg org = SysOrg.create("待删除部门", OrgType.ACADEMIC);

        org.delete();

        assertTrue(org.getIsDeleted());
    }

    @Test
    @DisplayName("Should validate SysOrg successfully")
    void shouldValidateSysOrgSuccessfully() {
        SysOrg org = SysOrg.create("有效的部门", OrgType.ACADEMIC);

        assertDoesNotThrow(org::validate);
    }

    @Test
    @DisplayName("Should validate SysOrg with invalid parameters")
    void shouldValidateSysOrgWithInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> {
            SysOrg.create(null, null);
        });
    }

    @Test
    @DisplayName("Should be considered equal when ids are the same")
    void shouldBeConsideredEqualWhenIdsAreTheSame() {
        SysOrg org1 = SysOrg.create("部门1", OrgType.ACADEMIC);
        SysOrg org2 = SysOrg.create("部门2", OrgType.FUNCTIONAL);
        org2.setId(org1.getId());

        assertEquals(org1, org2);
        assertEquals(org1.hashCode(), org2.hashCode());
    }

    @Test
    @DisplayName("Should not be considered equal when ids are different")
    void shouldNotBeConsideredEqualWhenIdsAreDifferent() {
        SysOrg org1 = SysOrg.create("部门1", OrgType.ACADEMIC);
        org1.setId(1L);

        SysOrg org2 = SysOrg.create("部门1", OrgType.ACADEMIC);
        org2.setId(2L);

        assertNotEquals(org1, org2);
    }
}
