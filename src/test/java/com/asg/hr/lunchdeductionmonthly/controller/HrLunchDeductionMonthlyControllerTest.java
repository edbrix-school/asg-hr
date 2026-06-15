package com.asg.hr.lunchdeductionmonthly.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.exceptions.GlobalExceptionHandler;
import com.asg.hr.exceptions.ResourceAlreadyExistsException;
import com.asg.hr.exceptions.ResourceNotFoundException;
import com.asg.hr.exceptions.ValidationException;
import com.asg.hr.lunchdeductionmonthly.dto.*;
import com.asg.hr.lunchdeductionmonthly.service.HrLunchDeductionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HrLunchDeductionMonthlyControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private MockedStatic<UserContext> userContextMock;

    @Mock private HrLunchDeductionService service;
    @Mock private LoggingService loggingService;

    @InjectMocks
    private HrLunchDeductionMonthlyController controller;

    private static final String BASE_URL = "/v1/lunch-deduction-monthly";
    private static final String DOC_ID = "800-115";

    private HrLunchDeductionResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        userContextMock = mockStatic(UserContext.class);
        userContextMock.when(UserContext::getDocumentId).thenReturn(DOC_ID);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setValidator(validator)
                .build();

        sampleResponse = HrLunchDeductionResponse.builder()
                .transactionPoid(1L)
                .docRef("LDM-001")
                .payrollMonth(LocalDate.of(2025, 9, 1))
                .build();
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    // -------------------------------------------------------------------------
    // POST /list
    // -------------------------------------------------------------------------
    @Nested
    class ListEndpoint {

        @Test
        void success_returnsOk() throws Exception {
            FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());
            when(service.list(any(FilterRequestDto.class), any())).thenReturn(Map.of("total", 1));

            mockMvc.perform(post(BASE_URL + "/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filters)))
                    .andExpect(status().isOk());

            verify(service).list(any(FilterRequestDto.class), any());
        }

        @Test
        void noBody_returnsOk() throws Exception {
            when(service.list(isNull(), any())).thenReturn(Map.of());

            mockMvc.perform(post(BASE_URL + "/list")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    // -------------------------------------------------------------------------
    // GET /{id}
    // -------------------------------------------------------------------------
    @Nested
    class GetByIdEndpoint {

        @Test
        void success_returnsOkAndLogsViewed() throws Exception {
            when(service.getById(1L)).thenReturn(sampleResponse);

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk());

            verify(service).getById(1L);
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, DOC_ID, "1");
        }

        @Test
        void notFound_returns404() throws Exception {
            when(service.getById(99L))
                    .thenThrow(new ResourceNotFoundException("Lunch Deduction", "transactionPoid", 99L));

            mockMvc.perform(get(BASE_URL + "/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // POST / (create)
    // -------------------------------------------------------------------------
    @Nested
    class CreateEndpoint {

        @Test
        void success_returnsOk() throws Exception {
            HrLunchDeductionRequest request = HrLunchDeductionRequest.builder()
                    .payrollMonth(LocalDate.of(2025, 9, 1))
                    .description("Sep 2025")
                    .build();

            when(service.create(any(HrLunchDeductionRequest.class))).thenReturn(sampleResponse);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(service).create(any(HrLunchDeductionRequest.class));
        }

        @Test
        void missingPayrollMonth_returns400() throws Exception {
            HrLunchDeductionRequest request = HrLunchDeductionRequest.builder()
                    .description("no month")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(service);
        }

        @Test
        void duplicatePayrollMonth_returns409() throws Exception {
            HrLunchDeductionRequest request = HrLunchDeductionRequest.builder()
                    .payrollMonth(LocalDate.of(2025, 9, 1))
                    .build();

            when(service.create(any()))
                    .thenThrow(new ResourceAlreadyExistsException("Lunch Deduction", "payroll month"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    // -------------------------------------------------------------------------
    // PUT /{id}
    // -------------------------------------------------------------------------
    @Nested
    class UpdateEndpoint {

        @Test
        void success_returnsOk() throws Exception {
            HrLunchDeductionUpdateRequest request = HrLunchDeductionUpdateRequest.builder()
                    .description("Updated").remarks("note").build();

            when(service.update(eq(1L), any(HrLunchDeductionUpdateRequest.class))).thenReturn(sampleResponse);

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(service).update(eq(1L), any(HrLunchDeductionUpdateRequest.class));
        }

        @Test
        void finalizedRecord_returns400() throws Exception {
            when(service.update(eq(1L), any())).thenThrow(
                    new ValidationException("Cannot modify record because payroll has already been finalized for this period."));

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new HrLunchDeductionUpdateRequest())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_returns404() throws Exception {
            when(service.update(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("Lunch Deduction", "transactionPoid", 99L));

            mockMvc.perform(put(BASE_URL + "/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new HrLunchDeductionUpdateRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // POST /{id}/load
    // -------------------------------------------------------------------------
    @Nested
    class LoadAndProcessEndpoint {

        @Test
        void success_returnsOkWithDetails() throws Exception {
            HrLunchDeductionLoadDto loadDto = HrLunchDeductionLoadDto.builder()
                    .lunchDetails(List.of(
                            HrLunchDeductionDtlResponse.builder().detRowId(1L).employeePoid(10L).build()
                    ))
                    .build();

            when(service.loadAndProcess(1L)).thenReturn(loadDto);

            mockMvc.perform(post(BASE_URL + "/1/load"))
                    .andExpect(status().isOk());

            verify(service).loadAndProcess(1L);
        }

        @Test
        void notFound_returns404() throws Exception {
            when(service.loadAndProcess(99L))
                    .thenThrow(new ResourceNotFoundException("Lunch Deduction", "transactionPoid", 99L));

            mockMvc.perform(post(BASE_URL + "/99/load"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void finalizedPayroll_returns400() throws Exception {
            when(service.loadAndProcess(1L)).thenThrow(
                    new ValidationException("Cannot modify record because payroll has already been finalized for this period."));

            mockMvc.perform(post(BASE_URL + "/1/load"))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // PUT /{id}/details
    // -------------------------------------------------------------------------
    @Nested
    class UpdateDetailEndpoint {

        @Test
        void success_returnsOk() throws Exception {
            HrLunchDeductionDtlRequest request = HrLunchDeductionDtlRequest.builder()
                    .detRowId(1L).leaveDays(3L).deductionType("DEDUCT").build();

            doNothing().when(service).updateDetail(eq(1L), any(HrLunchDeductionDtlRequest.class));

            mockMvc.perform(put(BASE_URL + "/1/details")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(service).updateDetail(eq(1L), any(HrLunchDeductionDtlRequest.class));
        }

        @Test
        void detailNotFound_returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Lunch Detail", "detRowId", 99L))
                    .when(service).updateDetail(eq(1L), any());

            mockMvc.perform(put(BASE_URL + "/1/details")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    HrLunchDeductionDtlRequest.builder().detRowId(99L).build())))
                    .andExpect(status().isNotFound());
        }

        @Test
        void finalizedRecord_returns400() throws Exception {
            doThrow(new ValidationException("Cannot modify record because payroll has already been finalized for this period."))
                    .when(service).updateDetail(eq(1L), any());

            mockMvc.perform(put(BASE_URL + "/1/details")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    HrLunchDeductionDtlRequest.builder().detRowId(1L).build())))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /{id}
    // -------------------------------------------------------------------------
    @Nested
    class DeleteEndpoint {

        @Test
        void success_returnsOk() throws Exception {
            DeleteReasonDto reason = new DeleteReasonDto();
            reason.setDeleteReason("No longer needed");

            doNothing().when(service).delete(eq(1L), any(DeleteReasonDto.class));

            mockMvc.perform(delete(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reason)))
                    .andExpect(status().isOk());

            verify(service).delete(eq(1L), any(DeleteReasonDto.class));
        }

        @Test
        void noBody_returnsOk() throws Exception {
            doNothing().when(service).delete(eq(1L), isNull());

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isOk());
        }

        @Test
        void notFound_returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Lunch Deduction", "transactionPoid", 99L))
                    .when(service).delete(eq(99L), any());

            mockMvc.perform(delete(BASE_URL + "/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DeleteReasonDto())))
                    .andExpect(status().isNotFound());
        }

        @Test
        void finalizedPayroll_returns400() throws Exception {
            doThrow(new ValidationException("Cannot modify record because payroll has already been finalized for this period."))
                    .when(service).delete(eq(1L), any());

            mockMvc.perform(delete(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DeleteReasonDto())))
                    .andExpect(status().isBadRequest());
        }
    }
}
