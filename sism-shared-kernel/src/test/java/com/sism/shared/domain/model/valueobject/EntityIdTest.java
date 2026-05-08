package com.sism.shared.domain.model.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityIdTest {

    @Test
    void getLongValueShouldParseNumericValues() {
        EntityId<Object> entityId = new EntityId<>("42", Object.class);

        assertEquals(42L, entityId.getLongValue());
    }

    @Test
    void getLongValueShouldFailFastForNonNumericValues() {
        EntityId<Object> entityId = new EntityId<>("abc", Object.class);

        IllegalStateException exception = assertThrows(IllegalStateException.class, entityId::getLongValue);
        assertTrue(exception.getMessage().contains("abc"));
    }
}
