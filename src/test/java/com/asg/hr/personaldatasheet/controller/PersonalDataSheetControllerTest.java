package com.asg.hr.personaldatasheet.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.personaldatasheet.dto.PersonalDataSheetRequestDto;
import com.asg.hr.personaldatasheet.dto.PersonalDataSheetResponseDto;
import com.asg.hr.personaldatasheet.service.PersonalDataSheetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalDataSheetControllerTest {

    @Test
    void list_usesUserDocumentId_andReturnsSuccessResponse() {
        PersonalDataSheetService service = mock(PersonalDataSheetService.class);
        LoggingService loggingService = mock(LoggingService.class);
        PersonalDataSheetController controller = new PersonalDataSheetController(service, loggingService);

        Pageable pageable = mock(Pageable.class);
        FilterRequestDto filters = mock(FilterRequestDto.class);
        when(service.list(eq("800-112"), same(filters), same(pageable))).thenReturn(Map.of("items", 1));

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getDocumentId).thenReturn("800-112");

            ResponseEntity<?> resp = controller.listPersonalDataSheets(pageable, filters);

            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            verify(service).list(eq("800-112"), same(filters), same(pageable));
            verifyNoInteractions(loggingService);
        }
    }

    @Test
    void create_delegatesToService() {
        PersonalDataSheetService service = mock(PersonalDataSheetService.class);
        LoggingService loggingService = mock(LoggingService.class);
        PersonalDataSheetController controller = new PersonalDataSheetController(service, loggingService);

        PersonalDataSheetRequestDto req = createValidRequest();
        PersonalDataSheetResponseDto created = createValidResponse();

        when(service.create(same(req))).thenReturn(created);

        ResponseEntity<?> resp = controller.createPersonalDataSheet(req);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(service).create(same(req));
        verifyNoInteractions(loggingService);
    }

    @Test
    void update_delegatesToService() {
        PersonalDataSheetService service = mock(PersonalDataSheetService.class);
        LoggingService loggingService = mock(LoggingService.class);
        PersonalDataSheetController controller = new PersonalDataSheetController(service, loggingService);

        PersonalDataSheetRequestDto req = createValidRequest();
        PersonalDataSheetResponseDto updated = createValidResponse();

        when(service.update(2L, req)).thenReturn(updated);

        ResponseEntity<?> resp = controller.updatePersonalDataSheet(2L, req);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(service).update(2L, req);
        verifyNoInteractions(loggingService);
    }

    @Test
    void getById_logsViewed_andDelegatesToService() {
        PersonalDataSheetService service = mock(PersonalDataSheetService.class);
        LoggingService loggingService = mock(LoggingService.class);
        PersonalDataSheetController controller = new PersonalDataSheetController(service, loggingService);

        PersonalDataSheetResponseDto dto = createValidResponse();
        when(service.getById(5L)).thenReturn(dto);

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getDocumentId).thenReturn("800-112");

            ResponseEntity<?> resp = controller.getPersonalDataSheetById(5L);

            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, "800-112", "5");
            verify(service).getById(5L);
        }
    }

    @Test
    void delete_delegatesToService_andReturnsSuccessResponse() {
        PersonalDataSheetService service = mock(PersonalDataSheetService.class);
        LoggingService loggingService = mock(LoggingService.class);
        PersonalDataSheetController controller = new PersonalDataSheetController(service, loggingService);

        DeleteReasonDto reason = mock(DeleteReasonDto.class);

        ResponseEntity<?> resp = controller.deletePersonalDataSheet(9L, reason);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(service).delete(9L, reason);
        verifyNoInteractions(loggingService);
    }

    @Test
    void getLoginUserEmployee_delegatesToService() {
        PersonalDataSheetService service = mock(PersonalDataSheetService.class);
        LoggingService loggingService = mock(LoggingService.class);
        PersonalDataSheetController controller = new PersonalDataSheetController(service, loggingService);

        Map<String, Object> mockResponse = Map.of("employeePoid", 123L, "employeeName", "John Doe");
        when(service.getLoginUserEmployee()).thenReturn(mockResponse);

        ResponseEntity<?> resp = controller.getLoginUserEmployee();

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(service).getLoginUserEmployee();
        verifyNoInteractions(loggingService);
    }

    @Test
    void loadUserPolicies_delegatesToService() {
        PersonalDataSheetService service = mock(PersonalDataSheetService.class);
        LoggingService loggingService = mock(LoggingService.class);
        PersonalDataSheetController controller = new PersonalDataSheetController(service, loggingService);

        Long employeePoid = 123L;
        List<Map<String, Object>> mockPolicies = Arrays.asList(
                Map.of("policyId", 1L, "policyName", "Privacy Policy"),
                Map.of("policyId", 2L, "policyName", "Code of Conduct")
        );
        when(service.loadUserPolicies(employeePoid)).thenReturn(mockPolicies);

        ResponseEntity<?> resp = controller.loadUserPolicies(employeePoid);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(service).loadUserPolicies(employeePoid);
        verifyNoInteractions(loggingService);
    }

    private PersonalDataSheetRequestDto createValidRequest() {
        PersonalDataSheetRequestDto request = new PersonalDataSheetRequestDto();
        request.setEmployeePoid(1L);
        request.setEmployeeNamePassport("John Doe");
        request.setResidentStatus("Resident");
        request.setCurrentFlat("123");
        request.setCurrentBldg("Building A");
        request.setCurrentRoad("Main Street");
        request.setCurrentBlock("Block 1");
        request.setCurrentArea("Downtown");
        request.setCurrentMobile("12345678");
        request.setPermanentAddress("123 Main Street");
        return request;
    }

    private PersonalDataSheetResponseDto createValidResponse() {
        PersonalDataSheetResponseDto response = new PersonalDataSheetResponseDto();
        response.setTransactionPoid(1L);
        response.setGroupPoid(1L);
        response.setCompanyPoid(1L);
        response.setTransactionDate(LocalDate.now());
        response.setEmployeePoid(1L);
        response.setEmployeeNamePassport("John Doe");
        response.setResidentStatus("Resident");
        response.setCurrentFlat("123");
        response.setCurrentBldg("Building A");
        response.setCurrentRoad("Main Street");
        response.setCurrentBlock("Block 1");
        response.setCurrentArea("Downtown");
        response.setCurrentMobile("12345678");
        response.setPermanentAddress("123 Main Street");
        response.setStatus("DRAFT");
        return response;
    }
}