package com.asg.hr.competencyevaluation.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.competencyevaluation.dto.CompetencyEvaluationRequestDto;
import com.asg.hr.competencyevaluation.dto.CompetencyEvaluationResponseDto;
import com.asg.hr.competencyevaluation.service.CompetencyEvaluationService;
import com.asg.hr.exceptions.GlobalExceptionHandler;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CompetencyEvaluationController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.asg.hr.aspect.*"))
@ContextConfiguration(classes = {CompetencyEvaluationController.class, GlobalExceptionHandler.class})
class CompetencyEvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompetencyEvaluationService competencyEvaluationService;

    @MockitoBean
    private LoggingService loggingService;

    private CompetencyEvaluationRequestDto requestDto;
    private CompetencyEvaluationResponseDto responseDto;

    @BeforeEach
    void setUp() {
        reset(competencyEvaluationService, loggingService);
        objectMapper.findAndRegisterModules();

        requestDto = CompetencyEvaluationRequestDto.builder()
                .docRef("CE-001")
                .employeePoid(1L)
                .reviewedByPoid(2L)
                .compSchedulePoid(3L)
                .evaluationDate(LocalDate.now())
                .status("PENDING")
                .details(List.of(
                        CompetencyEvaluationRequestDto.CompetencyEvaluationDetailRequestDto.builder()
                                .actionType("isCreated")
                                .competencyPoid(7L)
                                .rating("GOOD")
                                .hodComments("ok")
                                .build()
                ))
                .build();

        responseDto = CompetencyEvaluationResponseDto.builder()
                .transactionPoid(10L)
                .docRef("CE-001")
                .employeePoid(1L)
                .reviewedByPoid(2L)
                .compSchedulePoid(3L)
                .evaluationDate(LocalDate.now())
                .status("PENDING")
                .totalRating(new BigDecimal("3.00"))
                .avgRatingPercent(new BigDecimal("75.00"))
                .employeeAgreedPercent(BigDecimal.ZERO)
                .details(List.of())
                .build();
    }

    @Test
    void create_success() throws Exception {
        when(competencyEvaluationService.create(any())).thenReturn(responseDto);

        mockMvc.perform(post("/v1/competency-evaluation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.data.transactionPoid").value(10));

        verify(competencyEvaluationService).create(any());
    }

    @Test
    void create_validationError_returns400() throws Exception {
        when(competencyEvaluationService.create(any())).thenThrow(new ValidationException("bad"));

        mockMvc.perform(post("/v1/competency-evaluation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_success() throws Exception {
        when(competencyEvaluationService.getById(10L)).thenReturn(responseDto);

        try (MockedStatic<UserContext> uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getDocumentId).thenReturn("DOC800");

            mockMvc.perform(get("/v1/competency-evaluation/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.transactionPoid").value(10));

            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, "DOC800", "10");
        }
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(competencyEvaluationService.getById(99L)).thenThrow(new ResourceNotFoundException("x", "y", 99L));

        mockMvc.perform(get("/v1/competency-evaluation/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_success() throws Exception {
        when(competencyEvaluationService.update(anyLong(), any())).thenReturn(responseDto);

        mockMvc.perform(put("/v1/competency-evaluation/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    void list_success() throws Exception {
        when(competencyEvaluationService.list(any(), any(), any(), any(Pageable.class)))
                .thenReturn(Map.of("content", List.of()));

        mockMvc.perform(post("/v1/competency-evaluation/list")
                        .param("startDate", "2026-02-25")
                        .param("endDate", "2026-06-25")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FilterRequestDto("AND", "N", List.of()))))
                .andExpect(status().isOk());

        verify(competencyEvaluationService).list(any(), eq(LocalDate.of(2026, 2, 25)), eq(LocalDate.of(2026, 6, 25)), any(Pageable.class));
    }

    @Test
    void list_onlyStartDate_returns400() throws Exception {
        mockMvc.perform(post("/v1/competency-evaluation/list")
                        .param("startDate", "2026-02-25")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FilterRequestDto("AND", "N", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_success() throws Exception {
        mockMvc.perform(delete("/v1/competency-evaluation/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeleteReasonDto())))
                .andExpect(status().isOk());
    }

    @Test
    void calculateScores_success() throws Exception {
        when(competencyEvaluationService.calculateScores(10L)).thenReturn(responseDto);

        mockMvc.perform(post("/v1/competency-evaluation/10/calculate-scores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void calculateScores_validation_returns400() throws Exception {
        when(competencyEvaluationService.calculateScores(10L)).thenThrow(new ValidationException("missing"));

        mockMvc.perform(post("/v1/competency-evaluation/10/calculate-scores"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_unexpectedError_returns500() throws Exception {
        when(competencyEvaluationService.create(any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/v1/competency-evaluation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void update_notFound_returns404() throws Exception {
        when(competencyEvaluationService.update(anyLong(), any()))
                .thenThrow(new ResourceNotFoundException("x", "y", 10L));

        mockMvc.perform(put("/v1/competency-evaluation/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_unexpectedError_returns500() throws Exception {
        when(competencyEvaluationService.update(anyLong(), any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(put("/v1/competency-evaluation/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getById_unexpectedError_returns500() throws Exception {
        when(competencyEvaluationService.getById(10L)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/v1/competency-evaluation/10"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void list_unexpectedError_returns500() throws Exception {
        when(competencyEvaluationService.list(any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/v1/competency-evaluation/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FilterRequestDto("AND", "N", List.of()))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("x", "y", 10L))
                .when(competencyEvaluationService).delete(anyLong(), any());

        mockMvc.perform(delete("/v1/competency-evaluation/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeleteReasonDto())))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_unexpectedError_returns500() throws Exception {
        doThrow(new RuntimeException("boom"))
                .when(competencyEvaluationService).delete(anyLong(), any());

        mockMvc.perform(delete("/v1/competency-evaluation/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeleteReasonDto())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void calculateScores_notFound_returns404() throws Exception {
        when(competencyEvaluationService.calculateScores(10L))
                .thenThrow(new ResourceNotFoundException("x", "y", 10L));

        mockMvc.perform(post("/v1/competency-evaluation/10/calculate-scores"))
                .andExpect(status().isNotFound());
    }

    @Test
    void calculateScores_unexpectedError_returns500() throws Exception {
        when(competencyEvaluationService.calculateScores(10L)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/v1/competency-evaluation/10/calculate-scores"))
                .andExpect(status().isInternalServerError());
    }
}
