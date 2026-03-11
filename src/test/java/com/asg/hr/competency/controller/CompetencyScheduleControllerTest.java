package com.asg.hr.competency.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.competency.dto.CompetencyScheduleRequestDto;
import com.asg.hr.competency.dto.CompetencyScheduleResponseDto;
import com.asg.hr.competency.dto.CreateBatchRequest;
import com.asg.hr.competency.service.CompetencyScheduleService;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CompetencyScheduleControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private MockedStatic<UserContext> mockedUserContext;

    @Mock
    private CompetencyScheduleService service;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private CompetencyScheduleController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockedUserContext = mockStatic(UserContext.class);
        mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

        PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(pageableResolver)
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        mockedUserContext.close();
    }

    @Test
    void createSchedule_Success() throws Exception {
        CompetencyScheduleRequestDto request = createMockRequest();

        when(service.createSchedule(any(CompetencyScheduleRequestDto.class))).thenReturn(1L);

        mockMvc.perform(post("/v1/competency-schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(service).createSchedule(any(CompetencyScheduleRequestDto.class));
    }

    @Test
    void updateSchedule_Success() throws Exception {
        CompetencyScheduleRequestDto request = createMockRequest();

        when(service.updateSchedule(eq(1L), any(CompetencyScheduleRequestDto.class))).thenReturn(1L);

        mockMvc.perform(put("/v1/competency-schedule/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(service).updateSchedule(eq(1L), any(CompetencyScheduleRequestDto.class));
    }

    @Test
    void getScheduleById_Success() throws Exception {
        CompetencyScheduleResponseDto response = createMockResponse();

        when(service.getScheduleById(eq(1L))).thenReturn(response);
        doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

        mockMvc.perform(get("/v1/competency-schedule/1"))
                .andExpect(status().isOk());

        verify(service).getScheduleById(eq(1L));
    }

    @Test
    void listSchedules_Success() throws Exception {
        FilterRequestDto filters = new FilterRequestDto("OR", "N", List.of());

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", List.of(createMockResponse()));
        responseMap.put("totalElements", 1);
        responseMap.put("totalPages", 1);

        when(service.listSchedules(any(FilterRequestDto.class), any())).thenReturn(responseMap);

        mockMvc.perform(post("/v1/competency-schedule/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isOk());

        verify(service).listSchedules(any(FilterRequestDto.class), any());
    }

    @Test
    void deleteSchedule_Success() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("No longer needed");

        doNothing().when(service).deleteSchedule(eq(1L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/competency-schedule/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk());

        verify(service).deleteSchedule(eq(1L), any(DeleteReasonDto.class));
    }

    @Test
    void createBatchEvaluation_Success() throws Exception {
        CreateBatchRequest batchRequest = CreateBatchRequest.builder()
                .evaluationDate(LocalDate.of(2024, 12, 15))
                .recreate(true)
                .build();

        doNothing().when(service).createBatchEvaluation(eq(1L), any(LocalDate.class), anyBoolean());

        mockMvc.perform(put("/v1/competency-schedule/1/create-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk());

        verify(service).createBatchEvaluation(eq(1L), any(LocalDate.class), eq(true));
    }

    private CompetencyScheduleRequestDto createMockRequest() {
        return CompetencyScheduleRequestDto.builder()
                .scheduleDescription("Annual Review 2024")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .seqNo(1)
                .active("Y")
                .evaluationDate(LocalDate.of(2024, 12, 15))
                .build();
    }

    private CompetencyScheduleResponseDto createMockResponse() {
        return CompetencyScheduleResponseDto.builder()
                .schedulePoid(1L)
                .scheduleDescription("Annual Review 2024")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .seqNo(1)
                .active("Y")
                .evaluationDate(LocalDate.of(2024, 12, 15))
                .build();
    }
}
