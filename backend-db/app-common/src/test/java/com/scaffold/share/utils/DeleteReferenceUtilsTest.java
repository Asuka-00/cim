package com.scaffold.share.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.scaffold.share.exception.BusinessException;

class DeleteReferenceUtilsTest {

    @Test
    void requireNoReferences_whenEmpty_doesNotThrow() {
        assertDoesNotThrow(() -> DeleteReferenceUtils.requireNoReferences(
                300001, "租户",
                DeleteReferenceUtils.reference("用户", 0),
                DeleteReferenceUtils.reference("角色", 0)));
    }

    @Test
    void requireNoReferences_whenBound_throwsWithErrorParams() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> DeleteReferenceUtils.requireNoReferences(
                        300001, "租户",
                        DeleteReferenceUtils.reference("用户", 2),
                        DeleteReferenceUtils.reference("角色", 1)));

        assertEquals(300001, ex.getCode());
        assertTrue(ex.getMessage().contains("用户 2 条"));
        assertTrue(ex.getMessage().contains("角色 1 条"));

        Map<String, String> params = ex.getErrorParams();
        assertEquals("租户", params.get("target"));
        assertEquals("用户, 角色", params.get("boundBy"));
        assertTrue(params.get("details").contains("用户 2 条"));
    }
}
