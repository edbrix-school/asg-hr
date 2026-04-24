package com.asg.hr.competency.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    @Mock
    private LovDataService lovDataService;

    @InjectMocks
    private CompetencyScheduleServiceImpl service;

    private CompetencyScheduleRequestDto request;
    private HrCompetencySchedule entity;

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
    }

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

    @Test
    void testGetById_Success() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);

            when(scheduleRepository.findByIdAndGroupPoidAndNotDeleted(1L, 100L)).thenReturn(Optional.of(entity));
            when(lovDataService.getDetailsByPoidAndLovNameFast(1L, "HR_COMPETENCY_SCHEDULES"))
                    .thenReturn(new LovGetListDto(1L, "1", "Annual Review 2024", 1L, "Annual Review 2024", 1, null));

            CompetencyScheduleResponseDto result = service.getScheduleById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getSchedulePoid());
            assertEquals("Annual Review 2024", result.getScheduleDescription());
            assertNotNull(result.getScheduleDtl());
        }
    }

    @Test
    void testGetById_NotFound() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            when(scheduleRepository.findByIdAndGroupPoidAndNotDeleted(1L, 100L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getScheduleById(1L));
        }
    }

    @Test
    void testCreate_Success() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(scheduleRepository.existsOverlappingPeriod(anyLong(), any(), any(), anyLong())).thenReturn(false);
            when(scheduleRepository.save(any(HrCompetencySchedule.class))).thenReturn(entity);
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), any(), any());

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

    @Test
    void testUpdate_Success() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(scheduleRepository.findByIdAndGroupPoidAndNotDeleted(1L, 100L)).thenReturn(Optional.of(entity));
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
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            when(scheduleRepository.findByIdAndGroupPoidAndNotDeleted(1L, 100L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.updateSchedule(1L, request));
        }
    }

    @Test
    void testUpdate_OverlappingPeriod() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);

            when(scheduleRepository.findByIdAndGroupPoidAndNotDeleted(1L, 100L)).thenReturn(Optional.of(entity));
            when(scheduleRepository.existsOverlappingPeriod(anyLong(), any(), any(), anyLong())).thenReturn(true);

            assertThrows(ValidationException.class, () -> service.updateSchedule(1L, request));
        }
    }

    @Test
    void testDelete_Success() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("No longer needed");

        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            when(scheduleRepository.findByIdAndGroupPoidAndNotDeleted(1L, 100L)).thenReturn(Optional.of(entity));

            assertDoesNotThrow(() -> service.deleteSchedule(1L, deleteReasonDto));

            verify(scheduleRepository).findByIdAndGroupPoidAndNotDeleted(1L, 100L);
            verify(documentDeleteService).deleteDocument(eq(1L), eq("HR_COMPETENCY_SCHEDULE"),
                    eq("COMP_SCHEDULE_POID"), eq(deleteReasonDto), isNull());
        }
    }

    @Test
    void testDelete_NotFound() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            when(scheduleRepository.findByIdAndGroupPoidAndNotDeleted(1L, 100L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.deleteSchedule(1L, null));
        }
    }

    @Test
    void testCreateBatchEvaluation_Success() {
        LocalDate evaluationDate = LocalDate.of(2024, 12, 15);

        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(scheduleRepository.findByIdAndGroupPoidAndNotDeleted(1L, 100L)).thenReturn(Optional.of(entity));
            when(procRepository.createBatchEvaluation(anyLong(), anyLong(), any(), any(LocalDate.class)))
                    .thenReturn("Batch created");

            String result = assertDoesNotThrow(() -> service.createBatchEvaluation(1L, evaluationDate, true));

            assertEquals("Batch created", result);
            verify(procRepository).createBatchEvaluation(eq(1L), eq(100L), eq(true), eq(evaluationDate));
            verify(loggingService).createLogSummaryEntry("DOC123", "1", "Batch created");
        }
    }

    @Test
    void testCreateBatchEvaluation_UseScheduleEvaluationDate() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(scheduleRepository.findByIdAndGroupPoidAndNotDeleted(1L, 100L)).thenReturn(Optional.of(entity));
            when(procRepository.createBatchEvaluation(anyLong(), anyLong(), any(), any(LocalDate.class)))
                    .thenReturn("Batch created");

            String result = assertDoesNotThrow(() -> service.createBatchEvaluation(1L, null, false));

            assertEquals("Batch created", result);
            verify(procRepository).createBatchEvaluation(eq(1L), eq(100L), eq(false), eq(entity.getEvaluationDate()));
        }
    }

    @Test
    void testCreateBatchEvaluation_RejectWhenEvaluationDateMissing() {
        HrCompetencySchedule noEvaluationDateEntity = HrCompetencySchedule.builder()
                .schedulePoid(1L)
                .groupPoid(100L)
                .scheduleDescription("Annual Review 2024")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .seqNo(1)
                .active("Y")
                .deleted("N")
                .build();

        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            when(scheduleRepository.findByIdAndGroupPoidAndNotDeleted(1L, 100L)).thenReturn(Optional.of(noEvaluationDateEntity));

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.createBatchEvaluation(1L, null, false));

            assertEquals("Please enter Evaluation Date ...", ex.getMessage());
            verifyNoInteractions(procRepository);
        }
    }

    @Test
    void testCreateBatchEvaluation_NotFound() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            when(scheduleRepository.findByIdAndGroupPoidAndNotDeleted(1L, 100L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.createBatchEvaluation(1L, null, false));
        }
    }
}
