package com.asg.hr.leaverequest.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.hr.leaverequest.dto.LeaveCalculationResponseDto;
import com.asg.hr.leaverequest.dto.LeaveCreateRequestDto;
import com.asg.hr.leaverequest.dto.LeaveResponseDto;
import com.asg.hr.leaverequest.dto.LeaveTicketUpdateRequestDto;
import com.asg.hr.leaverequest.dto.LeaveUpdateRequestDto;
import com.asg.hr.leaverequest.service.HrLeaveRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrLeaveRequestControllerTest {

    @Mock
    private HrLeaveRequestService service;

    @InjectMocks
    private HrLeaveRequestController controller;

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void createLeave_success() {
        LeaveCreateRequestDto request = new LeaveCreateRequestDto();
        LeaveResponseDto response = new LeaveResponseDto();
        response.setTransactionPoid(1L);

        when(service.create(any(LeaveCreateRequestDto.class))).thenReturn(response);

        ResponseEntity<?> result = controller.createLeave(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(service).create(any(LeaveCreateRequestDto.class));
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void updateLeave_success() {
        LeaveUpdateRequestDto request = new LeaveUpdateRequestDto();
        request.setTransactionPoid(1L);
        LeaveResponseDto response = new LeaveResponseDto();
        response.setTransactionPoid(1L);

        when(service.update(any(LeaveUpdateRequestDto.class))).thenReturn(response);

        ResponseEntity<?> result = controller.updateLeave(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(service).update(any(LeaveUpdateRequestDto.class));
    }

    // -------------------------------------------------------------------------
    // getById
    // -------------------------------------------------------------------------

    @Test
    void getById_success() {
        LeaveResponseDto response = new LeaveResponseDto();
        response.setTransactionPoid(1L);

        when(service.getById(1L)).thenReturn(response);

        ResponseEntity<?> result = controller.getById(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(service).getById(1L);
    }

    // -------------------------------------------------------------------------
    // list
    // -------------------------------------------------------------------------

    @Test
    void list_success_withDates() {
        Pageable pageable = PageRequest.of(0, 10);
        FilterRequestDto filters = new FilterRequestDto("AND", "N", null);
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        Map<String, Object> mockData = new HashMap<>();
        mockData.put("content", "data");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC-001");
            when(service.list(anyString(), any(), any(), any(), any(Pageable.class))).thenReturn(mockData);

            ResponseEntity<?> result = controller.list(pageable, filters, startDate, endDate);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            verify(service).list(anyString(), any(), eq(startDate), eq(endDate), eq(pageable));
        }
    }

    @Test
    void list_success_withoutDates() {
        Pageable pageable = PageRequest.of(0, 10);
        Map<String, Object> mockData = new HashMap<>();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC-001");
            when(service.list(anyString(), any(), isNull(), isNull(), any(Pageable.class))).thenReturn(mockData);

            ResponseEntity<?> result = controller.list(pageable, null, null, null);

            assertEquals(HttpStatus.OK, result.getStatusCode());
        }
    }

    @Test
    void list_badRequest_onlyStartDate() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate startDate = LocalDate.now();

        ResponseEntity<?> result = controller.list(pageable, null, startDate, null);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void list_badRequest_onlyEndDate() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate endDate = LocalDate.now();

        ResponseEntity<?> result = controller.list(pageable, null, null, endDate);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verifyNoInteractions(service);
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void delete_success() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        doNothing().when(service).delete(1L, deleteReasonDto);

        ResponseEntity<?> result = controller.delete(1L, deleteReasonDto);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service).delete(1L, deleteReasonDto);
    }

    @Test
    void delete_withNullDeleteReason() {
        doNothing().when(service).delete(2L, null);

        ResponseEntity<?> result = controller.delete(2L, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service).delete(2L, null);
    }

    // -------------------------------------------------------------------------
    // getEmployeeDetails
    // -------------------------------------------------------------------------

    @Test
    void getEmployeeDetails_success() {
        Map<String, Object> response = Map.of("data", "employee");
        when(service.getEmployeeDetails(1L)).thenReturn(response);

        ResponseEntity<Map<String, Object>> result = controller.getEmployeeDetails(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(service).getEmployeeDetails(1L);
    }

    // -------------------------------------------------------------------------
    // getEmployeeHod
    // -------------------------------------------------------------------------

    @Test
    void getEmployeeHod_success() {
        Map<String, Object> response = Map.of("hod", 100L);
        when(service.getEmployeeHod(1L)).thenReturn(response);

        ResponseEntity<Map<String, Object>> result = controller.getEmployeeHod(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(service).getEmployeeHod(1L);
    }

    // -------------------------------------------------------------------------
    // getEligibleLeaveDays
    // -------------------------------------------------------------------------

    @Test
    void getEligibleLeaveDays_success() {
        Map<String, Object> response = Map.of("data", "eligible");
        LocalDate leaveStartDate = LocalDate.now();

        when(service.getEligibleLeaveDays(1L, 2L, leaveStartDate, 3L)).thenReturn(response);

        ResponseEntity<Map<String, Object>> result =
                controller.getEligibleLeaveDays(1L, 2L, leaveStartDate, 3L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(service).getEligibleLeaveDays(1L, 2L, leaveStartDate, 3L);
    }

    @Test
    void getEligibleLeaveDays_withoutSettlementPoid() {
        Map<String, Object> response = Map.of("data", "eligible");
        LocalDate leaveStartDate = LocalDate.now();

        when(service.getEligibleLeaveDays(1L, 2L, leaveStartDate, null)).thenReturn(response);

        ResponseEntity<Map<String, Object>> result =
                controller.getEligibleLeaveDays(1L, 2L, leaveStartDate, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service).getEligibleLeaveDays(1L, 2L, leaveStartDate, null);
    }

    // -------------------------------------------------------------------------
    // getTicketFamilyDetails
    // -------------------------------------------------------------------------

    @Test
    void getTicketFamilyDetails_success() {
        Map<String, Object> response = Map.of("data", "family");
        when(service.getTicketFamilyDetails(1L)).thenReturn(response);

        ResponseEntity<Map<String, Object>> result = controller.getTicketFamilyDetails(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(service).getTicketFamilyDetails(1L);
    }

    // -------------------------------------------------------------------------
    // updateLeaveHistoryLegacyParams
    // -------------------------------------------------------------------------

    @Test
    void updateLeaveHistoryLegacyParams_success() {
        Map<String, Object> response = Map.of("status", "SUCCESS");
        when(service.updateLeaveHistory(1L, "TYPE", "01-Jan-2024", "2")).thenReturn(response);

        ResponseEntity<Map<String, Object>> result =
                controller.updateLeaveHistoryLegacyParams(1L, "TYPE", "01-Jan-2024", "2");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(service).updateLeaveHistory(1L, "TYPE", "01-Jan-2024", "2");
    }

    @Test
    void updateLeaveHistoryLegacyParams_withNullOptionalParams() {
        Map<String, Object> response = Map.of("status", "SUCCESS");
        when(service.updateLeaveHistory(1L, null, null, null)).thenReturn(response);

        ResponseEntity<Map<String, Object>> result =
                controller.updateLeaveHistoryLegacyParams(1L, null, null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service).updateLeaveHistory(1L, null, null, null);
    }

    // -------------------------------------------------------------------------
    // cancelLeaveHistory
    // -------------------------------------------------------------------------

    @Test
    void cancelLeaveHistory_success() {
        Map<String, Object> response = Map.of("status", "SUCCESS");
        when(service.cancelLeaveHistory(1L)).thenReturn(response);

        ResponseEntity<Map<String, Object>> result = controller.cancelLeaveHistory(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(service).cancelLeaveHistory(1L);
    }

    // -------------------------------------------------------------------------
    // updateTicketDetails
    // -------------------------------------------------------------------------

    @Test
    void updateTicketDetails_success() {
        LeaveTicketUpdateRequestDto request = new LeaveTicketUpdateRequestDto();
        request.setTransactionPoid(1L);
        Map<String, Object> response = Map.of("status", "SUCCESS");

        when(service.updateTicketDetails(any(LeaveTicketUpdateRequestDto.class))).thenReturn(response);

        ResponseEntity<Map<String, Object>> result = controller.updateTicketDetails(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(service).updateTicketDetails(any(LeaveTicketUpdateRequestDto.class));
    }

    // -------------------------------------------------------------------------
    // calculateLeaveDays
    // -------------------------------------------------------------------------

    @Test
    void calculateLeaveDays_success() {
        LocalDate startDate = LocalDate.now();
        LocalDate rejoinDate = LocalDate.now().plusDays(10);
        LeaveCalculationResponseDto response = new LeaveCalculationResponseDto();
        response.setLeaveDays(BigDecimal.TEN);

        when(service.calculateLeaveDays(
                isNull(), eq(1L), eq("ANNUAL"), eq("WITH_PAY"), isNull(), isNull(),
                eq(startDate), eq(rejoinDate), eq(BigDecimal.TEN), eq("DEFAULT")))
                .thenReturn(response);

        ResponseEntity<LeaveCalculationResponseDto> result = controller.calculateLeaveDays(
                null, 1L, "ANNUAL", "WITH_PAY", null, null,
                startDate, rejoinDate, BigDecimal.TEN, "DEFAULT");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void calculateLeaveDays_withAllOptionalParams() {
        LocalDate startDate = LocalDate.now();
        LocalDate rejoinDate = LocalDate.now().plusDays(5);
        LeaveCalculationResponseDto response = new LeaveCalculationResponseDto();

        when(service.calculateLeaveDays(
                eq(10L), eq(2L), eq("EMERGENCY"), isNull(), eq("EMERGENCY_PAID"), isNull(),
                eq(startDate), eq(rejoinDate), isNull(), isNull()))
                .thenReturn(response);

        ResponseEntity<LeaveCalculationResponseDto> result = controller.calculateLeaveDays(
                10L, 2L, "EMERGENCY", null, "EMERGENCY_PAID", null,
                startDate, rejoinDate, null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service).calculateLeaveDays(
                eq(10L), eq(2L), eq("EMERGENCY"), isNull(), eq("EMERGENCY_PAID"), isNull(),
                eq(startDate), eq(rejoinDate), isNull(), isNull());
    }

    // -------------------------------------------------------------------------
    // handleLeaveTypeChange
    // -------------------------------------------------------------------------

    @Test
    void handleLeaveTypeChange_success() {
        Map<String, Object> response = Map.of("annualLeaveTypeVisible", true);
        when(service.handleLeaveTypeChange("ANNUAL", "DEFAULT")).thenReturn(response);

        ResponseEntity<Map<String, Object>> result =
                controller.handleLeaveTypeChange("ANNUAL", "DEFAULT");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(service).handleLeaveTypeChange("ANNUAL", "DEFAULT");
    }

    @Test
    void handleLeaveTypeChange_withNullMethod() {
        Map<String, Object> response = Map.of("emergencyLeaveTypeVisible", true);
        when(service.handleLeaveTypeChange("EMERGENCY", null)).thenReturn(response);

        ResponseEntity<Map<String, Object>> result =
                controller.handleLeaveTypeChange("EMERGENCY", null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service).handleLeaveTypeChange("EMERGENCY", null);
    }

    // -------------------------------------------------------------------------
    // attendanceReport
    // -------------------------------------------------------------------------

    @Test
    void attendanceReport_withEmployeeCode() {
        ResponseEntity<Map<String, Object>> result =
                controller.attendanceReport(1L, "EMP001");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("800-216", result.getBody().get("reportId"));
        assertTrue(result.getBody().get("parameters").toString().contains("EMP001"));
    }

    @Test
    void attendanceReport_withoutEmployeeCode() {
        ResponseEntity<Map<String, Object>> result =
                controller.attendanceReport(1L, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("800-216", result.getBody().get("reportId"));
        assertTrue(result.getBody().get("parameters").toString().contains("EMP_CODE="));
    }

    // -------------------------------------------------------------------------
    // leaveHistoryReport
    // -------------------------------------------------------------------------

    @Test
    void leaveHistoryReport_withDates() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);

        ResponseEntity<Map<String, Object>> result =
                controller.leaveHistoryReport(1L, from, to);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("800-217", result.getBody().get("reportId"));
        assertTrue(result.getBody().get("filters").toString().contains("EMPLOYEE_POID=1"));
    }

    @Test
    void leaveHistoryReport_withoutDates_usesDefaults() {
        ResponseEntity<Map<String, Object>> result =
                controller.leaveHistoryReport(2L, null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("800-217", result.getBody().get("reportId"));
        assertTrue(result.getBody().get("filters").toString().contains("EMPLOYEE_POID=2"));
    }

    // -------------------------------------------------------------------------
    // leaveScheduleReport
    // -------------------------------------------------------------------------

    @Test
    void leaveScheduleReport_success() {
        ResponseEntity<Map<String, Object>> result = controller.leaveScheduleReport();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("800-220", result.getBody().get("reportId"));
    }

    // -------------------------------------------------------------------------
    // print
    // -------------------------------------------------------------------------

    @Test
    void print_success() throws Exception {
        byte[] pdf = "PDF_CONTENT".getBytes();
        when(service.print(1L)).thenReturn(pdf);

        ResponseEntity<?> result = controller.print(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertArrayEquals(pdf, (byte[]) result.getBody());
        verify(service).print(1L);
    }

    @Test
    void print_failure_returnsInternalServerError() throws Exception {
        when(service.print(1L)).thenThrow(new RuntimeException("PDF generation failed"));

        ResponseEntity<?> result = controller.print(1L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().toString().contains("PDF generation failed"));
    }
}
