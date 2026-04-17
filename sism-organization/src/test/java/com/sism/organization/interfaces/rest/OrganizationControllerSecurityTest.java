package com.sism.organization.interfaces.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OrganizationController Security Annotation Tests")
class OrganizationControllerSecurityTest {

    @Test
    @DisplayName("Should protect organization endpoints with PreAuthorize")
    void shouldProtectOrganizationEndpointsWithPreAuthorize() throws Exception {
        assertRole("createOrganization", "hasAnyRole('STRATEGY_DEPT_HEAD', 'VICE_PRESIDENT')");
        assertRole("getAllOrganizations", "hasAnyRole('REPORTER', 'APPROVER', 'STRATEGY_DEPT_HEAD', 'VICE_PRESIDENT')");
        assertRole("getAllOrganizationsPage", "hasAnyRole('REPORTER', 'APPROVER', 'STRATEGY_DEPT_HEAD', 'VICE_PRESIDENT')");
        assertRole("getAllOrganizationsPageAlias", "hasAnyRole('REPORTER', 'APPROVER', 'STRATEGY_DEPT_HEAD', 'VICE_PRESIDENT')");
        assertRole("getAllDepartments", "hasAnyRole('REPORTER', 'APPROVER', 'STRATEGY_DEPT_HEAD', 'VICE_PRESIDENT')");
        assertRole("getOrganizationById", "hasAnyRole('REPORTER', 'APPROVER', 'STRATEGY_DEPT_HEAD', 'VICE_PRESIDENT')");
        assertRole("getOrganizationTree", "hasAnyRole('REPORTER', 'APPROVER', 'STRATEGY_DEPT_HEAD', 'VICE_PRESIDENT')");
        assertRole("getUsersByOrganizationId", "hasAnyRole('REPORTER', 'APPROVER', 'STRATEGY_DEPT_HEAD', 'VICE_PRESIDENT')");
        assertRole("activateOrganization", "hasAnyRole('STRATEGY_DEPT_HEAD', 'VICE_PRESIDENT')");
        assertRole("deactivateOrganization", "hasAnyRole('STRATEGY_DEPT_HEAD', 'VICE_PRESIDENT')");
        assertRole("renameOrganization", "hasAnyRole('STRATEGY_DEPT_HEAD', 'VICE_PRESIDENT')");
        assertRole("changeOrganizationType", "hasAnyRole('STRATEGY_DEPT_HEAD', 'VICE_PRESIDENT')");
        assertRole("updateSortOrder", "hasAnyRole('STRATEGY_DEPT_HEAD', 'VICE_PRESIDENT')");
        assertRole("updateParentOrganization", "hasAnyRole('STRATEGY_DEPT_HEAD', 'VICE_PRESIDENT')");

        assertMappingParams("getAllOrganizationsPage", "pageNum", "pageSize");
        assertMappingParams("getAllOrganizationsPageAlias", "page", "size");
        assertMethodParameterNames("getAllDepartments", "includeDisabled");
        assertMethodParameterNames("getOrganizationTree", "includeUsers", "includeDisabled");
        assertMethodParameterNames("renameOrganization", "id", "request");
        assertPaginationBounds("getAllOrganizationsPage");
        assertPaginationBounds("getAllOrganizationsPageAlias");
    }

    private void assertRole(String methodName, String expectedValue) throws Exception {
        Method method = findMethod(methodName);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, methodName + " should have PreAuthorize");
        assertEquals(expectedValue, preAuthorize.value(), methodName + " should use expected role policy");
    }

    private Method findMethod(String methodName) throws NoSuchMethodException {
        for (Method method : OrganizationController.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private void assertMappingParams(String methodName, String... expectedParams) throws Exception {
        Method method = findMethod(methodName);
        var getMapping = method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class);
        assertNotNull(getMapping, methodName + " should have GetMapping");
        assertTrue(Arrays.equals(expectedParams, getMapping.params()),
                methodName + " should expose expected request params");
    }

    private void assertMethodParameterNames(String methodName, String... expectedParams) throws Exception {
        Method method = findMethod(methodName);
        String[] actual = Arrays.stream(method.getParameters())
                .map(parameter -> parameter.getName())
                .toArray(String[]::new);
        assertTrue(Arrays.equals(expectedParams, actual),
                methodName + " should expose expected parameter names");
    }

    private void assertPaginationBounds(String methodName) throws Exception {
        Method method = findMethod(methodName);
        Parameter[] parameters = method.getParameters();
        assertTrue(parameters.length >= 2, methodName + " should expose paging parameters");

        assertNotNull(parameters[0].getAnnotation(Min.class), methodName + " page parameter should have @Min");
        assertNotNull(parameters[1].getAnnotation(Min.class), methodName + " page size parameter should have @Min");
        assertNotNull(parameters[1].getAnnotation(Max.class), methodName + " page size parameter should have @Max");
    }
}
