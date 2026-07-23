package com.asg.hr.employeeinduction.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.exceptions.GlobalExceptionHandler;
import com.asg.hr.employeeinduction.dto.EmployeeInductionRequestDto;
import com.asg.hr.employeeinduction.dto.EmployeeInductionResponseDto;
import com.asg.hr.employeeinduction.dto.InductionCategoryDto;
import com.asg.hr.employeeinduction.service.EmployeeInductionService;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = EmployeeInductionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.asg.hr.aspect.*"))
@ContextConfiguration(classes = {EmployeeInductionController.class, GlobalExceptionHandler.class})
class EmployeeInductionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeInductionService employeeInductionService;

    @MockitoBean
    private LoggingService loggingService;

    private EmployeeInductionRequestDto requestDto;
    private EmployeeInductionResponseDto responseDto;

    @BeforeEach
    void setUp() {
        reset(employeeInductionService, loggingService);
        objectMapper.findAndRegisterModules(); // ✅ fixes LocalDate serialization

        requestDto = EmployeeInductionRequestDto.builder()
                .employeePoid(1L)
                .remarks("Test induction")
                .details(createTestDetails())
                .build();

        responseDto = EmployeeInductionResponseDto.builder()
                .poid(1L)
                .docId("IND-001")
                .employeePoid(1L)
                .remarks("Test induction")
                .details(createTestResponseDetails())
                .build();
    }

    @Test
    void createInduction_Success() throws Exception {
        when(employeeInductionService.createInduction(any()))
                .thenReturn(responseDto);

        mockMvc.perform(post("/v1/employee-induction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.data.poid").value(1));

        verify(employeeInductionService).createInduction(any());
    }

    @Test
    void createInduction_InvalidInput_ReturnsError() throws Exception {
        when(employeeInductionService.createInduction(any()))
                .thenThrow(new IllegalArgumentException("Employee is required"));

        mockMvc.perform(post("/v1/employee-induction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateInduction_Success() throws Exception {
        when(employeeInductionService.updateInduction(eq(1L), any()))
                .thenReturn(responseDto);

        mockMvc.perform(put("/v1/employee-induction/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(employeeInductionService).updateInduction(eq(1L), any());
    }

    @Test
    void getInductionById_Success() throws Exception {
        when(employeeInductionService.getInductionById(1L))
                .thenReturn(responseDto);

        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mocked.when(UserContext::getDocumentId).thenReturn("DOC123");

            mockMvc.perform(get("/v1/employee-induction/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.data.poid").value(1));

            verify(loggingService)
                    .createLogSummaryEntry(any(LogDetailsEnum.class), any(), eq("1"));
        }
    }

    @Test
    void getInductionsByEmployee_Success() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("inductions", List.of(responseDto));
        result.put("totalCount", 1);

        when(employeeInductionService.getInductionsByEmployee(1L))
                .thenReturn(result);

        mockMvc.perform(get("/v1/employee-induction/employee/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.data.totalCount").value(1));

        verify(employeeInductionService).getInductionsByEmployee(1L);
    }

    @Test
    void list_Success() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("content", List.of(responseDto));
        result.put("totalElements", 1);

        when(employeeInductionService.list(any(), any(), any(), any(Pageable.class)))
                .thenReturn(result);

        FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", List.of());

        mockMvc.perform(post("/v1/employee-induction/list")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(employeeInductionService)
                .list(any(), any(), any(), any(Pageable.class));
    }

    @Test
    void deleteInduction_Success() throws Exception {
        doNothing().when(employeeInductionService)
                .deleteInduction(eq(1L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/employee-induction/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeleteReasonDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(employeeInductionService)
                .deleteInduction(eq(1L), any(DeleteReasonDto.class));
    }

    @Test
    void sendOverdueNotifications_Success() throws Exception {
        doNothing().when(employeeInductionService).sendOverdueNotifications();

        mockMvc.perform(post("/v1/employee-induction/send-overdue-notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(employeeInductionService).sendOverdueNotifications();
    }

    @Test
    void loadInductionByEmployee_Success() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("inductions", List.of(responseDto));
        result.put("totalCount", 1);

        when(employeeInductionService.loadInductionByEmployee(1L))
                .thenReturn(result);

        mockMvc.perform(get("/v1/employee-induction/load/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.data.totalCount").value(1));

        verify(employeeInductionService).loadInductionByEmployee(1L);
    }

    @Test
    void getInductionCategories_Success() throws Exception {
        List<InductionCategoryDto> categories = List.of(
                InductionCategoryDto.builder()
                        .inductionCatgPoid(1L)
                        .status("N")
                        .build(),
                InductionCategoryDto.builder()
                        .inductionCatgPoid(2L)
                        .status("N")
                        .build()
        );

        when(employeeInductionService.getInductionCategories()).thenReturn(categories);

        mockMvc.perform(get("/v1/employee-induction/induction-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.data").isArray())
                .andExpect(jsonPath("$.result.data[0].inductionCatgPoid").value(1))
                .andExpect(jsonPath("$.result.data[0].status").value("N"))
                .andExpect(jsonPath("$.result.data[1].inductionCatgPoid").value(2))
                .andExpect(jsonPath("$.result.data[1].status").value("N"));

        verify(employeeInductionService).getInductionCategories();
    }

    @Test
    void getInductionCategories_ServiceException_ReturnsInternalServerError() throws Exception {
        when(employeeInductionService.getInductionCategories())
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/v1/employee-induction/induction-categories"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));

        verify(employeeInductionService).getInductionCategories();
    }

    // ---------------- helper methods ----------------

    private List<EmployeeInductionRequestDto.EmployeeInductionDetailRequestDto> createTestDetails() {
        return List.of(
                EmployeeInductionRequestDto.EmployeeInductionDetailRequestDto.builder()
                        .sn(1)
                        .inductionCategory("Company Policies")
                        .assigneePoid(2L)
                        .scheduledDate(LocalDate.now().plusDays(1))
                        .status("N")
                        .remarks("Test detail")
                        .build()
        );
    }

    private List<EmployeeInductionResponseDto.EmployeeInductionDetailResponseDto> createTestResponseDetails() {
        return List.of(
                EmployeeInductionResponseDto.EmployeeInductionDetailResponseDto.builder()
                        .sn(1)
                        .inductionCategory("Company Policies")
                        .assigneePoid(2L)
                        .scheduledDate(LocalDate.now().plusDays(1))
                        .status("N")
                        .remarks("Test detail")
                        .build()
        );
    }

    @Test
    void print_Success() throws Exception {
        // Given
        byte[] mockPdf = "mock pdf content".getBytes();
        when(employeeInductionService.print(1L)).thenReturn(mockPdf);

        // When & Then
        mockMvc.perform(get("/v1/employee-induction/print/1")
                        .header("X-Document-id", "800-107")
                        .header("X-Action-Requested", "Print"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=employee-induction-1.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(mockPdf));

        verify(employeeInductionService).print(1L);
    }

    @Test
    void print_ServiceException_ReturnsInternalServerError() throws Exception {
        // Given
        when(employeeInductionService.print(1L))
                .thenThrow(new RuntimeException("PDF generation failed"));

        // When & Then
        mockMvc.perform(get("/v1/employee-induction/print/1")
                        .header("X-Document-id", "800-107")
                        .header("X-Action-Requested", "Print"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));

        verify(employeeInductionService).print(1L);
    }
}
