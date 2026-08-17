package com.asg.hr.common.service;

import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.PaginationProperties;
import com.asg.hr.common.dto.CurrentUserEmployeeDto;
import com.asg.hr.common.dto.EmployeeLovDto;
import com.asg.hr.common.dto.EmployeeLovQuery;
import com.asg.hr.common.security.EmployeeRowRestriction;
import com.asg.hr.common.security.UserRightsReader;
import com.asg.hr.employeemaster.entity.HrEmployeeMaster;
import com.asg.hr.employeemaster.repository.HrEmployeeMasterRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserEmployeeServiceTest {

    private static final String DOCUMENT_ID = "800-106";
    private static final String LOV_NAME = "EMPLOYEE_NAME";

    /** No LOV asked for, so no list is fetched. */
    private static final EmployeeLovQuery NO_LOV = new EmployeeLovQuery(null, null, null, null, null, null);

    private static final EmployeeLovQuery EMPLOYEE_LOV =
            new EmployeeLovQuery(LOV_NAME, null, null, null, null, null);

    @Mock
    private EmployeeRowRestriction employeeRowRestriction;

    @Mock
    private UserRightsReader userRightsReader;

    @Mock
    private HrEmployeeMasterRepository employeeMasterRepository;

    @Mock
    private LovDataService lovDataService;

    private CurrentUserEmployeeService service;

    private MockedStatic<UserContext> mockedUserContext;

    @BeforeEach
    void setUp() {
        service = new CurrentUserEmployeeService(employeeRowRestriction, userRightsReader,
                employeeMasterRepository, lovDataService, new PaginationProperties());

        mockedUserContext = mockStatic(UserContext.class);
        mockedUserContext.when(UserContext::getUserId).thenReturn("ksharma");
        mockedUserContext.when(UserContext::getUserPoid).thenReturn(5L);
        mockedUserContext.when(UserContext::getDocumentId).thenReturn(DOCUMENT_ID);
        mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
        mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        mockedUserContext.close();
    }

    private HrEmployeeMaster employee(String code, String name, String name2) {
        HrEmployeeMaster employee = new HrEmployeeMaster();
        employee.setEmployeePoid(77L);
        employee.setEmployeeCode(code);
        employee.setEmployeeName(name);
        employee.setEmployeeName2(name2);
        employee.setActive("Y");
        return employee;
    }

    private LovGetListDto lovRow(Long poid, String code, String description) {
        LovGetListDto row = new LovGetListDto();
        row.setPoid(poid);
        row.setCode(code);
        row.setDescription(description);
        row.setLabel(description);
        row.setValue(poid);
        return row;
    }

    private void linkedEmployee() {
        when(employeeRowRestriction.loginUserEmployeePoid()).thenReturn(77L);
        when(employeeMasterRepository.findByEmployeePoid(77L))
                .thenReturn(Optional.of(employee("EMP-77", "Kamal Sharma", null)));
    }

    private void rights(boolean granted) {
        when(userRightsReader.isGranted(DOCUMENT_ID, UserRolesRightsEnum.EDIT)).thenReturn(granted);
    }

    @Test
    void returnsTheEmployeeTheLoginIsLinkedTo() {
        linkedEmployee();
        rights(false);

        CurrentUserEmployeeDto response = service.getCurrentUserEmployee(NO_LOV);

        assertEquals(77L, response.getEmployeePoid());
        assertEquals("EMP-77", response.getEmployeeCode());
        assertEquals("Kamal Sharma", response.getEmployeeName());
        assertEquals("Y", response.getActive());
        assertEquals("ksharma", response.getUserId());
        assertEquals(5L, response.getUserPoid());
        assertEquals(DOCUMENT_ID, response.getDocumentId());
        assertTrue(response.isLinkedToEmployee());
        assertFalse(response.isCanSelectAnyEmployee(), "no Edit on the calling document, so the field stays locked");
    }

    @Test
    void readsNoLovAtAllWhenTheCallerNamesNone() {
        linkedEmployee();
        rights(true);

        CurrentUserEmployeeDto response = service.getCurrentUserEmployee(NO_LOV);

        assertNull(response.getEmployeeLov(), "no lovName, no list");
        verifyNoInteractions(lovDataService);
    }

    @Test
    void toleratesAMissingQuery() {
        linkedEmployee();
        rights(false);

        assertNull(service.getCurrentUserEmployee(null).getEmployeeLov());
        verifyNoInteractions(lovDataService);
    }

    @Test
    void opensThePickerForAUserHoldingEditOnTheCallingDocument() {
        linkedEmployee();
        rights(true);

        assertTrue(service.getCurrentUserEmployee(NO_LOV).isCanSelectAnyEmployee());
    }

    @Test
    void readsTheRightsOfTheCallingDocumentRatherThanTheEmployeeSelectionDocument() {
        linkedEmployee();
        rights(true);

        service.getCurrentUserEmployee(NO_LOV);

        verify(userRightsReader).isGranted(DOCUMENT_ID, UserRolesRightsEnum.EDIT);
        verify(employeeRowRestriction, never()).canSeeAllEmployees();
    }

    @Test
    void locksTheFieldWhenTheRequestCarriesNoDocumentId() {
        mockedUserContext.when(UserContext::getDocumentId).thenReturn(null);
        linkedEmployee();
        when(userRightsReader.isGranted(null, UserRolesRightsEnum.EDIT)).thenReturn(false);
        when(lovDataService.getDetailsByPoidAndLovNameFast(77L, LOV_NAME))
                .thenReturn(lovRow(77L, "EMP-77", "Kamal Sharma"));

        CurrentUserEmployeeDto response = service.getCurrentUserEmployee(EMPLOYEE_LOV);

        assertNull(response.getDocumentId());
        assertFalse(response.isCanSelectAnyEmployee(), "no document id grants nothing rather than everything");
        assertTrue(response.getEmployeeLov().isRestrictedToOwnEmployee());
    }

    @Test
    void returnsTheWholeLovForAUserWhoMaySelectAnyEmployee() {
        linkedEmployee();
        rights(true);
        when(lovDataService.getLovList("HR", 1L, 1L, 5L, LOV_NAME, 2, 50, "code", "desc"))
                .thenReturn(Map.of(
                        "totalRecords", 137,
                        "data", List.of(lovRow(77L, "EMP-77", "Kamal Sharma"), lovRow(78L, "EMP-78", "Asha Nair"))));

        EmployeeLovDto lov = service.getCurrentUserEmployee(
                new EmployeeLovQuery(LOV_NAME, "HR", 2, 50, "code", "desc")).getEmployeeLov();

        assertEquals(LOV_NAME, lov.getLovName());
        assertFalse(lov.isRestrictedToOwnEmployee());
        assertEquals(137, lov.getTotalRecords(), "the count before paging, as the LOV reports it");
        assertEquals(2, lov.getData().size());
        verify(lovDataService, never()).getDetailsByPoidAndLovNameFast(anyLong(), anyString());
    }

    @Test
    void defaultsPagingFromThePaginationPropertiesWhenTheCallerSendsNone() {
        linkedEmployee();
        rights(true);
        PaginationProperties defaults = new PaginationProperties();
        when(lovDataService.getLovList(null, 1L, 1L, 5L, LOV_NAME,
                defaults.getPageNumber(), defaults.getPageSize(), null, null))
                .thenReturn(Map.of("totalRecords", 1, "data", List.of(lovRow(77L, "EMP-77", "Kamal Sharma"))));

        assertEquals(1, service.getCurrentUserEmployee(EMPLOYEE_LOV).getEmployeeLov().getData().size());
    }

    @Test
    void pullsOnlyTheOwnEmployeeForARestrictedUser() {
        linkedEmployee();
        rights(false);
        when(lovDataService.getDetailsByPoidAndLovNameFast(77L, LOV_NAME))
                .thenReturn(lovRow(77L, "EMP-77", "Kamal Sharma"));

        EmployeeLovDto lov = service.getCurrentUserEmployee(EMPLOYEE_LOV).getEmployeeLov();

        assertTrue(lov.isRestrictedToOwnEmployee());
        assertEquals(1, lov.getTotalRecords());
        assertEquals(77L, lov.getData().get(0).getPoid());
        verify(lovDataService, never()).getLovList(any(), any(), any(), any(), anyString(),
                anyInt(), anyInt(), any(), any());
    }

    @Test
    void fillsTheOwnEmployeeRowFromTheEmployeeRecordWhenTheLovDoesNotCarryIt() {
        linkedEmployee();
        rights(false);
        LovGetListDto poidOnlyStub = new LovGetListDto();
        poidOnlyStub.setPoid(77L);
        when(lovDataService.getDetailsByPoidAndLovNameFast(77L, LOV_NAME)).thenReturn(poidOnlyStub);

        LovGetListDto row = service.getCurrentUserEmployee(EMPLOYEE_LOV).getEmployeeLov().getData().get(0);

        assertEquals(77L, row.getPoid());
        assertEquals(77L, row.getValue());
        assertEquals("EMP-77", row.getCode());
        assertEquals("Kamal Sharma", row.getDescription());
        assertEquals("Kamal Sharma", row.getLabel(), "so the locked picker still has something to render");
    }

    @Test
    void returnsAnEmptyListForARestrictedUserWithNoLinkedEmployee() {
        when(employeeRowRestriction.loginUserEmployeePoid()).thenReturn(null);
        rights(false);

        EmployeeLovDto lov = service.getCurrentUserEmployee(EMPLOYEE_LOV).getEmployeeLov();

        assertTrue(lov.getData().isEmpty(), "no employee and no right means nothing to pick");
        assertEquals(0, lov.getTotalRecords());
        verifyNoInteractions(lovDataService);
    }

    @Test
    void returnsNoEmployeeWhenTheLoginIsNotLinkedToOne() {
        when(employeeRowRestriction.loginUserEmployeePoid()).thenReturn(null);
        rights(true);

        CurrentUserEmployeeDto response = service.getCurrentUserEmployee(NO_LOV);

        assertNull(response.getEmployeePoid());
        assertNull(response.getEmployeeCode());
        assertFalse(response.isLinkedToEmployee());
        assertTrue(response.isCanSelectAnyEmployee(), "an unlinked user may still pick employees with the right");
        verifyNoInteractions(employeeMasterRepository);
    }

    @Test
    void keepsTheIdWhenTheEmployeeRecordCannotBeRead() {
        when(employeeRowRestriction.loginUserEmployeePoid()).thenReturn(77L);
        when(employeeMasterRepository.findByEmployeePoid(77L)).thenReturn(Optional.empty());
        rights(false);

        CurrentUserEmployeeDto response = service.getCurrentUserEmployee(NO_LOV);

        assertEquals(77L, response.getEmployeePoid(), "the id the lists restrict on is still the answer");
        assertNull(response.getEmployeeName());
        assertTrue(response.isLinkedToEmployee());
    }

    @Test
    void fallsBackToTheSecondaryNameWhenThePrimaryOneIsBlank() {
        when(employeeRowRestriction.loginUserEmployeePoid()).thenReturn(77L);
        when(employeeMasterRepository.findByEmployeePoid(77L))
                .thenReturn(Optional.of(employee("EMP-77", "  ", "كمال شارما")));
        rights(false);

        assertEquals("كمال شارما", service.getCurrentUserEmployee(NO_LOV).getEmployeeName());
    }

    @Test
    void survivesALovThatReturnsNothing() {
        linkedEmployee();
        rights(true);
        when(lovDataService.getLovList(eq(null), any(), any(), any(), eq(LOV_NAME), anyInt(), anyInt(), any(), any()))
                .thenReturn(null);

        EmployeeLovDto lov = service.getCurrentUserEmployee(EMPLOYEE_LOV).getEmployeeLov();

        assertTrue(lov.getData().isEmpty());
        assertEquals(0, lov.getTotalRecords());
    }
}
