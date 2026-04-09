package com.asg.hr.lunchdeduction.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.lunchdeduction.dto.LunchDeductionActionType;
import com.asg.hr.lunchdeduction.dto.LunchDeductionDetailRequestDto;
import com.asg.hr.lunchdeduction.dto.LunchDeductionMonthlyRequestDto;
import com.asg.hr.lunchdeduction.dto.LunchDeductionMonthlyResponseDto;
import com.asg.hr.lunchdeduction.entity.LunchDeductionMonthlyDetail;
import com.asg.hr.lunchdeduction.entity.LunchDeductionMonthlyDetailId;
import com.asg.hr.lunchdeduction.entity.LunchDeductionMonthlyHeader;
import com.asg.hr.lunchdeduction.repository.LunchDeductionMonthlyRepository;
import com.asg.hr.lunchdeduction.util.LunchDeductionMonthlyMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LunchDeductionMonthlyServiceImplTest {

    @Mock
    private LunchDeductionMonthlyRepository repository;

    @Mock
    private DocumentSearchService documentSearchService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private LoggingService loggingService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private LunchDeductionMonthlyServiceImpl service;

    private LunchDeductionMonthlyMapper mapper;
    private LunchDeductionMonthlyHeader header;

    @BeforeEach
    void setUp() {
        mapper = new LunchDeductionMonthlyMapper();
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        header = LunchDeductionMonthlyHeader.builder()
                .transactionPoid(1L)
                .groupPoid(10L)
                .companyPoid(20L)
                .transactionDate(LocalDate.of(2030, 1, 1))
                .payrollMonth(LocalDate.of(2030, 1, 1))
                .lunchDescription("January Lunch")
                .remarks("remarks")
                .deleted("N")
                .details(new java.util.ArrayList<>(List.of(
                        LunchDeductionMonthlyDetail.builder()
                                .id(LunchDeductionMonthlyDetailId.builder().transactionPoid(1L).detRowId(1L).build())
                                .employeePoid(100L)
                                .deductionType("DEDUCT")
                                .monthDays(22L)
                                .offDays(2L)
                                .costPerDay(BigDecimal.valueOf(5))
                                .remarks("old")
                                .build()
                )))
                .build();
        header.getDetails().forEach(detail -> detail.setHeader(header));
        mapper.recalculateDetail(header.getDetails().getFirst());
    }

    @Test
    void listLunchDeductions_ReturnsPaginatedMap() {
        FilterDto filter = new FilterDto("GLOBALSEARCH", "Lunch");
        FilterRequestDto filterRequestDto = new FilterRequestDto("OR", "N", List.of(filter));
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of("LUNCH_DESCRIPTION", "January Lunch", "TRANSACTION_POID", 1L)),
                Map.of("LUNCH_DESCRIPTION", "Description"),
                1L
        );

        when(documentSearchService.resolveOperator(filterRequestDto)).thenReturn("OR");
        when(documentSearchService.resolveIsDeleted(filterRequestDto)).thenReturn("N");
        when(documentSearchService.resolveFilters(filterRequestDto)).thenReturn(List.of(filter));
        when(documentSearchService.search(eq("800-115"), any(), eq("OR"), eq(pageable), eq("N"), eq("LUNCH_DESCRIPTION"), eq("TRANSACTION_POID")))
                .thenReturn(raw);

        Map<String, Object> result = service.listLunchDeductions("800-115", filterRequestDto, pageable);

        assertNotNull(result);
        verify(documentSearchService).search(eq("800-115"), any(), eq("OR"), eq(pageable), eq("N"), eq("LUNCH_DESCRIPTION"), eq("TRANSACTION_POID"));
    }

    @Test
    void getById_WhenFound_ReturnsResponse() {
        when(repository.findDetailedByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(header));

        LunchDeductionMonthlyResponseDto responseDto = service.getById(1L);

        assertEquals(1L, responseDto.getTransactionPoid());
        assertEquals(1L, responseDto.getDetailCount());
    }

    @Test
    void getById_WhenMissing_ThrowsException() {
        when(repository.findDetailedByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(1L));
    }

    @Test
    void create_WhenSuccess_LogsAndReturnsFreshEntity() {
        LunchDeductionMonthlyRequestDto requestDto = LunchDeductionMonthlyRequestDto.builder()
                .transactionDate(LocalDate.of(2030, 1, 1))
                .payrollMonth(LocalDate.of(2030, 1, 1))
                .lunchDescription("January Lunch")
                .build();

        when(repository.saveAndFlush(any(LunchDeductionMonthlyHeader.class))).thenAnswer(invocation -> {
            LunchDeductionMonthlyHeader saved = invocation.getArgument(0);
            saved.setTransactionPoid(1L);
            return saved;
        });
        when(repository.findDetailedByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(header));
        doNothing().when(entityManager).clear();
        doNothing().when(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "800-115", "1");

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(10L);
            userContext.when(UserContext::getCompanyPoid).thenReturn(20L);
            userContext.when(UserContext::getDocumentId).thenReturn("800-115");

            LunchDeductionMonthlyResponseDto responseDto = service.create(requestDto);

            assertEquals(1L, responseDto.getTransactionPoid());
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "800-115", "1");
        }
    }

    @Test
    void update_WhenDetailActionChangesData_LogsSummaryAndReturnsDocument() {
        LunchDeductionMonthlyRequestDto requestDto = LunchDeductionMonthlyRequestDto.builder()
                .transactionDate(LocalDate.of(2030, 1, 2))
                .payrollMonth(LocalDate.of(2030, 1, 1))
                .lunchDescription("Updated Lunch")
                .details(List.of(
                        LunchDeductionDetailRequestDto.builder()
                                .detRowId(1L)
                                .monthDays(22L)
                                .offDays(3L)
                                .costPerDay(BigDecimal.valueOf(5))
                                .remarks("new")
                                .deductionType("DEDUCT")
                                .actionType(LunchDeductionActionType.UPDATE)
                                .build()
                ))
                .build();

        when(repository.findDetailedByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(header));
        when(repository.saveAndFlush(any(LunchDeductionMonthlyHeader.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(entityManager).clear();
        doNothing().when(loggingService).logChanges(any(), any(), eq(com.asg.hr.lunchdeduction.entity.LunchDeductionMonthlyHeader.class), eq("800-115"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
        doNothing().when(loggingService).createLogSummaryEntry(eq("800-115"), eq("1"), anyString());

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("800-115");

            LunchDeductionMonthlyResponseDto responseDto = service.update(1L, requestDto);

            assertEquals("Updated Lunch", responseDto.getLunchDescription());
            verify(loggingService).createLogSummaryEntry(eq("800-115"), eq("1"), anyString());
        }
    }

    @Test
    void update_WhenDetailOffDaysExceedMonthDays_ThrowsValidationException() {
        LunchDeductionMonthlyRequestDto requestDto = LunchDeductionMonthlyRequestDto.builder()
                .payrollMonth(LocalDate.of(2030, 1, 1))
                .details(List.of(
                        LunchDeductionDetailRequestDto.builder()
                                .detRowId(1L)
                                .monthDays(2L)
                                .offDays(3L)
                                .actionType(LunchDeductionActionType.UPDATE)
                                .build()
                ))
                .build();

        when(repository.findDetailedByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(header));

        assertThrows(ValidationException.class, () -> service.update(1L, requestDto));
    }

    @Test
    void delete_WhenFound_DelegatesToDocumentDeleteService() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        when(repository.findDetailedByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(header));
        when(documentDeleteService.deleteDocument(eq(1L), eq("HR_MONTHLY_LUNCH_HDR"), eq("TRANSACTION_POID"), eq(deleteReasonDto), eq(LocalDate.of(2030, 1, 1))))
                .thenReturn("SUCCESS");

        assertDoesNotThrow(() -> service.delete(1L, deleteReasonDto));
        verify(documentDeleteService).deleteDocument(eq(1L), eq("HR_MONTHLY_LUNCH_HDR"), eq("TRANSACTION_POID"), eq(deleteReasonDto), eq(LocalDate.of(2030, 1, 1)));
    }

    @Test
    void importLunchDetails_WhenSuccess_ReturnsReloadedDocument() {
        when(repository.findDetailedByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(header));
        doNothing().when(entityManager).clear();
        doNothing().when(loggingService).createLogSummaryEntry("800-115", "1", "Lunch Records loaded..");

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class);
             MockedConstruction<SimpleJdbcCall> mockedConstruction = Mockito.mockConstruction(
                     SimpleJdbcCall.class,
                     (mock, context) -> {
                         when(mock.withProcedureName(anyString())).thenReturn(mock);
                         when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                         when(mock.execute(anyMap())).thenReturn(Map.of("P_STATUS", "SUCCESS: Attendance imported...."));
                     })) {

            userContext.when(UserContext::getUserPoid).thenReturn(99L);
            userContext.when(UserContext::getDocumentId).thenReturn("800-115");

            LunchDeductionMonthlyResponseDto responseDto = service.importLunchDetails(1L);

            assertEquals(1L, responseDto.getTransactionPoid());
            assertEquals(1, mockedConstruction.constructed().size());
        }
    }

    @Test
    void importLunchDetails_WhenProcedureReturnsError_ThrowsValidationException() {
        when(repository.findDetailedByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(header));

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class);
             MockedConstruction<SimpleJdbcCall> mockedConstruction = Mockito.mockConstruction(
                     SimpleJdbcCall.class,
                     (mock, context) -> {
                         when(mock.withProcedureName(anyString())).thenReturn(mock);
                         when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                         when(mock.execute(anyMap())).thenReturn(Map.of("P_STATUS", "ERROR : Payroll already processed"));
                     })) {

            userContext.when(UserContext::getUserPoid).thenReturn(99L);

            ValidationException exception = assertThrows(ValidationException.class, () -> service.importLunchDetails(1L));
            assertEquals("ERROR : Payroll already processed", exception.getMessage());
            assertEquals(1, mockedConstruction.constructed().size());
        }
    }

    @Test
    void importLunchDetails_WhenDatabaseFails_ThrowsValidationException() {
        when(repository.findDetailedByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(header));

        RuntimeException rootCause = new RuntimeException("Procedure failed");
        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class);
             MockedConstruction<SimpleJdbcCall> mockedConstruction = Mockito.mockConstruction(
                     SimpleJdbcCall.class,
                     (mock, context) -> {
                         when(mock.withProcedureName(anyString())).thenReturn(mock);
                         when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                         when(mock.execute(anyMap())).thenThrow(new DataAccessResourceFailureException("DB error", rootCause));
                     })) {

            userContext.when(UserContext::getUserPoid).thenReturn(99L);

            ValidationException exception = assertThrows(ValidationException.class, () -> service.importLunchDetails(1L));
            assertEquals("Error while loading lunch deduction details: Procedure failed", exception.getMessage());
            assertEquals(1, mockedConstruction.constructed().size());
        }
    }
}