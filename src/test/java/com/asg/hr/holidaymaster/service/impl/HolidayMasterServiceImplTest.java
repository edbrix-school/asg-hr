package com.asg.hr.holidaymaster.service.impl;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.holidaymaster.dto.HolidayBatchCreateRequest;
import com.asg.hr.holidaymaster.dto.HolidayMasterRequest;
import com.asg.hr.holidaymaster.dto.HolidayMasterResponse;
import com.asg.hr.holidaymaster.entity.HolidayMasterEntity;
import com.asg.hr.holidaymaster.repository.HolidayMasterRepository;
import com.asg.hr.holidaymaster.util.HolidayMasterMapper;
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
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayMasterServiceImplTest {

    @Mock
    private HolidayMasterRepository repository;

    @Mock
    private HolidayMasterMapper mapper;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private LoggingService loggingService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @InjectMocks
    private HolidayMasterServiceImpl service;

    private HolidayMasterEntity entity;
    private HolidayMasterRequest request;
    private HolidayMasterResponse response;

    @BeforeEach
    void setUp() {
        entity = HolidayMasterEntity.builder()
                .holidayPoid(1L)
                .holidayDate(LocalDate.of(2030, 1, 1))
                .holidayReason("New Year")
                .active("Y")
                .deleted("N")
                .status("O")
                .seqno(BigInteger.ONE)
                .build();

        request = new HolidayMasterRequest();
        request.setHolidayDate(LocalDate.of(2030, 1, 1));
        request.setHolidayReason("New Year");
        request.setSeqNo(1);
        request.setActive("Y");

        response = new HolidayMasterResponse();
        response.setHolidayPoid(1L);
        response.setHolidayDate(LocalDate.of(2030, 1, 1));
        response.setHolidayReason("New Year");
    }

    @Test
    void listHolidays_ReturnsPaginatedMap() {
        FilterDto filter = new FilterDto("GLOBALSEARCH", "new");
        FilterRequestDto filterRequest = new FilterRequestDto("OR", "N", List.of(filter));
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of("HOLIDAY_REASON", "New Year", "HOLIDAY_POID", 1L)),
                Map.of("HOLIDAY_REASON", "Reason"),
                1L
        );

        when(documentService.resolveOperator(filterRequest)).thenReturn("OR");
        when(documentService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentService.resolveFilters(filterRequest)).thenReturn(List.of(filter));
        when(documentService.search(eq("800-011"), any(), eq("OR"), eq(pageable), eq("N"), eq("HOLIDAY_REASON"), eq("HOLIDAY_POID")))
                .thenReturn(raw);

        Map<String, Object> result = service.listHolidays("800-011", filterRequest, pageable);

        assertNotNull(result);
        verify(documentService).search(eq("800-011"), any(), eq("OR"), eq(pageable), eq("N"),
                eq("HOLIDAY_REASON"), eq("HOLIDAY_POID"));
    }

    @Test
    void getById_WhenFound_ReturnsResponse() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        HolidayMasterResponse result = service.getById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getHolidayPoid());
        verify(mapper).toResponse(entity);
    }

    @Test
    void getById_WhenNotFound_ThrowsException() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(1L));
    }

    @Test
    void create_WhenDuplicateDate_ThrowsValidationException() {
        when(repository.existsByHolidayDate(request.getHolidayDate())).thenReturn(true);
        assertThrows(ValidationException.class, () -> service.create(request));
    }

    @Test
    void create_WhenSuccess_UsesFallbackSystemUserIfContextMissing() {
        when(repository.existsByHolidayDate(request.getHolidayDate())).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);
        doNothing().when(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.CREATED), any(), eq("1"));

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getUserId).thenReturn(null);
            userContext.when(UserContext::getDocumentId).thenReturn("800-011");

            HolidayMasterResponse result = service.create(request);

            assertNotNull(result);
            verify(mapper).toEntity(request);
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "800-011", "1");
        }
    }

    @Test
    void update_WhenDuplicateDate_ThrowsValidationException() {
        HolidayMasterRequest updateRequest = new HolidayMasterRequest();
        updateRequest.setHolidayDate(LocalDate.of(2030, 12, 25));

        when(repository.findByHolidayPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(entity));
        when(repository.existsByHolidayDate(LocalDate.of(2030, 12, 25))).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.update(1L, updateRequest));
    }

    @Test
    void update_WhenNotFound_ThrowsException() {
        when(repository.findByHolidayPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.update(1L, request));
    }

    @Test
    void update_WhenSuccess_LogsChanges() {
        HolidayMasterRequest updateRequest = new HolidayMasterRequest();
        updateRequest.setHolidayReason("Updated");

        when(repository.findByHolidayPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);
        when(repository.save(entity)).thenReturn(entity);

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getUserId).thenReturn("user1");
            userContext.when(UserContext::getDocumentId).thenReturn("800-011");

            HolidayMasterResponse result = service.update(1L, updateRequest);

            assertNotNull(result);
            verify(mapper).updateEntity(entity, updateRequest);
            verify(loggingService).logChanges(any(), eq(entity), eq(HolidayMasterEntity.class), eq("800-011"),
                    eq("1"), eq(LogDetailsEnum.MODIFIED), eq("HOLIDAY_POID"));
        }
    }

    @Test
    void delete_WhenSuccess_CallsDocumentDeleteService() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        when(documentDeleteService.deleteDocument(
                eq(1L),
                eq("HR_HOLIDAY_MASTER"),
                eq("HOLIDAY_POID"),
                eq(deleteReasonDto),
                any(LocalDate.class)
        )).thenReturn("SUCCESS");

        assertDoesNotThrow(() -> service.delete(1L, deleteReasonDto));

        verify(documentDeleteService).deleteDocument(
                eq(1L),
                eq("HR_HOLIDAY_MASTER"),
                eq("HOLIDAY_POID"),
                eq(deleteReasonDto),
                any(LocalDate.class)
        );
    }

    @Test
    void delete_WhenNotFound_ThrowsException() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(1L, deleteReasonDto));
    }

    @Test
    void batchCreateHolidays_WhenSuccess_ReturnsStatus() {
        HolidayBatchCreateRequest batchRequest = new HolidayBatchCreateRequest(
                LocalDate.of(2030, 1, 1),
                "Festival",
                2
        );

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class);
             MockedConstruction<SimpleJdbcCall> mockedConstruction = Mockito.mockConstruction(
                     SimpleJdbcCall.class,
                     (mock, context) -> {
                         when(mock.withProcedureName(anyString())).thenReturn(mock);
                         when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                         when(mock.execute(anyMap())).thenReturn(Map.of("P_STATUS", "SUCCESS"));
                     })) {

            userContext.when(UserContext::getUserPoid).thenReturn(99L);

            String status = service.batchCreateHolidays(batchRequest);

            assertEquals("SUCCESS", status);
            assertEquals(1, mockedConstruction.constructed().size());
        }
    }

    @Test
    void batchCreateHolidays_WhenProcedureFails_ThrowsValidationException() {
        HolidayBatchCreateRequest batchRequest = new HolidayBatchCreateRequest(
                LocalDate.of(2030, 1, 1),
                "Festival",
                2
        );

        RuntimeException rootCause = new RuntimeException("Procedure failed");

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class);
             MockedConstruction<SimpleJdbcCall> mockedConstruction = Mockito.mockConstruction(
                     SimpleJdbcCall.class,
                     (mock, context) -> {
                         when(mock.withProcedureName(anyString())).thenReturn(mock);
                         when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                         when(mock.execute(anyMap()))
                                 .thenThrow(new DataAccessResourceFailureException("DB error", rootCause));
                     })) {

            userContext.when(UserContext::getUserPoid).thenReturn(99L);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.batchCreateHolidays(batchRequest));

            assertTrue(ex.getMessage().contains("Procedure failed"));
            assertEquals(1, mockedConstruction.constructed().size());
        }
    }
}
