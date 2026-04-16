package com.asg.hr.allowanceanddeductionmaster.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.allowanceanddeductionmaster.dto.AllowanceDeductionRequestDTO;
import com.asg.hr.allowanceanddeductionmaster.dto.AllowanceDeductionResponseDTO;
import com.asg.hr.allowanceanddeductionmaster.service.AllowanceDeductionMasterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AllowanceDeductionMasterControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private MockedStatic<UserContext> mockedUserContext;

    @Mock
    private AllowanceDeductionMasterService service;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private AllowanceDeductionMasterController controller;

    private AllowanceDeductionRequestDTO requestDTO;
    private AllowanceDeductionResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        // Initialize ObjectMapper first
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Setup static mock before any other operations
        mockedUserContext = mockStatic(UserContext.class);
        mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-004");

        // Setup MockMvc
        PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(pageableResolver)
                .setValidator(validator)
                .build();

        // Initialize test data
        setupTestData();
    }

    private void setupTestData() {
        requestDTO = AllowanceDeductionRequestDTO.builder()
                .code("BASIC_PAY")
                .description("Basic Salary")
                .variableFixed("FIXED")
                .type("ALLOWANCE")
                .formula("BASE_SALARY")
                .glPoid(1L)
                .mandatory("Y")
                .payrollFieldName("basic_pay")
                .seqno(1)
                .active("Y")
                .build();

        responseDTO = AllowanceDeductionResponseDTO.builder()
                .allowaceDeductionPoid(1L)
                .code("BASIC_PAY")
                .description("Basic Salary")
                .variableFixed("FIXED")
                .type("ALLOWANCE")
                .formula("BASE_SALARY")
                .glPoid(1L)
                .mandatory("Y")
                .payrollFieldName("basic_pay")
                .seqno(1)
                .active("Y")
                .groupPoid(1L)
                .deleted("N")
                .createdBy("admin")
                .createdDate(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        if (mockedUserContext != null) {
            mockedUserContext.close();
        }
    }

    @Test
    void createAllowanceDeduction_Success() throws Exception {
        when(service.create(any(AllowanceDeductionRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/v1/allowance-deduction-master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());

        verify(service).create(any(AllowanceDeductionRequestDTO.class));
    }

    @Test
    void createAllowanceDeduction_Exception() throws Exception {
        when(service.create(any(AllowanceDeductionRequestDTO.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/v1/allowance-deduction-master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isInternalServerError());

        verify(service).create(any(AllowanceDeductionRequestDTO.class));
    }

    @Test
    void createAllowanceDeduction_ValidationError() throws Exception {
        AllowanceDeductionRequestDTO invalidRequest = AllowanceDeductionRequestDTO.builder()
                .code("BASIC_PAY")
                // Missing required fields
                .build();

        mockMvc.perform(post("/v1/allowance-deduction-master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any(AllowanceDeductionRequestDTO.class));
    }

    @Test
    void updateAllowanceDeduction_Success() throws Exception {
        when(service.update(anyLong(), any(AllowanceDeductionRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/v1/allowance-deduction-master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());

        verify(service).update(anyLong(), any(AllowanceDeductionRequestDTO.class));
    }

    @Test
    void updateAllowanceDeduction_Exception() throws Exception {
        when(service.update(anyLong(), any(AllowanceDeductionRequestDTO.class)))
                .thenThrow(new RuntimeException("Update failed"));

        mockMvc.perform(put("/v1/allowance-deduction-master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isInternalServerError());

        verify(service).update(anyLong(), any(AllowanceDeductionRequestDTO.class));
    }

    @Test
    void getById_Success() throws Exception {
        when(service.getById(anyLong())).thenReturn(responseDTO);
        doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

        mockMvc.perform(get("/v1/allowance-deduction-master/1"))
                .andExpect(status().isOk());

        verify(service).getById(anyLong());
    }

    @Test
    void getById_NotFound() throws Exception {
        when(service.getById(anyLong())).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/v1/allowance-deduction-master/1"))
                .andExpect(status().isNotFound());

        verify(service).getById(anyLong());
    }

    @Test
    void searchAllowanceDeductions_Success() throws Exception {
        FilterRequestDto filters = new FilterRequestDto("OR", "N", List.of());

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", List.of(responseDTO));
        responseMap.put("totalElements", 1);
        responseMap.put("totalPages", 1);

        when(service.list(any(FilterRequestDto.class), any(), any(), any())).thenReturn(responseMap);

        mockMvc.perform(post("/v1/allowance-deduction-master/search?startDate=2024-01-01&endDate=2024-12-31")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isOk());

        verify(service).list(any(FilterRequestDto.class), any(), any(), any());
    }

    @Test
    void searchAllowanceDeductions_Exception() throws Exception {
        FilterRequestDto filters = new FilterRequestDto("OR", "N", List.of());

        when(service.list(any(FilterRequestDto.class), any(), any(), any()))
                .thenThrow(new RuntimeException("Search failed"));

        mockMvc.perform(post("/v1/allowance-deduction-master/search?startDate=2024-01-01&endDate=2024-12-31")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isInternalServerError());

        verify(service).list(any(FilterRequestDto.class), any(), any(), any());
    }

    @Test
    void searchAllowanceDeductions_WithoutDates() throws Exception {
        FilterRequestDto filters = new FilterRequestDto("OR", "N", List.of());

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", List.of(responseDTO));
        responseMap.put("totalElements", 1);
        responseMap.put("totalPages", 1);

        when(service.list(any(FilterRequestDto.class), isNull(), isNull(), any())).thenReturn(responseMap);

        mockMvc.perform(post("/v1/allowance-deduction-master/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isOk());

        verify(service).list(any(FilterRequestDto.class), isNull(), isNull(), any());
    }

    @Test
    void searchAllowanceDeductions_WithoutRequestBody() throws Exception {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", List.of(responseDTO));
        responseMap.put("totalElements", 1);
        responseMap.put("totalPages", 1);

        when(service.list(isNull(), isNull(), isNull(), any())).thenReturn(responseMap);

        mockMvc.perform(post("/v1/allowance-deduction-master/search")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(service).list(isNull(), isNull(), isNull(), any());
    }

    @Test
    void deleteAllowanceDeduction_Success() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("No longer needed");

        doNothing().when(service).delete(anyLong(), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/allowance-deduction-master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk());

        verify(service).delete(anyLong(), any(DeleteReasonDto.class));
    }

    @Test
    void deleteAllowanceDeduction_WithoutRequestBody() throws Exception {
        doNothing().when(service).delete(anyLong(), isNull());

        mockMvc.perform(delete("/v1/allowance-deduction-master/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(service).delete(anyLong(), isNull());
    }

    @Test
    void deleteAllowanceDeduction_Exception() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("No longer needed");

        doThrow(new RuntimeException("Delete failed")).when(service).delete(anyLong(), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/allowance-deduction-master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isNotFound()); // Controller returns 404 for exceptions in delete method

        verify(service).delete(anyLong(), any(DeleteReasonDto.class));
    }
}
