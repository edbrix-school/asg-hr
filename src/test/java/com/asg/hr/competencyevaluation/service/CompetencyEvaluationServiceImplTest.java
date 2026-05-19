package com.asg.hr.competencyevaluation.service;

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
import jakarta.persistence.EntityManager;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.hr.competency.entity.CompetencyMasterEntity;
import com.asg.hr.competency.entity.HrCompetencySchedule;
import com.asg.hr.competency.repository.CompetencyMasterRepository;
import com.asg.hr.competency.repository.HrCompetencyScheduleRepository;
import com.asg.hr.competencyevaluation.dto.CompetencyEvaluationRequestDto;
import com.asg.hr.competencyevaluation.dto.CompetencyEvaluationResponseDto;
import com.asg.hr.competencyevaluation.entity.HrCompetencyEvaluationDtl;
import com.asg.hr.competencyevaluation.entity.HrCompetencyEvaluationHdr;
import com.asg.hr.competencyevaluation.repository.HrCompetencyEvaluationDtlRepository;
import com.asg.hr.competencyevaluation.repository.HrCompetencyEvaluationHdrRepository;
import com.asg.hr.competencyevaluation.util.CompetencyEvaluationConstants;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompetencyEvaluationServiceImplTest {

    @Mock
    private HrCompetencyEvaluationHdrRepository hdrRepository;
    @Mock
    private HrCompetencyEvaluationDtlRepository dtlRepository;
    @Mock
    private HrCompetencyScheduleRepository scheduleRepository;
    @Mock
    private CompetencyMasterRepository competencyMasterRepository;
    @Mock
    private DocumentSearchService documentSearchService;
    @Mock
    private DocumentDeleteService documentDeleteService;
    @Mock
    private LoggingService loggingService;
    @Mock
    private LovDataService lovDataService;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CompetencyEvaluationServiceImpl service;

    private HrCompetencySchedule schedule;
    private CompetencyMasterEntity competency;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();
        schedule = HrCompetencySchedule.builder()
                .schedulePoid(5L)
                .groupPoid(100L)
                .periodFrom(today.minusMonths(2))
                .periodTo(today.plusMonths(6))
                .deleted("N")
                .build();
        competency = CompetencyMasterEntity.builder()
                .competencyPoid(7L)
                .groupPoid(100L)
                .active("Y")
                .build();
    }

    private CompetencyEvaluationRequestDto baseRequest() {
        LocalDate today = LocalDate.now();
        return CompetencyEvaluationRequestDto.builder()
                .docRef("CE-001")
                .employeePoid(1L)
                .departmentPoid(2L)
                .designationPoid(3L)
                .reviewedByPoid(4L)
                .compSchedulePoid(5L)
                .evaluationDate(today)
                .status("PENDING")
                .details(List.of(
                        CompetencyEvaluationRequestDto.CompetencyEvaluationDetailRequestDto.builder()
                                .actionType(CompetencyEvaluationConstants.ACTION_IS_CREATED)
                                .competencyPoid(7L)
                                .rating("GOOD")
                                .hodComments("ok")
                                .employeeAgreed("AGREE")
                                .employeeComments("thanks")
                                .build()
                ))
                .build();
    }

    @Test
    void create_throwsWhenScheduleNotFound() {
        try (var uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(100L);
            when(scheduleRepository.findById(5L)).thenReturn(Optional.empty());

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(baseRequest()));
            assertTrue(ex.getMessage().toLowerCase().contains("no competency review schedule found"));
        }
    }

    @Test
    void create_throwsWhenScheduleMarkedDeleted() {
        try (var uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(100L);
            HrCompetencySchedule deletedRow = HrCompetencySchedule.builder()
                    .schedulePoid(5L)
                    .groupPoid(100L)
                    .periodFrom(LocalDate.now().minusMonths(1))
                    .periodTo(LocalDate.now().plusMonths(6))
                    .deleted("Y")
                    .build();
            when(scheduleRepository.findById(5L)).thenReturn(Optional.of(deletedRow));

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(baseRequest()));
            assertTrue(ex.getMessage().toLowerCase().contains("deleted"));
        }
    }

    @Test
    void create_throwsWhenScheduleWrongGroup() {
        try (var uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(100L);
            HrCompetencySchedule otherGroup = HrCompetencySchedule.builder()
                    .schedulePoid(5L)
                    .groupPoid(999L)
                    .periodFrom(LocalDate.now().minusMonths(1))
                    .periodTo(LocalDate.now().plusMonths(6))
                    .deleted("N")
                    .build();
            when(scheduleRepository.findById(5L)).thenReturn(Optional.of(otherGroup));

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(baseRequest()));
            assertTrue(ex.getMessage().contains("belongs to group"));
            assertTrue(ex.getMessage().contains("999"));
        }
    }

    @Test
    void create_throwsWhenLoginGroupMissing() {
        try (var uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(null);

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(baseRequest()));
            assertTrue(ex.getMessage().toLowerCase().contains("group"));
        }
    }

    @Test
    void create_throwsWhenScheduleNotInCurrentYear() {
        try (var uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(100L);
            HrCompetencySchedule old = HrCompetencySchedule.builder()
                    .schedulePoid(5L)
                    .groupPoid(100L)
                    .periodFrom(LocalDate.of(2020, 1, 1))
                    .periodTo(LocalDate.of(2020, 12, 31))
                    .deleted("N")
                    .build();
            when(scheduleRepository.findById(5L)).thenReturn(Optional.of(old));

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(baseRequest()));
            assertTrue(ex.getMessage().contains("current year"));
        }
    }

    @Test
    void create_throwsWhenEvaluationDateBeforeToday() {
        try (var uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(100L);
            when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

            CompetencyEvaluationRequestDto req = baseRequest();
            req.setEvaluationDate(LocalDate.now().minusDays(1));

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(req));
            assertTrue(ex.getMessage().contains("today or a future"));
        }
    }

    @Test
    void create_throwsWhenEvaluationOutsideSchedulePeriod() {
        try (var uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(100L);
            when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

            CompetencyEvaluationRequestDto req = baseRequest();
            req.setEvaluationDate(schedule.getPeriodTo().plusDays(10));

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(req));
            assertTrue(ex.getMessage().contains("review period"));
        }
    }

    @Test
    void create_throwsWhenRatingMissing() {
        try (var uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(100L);
            when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));
            when(competencyMasterRepository.findByIdAndGroupPoidAndNotDeleted(7L, 100L)).thenReturn(Optional.of(competency));

            CompetencyEvaluationRequestDto req = baseRequest();
            req.getDetails().get(0).setRating("  ");

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(req));
            assertTrue(ex.getMessage().contains("rating"));
        }
    }

    @Test
    void create_throwsWhenCompetencyInactive() {
        try (var uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(100L);
            when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));
            CompetencyMasterEntity inactive = CompetencyMasterEntity.builder()
                    .competencyPoid(7L)
                    .groupPoid(100L)
                    .active("N")
                    .build();
            when(competencyMasterRepository.findByIdAndGroupPoidAndNotDeleted(7L, 100L)).thenReturn(Optional.of(inactive));

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(baseRequest()));
            assertTrue(ex.getMessage().contains("active"));
        }
    }

    @Test
    void create_success() {
        try (var uc = mockStatic(UserContext.class);
             var au = mockStatic(ASGHelperUtils.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(100L);
            uc.when(UserContext::getCompanyPoid).thenReturn(200L);
            uc.when(UserContext::getDocumentId).thenReturn("DOC800");
            au.when(ASGHelperUtils::getCurrentUser).thenReturn("tester");

            when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));
            when(competencyMasterRepository.findByIdAndGroupPoidAndNotDeleted(7L, 100L)).thenReturn(Optional.of(competency));

            when(hdrRepository.save(any(HrCompetencyEvaluationHdr.class))).thenAnswer(inv -> {
                HrCompetencyEvaluationHdr h = inv.getArgument(0);
                if (h.getTransactionPoid() == null) {
                    h.setTransactionPoid(99L);
                }
                return h;
            });
            when(dtlRepository.save(any(HrCompetencyEvaluationDtl.class))).thenAnswer(inv -> inv.getArgument(0));

            HrCompetencyEvaluationDtl line = HrCompetencyEvaluationDtl.builder()
                    .transactionPoid(99L)
                    .detRowId(1L)
                    .rating("GOOD")
                    .employeeAgreed("AGREE")
                    .build();
            when(dtlRepository.findByTransactionPoidOrderByDetRowId(99L)).thenReturn(List.of(line));

            HrCompetencyEvaluationHdr withScores = HrCompetencyEvaluationHdr.builder()
                    .transactionPoid(99L)
                    .docRef("CE-001")
                    .employeePoid(1L)
                    .compSchedulePoid(5L)
                    .status("PENDING")
                    .deleted("N")
                    .build();
            when(hdrRepository.findActiveById(99L)).thenReturn(Optional.of(withScores));

            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), any(), any());

            CompetencyEvaluationResponseDto dto = service.create(baseRequest());

            assertNotNull(dto);
            assertEquals(99L, dto.getTransactionPoid());
            verify(dtlRepository).save(any(HrCompetencyEvaluationDtl.class));
        }
    }

    @Test
    void create_usesTransactionDateFromRequestWhenProvided() {
        LocalDate txn = LocalDate.of(2026, 3, 15);
        try (var uc = mockStatic(UserContext.class);
             var au = mockStatic(ASGHelperUtils.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(100L);
            uc.when(UserContext::getCompanyPoid).thenReturn(200L);
            uc.when(UserContext::getDocumentId).thenReturn("DOC800");
            au.when(ASGHelperUtils::getCurrentUser).thenReturn("tester");

            when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));
            when(competencyMasterRepository.findByIdAndGroupPoidAndNotDeleted(7L, 100L)).thenReturn(Optional.of(competency));

            CompetencyEvaluationRequestDto req = baseRequest();
            req.setTransactionDate(txn);

            when(hdrRepository.save(any(HrCompetencyEvaluationHdr.class))).thenAnswer(inv -> {
                HrCompetencyEvaluationHdr h = inv.getArgument(0);
                if (h.getTransactionPoid() == null) {
                    assertEquals(txn, h.getTransactionDate());
                    h.setTransactionPoid(99L);
                }
                return h;
            });
            when(dtlRepository.save(any(HrCompetencyEvaluationDtl.class))).thenAnswer(inv -> inv.getArgument(0));

            HrCompetencyEvaluationDtl line = HrCompetencyEvaluationDtl.builder()
                    .transactionPoid(99L)
                    .detRowId(1L)
                    .rating("GOOD")
                    .employeeAgreed("AGREE")
                    .build();
            when(dtlRepository.findByTransactionPoidOrderByDetRowId(99L)).thenReturn(List.of(line));

            HrCompetencyEvaluationHdr withScores = HrCompetencyEvaluationHdr.builder()
                    .transactionPoid(99L)
                    .transactionDate(txn)
                    .docRef("CE-001")
                    .employeePoid(1L)
                    .compSchedulePoid(5L)
                    .status("PENDING")
                    .deleted("N")
                    .build();
            when(hdrRepository.findActiveById(99L)).thenReturn(Optional.of(withScores));

            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), any(), any());

            assertNotNull(service.create(req));
        }
    }

    @Test
    void update_throwsWhenCompleted() {
        HrCompetencyEvaluationHdr hdr = HrCompetencyEvaluationHdr.builder()
                .transactionPoid(1L)
                .status("COMPLETED")
                .deleted("N")
                .build();
        when(hdrRepository.findActiveById(1L)).thenReturn(Optional.of(hdr));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(1L, baseRequest()));
        assertTrue(ex.getMessage().contains("Completed"));
    }

    @Test
    void calculateScores_throwsWhenRatingMissing() {
        HrCompetencyEvaluationHdr hdr = HrCompetencyEvaluationHdr.builder()
                .transactionPoid(1L)
                .status("PENDING")
                .deleted("N")
                .build();
        when(hdrRepository.findActiveById(1L)).thenReturn(Optional.of(hdr));
        when(dtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(
                HrCompetencyEvaluationDtl.builder().rating(null).build()
        ));

        ValidationException ex = assertThrows(ValidationException.class, () -> service.calculateScores(1L));
        assertTrue(ex.getMessage().contains("rating"));
    }

    @Test
    void getById_notFound() {
        when(hdrRepository.findActiveById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(1L));
    }

    @Test
    void getById_enrichesLovDetails() {
        HrCompetencyEvaluationHdr hdr = HrCompetencyEvaluationHdr.builder()
                .transactionPoid(1L)
                .employeePoid(10L)
                .departmentPoid(2L)
                .designationPoid(3L)
                .reviewedByPoid(40L)
                .compSchedulePoid(125L)
                .deleted("N")
                .build();
        HrCompetencyEvaluationDtl line = HrCompetencyEvaluationDtl.builder()
                .detRowId(1L)
                .competencyPoid(24L)
                .compSchedulePoid(125L)
                .rating("EXCELLENT")
                .build();

        when(hdrRepository.findActiveById(1L)).thenReturn(Optional.of(hdr));
        when(dtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(line));
        when(lovDataService.getDetailsByPoidAndLovNameFast(10L, "EMPLOYEE_NAME"))
                .thenReturn(new LovGetListDto(10L, "E10", "Employee Ten", 10L, "Employee Ten", null, null));
        when(lovDataService.getDetailsByPoidAndLovNameFast(40L, "EMPLOYEE_NAME"))
                .thenReturn(new LovGetListDto(40L, "E40", "Reviewer Forty", 40L, "Reviewer Forty", null, null));
        when(lovDataService.getDetailsByPoidAndLovNameFast(125L, "HR_COMPETENCY_SCHEDULES"))
                .thenReturn(new LovGetListDto(125L, "125", "Q3 Review", 125L, "Q3 Review", null, null));

        jakarta.persistence.Query deptQuery = org.mockito.Mockito.mock(jakarta.persistence.Query.class);
        jakarta.persistence.Query desigQuery = org.mockito.Mockito.mock(jakarta.persistence.Query.class);
        jakarta.persistence.Query compQuery = org.mockito.Mockito.mock(jakarta.persistence.Query.class);
        when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.contains("HR_DEPARTMENT_MASTER")))
                .thenReturn(deptQuery);
        when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.contains("HR_DESIGNATION_MASTER")))
                .thenReturn(desigQuery);
        when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.contains("HR_COMPETENCY_MASTER")))
                .thenReturn(compQuery);
        when(deptQuery.setParameter(org.mockito.ArgumentMatchers.eq("poids"), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(deptQuery);
        when(desigQuery.setParameter(org.mockito.ArgumentMatchers.eq("poids"), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(desigQuery);
        when(compQuery.setParameter(org.mockito.ArgumentMatchers.eq("poids"), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(compQuery);
        when(deptQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{2L, "D02", "Finance"}));
        when(desigQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{3L, "DS03", "Analyst"}));
        when(compQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{24L, "C24", "Teamwork"}));

        CompetencyEvaluationResponseDto result = service.getById(1L);

        assertNotNull(result.getEmployeeDet());
        assertEquals("Employee Ten", result.getEmployeeDet().getDescription());
        assertNotNull(result.getReviewedByDet());
        assertEquals("Reviewer Forty", result.getReviewedByDet().getDescription());
        assertNotNull(result.getCompScheduleDet());
        assertEquals("Q3 Review", result.getCompScheduleDet().getDescription());
        assertEquals("Finance", result.getDepartmentDet().getDescription());
        assertEquals("Analyst", result.getDesignationDet().getDescription());
        assertEquals(1, result.getDetails().size());
        assertEquals("Teamwork", result.getDetails().get(0).getCompetencyDet().getDescription());
        assertEquals("Q3 Review", result.getDetails().get(0).getCompScheduleDet().getDescription());
    }

    @Test
    void delete_callsDocumentDelete() {
        when(hdrRepository.findActiveById(1L)).thenReturn(Optional.of(
                HrCompetencyEvaluationHdr.builder().transactionPoid(1L).deleted("N").build()));

        service.delete(1L, new DeleteReasonDto());

        verify(documentDeleteService).deleteDocument(eq(1L), eq("HR_COMPETENCY_EVALUATION_HDR"),
                eq("TRANSACTION_POID"), any(), any());
    }

    @Test
    void list_delegatesToDocumentSearch() {
        List<FilterDto> filterList = List.of(new FilterDto("DOC_REF", "X"));
        FilterRequestDto fr = new FilterRequestDto("AND", "N", filterList);
        Pageable pageable = PageRequest.of(0, 5);
        RawSearchResult raw = new RawSearchResult(List.of(Map.of("DOC_REF", "CE-1")), Map.of(), 1L);
        when(documentSearchService.resolveOperator(fr)).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(fr)).thenReturn("N");
        when(documentSearchService.resolveFilters(fr)).thenReturn(filterList);
        when(documentSearchService.search(any(), any(), eq("AND"), eq(pageable), eq("N"), any(), any()))
                .thenReturn(raw);

        try (var uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getDocumentId).thenReturn("DOC800");
            Map<String, Object> result = service.list(fr, pageable);
            assertNotNull(result);
        }
    }
}
