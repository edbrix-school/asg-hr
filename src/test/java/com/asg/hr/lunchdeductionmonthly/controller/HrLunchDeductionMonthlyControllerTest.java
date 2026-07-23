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

import java.math.BigDecimal;
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
                .details(List.of(
                        HrLunchDeductionDtlResponse.builder()
                                .detRowId(1L)
                                .transactionPoid(1L)
                                .deductionType("LUNCH")
                                .amount(new BigDecimal("500.00"))
                                .build()
                ))
                .build();
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    @Nested
    class ListEndpoint {

        @Test
        void success_returnsOk() throws Exception {
            FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());
            when(service.list(any(FilterRequestDto.class), any(),any(),any())).thenReturn(Map.of("total", 1));

            mockMvc.perform(post(BASE_URL + "/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filters)))
                    .andExpect(status().isOk());

            verify(service).list(any(FilterRequestDto.class), any(),any(),any());
        }

        @Test
        void noBody_returnsOk() throws Exception {
            when(service.list(isNull(), any(),any(),any())).thenReturn(Map.of());

            mockMvc.perform(post(BASE_URL + "/list")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

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

    @Nested
    class CreateEndpoint {

        @Test
        void success_withDetails_returnsOk() throws Exception {
            HrLunchDeductionRequest request = HrLunchDeductionRequest.builder()
                    .payrollMonth(LocalDate.of(2025, 9, 1))
                    .description("Sep 2025")
                    .details(List.of(
                            HrLunchDeductionDtlRequest.builder()
                                    .deductionType("LUNCH")
                                    .leaveDays(2L)
                                    .amount(new BigDecimal("500.00"))
                                    .build()
                    ))
                    .build();

            when(service.create(any(HrLunchDeductionRequest.class))).thenReturn(sampleResponse);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(service).create(any(HrLunchDeductionRequest.class));
        }

        @Test
        void success_noDetails_returnsOk() throws Exception {
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
            HrLunchDeductionRequest request = new HrLunchDeductionRequest();
            request.setDescription("no month");

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
                    .details(List.of())
                    .build();

            when(service.create(any()))
                    .thenThrow(new ResourceAlreadyExistsException("Lunch Deduction", "payroll month"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    class UpdateEndpoint {

        @Test
        void success_withDetails_returnsOk() throws Exception {
            HrLunchDeductionRequest request = HrLunchDeductionRequest.builder()
                    .payrollMonth(LocalDate.of(2025, 9, 1))
                    .description("Updated")
                    .remarks("note")
                    .details(List.of(
                            HrLunchDeductionDtlRequest.builder()
                                    .detRowId(1L)
                                    .actionType("UPDATED")
                                    .leaveDays(3L)
                                    .amount(new BigDecimal("750.00"))
                                    .build(),
                            HrLunchDeductionDtlRequest.builder()
                                    .actionType("CREATED")
                                    .deductionType("LUNCH")
                                    .leaveDays(2L)
                                    .amount(new BigDecimal("500.00"))
                                    .build()
                    ))
                    .build();

            when(service.update(eq(1L), any(HrLunchDeductionRequest.class))).thenReturn(sampleResponse);

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(service).update(eq(1L), any(HrLunchDeductionRequest.class));
        }

        @Test
        void success_headerOnly_returnsOk() throws Exception {
            HrLunchDeductionRequest request = HrLunchDeductionRequest.builder()
                    .payrollMonth(LocalDate.of(2025, 9, 1))
                    .description("Updated")
                    .remarks("note")
                    .details(List.of())
                    .build();

            when(service.update(eq(1L), any(HrLunchDeductionRequest.class))).thenReturn(sampleResponse);

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(service).update(eq(1L), any(HrLunchDeductionRequest.class));
        }

        @Test
        void notFound_returns404() throws Exception {
            when(service.update(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("Lunch Deduction", "transactionPoid", 99L));

            HrLunchDeductionRequest request = HrLunchDeductionRequest.builder()
                    .payrollMonth(LocalDate.of(2025, 9, 1))
                    .details(List.of())
                    .build();

            mockMvc.perform(put(BASE_URL + "/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

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
    }

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
    }
}
