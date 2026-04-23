package com.asg.hr.leaverequest.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.hr.exceptions.ResourceNotFoundException;
import com.asg.hr.exceptions.ValidationException;
import com.asg.hr.leaverequest.dto.LeaveCalculationResponseDto;
import com.asg.hr.leaverequest.dto.LeaveCreateRequestDto;
import com.asg.hr.leaverequest.dto.LeaveHistoryUpdateRequestDto;
import com.asg.hr.leaverequest.dto.LeaveRequestDetailDto;
import com.asg.hr.leaverequest.dto.LeaveResponseDto;
import com.asg.hr.leaverequest.dto.LeaveTicketUpdateRequestDto;
import com.asg.hr.leaverequest.dto.LeaveUpdateRequestDto;
import com.asg.hr.leaverequest.entity.HrLeaveRequestDtl;
import com.asg.hr.leaverequest.entity.HrLeaveRequestDtlId;
import com.asg.hr.leaverequest.entity.HrLeaveRequestHdrEntity;
import com.asg.hr.leaverequest.repository.HrLeaveProcedureRepository;
import com.asg.hr.leaverequest.repository.HrLeaveRequestDtlRepository;
import com.asg.hr.leaverequest.repository.HrLeaveRequestHdrRepository;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrLeaveRequestServiceImplTest {

    @Mock
    private HrLeaveRequestHdrRepository hdrRepo;

    @Mock
    private HrLeaveRequestDtlRepository dtlRepo;

    @Mock
    private HrLeaveProcedureRepository repository;

    @Mock
    private LeaveFullValidationService validation;

    @Mock
    private PrintService printService;

    @Mock
    private DataSource dataSource;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private LovDataService lovService;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private HrLeaveRequestServiceImpl service;

    private HrLeaveRequestHdrEntity entity;

    @BeforeEach
    void setUp() {
        entity = new HrLeaveRequestHdrEntity();
        entity.setTransactionPoid(10L);
        entity.setGroupPoid(1L);
        entity.setCompanyPoid(2L);
        entity.setEmployeePoid(3L);
        entity.setTransactionDate(LocalDate.of(2026, 4, 23));
        entity.setLeaveType("SPECIAL_LEAVE");
        entity.setSplLeaveTypes("MARRIAGE");
        entity.setLeaveStartDate(LocalDate.of(2026, 5, 1));
        entity.setPlanedRejoinDate(LocalDate.of(2026, 5, 4));
        entity.setLeaveDays(new BigDecimal("3"));
        entity.setEligibleLeaveDays(new BigDecimal("12"));
        entity.setStatus("SUBMIT_FOR_APPROVAL");
        entity.setDeleted("N");
    }

    @Test
    void create_WhenValid_SavesHeaderDetailsAndReturnsResponse() {
        LeaveCreateRequestDto request = createRequest();
        LeaveRequestDetailDto detail = new LeaveRequestDetailDto();
        detail.setName("Spouse");
        detail.setRelation("WIFE");
        request.setDetails(List.of(detail));

        when(repository.validateLeave(eq(null), eq(request.getLeaveStartDate()), eq(request.getPlanedRejoinDate()),
                eq(3L), eq("SPECIAL_LEAVE"), eq("MARRIAGE"), eq(99L)))
                .thenReturn(Map.of("status", "SUCCESS", "leaveDays", "3"));
        when(hdrRepo.save(any(HrLeaveRequestHdrEntity.class))).thenAnswer(invocation -> {
            HrLeaveRequestHdrEntity saved = invocation.getArgument(0);
            saved.setTransactionPoid(10L);
            return saved;
        });
        when(dtlRepo.findMaxDetRowIdByTransactionPoid(10L)).thenReturn(null);
        stubGetById();

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getUserPoid).thenReturn(99L);
            userContext.when(UserContext::getDocumentId).thenReturn("800-100");

            LeaveResponseDto response = service.create(request);

            assertEquals(10L, response.getTransactionPoid());
            assertEquals(new BigDecimal("3"), response.getLeaveDays());
            ArgumentCaptor<HrLeaveRequestHdrEntity> headerCaptor = ArgumentCaptor.forClass(HrLeaveRequestHdrEntity.class);
            verify(hdrRepo).save(headerCaptor.capture());
            assertEquals("SUBMIT_FOR_APPROVAL", headerCaptor.getValue().getStatus());
            assertEquals("N", headerCaptor.getValue().getDeleted());
            verify(dtlRepo).save(any(HrLeaveRequestDtl.class));
            verify(loggingService).createLogSummaryEntry(eq("800-100"), eq("10"), anyString());
        }
    }

    @Test
    void create_WhenProcedureReturnsError_ThrowsValidationException() {
        LeaveCreateRequestDto request = createRequest();
        when(repository.validateLeave(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Map.of("status", "ERROR: invalid leave"));

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getUserPoid).thenReturn(99L);
            userContext.when(UserContext::getDocumentId).thenReturn("800-100");

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(request));
            assertEquals("ERROR: invalid leave", ex.getMessage());
        }
    }

    @Test
    void create_WhenLeaveDaysMissing_ThrowsValidationException() {
        LeaveCreateRequestDto request = createRequest();
        when(repository.validateLeave(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Map.of("status", "SUCCESS"));

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getUserPoid).thenReturn(99L);
            userContext.when(UserContext::getDocumentId).thenReturn("800-100");

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(request));
            assertTrue(ex.getMessage().contains("Leave days is not calculated"));
        }
    }

    @Test
    void create_WhenAnnualSubtypeMissing_ThrowsValidationException() {
        LeaveCreateRequestDto request = createRequest();
        request.setLeaveType("SPECIAL_LEAVE");
        request.setSplLeaveTypes(" ");

        ValidationException ex = assertThrows(ValidationException.class, () -> service.create(request));

        assertEquals("Leave subtype is required", ex.getMessage());
    }

    @Test
    void update_WhenValid_MergesDetailsUpdatesHistoryAndReturnsResponse() {
        LeaveUpdateRequestDto request = updateRequest();
        request.setDetails(List.of(
                detailDto(1L, "ISDELETED"),
                detailDto(2L, "ISUPDATED"),
                detailDto(3L, "NOCHANGE"),
                detailDto(null, null)
        ));
        request.setUpdateHistory(true);
        request.setCancelHistory(true);

        HrLeaveRequestDtl existingDetail = detail(10L, 2L, "Old");
        when(hdrRepo.findById(10L)).thenReturn(Optional.of(entity));
        when(repository.validateLeave(10L, request.getLeaveStartDate(), request.getPlanedRejoinDate(),
                3L, "SPECIAL_LEAVE", "MARRIAGE", 99L)).thenReturn(Map.of("status", "SUCCESS", "leaveDays", "4"));
        when(dtlRepo.findMaxDetRowIdByTransactionPoid(10L)).thenReturn(5L);
        when(dtlRepo.findById(any(HrLeaveRequestDtlId.class))).thenReturn(Optional.of(existingDetail));
        when(hdrRepo.save(entity)).thenReturn(entity);
        stubGetById();

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getUserPoid).thenReturn(99L);
            userContext.when(UserContext::getDocumentId).thenReturn("800-100");

            LeaveResponseDto response = service.update(request);

            assertEquals(10L, response.getTransactionPoid());
            assertEquals(new BigDecimal("4"), entity.getLeaveDays());
            verify(dtlRepo).deleteById(any(HrLeaveRequestDtlId.class));
            verify(dtlRepo, times(2)).save(any(HrLeaveRequestDtl.class));
            verify(loggingService).createLogBatch(anyList());
            verify(repository).updateLeaveHistory(10L, null, null, null);
            verify(repository).unUpdateLeaveHistory(10L);
        }
    }

    @Test
    void update_WhenMissing_ThrowsResourceNotFoundException() {
        LeaveUpdateRequestDto request = updateRequest();
        when(hdrRepo.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(request));
    }

    @Test
    void update_WhenOverlapExists_ThrowsValidationException() {
        LeaveUpdateRequestDto request = updateRequest();
        when(hdrRepo.findById(10L)).thenReturn(Optional.of(entity));
        when(hdrRepo.countOverlappingLeaveRequests(3L, 10L, request.getLeaveStartDate(), request.getPlanedRejoinDate()))
                .thenReturn(1L);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.update(request));

        assertEquals("Leave request overlaps with an existing leave request", ex.getMessage());
    }

    @Test
    void getById_WhenFound_MapsHeaderDetailsAndEligibleData() {
        HrLeaveRequestDtl detail = detail(10L, 1L, "Ali");
        when(hdrRepo.findById(10L)).thenReturn(Optional.of(entity));
        when(dtlRepo.findByIdTransactionPoid(10L)).thenReturn(List.of(detail));
        when(repository.getEligibleLeaveDays(2L, 3L, entity.getLeaveStartDate(), null))
                .thenReturn(Map.of("data", List.of(Map.of(
                        "earnedTicket", "2",
                        "eligibleTicket", "1",
                        "ticketPeriod", "12",
                        "paidLeavesTaken", "4",
                        "medicalTaken", "5",
                        "medicalEligible", "6",
                        "lastLeaveDetails", "last leave",
                        "lastTicketDetails", "last ticket"
                ))));

        LeaveResponseDto response = service.getById(10L);

        assertEquals(10L, response.getTransactionPoid());
        assertEquals("Ali", response.getDetails().get(0).getName());
        assertEquals(new BigDecimal("9"), response.getBalanceTillRejoin());
        assertEquals(new BigDecimal("2"), response.getTicketEarned());
        assertEquals("last ticket", response.getLastTicketDetails());
    }

    @Test
    void getById_WhenMissing_ThrowsResourceNotFoundException() {
        when(hdrRepo.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(10L));
    }

    @Test
    void list_DelegatesToDocumentSearchAndWrapsPage() {
        FilterDto filter = new FilterDto("GLOBALSEARCH", "leave");
        FilterRequestDto filters = new FilterRequestDto("OR", "N", List.of(filter));
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of("DOC_REF", "LR-1", "TRANSACTION_POID", 10L)),
                Map.of("DOC_REF", "Doc Ref"),
                1L
        );

        when(documentService.resolveOperator(filters)).thenReturn("OR");
        when(documentService.resolveIsDeleted(filters)).thenReturn("N");
        when(documentService.resolveDateFilters(filters, "TRANSACTION_DATE",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31))).thenReturn(List.of(filter));
        when(documentService.search(eq("800-100"), anyList(), eq("OR"), eq(pageable), eq("N"),
                eq("DOC_REF"), eq("TRANSACTION_POID"))).thenReturn(raw);

        Map<String, Object> result = service.list("800-100", filters,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), pageable);

        assertNotNull(result);
        verify(documentService).search(eq("800-100"), anyList(), eq("OR"), eq(pageable), eq("N"),
                eq("DOC_REF"), eq("TRANSACTION_POID"));
    }

    @Test
    void delete_WhenApproved_ThrowsValidationException() {
        entity.setStatus("APPROVED");
        when(hdrRepo.findById(10L)).thenReturn(Optional.of(entity));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.delete(10L, new DeleteReasonDto()));

        assertEquals("Approved leave cannot be deleted", ex.getMessage());
    }

    @Test
    void delete_WhenOpen_DelegatesToDocumentDeleteService() {
        when(hdrRepo.findById(10L)).thenReturn(Optional.of(entity));
        DeleteReasonDto reason = new DeleteReasonDto();

        service.delete(10L, reason);

        verify(documentDeleteService).deleteDocument(10L, "HR_LEAVE_REQUEST_HDR",
                "TRANSACTION_POID", reason, entity.getTransactionDate());
    }

    @Test
    void delete_WhenMissing_ThrowsResourceNotFoundException() {
        when(hdrRepo.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(10L, new DeleteReasonDto()));
    }

    @Test
    void getEmployeeDetails_WhenSuccess_ReturnsData() {
        List<Map<String, Object>> data = List.of(Map.of("employee", "Ali"));
        when(repository.getEmployeeDetails(3L)).thenReturn(Map.of("status", "SUCCESS", "data", data));

        Map<String, Object> response = service.getEmployeeDetails(3L);

        assertEquals("SUCCESS", response.get("status"));
        assertEquals(data, response.get("data"));
    }

    @Test
    void getEmployeeDetails_WhenRepositoryThrows_ReturnsErrorResponse() {
        when(repository.getEmployeeDetails(3L)).thenThrow(new RuntimeException("database down"));

        Map<String, Object> response = service.getEmployeeDetails(3L);

        assertTrue(response.get("status").toString().contains("ERROR: database down"));
        assertEquals(Collections.emptyList(), response.get("data"));
    }

    @Test
    void getEmployeeDetails_WhenNotSuccess_ReturnsEmptyData() {
        when(repository.getEmployeeDetails(3L)).thenReturn(Map.of("status", "NO_DATA"));

        Map<String, Object> response = service.getEmployeeDetails(3L);

        assertEquals("NO_DATA", response.get("status"));
        assertEquals(Collections.emptyList(), response.get("data"));
    }

    @Test
    void getEmployeeHod_WhenSuccess_ReturnsHod() {
        when(repository.getEmployeeHod(3L)).thenReturn(Map.of("status", "SUCCESS", "hod", 20L));

        Map<String, Object> response = service.getEmployeeHod(3L);

        assertEquals("SUCCESS", response.get("status"));
        assertEquals(20L, response.get("hod"));
    }

    @Test
    void getEmployeeHod_WhenNoSuccessOrException_ReturnsNullHod() {
        when(repository.getEmployeeHod(3L)).thenReturn(Map.of("status", "NO_DATA"));
        Map<String, Object> noData = service.getEmployeeHod(3L);
        assertNull(noData.get("hod"));

        when(repository.getEmployeeHod(4L)).thenThrow(new RuntimeException("hod error"));
        Map<String, Object> error = service.getEmployeeHod(4L);
        assertTrue(error.get("status").toString().contains("ERROR: hod error"));
        assertNull(error.get("hod"));
    }

    @Test
    void getEligibleLeaveDays_WhenSuccessOrException_ReturnsExpectedData() {
        LocalDate date = LocalDate.of(2026, 5, 1);
        List<Map<String, Object>> data = List.of(Map.of("eligible", "10"));
        when(repository.getEligibleLeaveDays(2L, 3L, date, 5L)).thenReturn(Map.of("status", "SUCCESS", "data", data));

        Map<String, Object> success = service.getEligibleLeaveDays(2L, 3L, date, 5L);
        assertEquals(data, success.get("data"));

        when(repository.getEligibleLeaveDays(2L, 4L, date, 5L)).thenThrow(new RuntimeException("eligible error"));
        Map<String, Object> error = service.getEligibleLeaveDays(2L, 4L, date, 5L);
        assertTrue(error.get("status").toString().contains("ERROR: eligible error"));
        assertEquals(Collections.emptyMap(), error.get("data"));
    }

    @Test
    void getEligibleLeaveDays_WhenNoSuccess_ReturnsEmptyMapData() {
        when(repository.getEligibleLeaveDays(2L, 3L, LocalDate.of(2026, 5, 1), 5L))
                .thenReturn(Map.of("status", "NO_DATA"));

        Map<String, Object> response = service.getEligibleLeaveDays(2L, 3L, LocalDate.of(2026, 5, 1), 5L);

        assertEquals("NO_DATA", response.get("status"));
        assertEquals(Collections.emptyMap(), response.get("data"));
    }

    @Test
    void getTicketFamilyDetails_WhenDataExists_ReturnsSuccess() {
        List<Map<String, Object>> data = List.of(Map.of("name", "Aisha"));
        when(repository.getTicketFamilyDetails(3L)).thenReturn(data);

        Map<String, Object> response = service.getTicketFamilyDetails(3L);

        assertEquals("SUCCESS", response.get("status"));
        assertEquals(data, response.get("data"));
    }

    @Test
    void getTicketFamilyDetails_WhenEmptyNullOrException_ReturnsFallbacks() {
        when(repository.getTicketFamilyDetails(3L)).thenReturn(Collections.emptyList());
        Map<String, Object> empty = service.getTicketFamilyDetails(3L);
        assertEquals("NO_DATA", empty.get("status"));

        when(repository.getTicketFamilyDetails(4L)).thenReturn(null);
        Map<String, Object> nullData = service.getTicketFamilyDetails(4L);
        assertEquals("NO_DATA", nullData.get("status"));

        when(repository.getTicketFamilyDetails(5L)).thenThrow(new RuntimeException("family error"));
        Map<String, Object> error = service.getTicketFamilyDetails(5L);
        assertTrue(error.get("status").toString().contains("ERROR: family error"));
        assertEquals(Collections.emptyList(), error.get("data"));
    }

    @Test
    void updateLeaveHistory_WhenSimpleCallSucceeds_ReturnsUpdated() {
        when(repository.updateLeaveHistory(10L, "ISSUE", "01-May-2026", "2")).thenReturn("SUCCESS");

        Map<String, Object> response = service.updateLeaveHistory(10L, "ISSUE", "01-May-2026", "2");

        assertEquals("SUCCESS", response.get("status"));
        assertEquals("UPDATED", response.get("data"));
    }

    @Test
    void updateLeaveHistory_WhenSimpleCallNoDataOrException_ReturnsFallbacks() {
        when(repository.updateLeaveHistory(10L, "ISSUE", null, null)).thenReturn("NO_DATA");
        Map<String, Object> noData = service.updateLeaveHistory(10L, "ISSUE", null, null);
        assertEquals("NO_DATA", noData.get("status"));
        assertNull(noData.get("data"));

        when(repository.updateLeaveHistory(11L, "ISSUE", null, null)).thenThrow(new RuntimeException("history error"));
        Map<String, Object> error = service.updateLeaveHistory(11L, "ISSUE", null, null);
        assertTrue(error.get("status").toString().contains("ERROR: history error"));
        assertNull(error.get("data"));
    }

    @Test
    void updateLeaveHistory_WhenRequestHasNullTransaction_ThrowsValidationException() {
        LeaveHistoryUpdateRequestDto request = new LeaveHistoryUpdateRequestDto();

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.updateLeaveHistory(request));

        assertEquals("Transaction poid is required", ex.getMessage());
    }

    @Test
    void updateLeaveHistory_WhenRequestSucceeds_UpdatesEntityAndLogs() {
        LeaveHistoryUpdateRequestDto request = new LeaveHistoryUpdateRequestDto();
        request.setTransactionPoid(10L);
        request.setHrTicketIssueType("ISSUE");
        request.setHrTicketTillDate(LocalDate.of(2026, 5, 15));
        request.setHrTicketIssuedCount(new BigDecimal("2"));

        when(hdrRepo.findById(10L)).thenReturn(Optional.of(entity));
        when(repository.updateLeaveHistory(10L, "ISSUE", "15-May-2026", "2")).thenReturn("SUCCESS");

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("800-100");

            Map<String, Object> response = service.updateLeaveHistory(request);

            assertEquals("SUCCESS", response.get("status"));
            assertEquals("ISSUE", entity.getHrTicketIssueType());
            assertEquals(LocalDate.of(2026, 5, 15), entity.getHrTicketTillDate());
            verify(hdrRepo).save(entity);
            verify(loggingService).createLogSummaryEntry(eq("800-100"), eq("10"), anyString());
        }
    }

    @Test
    void updateLeaveHistory_WhenRequestEntityMissingOrProcedureError_ThrowsException() {
        LeaveHistoryUpdateRequestDto request = new LeaveHistoryUpdateRequestDto();
        request.setTransactionPoid(10L);

        when(hdrRepo.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateLeaveHistory(request));

        when(hdrRepo.findById(11L)).thenReturn(Optional.of(entity));
        when(repository.updateLeaveHistory(11L, null, null, null)).thenReturn("ERROR: history failed");
        request.setTransactionPoid(11L);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.updateLeaveHistory(request));
        assertEquals("ERROR: history failed", ex.getMessage());
    }

    @Test
    void updateLeaveHistory_WhenProcedureReturnsNull_UsesSuccessDefault() {
        LeaveHistoryUpdateRequestDto request = new LeaveHistoryUpdateRequestDto();
        request.setTransactionPoid(10L);
        when(hdrRepo.findById(10L)).thenReturn(Optional.of(entity));
        when(repository.updateLeaveHistory(10L, null, null, null)).thenReturn(null);

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("800-100");

            Map<String, Object> response = service.updateLeaveHistory(request);

            assertEquals("SUCCESS", response.get("status"));
        }
    }

    @Test
    void cancelLeaveHistory_WhenNoData_ThrowsResourceNotFoundException() {
        when(hdrRepo.findById(10L)).thenReturn(Optional.of(entity));
        when(repository.unUpdateLeaveHistory(10L)).thenReturn("NO_DATA");

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.cancelLeaveHistory(10L));

        assertEquals("Please check, no record found", ex.getMessage());
    }

    @Test
    void cancelLeaveHistory_WhenNullMissingErrorOrNullStatus_UsesExpectedBranches() {
        ValidationException nullId = assertThrows(ValidationException.class, () -> service.cancelLeaveHistory(null));
        assertEquals("Transaction poid is required", nullId.getMessage());

        when(hdrRepo.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.cancelLeaveHistory(10L));

        when(hdrRepo.findById(11L)).thenReturn(Optional.of(entity));
        when(repository.unUpdateLeaveHistory(11L)).thenReturn("ERROR: cancel failed");
        assertThrows(ValidationException.class, () -> service.cancelLeaveHistory(11L));

        when(hdrRepo.findById(12L)).thenReturn(Optional.of(entity));
        when(repository.unUpdateLeaveHistory(12L)).thenReturn(null);
        Map<String, Object> response = service.cancelLeaveHistory(12L);
        assertEquals("SUCCESS", response.get("status"));
    }

    @Test
    void cancelLeaveHistory_WhenSuccess_ReturnsMessage() {
        when(hdrRepo.findById(10L)).thenReturn(Optional.of(entity));
        when(repository.unUpdateLeaveHistory(10L)).thenReturn("SUCCESS");

        Map<String, Object> response = service.cancelLeaveHistory(10L);

        assertEquals("SUCCESS", response.get("status"));
        assertEquals("This record is removed from history", response.get("message"));
    }

    @Test
    void updateTicketDetails_WhenProcedureErrors_ThrowsValidationException() {
        LeaveTicketUpdateRequestDto request = ticketRequest();
        when(hdrRepo.findById(10L)).thenReturn(Optional.of(entity));
        when(repository.updateTicketDetails(eq(10L), any(), any(), any(), any(), any()))
                .thenReturn("ERROR: ticket issue failed");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.updateTicketDetails(request));

        assertEquals("ERROR: ticket issue failed", ex.getMessage());
    }

    @Test
    void updateTicketDetails_WhenSuccess_UpdatesEntityAndLogs() {
        LeaveTicketUpdateRequestDto request = ticketRequest();
        when(hdrRepo.findById(10L)).thenReturn(Optional.of(entity));
        when(repository.updateTicketDetails(10L, "HR", "Y", "Booked", new BigDecimal("2"), "PJ-1"))
                .thenReturn("SUCCESS");

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("800-100");

            Map<String, Object> response = service.updateTicketDetails(request);

            assertEquals("SUCCESS", response.get("status"));
            assertEquals("HR", entity.getTicketBookBy());
            assertEquals("PJ-1", entity.getPjDocRef());
            verify(hdrRepo).save(entity);
            verify(loggingService).createLogSummaryEntry(eq("800-100"), eq("10"), anyString());
        }
    }

    @Test
    void updateTicketDetails_WhenNullTransactionMissingOrNullStatus_UsesExpectedBranches() {
        LeaveTicketUpdateRequestDto nullRequest = ticketRequest();
        nullRequest.setTransactionPoid(null);
        ValidationException nullId = assertThrows(ValidationException.class, () -> service.updateTicketDetails(nullRequest));
        assertEquals("Transaction poid is required", nullId.getMessage());

        LeaveTicketUpdateRequestDto missingRequest = ticketRequest();
        when(hdrRepo.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateTicketDetails(missingRequest));

        LeaveTicketUpdateRequestDto request = ticketRequest();
        request.setTransactionPoid(11L);
        when(hdrRepo.findById(11L)).thenReturn(Optional.of(entity));
        when(repository.updateTicketDetails(eq(11L), any(), any(), any(), any(), any())).thenReturn(null);
        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("800-100");
            Map<String, Object> response = service.updateTicketDetails(request);
            assertEquals("SUCCESS", response.get("status"));
        }
    }

    @Test
    void calculateLeaveDays_WhenDatesInvalid_ThrowsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class, () -> service.calculateLeaveDays(
                null, 3L, "ANNUAL", "AGAINST_ACCUM_LEAVE", null, null,
                LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 1), BigDecimal.TEN, "DEFAULT"));

        assertEquals("Planned rejoin date should be after leave start date", ex.getMessage());
    }

    @Test
    void calculateLeaveDays_WhenRequiredInputsMissing_ThrowsValidationException() {
        assertEquals("Employee not selected", assertThrows(ValidationException.class, () -> service.calculateLeaveDays(
                null, null, "ANNUAL", null, null, null, LocalDate.now(), LocalDate.now(), null, null)).getMessage());
        assertEquals("Leave type required", assertThrows(ValidationException.class, () -> service.calculateLeaveDays(
                null, 3L, " ", null, null, null, LocalDate.now(), LocalDate.now(), null, null)).getMessage());
        assertEquals("Leave dates required", assertThrows(ValidationException.class, () -> service.calculateLeaveDays(
                null, 3L, "ANNUAL", null, null, null, null, LocalDate.now(), null, null)).getMessage());
    }

    @Test
    void calculateLeaveDays_WhenValid_ReturnsCalculatedResponse() {
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 4);
        when(repository.validateLeave(10L, start, end, 3L, "ANNUAL", "AGAINST_ACCUM_LEAVE", 99L))
                .thenReturn(Map.of("status", "SUCCESS", "leaveDays", "3"));
        when(repository.getHolidayCount(start, end)).thenReturn(BigDecimal.ONE);

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getUserPoid).thenReturn(99L);

            LeaveCalculationResponseDto response = service.calculateLeaveDays(
                    10L, 3L, "ANNUAL", "AGAINST_ACCUM_LEAVE", null, null,
                    start, end, BigDecimal.TEN, "DEFAULT");

            assertTrue(response.getLeaveHolidayRunning());
            assertEquals(new BigDecimal("3"), response.getLeaveDays());
            assertEquals(new BigDecimal("7"), response.getBalanceTillRejoin());
            assertEquals(BigDecimal.ONE, response.getHolidays());
        }
    }

    @Test
    void calculateLeaveDays_WhenEmergencyOrSpecial_UsesMatchingSubtypeAndDefaults() {
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 4);
        when(repository.validateLeave(null, start, end, 3L, "EMERGENCY", "URGENT", 99L))
                .thenReturn(Map.of("status", "SUCCESS"));
        when(repository.validateLeave(null, start, end, 3L, "SPECIAL_LEAVE", "MARRIAGE", 99L))
                .thenReturn(Map.of("status", "SUCCESS", "leaveDays", "2"));
        when(repository.getHolidayCount(start, end)).thenReturn(BigDecimal.ZERO);

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getUserPoid).thenReturn(99L);

            LeaveCalculationResponseDto emergency = service.calculateLeaveDays(
                    null, 3L, "EMERGENCY", null, "URGENT", null, start, end, null, null);
            assertEquals(BigDecimal.ZERO, emergency.getLeaveDays());
            assertNull(emergency.getBalanceTillRejoin());

            LeaveCalculationResponseDto special = service.calculateLeaveDays(
                    null, 3L, "SPECIAL_LEAVE", null, null, "MARRIAGE", start, end, BigDecimal.TEN, "DEFAULT");
            assertFalse(special.getLeaveHolidayRunning());
            assertEquals(new BigDecimal("8"), special.getBalanceTillRejoin());
        }
    }

    @Test
    void handleLeaveTypeChange_WhenEmergency_ReturnsEmergencyVisibilityAndClearsOtherTypes() {
        Map<String, Object> response = service.handleLeaveTypeChange("EMERGENCY", "START_END_DURATION");

        assertFalse((Boolean) response.get("leaveHolidayRunning"));
        assertTrue((Boolean) response.get("emergencyLeaveTypeVisible"));
        assertNull(response.get("annualLeaveType"));
        assertNull(response.get("splLeaveTypes"));
    }

    @Test
    void handleLeaveTypeChange_WhenAnnualSpecialMedical_ReturnsExpectedVisibility() {
        Map<String, Object> annual = service.handleLeaveTypeChange("ANNUAL", null);
        assertTrue((Boolean) annual.get("annualLeaveTypeVisible"));
        assertTrue((Boolean) annual.get("leaveHolidayRunning"));

        Map<String, Object> special = service.handleLeaveTypeChange("SPECIAL_LEAVE", "DEFAULT");
        assertTrue((Boolean) special.get("specialLeaveTypeVisible"));
        assertFalse((Boolean) special.get("leaveHolidayRunning"));

        Map<String, Object> medical = service.handleLeaveTypeChange("MEDICAL", "DEFAULT");
        assertNull(medical.get("annualLeaveType"));
        assertNull(medical.get("emergencyLeaveType"));
    }

    @Test
    void print_DelegatesToPrintService() throws Exception {
        JasperReport subReport = Mockito.mock(JasperReport.class);
        JasperReport mainReport = Mockito.mock(JasperReport.class);
        byte[] pdf = new byte[] {1, 2, 3};

        when(printService.buildBaseParams(10L, "800-100")).thenReturn(new java.util.HashMap<>());
        when(printService.load("HR/Emp_leave_Request_subreport1.jrxml")).thenReturn(subReport);
        when(printService.load("HR/Emp_leave_Request.jrxml")).thenReturn(mainReport);
        when(printService.fillReportToPdf(eq(mainReport), any(), eq(dataSource))).thenReturn(pdf);

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("800-100");

            byte[] response = service.print(10L);

            assertArrayEquals(pdf, response);
            verify(printService).fillReportToPdf(eq(mainReport), any(), eq(dataSource));
        }
    }

    @Test
    void print_WhenPrintServiceThrows_WrapsNonJasperAndRethrowsJasper() throws Exception {
        JasperReport subReport = Mockito.mock(JasperReport.class);
        JasperReport mainReport = Mockito.mock(JasperReport.class);
        when(printService.buildBaseParams(10L, "800-100")).thenReturn(new java.util.HashMap<>());
        when(printService.load("HR/Emp_leave_Request_subreport1.jrxml")).thenReturn(subReport);
        when(printService.load("HR/Emp_leave_Request.jrxml")).thenReturn(mainReport);

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("800-100");
            when(printService.fillReportToPdf(eq(mainReport), any(), eq(dataSource)))
                    .thenThrow(new RuntimeException("print failed"));
            assertThrows(net.sf.jasperreports.engine.JRException.class, () -> service.print(10L));
        }

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("800-100");
            when(printService.fillReportToPdf(eq(mainReport), any(), eq(dataSource)))
                    .thenThrow(new net.sf.jasperreports.engine.JRException("jasper failed"));
            assertThrows(net.sf.jasperreports.engine.JRException.class, () -> service.print(10L));
        }
    }

    @Test
    void privateHelpers_CoverValidationNormalizationExtractionAndConversionBranches() throws Exception {
        invoke("saveDetails", new Class<?>[] {Long.class, List.class}, 10L, null);
        verify(dtlRepo, never()).findMaxDetRowIdByTransactionPoid(99L);

        LeaveCreateRequestDto annual = createRequest();
        annual.setLeaveType("ANNUAL");
        annual.setAnnualLeaveType("AGAINST_ACCUM_LEAVE");
        annual.setEmergencyLeaveType("EMERGENCY_TYPE");
        annual.setSplLeaveTypes("SPL");
        invoke("normalizeLeaveTypeFields", new Class<?>[] {LeaveCreateRequestDto.class}, annual);
        assertNull(annual.getEmergencyLeaveType());
        assertNull(annual.getSplLeaveTypes());

        LeaveCreateRequestDto emergency = createRequest();
        emergency.setLeaveType("EMERGENCY");
        emergency.setAnnualLeaveType("ANNUAL_TYPE");
        emergency.setEmergencyLeaveType("URGENT");
        invoke("normalizeLeaveTypeFields", new Class<?>[] {LeaveCreateRequestDto.class}, emergency);
        assertNull(emergency.getAnnualLeaveType());

        LeaveCreateRequestDto medical = createRequest();
        medical.setLeaveType("MEDICAL");
        medical.setAnnualLeaveType("ANNUAL_TYPE");
        medical.setEmergencyLeaveType("URGENT");
        invoke("normalizeLeaveTypeFields", new Class<?>[] {LeaveCreateRequestDto.class}, medical);
        assertNull(medical.getEmergencyLeaveType());

        LeaveUpdateRequestDto update = updateRequest();
        update.setLeaveType("ANNUAL");
        update.setAnnualLeaveType("AGAINST_ACCUM_LEAVE");
        update.setEmergencyLeaveType("URGENT");
        update.setSplLeaveTypes("SPL");
        invoke("normalizeLeaveTypeFields", new Class<?>[] {LeaveUpdateRequestDto.class}, update);
        assertNull(update.getEmergencyLeaveType());

        update.setLeaveType("EMERGENCY");
        update.setAnnualLeaveType("ANNUAL_TYPE");
        update.setEmergencyLeaveType("URGENT");
        update.setSplLeaveTypes("SPL");
        invoke("normalizeLeaveTypeFields", new Class<?>[] {LeaveUpdateRequestDto.class}, update);
        assertNull(update.getAnnualLeaveType());

        update.setLeaveType("MEDICAL");
        update.setAnnualLeaveType("ANNUAL_TYPE");
        update.setEmergencyLeaveType("URGENT");
        invoke("normalizeLeaveTypeFields", new Class<?>[] {LeaveUpdateRequestDto.class}, update);
        assertNull(update.getEmergencyLeaveType());

        assertEquals("URGENT", invoke("getSubType", new Class<?>[] {LeaveUpdateRequestDto.class}, updateRequestEmergency()));
        LeaveUpdateRequestDto annualUpdate = updateRequest();
        annualUpdate.setLeaveType("ANNUAL");
        annualUpdate.setAnnualLeaveType("AGAINST_ACCUM_LEAVE");
        assertEquals("AGAINST_ACCUM_LEAVE", invoke("getSubType", new Class<?>[] {LeaveUpdateRequestDto.class}, annualUpdate));
        assertEquals("MARRIAGE", invoke("getSubType", new Class<?>[] {LeaveUpdateRequestDto.class}, updateRequest()));

        LeaveCreateRequestDto annualCreate = createRequest();
        annualCreate.setLeaveType("ANNUAL");
        annualCreate.setAnnualLeaveType("AGAINST_ACCUM_LEAVE");
        assertEquals("AGAINST_ACCUM_LEAVE", invoke("getSubType", new Class<?>[] {LeaveCreateRequestDto.class}, annualCreate));
        LeaveCreateRequestDto emergencyCreate = createRequest();
        emergencyCreate.setLeaveType("EMERGENCY");
        emergencyCreate.setEmergencyLeaveType("URGENT");
        assertEquals("URGENT", invoke("getSubType", new Class<?>[] {LeaveCreateRequestDto.class}, emergencyCreate));

        assertEquals(7, invoke("extractInteger", new Class<?>[] {Object.class, String.class, int.class},
                Map.of("probation", 7), "probation", 4));
        assertNull(invoke("extractInteger", new Class<?>[] {Object.class, String.class, int.class},
                Map.of("probation", "seven"), "probation", 4));
        assertEquals(LocalDate.of(2026, 1, 1), invoke("extractLocalDate", new Class<?>[] {Object.class, String.class, int.class},
                Map.of("joinDate", LocalDate.of(2026, 1, 1)), "joinDate", 3));
        assertEquals(LocalDate.of(2026, 1, 2), invoke("extractLocalDate", new Class<?>[] {Object.class, String.class, int.class},
                new Object[] {"a", "b", "c", java.sql.Date.valueOf(LocalDate.of(2026, 1, 2))}, "joinDate", 3));
        assertEquals(LocalDate.of(2026, 1, 3), invoke("extractLocalDate", new Class<?>[] {Object.class, String.class, int.class},
                new Object[] {"a", "b", "c", Timestamp.valueOf("2026-01-03 10:00:00")}, "joinDate", 3));
        Date legacyDate = Date.from(LocalDate.of(2026, 1, 4).atStartOfDay(ZoneId.systemDefault()).toInstant());
        assertEquals(LocalDate.of(2026, 1, 4), invoke("extractLocalDate", new Class<?>[] {Object.class, String.class, int.class},
                new Object[] {"a", "b", "c", legacyDate}, "joinDate", 3));
        assertNull(invoke("extractLocalDate", new Class<?>[] {Object.class, String.class, int.class},
                new Object[] {"a", "b", "c", "not-a-date"}, "joinDate", 3));
        assertNull(invoke("extractLocalDate", new Class<?>[] {Object.class, String.class, int.class},
                new Object[] {"a"}, "joinDate", 3));
        assertNull(invoke("extractValue", new Class<?>[] {Object.class, String.class, int.class}, "row", "joinDate", 3));
    }

    @Test
    void privateHelpers_CoverExceptionBranches() throws Exception {
        ValidationException nullStatus = assertPrivateThrows(ValidationException.class,
                "handleValidationStatus", new Class<?>[] {String.class}, new Object[] {null});
        assertEquals("Leave validation did not return a status", nullStatus.getMessage());

        ValidationException negativeDays = assertPrivateThrows(ValidationException.class,
                "getRequiredLeaveDays", new Class<?>[] {Map.class},
                new Object[] {Map.of("leaveDays", "-1")});
        assertTrue(negativeDays.getMessage().contains("Leave days is not calculated"));

        LeaveCreateRequestDto annualWrong = createRequest();
        annualWrong.setLeaveType("ANNUAL");
        annualWrong.setLeaveDaysMethod("DEFAULT");
        annualWrong.setAnnualLeaveType("WRONG");
        assertPrivateThrows(ValidationException.class, "validateAgainstAccumLeave",
                new Class<?>[] {LeaveCreateRequestDto.class}, new Object[] {annualWrong});

        LeaveCreateRequestDto annualEncash = createRequest();
        annualEncash.setLeaveType("ANNUAL");
        annualEncash.setLeaveDaysMethod("DEFAULT");
        annualEncash.setAnnualLeaveType("WRONG");
        annualEncash.setAnnualEncashmentRight(true);
        invoke("validateAgainstAccumLeave", new Class<?>[] {LeaveCreateRequestDto.class}, annualEncash);

        LeaveCreateRequestDto nonAnnual = createRequest();
        nonAnnual.setLeaveType("SPECIAL_LEAVE");
        invoke("validateAgainstAccumLeave", new Class<?>[] {LeaveCreateRequestDto.class}, nonAnnual);

        LeaveCreateRequestDto emergencyRunning = createRequest();
        emergencyRunning.setLeaveType("EMERGENCY");
        emergencyRunning.setEmergencyLeaveType("URGENT");
        invoke("validateAgainstAccumLeave", new Class<?>[] {LeaveCreateRequestDto.class}, emergencyRunning);

        LeaveCreateRequestDto durationAnnual = createRequest();
        durationAnnual.setLeaveType("ANNUAL");
        durationAnnual.setLeaveDaysMethod("START_END_DURATION");
        invoke("validateAgainstAccumLeave", new Class<?>[] {LeaveCreateRequestDto.class}, durationAnnual);

        LeaveCreateRequestDto validAnnual = createRequest();
        validAnnual.setLeaveType("ANNUAL");
        validAnnual.setLeaveDaysMethod("DEFAULT");
        validAnnual.setAnnualLeaveType("AGAINST_ACCUM_LEAVE");
        invoke("validateAgainstAccumLeave", new Class<?>[] {LeaveCreateRequestDto.class}, validAnnual);

        LeaveCreateRequestDto probation = createRequest();
        probation.setLeaveType("ANNUAL");
        probation.setAnnualLeaveType("AGAINST_ACCUM_LEAVE");
        probation.setLeaveStartDate(LocalDate.of(2026, 2, 1));
        when(repository.getEmployeeDetails(3L)).thenReturn(Map.of("data", Collections.emptyList()));
        invoke("validateAnnualProbation", new Class<?>[] {LeaveCreateRequestDto.class}, probation);

        when(repository.getEmployeeDetails(3L)).thenReturn(Map.of("data", List.of(
                Map.of("probation", 3, "joinDate", LocalDate.of(2026, 1, 1)))));
        assertPrivateThrows(ValidationException.class, "validateAnnualProbation",
                new Class<?>[] {LeaveCreateRequestDto.class}, new Object[] {probation});

        when(repository.getEmployeeDetails(3L)).thenReturn(Map.of("data", List.of(
                Map.of("probation", 0, "joinDate", LocalDate.of(2025, 1, 1)))));
        invoke("validateAnnualProbation", new Class<?>[] {LeaveCreateRequestDto.class}, probation);

        probation.setLeaveStartDate(LocalDate.of(2026, 12, 1));
        when(repository.getEmployeeDetails(3L)).thenReturn(Map.of("data", List.of(
                Map.of("probation", 1, "joinDate", LocalDate.of(2026, 1, 1)))));
        invoke("validateAnnualProbation", new Class<?>[] {LeaveCreateRequestDto.class}, probation);

        when(hdrRepo.countOverlappingLeaveRequests(3L, null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4)))
                .thenReturn(0L);
        when(hdrRepo.countOverlappingLeaveHistory(3L, null, "800-100", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4)))
                .thenReturn(1L);
        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("800-100");
            ValidationException ex = assertPrivateThrows(ValidationException.class, "validateNoDateOverlap",
                    new Class<?>[] {Long.class, Long.class, LocalDate.class, LocalDate.class},
                    new Object[] {null, 3L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4)});
            assertEquals("Leave request overlaps with employee leave history", ex.getMessage());
        }

        assertNull(invoke("toBigDecimal", new Class<?>[] {Object.class}, new Object[] {null}));
        assertNull(invoke("toBigDecimal", new Class<?>[] {Object.class}, " "));
        assertNull(invoke("toBigDecimal", new Class<?>[] {Object.class}, "null"));
        assertNull(invoke("toBigDecimal", new Class<?>[] {Object.class}, "bad"));
        assertEquals(new BigDecimal("12.5"), invoke("toBigDecimal", new Class<?>[] {Object.class}, "12.5"));
    }

    private LeaveCreateRequestDto createRequest() {
        LeaveCreateRequestDto request = new LeaveCreateRequestDto();
        request.setCompanyPoid(2L);
        request.setEmployeePoid(3L);
        request.setLeaveType("SPECIAL_LEAVE");
        request.setSplLeaveTypes("MARRIAGE");
        request.setLeaveStartDate(LocalDate.of(2026, 5, 1));
        request.setPlanedRejoinDate(LocalDate.of(2026, 5, 4));
        request.setEligibleLeaveDays(new BigDecimal("12"));
        return request;
    }

    private LeaveUpdateRequestDto updateRequest() {
        LeaveUpdateRequestDto request = new LeaveUpdateRequestDto();
        request.setTransactionPoid(10L);
        request.setLeaveType("SPECIAL_LEAVE");
        request.setSplLeaveTypes("MARRIAGE");
        request.setLeaveStartDate(LocalDate.of(2026, 5, 2));
        request.setPlanedRejoinDate(LocalDate.of(2026, 5, 6));
        request.setEligibleLeaveDays(new BigDecimal("12"));
        request.setContactNumber("555");
        request.setTicketRequired("Y");
        request.setHrTicketTillDate(LocalDate.of(2026, 5, 15));
        return request;
    }

    private LeaveUpdateRequestDto updateRequestEmergency() {
        LeaveUpdateRequestDto request = updateRequest();
        request.setLeaveType("EMERGENCY");
        request.setEmergencyLeaveType("URGENT");
        request.setSplLeaveTypes(null);
        return request;
    }

    private LeaveRequestDetailDto detailDto(Long detRowId, String actionType) {
        LeaveRequestDetailDto detail = new LeaveRequestDetailDto();
        detail.setDetRowId(detRowId);
        detail.setActionType(actionType);
        detail.setName("Updated");
        detail.setRelation("SON");
        detail.setTicketAgeGroup("ADULT");
        detail.setDateFrom(LocalDate.of(2026, 5, 2));
        detail.setDateTo(LocalDate.of(2026, 5, 6));
        detail.setRemarks("Updated remark");
        return detail;
    }

    private LeaveTicketUpdateRequestDto ticketRequest() {
        LeaveTicketUpdateRequestDto request = new LeaveTicketUpdateRequestDto();
        request.setTransactionPoid(10L);
        request.setTicketBookBy("HR");
        request.setTicketProcessed("Y");
        request.setTicketRemarks("Booked");
        request.setTicketsIssued(new BigDecimal("2"));
        request.setPjDocRef("PJ-1");
        return request;
    }

    private HrLeaveRequestDtl detail(Long transactionPoid, Long detRowId, String name) {
        HrLeaveRequestDtlId id = new HrLeaveRequestDtlId();
        id.setTransactionPoid(transactionPoid);
        id.setDetRowId(detRowId);

        HrLeaveRequestDtl detail = new HrLeaveRequestDtl();
        detail.setId(id);
        detail.setName(name);
        detail.setRelation("SON");
        detail.setTicketAgeGroup("ADULT");
        detail.setDateFrom(LocalDate.of(2026, 5, 1));
        detail.setDateTo(LocalDate.of(2026, 5, 4));
        detail.setRemarks("remark");
        return detail;
    }

    private void stubGetById() {
        when(hdrRepo.findById(10L)).thenReturn(Optional.of(entity));
        when(dtlRepo.findByIdTransactionPoid(10L)).thenReturn(Collections.emptyList());
        when(repository.getEligibleLeaveDays(eq(2L), eq(3L), any(LocalDate.class), isNull()))
                .thenReturn(Map.of("data", Collections.emptyList()));
    }

    private Object invoke(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = HrLeaveRequestServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(service, args);
    }

    private <T extends Throwable> T assertPrivateThrows(
            Class<T> expectedType,
            String methodName,
            Class<?>[] parameterTypes,
            Object[] args) throws Exception {
        Method method = HrLeaveRequestServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> method.invoke(service, args));
        assertTrue(expectedType.isInstance(ex.getCause()));
        return expectedType.cast(ex.getCause());
    }
}
