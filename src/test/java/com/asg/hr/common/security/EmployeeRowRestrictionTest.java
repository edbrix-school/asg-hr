package com.asg.hr.common.security;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeRowRestrictionTest {

    @Mock
    private UserRightsReader userRightsReader;

    @Mock
    private EntityManager entityManager;

    @Mock
    private StoredProcedureQuery storedProcedureQuery;

    private EmployeeRowRestriction restriction;

    private final List<FilterDto> callerFilters = List.of(new FilterDto("DOC_REF", "LR-1"));

    @BeforeEach
    void setUp() {
        restriction = new EmployeeRowRestriction(userRightsReader, entityManager);
        ReflectionTestUtils.setField(restriction, "cacheTtlSeconds", 300L);
    }

    private void stubEmployeeLookup(Long employeePoid) {
        lenient().when(entityManager.createStoredProcedureQuery("PROC_GET_LOGIN_USER_EMP_ID"))
                .thenReturn(storedProcedureQuery);
        lenient().when(storedProcedureQuery.registerStoredProcedureParameter(anyInt(), any(), any()))
                .thenReturn(storedProcedureQuery);
        lenient().when(storedProcedureQuery.setParameter(anyInt(), any())).thenReturn(storedProcedureQuery);
        lenient().when(storedProcedureQuery.getOutputParameterValue(2)).thenReturn(employeePoid);
    }

    @Test
    void leavesTheSearchAloneForAUserHoldingTheEmployeeSelectionRight() {
        when(userRightsReader.isGranted(eq("000-219"), eq(UserRolesRightsEnum.EDIT))).thenReturn(true);

        EmployeeRowRestriction.ScopedSearch scoped = restriction.restrict(callerFilters, "OR");

        assertEquals(callerFilters, scoped.filters());
        assertEquals("OR", scoped.operator(), "a user who sees every row keeps the operator they asked for");
    }

    @Test
    void restrictsToTheOwnEmployeeWhenTheRightIsMissing() {
        when(userRightsReader.isGranted(anyString(), any())).thenReturn(false);
        stubEmployeeLookup(77L);

        try (var userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getUserPoid).thenReturn(5L);

            EmployeeRowRestriction.ScopedSearch scoped = restriction.restrict(callerFilters, "OR");

            assertTrue(scoped.filters().stream().anyMatch(f ->
                    "EMPLOYEE_POID".equals(f.searchField()) && "77".equals(f.searchValue())));
            assertTrue(scoped.filters().containsAll(callerFilters), "the caller's own filters are kept");
        }
    }

    @Test
    void forcesAndSoTheRestrictionCannotBeOredAway() {
        when(userRightsReader.isGranted(anyString(), any())).thenReturn(false);
        stubEmployeeLookup(77L);

        try (var userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getUserPoid).thenReturn(5L);

            assertEquals("AND", restriction.restrict(callerFilters, "OR").operator());
        }
    }

    @Test
    void showsNoRowsWhenTheUserHasNoLinkedEmployee() {
        when(userRightsReader.isGranted(anyString(), any())).thenReturn(false);
        stubEmployeeLookup(null);

        try (var userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getUserPoid).thenReturn(5L);
            userContext.when(UserContext::getUserId).thenReturn("ksharma");

            EmployeeRowRestriction.ScopedSearch scoped = restriction.restrict(callerFilters, "AND");

            assertTrue(scoped.filters().stream().anyMatch(f ->
                    "EMPLOYEE_POID".equals(f.searchField()) && "-1".equals(f.searchValue())),
                    "an employee poid that matches nothing, rather than no restriction at all");
        }
    }

    @Test
    void looksTheEmployeeUpOnceWhileTheCacheIsFresh() {
        when(userRightsReader.isGranted(anyString(), any())).thenReturn(false);
        stubEmployeeLookup(77L);

        try (var userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getUserPoid).thenReturn(5L);

            for (int i = 0; i < 10; i++) {
                restriction.restrict(callerFilters, "AND");
            }

            verify(entityManager, times(1)).createStoredProcedureQuery("PROC_GET_LOGIN_USER_EMP_ID");
        }
    }

    @Test
    void readsThroughWhenCachingIsDisabled() {
        ReflectionTestUtils.setField(restriction, "cacheTtlSeconds", 0L);
        when(userRightsReader.isGranted(anyString(), any())).thenReturn(false);
        stubEmployeeLookup(77L);

        try (var userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getUserPoid).thenReturn(5L);

            restriction.restrict(callerFilters, "AND");
            restriction.restrict(callerFilters, "AND");

            verify(entityManager, times(2)).createStoredProcedureQuery("PROC_GET_LOGIN_USER_EMP_ID");
        }
    }
}
