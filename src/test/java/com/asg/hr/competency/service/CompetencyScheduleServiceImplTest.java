package com.asg.hr.competency.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.competency.dto.CompetencyScheduleRequestDto;
import com.asg.hr.competency.dto.CompetencyScheduleResponseDto;
import com.asg.hr.competency.entity.HrCompetencySchedule;
import com.asg.hr.competency.repository.CompetencyScheduleProcRepository;
import com.asg.hr.competency.repository.HrCompetencyScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetencyScheduleServiceImplTest {

    @Mock
    private HrCompetencyScheduleRepository scheduleRepository;
    @Mock
    private CompetencyScheduleProcRepository procRepository;
    @Mock
    private LoggingService loggingService;
    @Mock
    private DocumentDeleteService documentDeleteService;
    @Mock
    private DocumentSearchService documentSearchService;

    @InjectMocks
    private CompetencyScheduleServiceImpl service;

    private CompetencyScheduleRequestDto request;
    private HrCompetencySchedule entity;
    private CompetencyScheduleResponseDto response;

    @BeforeEach
    void setup() {
        request = CompetencyScheduleRequestDto.builder()
                .scheduleDescription("Annual Review 2024")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .seqNo(1)
                .active("Y")
                .evaluationDate(LocalDate.of(2024, 12, 15))
                .build();

        entity = HrCompetencySchedule.builder()
                .schedulePoid(1L)
                .groupPoid(100L)
                .scheduleDescription("Annual Review 2024")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .seqNo(1)
                .active("Y")
                .evaluationDate(LocalDate.of(2024, 12, 15))
                .deleted("N")
                .build();

        response = CompetencyScheduleResponseDto.builder()
                .schedulePoid(1L)
                .scheduleDescription("Annual Review 2024")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .seqNo(1)
                .active("Y")
                .evaluationDate(LocalDate.of(2024, 12, 15))
                .build();
    }

    // ---------- LIST ----------
    @Test
    void testListSchedules() {
        FilterDto filter = new FilterDto("SCHEDULE_DESCRIPTION", "COMP_SCHEDULE_POID");
        FilterRequestDto filterRequest = new FilterRequestDto("OR", "N", List.of(filter));
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of("SCHEDULE_DESCRIPTION", "Annual Review")),
                Map.of("SCHEDULE_DESCRIPTION", "Schedule Description"),
                1L
        );

        when(documentSearchService.resolveOperator(filterRequest)).thenReturn("OR");
        when(documentSearchService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentSearchService.resolveFilters(filterRequest)).thenReturn(List.of(filter));
        when(documentSearchService.search(any(), any(), eq("OR"), eq(pageable), eq("N"), any(), any()))
                .thenReturn(raw);

        Map<String, Object> result = service.listSchedules(filterRequest, pageable);

        assertNotNull(result);
        verify(documentSearchService).search(any(), any(), any(), any(), any(), any(), any());
    }

    // ---------- GET BY ID ----------
    @Test
    void testGetById_Success() {
        when(scheduleRepository.findBySchedulePoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));

        CompetencyScheduleResponseDto result = service.getScheduleById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getSchedulePoid());
        assertEquals("Annual Review 2024", result.getScheduleDescription());
    }

    @Test
    void testGetById_NotFound() {
        when(scheduleRepository.findBySchedulePoidAndDeleted(1L, "N")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getScheduleById(1L));
    }

    // ---------- CREATE ----------
    @Test
    void testCreate_Success() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(scheduleRepository.existsOverlappingPeriod(anyLong(), any(), any(), anyLong())).thenReturn(false);
            when(scheduleRepository.save(any(HrCompetencySchedule.class))).thenReturn(entity);
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

            Long result = service.createSchedule(request);

            assertEquals(1L, result);
            verify(scheduleRepository).save(any(HrCompetencySchedule.class));
        }
    }

    @Test
    void testCreate_PeriodFromAfterPeriodTo() {
        request.setPeriodFrom(LocalDate.of(2024, 12, 31));
        request.setPeriodTo(LocalDate.of(2024, 1, 1));

        ValidationException ex = assertThrows(ValidationException.class, () -> service.createSchedule(request));
        assertTrue(ex.getMessage().contains("Period From date must be before Period To date"));
    }

    @Test
    void testCreate_OverlappingPeriod() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);

            when(scheduleRepository.existsOverlappingPeriod(anyLong(), any(), any(), anyLong())).thenReturn(true);

            ValidationException ex = assertThrows(ValidationException.class, () -> service.createSchedule(request));
            assertTrue(ex.getMessage().contains("Period overlaps with an existing schedule"));
        }
    }

    // ---------- UPDATE ----------
    @Test
    void testUpdate_Success() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(scheduleRepository.findBySchedulePoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));
            when(scheduleRepository.existsOverlappingPeriod(anyLong(), any(), any(), anyLong())).thenReturn(false);
            when(scheduleRepository.save(any(HrCompetencySchedule.class))).thenReturn(entity);
            doNothing().when(loggingService).logChanges(any(), any(), any(), any(), any(), any(), any());

            Long result = service.updateSchedule(1L, request);

            assertEquals(1L, result);
            verify(scheduleRepository).save(any(HrCompetencySchedule.class));
        }
    }

    @Test
    void testUpdate_NotFound() {
        when(scheduleRepository.findBySchedulePoidAndDeleted(1L, "N")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateSchedule(1L, request));
    }

    @Test
    void testUpdate_OverlappingPeriod() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);

            when(scheduleRepository.findBySchedulePoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));
            when(scheduleRepository.existsOverlappingPeriod(anyLong(), any(), any(), anyLong())).thenReturn(true);

            assertThrows(ValidationException.class, () -> service.updateSchedule(1L, request));
        }
    }

    // ---------- DELETE ----------
    @Test
    void testDelete_Success() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("No longer needed");

        when(scheduleRepository.findBySchedulePoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));

        assertDoesNotThrow(() -> service.deleteSchedule(1L, deleteReasonDto));

        verify(scheduleRepository).findBySchedulePoidAndDeleted(1L, "N");
        verify(documentDeleteService).deleteDocument(eq(1L), eq("HR_COMPETENCY_SCHEDULE"), 
                eq("COMP_SCHEDULE_POID"), eq(deleteReasonDto), isNull());
    }

    @Test
    void testDelete_NotFound() {
        when(scheduleRepository.findBySchedulePoidAndDeleted(1L, "N")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteSchedule(1L, null));
    }

    // ---------- CREATE BATCH EVALUATION ----------
    @Test
    void testCreateBatchEvaluation_Success() {
        LocalDate evaluationDate = LocalDate.of(2024, 12, 15);

        when(scheduleRepository.findBySchedulePoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));
        doNothing().when(procRepository).createBatchEvaluation(anyLong(), anyLong(), anyBoolean(), any(LocalDate.class));

        assertDoesNotThrow(() -> service.createBatchEvaluation(1L, evaluationDate, true));

        verify(procRepository).createBatchEvaluation(eq(1L), eq(100L), eq(true), eq(evaluationDate));
    }

    @Test
    void testCreateBatchEvaluation_UseScheduleEvaluationDate() {
        when(scheduleRepository.findBySchedulePoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));
        doNothing().when(procRepository).createBatchEvaluation(anyLong(), anyLong(), anyBoolean(), any(LocalDate.class));

        assertDoesNotThrow(() -> service.createBatchEvaluation(1L, null, false));

        verify(procRepository).createBatchEvaluation(eq(1L), eq(100L), eq(false), eq(entity.getEvaluationDate()));
    }

    @Test
    void testCreateBatchEvaluation_NotFound() {
        when(scheduleRepository.findBySchedulePoidAndDeleted(1L, "N")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createBatchEvaluation(1L, null, false));
    }
}
