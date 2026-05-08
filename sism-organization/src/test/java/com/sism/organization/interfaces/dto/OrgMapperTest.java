package com.sism.organization.interfaces.dto;
import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.SysOrg;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class OrgMapperTest {

    private final OrgMapper orgMapper = new OrgMapper();

    @Test
    void toResponseShouldNotExposeDeletedFlag() {
        SysOrg org = SysOrg.create("Finance", OrgType.functional);
        org.setId(7L);

        OrgResponse response = orgMapper.toResponse(org);

        assertEquals(7L, response.getId());
        assertNull(findField(OrgResponse.class, "isDeleted"));
    }

    private Field findField(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
