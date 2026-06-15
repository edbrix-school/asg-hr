package com.asg.hr.attendencerequest.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.attendencerequest.dto.AttendanceRequestDto;
import com.asg.hr.attendencerequest.dto.AttendanceResponseDto;
import com.asg.hr.attendencerequest.service.AttendanceSpecialService;
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
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AttendanceSpecialControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private MockedStatic<UserContext> mockedUserContext;

    @Mock
    private AttendanceSpecialService service;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private AttendanceSpecialController controller;

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
    void list_Success_delegatesToService() throws Exception {
        FilterRequestDto filters = new FilterRequestDto("OR", "N", List.of());

        when(service.list(eq("DOC123"), any(FilterRequestDto.class), any()))
                .thenReturn(Map.of("x", 1));

        mockMvc.perform(post("/v1/attendance-special/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isOk());

        verify(service).list(eq("DOC123"), any(FilterRequestDto.class), any());
        verifyNoInteractions(loggingService);
    }

    @Test
    void create_Success_delegatesToService() throws Exception {
        AttendanceRequestDto req = new AttendanceRequestDto();
        req.setEmployeePoid(10L);
        req.setAttendanceDate(LocalDate.of(2024, 1, 1));
        req.setExceptionType("E1");
        req.setReason("R1");
        req.setHodRemarks("H1");
        req.setStatus("IN_PROGRESS");

        AttendanceResponseDto created = AttendanceResponseDto.builder()
                .attendancePoid(1L)
                .build();

        when(service.create(any(AttendanceRequestDto.class))).thenReturn(created);

        mockMvc.perform(post("/v1/attendance-special")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(service).create(any(AttendanceRequestDto.class));
        verifyNoInteractions(loggingService);
    }

    @Test
    void update_Success_delegatesToService() throws Exception {
        AttendanceRequestDto req = new AttendanceRequestDto();
        req.setEmployeePoid(10L);
        req.setAttendanceDate(LocalDate.of(2024, 1, 1));
        req.setExceptionType("E1");
        req.setReason("R1");
        req.setHodRemarks("H1");
        req.setStatus("IN_PROGRESS");

        AttendanceResponseDto updated = AttendanceResponseDto.builder()
                .attendancePoid(2L)
                .build();

        when(service.update(eq(2L), any(AttendanceRequestDto.class))).thenReturn(updated);

        mockMvc.perform(put("/v1/attendance-special/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(service).update(eq(2L), any(AttendanceRequestDto.class));
        verifyNoInteractions(loggingService);
    }

    @Test
    void getById_Success_logsViewed_andDelegatesToService() throws Exception {
        AttendanceResponseDto response = AttendanceResponseDto.builder()
                .attendancePoid(5L)
                .build();

        when(service.getById(5L)).thenReturn(response);

        mockMvc.perform(get("/v1/attendance-special/5"))
                .andExpect(status().isOk());

        verify(service).getById(5L);
        verify(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, "DOC123", "5");
    }

    @Test
    void delete_Success_delegatesToService() throws Exception {
        DeleteReasonDto reason = new DeleteReasonDto();
        reason.setDeleteReason("no longer needed");

        doNothing().when(service).delete(eq(9L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/attendance-special/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reason)))
                .andExpect(status().isOk());

        verify(service).delete(eq(9L), any(DeleteReasonDto.class));
        verifyNoInteractions(loggingService);
    }
}

