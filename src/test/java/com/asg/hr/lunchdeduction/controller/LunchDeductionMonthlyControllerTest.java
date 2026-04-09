package com.asg.hr.lunchdeduction.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.lunchdeduction.dto.LunchDeductionMonthlyRequestDto;
import com.asg.hr.lunchdeduction.dto.LunchDeductionMonthlyResponseDto;
import com.asg.hr.lunchdeduction.service.LunchDeductionMonthlyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LunchDeductionMonthlyControllerTest {

    @Mock
    private LunchDeductionMonthlyService lunchDeductionMonthlyService;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private LunchDeductionMonthlyController controller;

    private LunchDeductionMonthlyRequestDto requestDto;
    private LunchDeductionMonthlyResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = LunchDeductionMonthlyRequestDto.builder()
                .transactionDate(LocalDate.of(2030, 1, 1))
                .payrollMonth(LocalDate.of(2030, 1, 1))
                .lunchDescription("January Lunch")
                .build();

        responseDto = LunchDeductionMonthlyResponseDto.builder()
                .transactionPoid(1L)
                .transactionDate(LocalDate.of(2030, 1, 1))
                .payrollMonth(LocalDate.of(2030, 1, 1))
                .lunchDescription("January Lunch")
                .build();
    }

    @Test
    void list_ShouldReturnSuccessResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        FilterRequestDto filterRequestDto = new FilterRequestDto("OR", "N", List.of(new FilterDto("GLOBALSEARCH", "Lunch")));
        when(lunchDeductionMonthlyService.listLunchDeductions("800-115", filterRequestDto, pageable))
                .thenReturn(Map.of("items", List.of(), "total", 0));

        try (MockedStatic<UserContext> userContext = org.mockito.Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("800-115");

            ResponseEntity<?> entity = controller.list(pageable, filterRequestDto);

            assertNotNull(entity);
            assertEquals(200, entity.getStatusCode().value());
            verify(lunchDeductionMonthlyService).listLunchDeductions("800-115", filterRequestDto, pageable);
        }
    }

    @Test
    void getById_ShouldReturnSuccessResponse() {
        when(lunchDeductionMonthlyService.getById(1L)).thenReturn(responseDto);

        try (MockedStatic<UserContext> userContext = org.mockito.Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("800-115");
            doNothing().when(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, "800-115", "1");

            ResponseEntity<?> entity = controller.getById(1L);

            assertNotNull(entity);
            assertEquals(200, entity.getStatusCode().value());
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, "800-115", "1");
        }
    }

    @Test
    void getDetails_ShouldReturnSuccessResponse() {
        when(lunchDeductionMonthlyService.getDetails(1L)).thenReturn(responseDto);

        try (MockedStatic<UserContext> userContext = org.mockito.Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("800-115");
            doNothing().when(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, "800-115", "1");

            ResponseEntity<?> entity = controller.getDetails(1L);

            assertNotNull(entity);
            assertEquals(200, entity.getStatusCode().value());
            verify(lunchDeductionMonthlyService).getDetails(1L);
        }
    }

    @Test
    void create_ShouldReturnSuccessResponse() {
        when(lunchDeductionMonthlyService.create(requestDto)).thenReturn(responseDto);

        ResponseEntity<?> entity = controller.create(requestDto);

        assertNotNull(entity);
        assertEquals(200, entity.getStatusCode().value());
        verify(lunchDeductionMonthlyService).create(requestDto);
    }

    @Test
    void update_ShouldReturnSuccessResponse() {
        when(lunchDeductionMonthlyService.update(1L, requestDto)).thenReturn(responseDto);

        ResponseEntity<?> entity = controller.update(1L, requestDto);

        assertNotNull(entity);
        assertEquals(200, entity.getStatusCode().value());
        verify(lunchDeductionMonthlyService).update(1L, requestDto);
    }

    @Test
    void delete_ShouldReturnSuccessResponse() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        doNothing().when(lunchDeductionMonthlyService).delete(1L, deleteReasonDto);

        ResponseEntity<?> entity = controller.delete(1L, deleteReasonDto);

        assertNotNull(entity);
        assertEquals(200, entity.getStatusCode().value());
        verify(lunchDeductionMonthlyService).delete(1L, deleteReasonDto);
    }

    @Test
    void importLunchDetails_ShouldReturnSuccessResponse() {
        when(lunchDeductionMonthlyService.importLunchDetails(1L)).thenReturn(responseDto);

        ResponseEntity<?> entity = controller.importLunchDetails(1L);

        assertNotNull(entity);
        assertEquals(200, entity.getStatusCode().value());
        verify(lunchDeductionMonthlyService).importLunchDetails(1L);
    }
}