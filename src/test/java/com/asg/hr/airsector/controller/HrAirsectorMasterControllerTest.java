package com.asg.hr.airsector.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.airsector.dto.HrAirsectorRequestDto;
import com.asg.hr.airsector.dto.HrAirsectorResponseDto;
import com.asg.hr.airsector.service.HrAirsectorService;
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
 class HrAirsectorMasterControllerTest {

    @Mock
    private HrAirsectorService service;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private HrAirsectorMasterController controller;

    @Test
    void testCreateAirsectorMaster_Success() {
        HrAirsectorRequestDto request = new HrAirsectorRequestDto();
        request.setAirsectorDescription("Test Sector");
        request.setActive("Y");
        request.setSeqno(1);
        request.setAverageTicketRate(new BigDecimal("1000.00"));

        HrAirsectorResponseDto response = HrAirsectorResponseDto.builder()
                .airsecPoid(1L)
                .airsectorDescription("TEST SECTOR")
                .active("Y")
                .build();

        when(service.create(any(HrAirsectorRequestDto.class))).thenReturn(response);

        ResponseEntity<?> result = controller.createAirsectorMaster(request);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service).create(any(HrAirsectorRequestDto.class));
    }

    @Test
    void testCreateAirsectorMaster_BadRequest() {
        HrAirsectorRequestDto request = new HrAirsectorRequestDto();
        request.setAirsectorDescription("Test Sector");

        when(service.create(any(HrAirsectorRequestDto.class)))
                .thenThrow(new ValidationException("Invalid input"));

        ResponseEntity<?> result = controller.createAirsectorMaster(request);

        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void testUpdateAirsectorMaster_Success() {
        Long airsecPoid = 1L;
        HrAirsectorRequestDto request = new HrAirsectorRequestDto();
        request.setAirsectorDescription("Updated Sector");
        request.setActive("Y");

        HrAirsectorResponseDto response = HrAirsectorResponseDto.builder()
                .airsecPoid(airsecPoid)
                .airsectorDescription("UPDATED SECTOR")
                .active("Y")
                .build();

        when(service.update(eq(airsecPoid), any(HrAirsectorRequestDto.class))).thenReturn(response);

        ResponseEntity<?> result = controller.updateAirsectorMaster(airsecPoid, request);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service).update(eq(airsecPoid), any(HrAirsectorRequestDto.class));
    }

    @Test
    void testUpdateAirsectorMaster_InternalServerError() {
        Long airsecPoid = 1L;
        HrAirsectorRequestDto request = new HrAirsectorRequestDto();
        request.setAirsectorDescription("Test");

        when(service.update(eq(airsecPoid), any(HrAirsectorRequestDto.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<?> result = controller.updateAirsectorMaster(airsecPoid, request);

        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void testDeleteAirsectorMaster_Success() {
        Long airsecPoid = 1L;
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();

        doNothing().when(service).deleteAirsectorMaster(airsecPoid, deleteReasonDto);

        ResponseEntity<?> result = controller.deleteAirsectorMaster(airsecPoid, deleteReasonDto);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service).deleteAirsectorMaster(airsecPoid, deleteReasonDto);
    }

    @Test
    void testGetAirsectorById_Success() {
        Long airsecPoid = 1L;
        HrAirsectorResponseDto response = HrAirsectorResponseDto.builder()
                .airsecPoid(airsecPoid)
                .airsectorDescription("TEST SECTOR")
                .active("Y")
                .build();

        when(service.findById(airsecPoid)).thenReturn(response);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            ResponseEntity<?> result = controller.getAirsectorById(airsecPoid);

            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
            verify(service).findById(airsecPoid);
            verify(loggingService).createLogSummaryEntry((LogDetailsEnum) any(), any(), eq(airsecPoid.toString()));
        }
    }

    @Test
    void testGetAirsectorById_NotFound() {
        Long airsecPoid = 1L;

        when(service.findById(airsecPoid))
                .thenThrow(new ResourceNotFoundException("Airsector not found", "AirsecPoid", airsecPoid));

        assertThrows(ResourceNotFoundException.class, () -> controller.getAirsectorById(airsecPoid));
        verify(service).findById(airsecPoid);
    }

    @Test
    void testGetAirsectorList_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        FilterRequestDto filters = new FilterRequestDto("AND", "N", null);
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        Map<String, Object> mockData = new HashMap<>();
        mockData.put("content", "test data");
        mockData.put("totalElements", 10);

        when(service.listOfRecordsAndGenericSearch(anyString(), any(), any(Pageable.class), 
                any(LocalDate.class), any(LocalDate.class))).thenReturn(mockData);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            ResponseEntity<?> result = controller.getAirsectorList(pageable, filters, startDate, endDate);

            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
            verify(service).listOfRecordsAndGenericSearch(anyString(), any(), any(Pageable.class), 
                    any(LocalDate.class), any(LocalDate.class));
        }
    }

    @Test
    void testGetAirsectorList_InternalServerError() {
        Pageable pageable = PageRequest.of(0, 10);
        FilterRequestDto filters = new FilterRequestDto("AND", "N", null);

        when(service.listOfRecordsAndGenericSearch(anyString(), any(), any(Pageable.class), 
                any(), any())).thenThrow(new RuntimeException("Database error"));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            ResponseEntity<?> result = controller.getAirsectorList(pageable, filters, null, null);

            assertNotNull(result);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        }
    }
}
