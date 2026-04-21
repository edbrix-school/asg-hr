package com.asg.hr.resignation.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.resignation.dto.HrResignationEmployeeDetailsResponse;
import com.asg.hr.resignation.dto.HrResignationRequest;
import com.asg.hr.resignation.dto.HrResignationResponse;
import com.asg.hr.resignation.service.HrResignationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HrResignationControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private HrResignationService resignationService;

    @Mock
    private LoggingService loggingService;

    private MockedStatic<UserContext> userContextMock;

    private static final String DOCUMENT_ID = "800-116";
    private static final Long GROUP_POID = 10L;
    private static final String USER_ID = "user1";

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        HrResignationController controller = new HrResignationController(resignationService, loggingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        userContextMock = Mockito.mockStatic(UserContext.class);
        userContextMock.when(UserContext::getDocumentId).thenReturn(DOCUMENT_ID);
        userContextMock.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);
        userContextMock.when(UserContext::getUserId).thenReturn(USER_ID);
    }

    @AfterEach
    void tearDown() {
        if (userContextMock != null) {
            userContextMock.close();
        }
    }

    @Nested
    @DisplayName("GET /v1/resignation/{transactionPoid}")
    class GetById {

        @Test
        void returns200AndLogsViewed() throws Exception {
            HrResignationResponse response = new HrResignationResponse();
            response.setTransactionPoid(123L);

            when(resignationService.getById(123L)).thenReturn(response);

            mockMvc.perform(get("/v1/resignation/{transactionPoid}", 123L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Resignation retrieved successfully"))
                    .andExpect(jsonPath("$.result.data.transactionPoid").value(123));

            verify(resignationService).getById(123L);
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, DOCUMENT_ID, "123");
        }
    }

    @Nested
    @DisplayName("POST /v1/resignation")
    class Create {

        @Test
        void returns200OnCreate() throws Exception {
            HrResignationRequest req = new HrResignationRequest();
            req.setEmployeePoid(1001L);
            req.setTransactionDate(LocalDate.now());
            req.setLastDateOfWork(LocalDate.now().plusDays(2));
            req.setResignationDetails("details");
            req.setResignationType("VOLUNTARY");

            HrResignationResponse resp = new HrResignationResponse();
            resp.setTransactionPoid(200L);

            when(resignationService.create(any(HrResignationRequest.class))).thenReturn(resp);

            mockMvc.perform(post("/v1/resignation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Resignation created successfully"))
                    .andExpect(jsonPath("$.result.data.transactionPoid").value(200));
        }
    }

    @Nested
    @DisplayName("DELETE /v1/resignation/{transactionPoid}")
    class Delete {

        @Test
        void returns200OnDeleteWithoutBody() throws Exception {
            Mockito.doNothing().when(resignationService).delete(eq(123L), eq(null));

            mockMvc.perform(delete("/v1/resignation/{transactionPoid}", 123L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Resignation deleted successfully"));
        }

        @Test
        void returns200OnDeleteWithBody() throws Exception {
            DeleteReasonDto reason = new DeleteReasonDto();
            Mockito.doNothing().when(resignationService).delete(eq(123L), eq(reason));

            mockMvc.perform(delete("/v1/resignation/{transactionPoid}", 123L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reason)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Resignation deleted successfully"));
        }
    }

    @Test
    void listResignations_Returns200() throws Exception {
        when(resignationService.listResignations(any(), any())).thenReturn(Map.of("totalElements", 0));
        mockMvc.perform(post("/v1/resignation/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Resignation list retrieved successfully"));
    }

    @Test
    void update_Returns200() throws Exception {
        HrResignationRequest req = new HrResignationRequest();
        req.setEmployeePoid(1001L);
        req.setTransactionDate(LocalDate.now());
        req.setLastDateOfWork(LocalDate.now().plusDays(2));
        req.setResignationDetails("details");
        req.setResignationType("VOLUNTARY");
        HrResignationResponse response = new HrResignationResponse();
        response.setTransactionPoid(10L);
        when(resignationService.update(eq(10L), any(HrResignationRequest.class))).thenReturn(response);

        mockMvc.perform(put("/v1/resignation/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Resignation updated successfully"));
    }

    @Test
    void getEmployeeDetails_Returns200() throws Exception {
        when(resignationService.getEmployeeDetails(1001L)).thenReturn(new HrResignationEmployeeDetailsResponse());
        mockMvc.perform(get("/v1/resignation/employee/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Employee details fetched successfully"));
    }

}

