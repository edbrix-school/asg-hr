package com.asg.hr.leaverejoin.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDownloadHeaderService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.exceptions.GlobalExceptionHandler;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinEmployeeDetailsResponse;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinLeaveDetailsResponse;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinRequest;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinResponse;
import com.asg.hr.leaverejoin.service.EmployeeLeaveRejoinService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = EmployeeLeaveRejoinController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.asg.hr.aspect.*"))
@ContextConfiguration(classes = {EmployeeLeaveRejoinController.class, GlobalExceptionHandler.class, DocumentDownloadHeaderService.class})
class EmployeeLeaveRejoinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean

    private JdbcTemplate jdbcTemplate;


    @MockitoBean
    private EmployeeLeaveRejoinService service;

    @MockitoBean
    private LoggingService loggingService;

    private EmployeeLeaveRejoinRequest request;
    private EmployeeLeaveRejoinResponse response;

    @BeforeEach
    void setUp() {
        reset(service, loggingService);

        request = EmployeeLeaveRejoinRequest.builder()
                .employeePoid(10L)
                .leaveRequestPoid(20L)
                .dateOfRejoining(LocalDate.of(2026, 2, 1))
                .remarks("Back from leave")
                .passportReceived("Y")
                .receivedBy("HR User")
                .remarksByHr("Checked")
                .build();

        response = EmployeeLeaveRejoinResponse.builder()
                .transactionPoid(1L)
                .employeePoid(10L)
                .leaveRequestPoid(20L)
                .dateOfRejoining(LocalDate.of(2026, 2, 1))
                .build();
    }

    @Test
    void create_Success() throws Exception {
        when(service.create(any(EmployeeLeaveRejoinRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/leave-rejoin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Leave rejoin created successfully"))
                .andExpect(jsonPath("$.result.data.transactionPoid").value(1L));

        verify(service).create(any(EmployeeLeaveRejoinRequest.class));
    }

    @Test
    void create_MissingRequired_Returns400() throws Exception {
        EmployeeLeaveRejoinRequest invalid = EmployeeLeaveRejoinRequest.builder().build();

        mockMvc.perform(post("/v1/leave-rejoin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
    }

    @Test
    void getById_Success() throws Exception {
        try (MockedStatic<UserContext> mockedUserContext = org.mockito.Mockito.mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("800-114");
            when(service.getById(1L)).thenReturn(response);

            mockMvc.perform(get("/v1/leave-rejoin/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Leave rejoin retrieved successfully"))
                    .andExpect(jsonPath("$.result.data.transactionPoid").value(1L));

            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, "800-114", "1");
        }
    }

    @Test
    void getById_NotFound_Returns404() throws Exception {
        when(service.getById(1L)).thenThrow(new ResourceNotFoundException("Leave Rejoin", "id", 1L));

        mockMvc.perform(get("/v1/leave-rejoin/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_Success() throws Exception {
        when(service.update(eq(1L), any(EmployeeLeaveRejoinRequest.class))).thenReturn(response);

        mockMvc.perform(put("/v1/leave-rejoin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Leave rejoin updated successfully"));
    }

    @Test
    void delete_Success() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("cleanup");
        doNothing().when(service).delete(eq(1L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/leave-rejoin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Leave rejoin deleted successfully"));
    }

    @Test
    void list_Success() throws Exception {
        Map<String, Object> listResponse = new HashMap<>();
        listResponse.put("data", java.util.Collections.emptyList());
        listResponse.put("totalRecords", 0);

        when(service.list(any(FilterRequestDto.class), any(), any(), any())).thenReturn(listResponse);

        mockMvc.perform(post("/v1/leave-rejoin/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Leave rejoin list retrieved successfully"));
    }

    @Test
    void getEmployeeDetails_Success() throws Exception {
        when(service.getEmployeeDetails(10L)).thenReturn(EmployeeLeaveRejoinEmployeeDetailsResponse.builder()
                .employeePoid(10L)
                .departmentName("Operations")
                .designationName("Technician")
                .status("SUCCESS")
                .build());

        mockMvc.perform(get("/v1/leave-rejoin/employee/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.employeePoid").value(10L))
                .andExpect(jsonPath("$.result.data.departmentName").value("Operations"));
    }

    @Test
    void getLeaveDetails_Success() throws Exception {
        when(service.getLeaveDetails(10L, 20L)).thenReturn(EmployeeLeaveRejoinLeaveDetailsResponse.builder()
                .employeePoid(10L)
                .leaveRequestPoid(20L)
                .dateProceededOnLeave(LocalDate.of(2026, 1, 1))
                .status("SUCCESS")
                .build());

        mockMvc.perform(get("/v1/leave-rejoin/employee/10/leave-request/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.employeePoid").value(10L))
                .andExpect(jsonPath("$.result.data.leaveRequestPoid").value(20L));
    }

    @Test
    void print_ReturnsPdfWhenServiceSucceeds() throws Exception {
        when(service.print(7L)).thenReturn(new byte[]{1, 2});

        mockMvc.perform(get("/v1/leave-rejoin/print/7"))
                .andExpect(status().isOk());
    }

    @Test
    void print_Returns500WhenServiceThrows() throws Exception {
        when(service.print(7L)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/v1/leave-rejoin/print/7"))
                .andExpect(status().isInternalServerError());
    }
}
