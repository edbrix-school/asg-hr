package com.asg.hr.employeemaster.controller;

import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.hr.employeemaster.dto.*;
import com.asg.hr.employeemaster.service.EmployeeMasterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmployeeMasterControllerTest {

    private MockMvc mockMvc;
    private MockedStatic<UserContext> userContextMock;

    @Mock private EmployeeMasterService employeeMasterService;
    @Mock private LoggingService loggingService;

    @InjectMocks
    private EmployeeMasterController controller;

    @BeforeEach
    void setUp() {
        userContextMock = mockStatic(UserContext.class);
        userContextMock.when(UserContext::getDocumentId).thenReturn("DOC");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        if (userContextMock != null) userContextMock.close();
    }

    @Test
    void dashboardCounts_returnsOk() throws Exception {
        when(employeeMasterService.getEmployeeCounts()).thenReturn(new EmployeeCountDto(1L, 1L, 0L));

        mockMvc.perform(get("/v1/employee-master/dashboard-details"))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardList_returnsOk() throws Exception {
        when(employeeMasterService.listEmployeeDashboardDetails(any(), any())).thenReturn(Map.of("content", java.util.List.of()));

        mockMvc.perform(post("/v1/employee-master/dashboard-details/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void listEmployees_usesUserContextDocId() throws Exception {
        when(employeeMasterService.listEmployees(eq("DOC"), any(), any())).thenReturn(Map.of("content", java.util.List.of()));

        mockMvc.perform(post("/v1/employee-master/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_logsViewed() throws Exception {
        when(employeeMasterService.getEmployeeById(5L)).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(5L).build());

        mockMvc.perform(get("/v1/employee-master/{employeePoid}", 5))
                .andExpect(status().isOk());

        verify(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), eq("DOC"), eq("5"));
    }

    @Test
    void getEmployeeLeaveDates_returnsOk() throws Exception {
        when(employeeMasterService.getEmployeeLeaveDates(5L)).thenReturn(EmployeeLeaveDatesResponseDto.builder().startDate("x").periodEndDate("y").build());

        mockMvc.perform(get("/v1/employee-master/{employeePoid}/leave-dates", 5))
                .andExpect(status().isOk());
    }

    @Test
    void createEmployee_returnsOk() throws Exception {
        when(employeeMasterService.createEmployee(any())).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(1L).build());

        mockMvc.perform(post("/v1/employee-master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEmployeeRequestJson()))
                .andExpect(status().isOk());
    }

    @Test
    void updateEmployee_returnsOk() throws Exception {
        when(employeeMasterService.updateEmployee(eq(1L), any())).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(1L).build());

        mockMvc.perform(put("/v1/employee-master/{employeePoid}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEmployeeRequestJson()))
                .andExpect(status().isOk());
    }

    @Test
    void updateLeaveRejoin_returnsOk() throws Exception {
        when(employeeMasterService.updateLeaveRejoin(eq(1L), any())).thenReturn("OK");

        mockMvc.perform(post("/v1/employee-master/{employeePoid}/leave-rejoin", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejoinDate\":\"2026-01-01\",\"rejoinLrqRef\":\"R\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void removeLeaveRejoin_returnsOk() throws Exception {
        when(employeeMasterService.removeLeaveRejoin(eq(1L), any())).thenReturn("OK");

        mockMvc.perform(post("/v1/employee-master/{employeePoid}/leave-rejoin/remove", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejoinLrqRef\":\"R\",\"rejoinDate\":\"2026-01-01\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteEmployee_returnsOk() throws Exception {
        doNothing().when(employeeMasterService).deleteEmployee(eq(1L), any());

        mockMvc.perform(delete("/v1/employee-master/{employeePoid}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deleteReason\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateEmployeePhoto_returnsOk() throws Exception {
        byte[] photo = "x".getBytes(StandardCharsets.UTF_8);
        when(employeeMasterService.updateEmployeePhoto(eq(1L), any())).thenReturn(EmployeePhotoUpdateResponseDto.builder().employeePoid(1L).photo(photo).build());

        mockMvc.perform(put("/v1/employee-master/photo")
                        .param("employeePoid", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photo\":\"eA==\"}"))
                .andExpect(status().isOk());
    }

    @Nested
    class Print {
        @Test
        void returnsPdfWhenServiceSucceeds() throws Exception {
            when(employeeMasterService.print(7L)).thenReturn(new byte[]{1, 2});

            mockMvc.perform(get("/v1/employee-master/print/{transactionPoid}", 7))
                    .andExpect(status().isOk());
        }

        @Test
        void returns500WhenServiceThrows() throws Exception {
            when(employeeMasterService.print(7L)).thenThrow(new RuntimeException("boom"));

            mockMvc.perform(get("/v1/employee-master/print/{transactionPoid}", 7))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Test
    void uploadLmraDetails_returnsOk() throws Exception {
        when(employeeMasterService.uploadLmraData()).thenReturn(LmraUploadResponse.builder().status("OK").build());

        mockMvc.perform(post("/v1/employee-master/upload-lmra-details"))
                .andExpect(status().isOk());
    }

    @Test
    void uploadExcel_returnsOk() throws Exception {
        when(employeeMasterService.uploadExcel(any())).thenReturn("ok");

        mockMvc.perform(multipart("/v1/employee-master/upload-excel")
                        .file("file", new byte[]{1, 2, 3})
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }

    private static String validEmployeeRequestJson() {
        // Minimal JSON that satisfies @Valid constraints on EmployeeMasterRequestDto.
        return """
                {
                  "firstName": "A",
                  "lastName": "B",
                  "locationPoid": 1,
                  "departmentPoid": 1,
                  "designationPoid": 1,
                  "joinDate": "2026-01-01",
                  "nationalityPoid": 1,
                  "religionPoid": 1,
                  "serviceStartDate": "2026-01-01",
                  "serviceType": "REMOTE",
                  "hod": 1,
                  "passportNo": "P1",
                  "issuedDate": "2026-01-01",
                  "expiryDate": "2027-01-01",
                  "empGlPoid": 1,
                  "actualDob": "2000-01-01",
                  "lifeInsurance": "N",
                  "active": "Y",
                  "deleted": "N"
                }
                """;
    }
}

