package com.asg.hr.locationmaster.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.hr.exceptions.GlobalExceptionHandler;
import com.asg.hr.locationmaster.dto.LocationMasterRequestDto;
import com.asg.hr.locationmaster.dto.LocationMasterResponseDto;
import com.asg.hr.locationmaster.service.LocationMasterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = LocationMasterController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.asg.hr.aspect.*"))
@ContextConfiguration(classes = {LocationMasterController.class, GlobalExceptionHandler.class})
class LocationMasterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LocationMasterService service;

    @MockitoBean
    private LoggingService loggingService;

    @MockitoBean
    private LovDataService lovDataService;

    private LocationMasterRequestDto requestDto;
    private LocationMasterResponseDto responseDto;

    @BeforeEach
    void setUp() {
        reset(service, loggingService, lovDataService);
        
        requestDto = LocationMasterRequestDto.builder()
                .locationCode("LOC001")
                .locationName("Main Location")
                .locationName2("Main Location 2")
                .address("123 Main Street")
                .siteSupervisorUserPoid(1L)
                .active("Y")
                .seqno(1)
                .build();

        responseDto = LocationMasterResponseDto.builder()
                .locationPoid(1L)
                .locationCode("LOC001")
                .locationName("Main Location")
                .locationName2("Main Location 2")
                .address("123 Main Street")
                .siteSupervisorUserPoid(1L)
                .active("Y")
                .seqno(1)
                .build();
    }

    // ─── CREATE ──────────────────────────────────────────────────────────────

    @Test
    void create_Success() throws Exception {
        when(service.create(any(LocationMasterRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/v1/location/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Location created successfully"))
                .andExpect(jsonPath("$.result.data.locationPoid").value(1L))
                .andExpect(jsonPath("$.result.data.locationCode").value("LOC001"));

        verify(service).create(any(LocationMasterRequestDto.class));
    }

    @Test
    void create_MissingRequired_Returns400() throws Exception {
        LocationMasterRequestDto invalidDto = LocationMasterRequestDto.builder().build();

        mockMvc.perform(post("/v1/location/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
    }

    @Test
    void create_LocationCodeTooLong_Returns400() throws Exception {
        LocationMasterRequestDto invalidDto = LocationMasterRequestDto.builder()
                .locationCode("a".repeat(21)) // Exceeds 20 character limit
                .locationName("Test Location")
                .build();

        mockMvc.perform(post("/v1/location/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
    }

    @Test
    void create_LocationNameTooLong_Returns400() throws Exception {
        LocationMasterRequestDto invalidDto = LocationMasterRequestDto.builder()
                .locationCode("LOC001")
                .locationName("a".repeat(101)) // Exceeds 100 character limit
                .build();

        mockMvc.perform(post("/v1/location/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
    }

    @Test
    void create_DuplicateLocationCode_Returns409() throws Exception {
        when(service.create(any(LocationMasterRequestDto.class)))
                .thenThrow(new ResourceAlreadyExistsException("Location", "LOC001"));

        mockMvc.perform(post("/v1/location/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict());

        verify(service).create(any(LocationMasterRequestDto.class));
    }

    @Test
    void create_InvalidJson_Returns400() throws Exception {
        mockMvc.perform(post("/v1/location/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid json"))
                .andExpect(status().isBadRequest());
    }

    // ─── GET ─────────────────────────────────────────────────────────────────

    @Test
    void getById_Success() throws Exception {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getDocumentId).thenReturn("500-004");
            when(service.getById(1L)).thenReturn(responseDto);

            mockMvc.perform(get("/v1/location/master/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Location retrieved successfully"))
                    .andExpect(jsonPath("$.result.data.locationPoid").value(1L))
                    .andExpect(jsonPath("$.result.data.locationCode").value("LOC001"));

            verify(service).getById(1L);
            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.VIEWED), eq("500-004"), eq("1"));
        }
    }

    @Test
    void getById_NotFound_Returns404() throws Exception {
        when(service.getById(1L)).thenThrow(new ResourceNotFoundException("Location", "id", 1L));

        mockMvc.perform(get("/v1/location/master/1"))
                .andExpect(status().isNotFound());

        verify(service).getById(1L);
    }

    @Test
    void getById_InvalidId_Returns400() throws Exception {
        mockMvc.perform(get("/v1/location/master/invalid"))
                .andExpect(status().isBadRequest());
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    @Test
    void update_Success() throws Exception {
        when(service.update(eq(1L), any(LocationMasterRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/v1/location/master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Location updated successfully"))
                .andExpect(jsonPath("$.result.data.locationPoid").value(1L));

        verify(service).update(eq(1L), any(LocationMasterRequestDto.class));
    }

    @Test
    void update_NotFound_Returns404() throws Exception {
        when(service.update(eq(1L), any(LocationMasterRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Location", "id", 1L));

        mockMvc.perform(put("/v1/location/master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());

        verify(service).update(eq(1L), any(LocationMasterRequestDto.class));
    }

    @Test
    void update_MissingRequired_Returns400() throws Exception {
        LocationMasterRequestDto invalidDto = LocationMasterRequestDto.builder().build();

        mockMvc.perform(put("/v1/location/master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(service, never()).update(anyLong(), any());
    }

    @Test
    void update_DuplicateLocationCode_Returns409() throws Exception {
        when(service.update(eq(1L), any(LocationMasterRequestDto.class)))
                .thenThrow(new ResourceAlreadyExistsException("Location", "LOC001"));

        mockMvc.perform(put("/v1/location/master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict());

        verify(service).update(eq(1L), any(LocationMasterRequestDto.class));
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @Test
    void delete_Success() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("No longer needed");

        doNothing().when(service).delete(eq(1L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/location/master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Location deleted successfully"));

        verify(service).delete(eq(1L), any(DeleteReasonDto.class));
    }

    @Test
    void delete_WithoutBody_Returns400() throws Exception {
        mockMvc.perform(delete("/v1/location/master/1"))
                .andExpect(status().isBadRequest());

        verify(service, never()).delete(anyLong(), any());
    }

    @Test
    void delete_NotFound_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Location", "id", 1L))
                .when(service).delete(eq(1L), any(DeleteReasonDto.class));

        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Test deletion");

        mockMvc.perform(delete("/v1/location/master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isNotFound());

        verify(service).delete(eq(1L), any(DeleteReasonDto.class));
    }

    // ─── LIST ────────────────────────────────────────────────────────────────

    @Test
    void list_Success() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("data", Collections.emptyList());
        data.put("totalRecords", 0);

        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getDocumentId).thenReturn("500-004");
            when(service.list(eq("500-004"), any(), any(Pageable.class))).thenReturn(data);

            mockMvc.perform(post("/v1/location/master/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Locations retrieved successfully"));

            verify(service).list(eq("500-004"), any(), any(Pageable.class));
        }
    }

    @Test
    void list_WithFilters_Success() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("data", Collections.emptyList());
        data.put("totalRecords", 0);

        FilterRequestDto filters = new FilterRequestDto("AND", "N", Collections.emptyList());

        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getDocumentId).thenReturn("500-004");
            when(service.list(eq("500-004"), any(FilterRequestDto.class), any(Pageable.class)))
                    .thenReturn(data);

            mockMvc.perform(post("/v1/location/master/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filters))
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(service).list(eq("500-004"), any(FilterRequestDto.class), any(Pageable.class));
        }
    }

    @Test
    void list_NoBody_Success() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("data", Collections.emptyList());

        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getDocumentId).thenReturn("500-004");
            when(service.list(eq("500-004"), isNull(), any(Pageable.class))).thenReturn(data);

            mockMvc.perform(post("/v1/location/master/list")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());

            verify(service).list(eq("500-004"), isNull(), any(Pageable.class));
        }
    }

    @Test
    void list_ServiceException_Returns500() throws Exception {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getDocumentId).thenReturn("500-004");
            when(service.list(anyString(), any(), any(Pageable.class)))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(post("/v1/location/master/list")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Test
    void create_ServiceException_Returns500() throws Exception {
        when(service.create(any(LocationMasterRequestDto.class)))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/v1/location/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void update_ServiceException_Returns500() throws Exception {
        when(service.update(eq(1L), any(LocationMasterRequestDto.class)))
                .thenThrow(new RuntimeException("Update service error"));

        mockMvc.perform(put("/v1/location/master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void delete_ServiceException_Returns500() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Test deletion");
        
        doThrow(new RuntimeException("Delete service error"))
                .when(service).delete(eq(1L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/location/master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getById_ServiceException_Returns500() throws Exception {
        when(service.getById(1L)).thenThrow(new RuntimeException("Get service error"));

        mockMvc.perform(get("/v1/location/master/1"))
                .andExpect(status().isInternalServerError());
    }
}