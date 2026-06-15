package com.asg.hr.employeetraining.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.hr.employeetraining.dto.EmployeeTrainingDetailRequest;
import com.asg.hr.employeetraining.dto.EmployeeTrainingRequest;
import com.asg.hr.employeetraining.dto.EmployeeTrainingResponse;
import com.asg.hr.employeetraining.entity.EmployeeTrainingDetailEntity;
import com.asg.hr.employeetraining.entity.EmployeeTrainingHeaderEntity;
import com.asg.hr.employeetraining.repository.EmployeeTrainingDetailRepository;
import com.asg.hr.employeetraining.repository.EmployeeTrainingHeaderRepository;
import com.asg.hr.employeetraining.repository.EmployeeTrainingProcedureRepository;
import com.asg.hr.employeetraining.util.EmployeeTrainingConstants;
import com.asg.hr.employeetraining.util.EmployeeTrainingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeTrainingServiceImplTest {

    @Mock
    private EmployeeTrainingHeaderRepository headerRepository;

    @Mock
    private EmployeeTrainingDetailRepository detailRepository;

    @Mock
    private EmployeeTrainingProcedureRepository procedureRepository;

    @Mock
    private EmployeeTrainingMapper mapper;

    @Mock
    private DocumentSearchService documentSearchService;

    @Mock
    private LoggingService loggingService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @InjectMocks
    private EmployeeTrainingServiceImpl service;

    private EmployeeTrainingRequest request;
    private EmployeeTrainingHeaderEntity header;
    private EmployeeTrainingResponse response;
    private List<EmployeeTrainingDetailEntity> detailEntities;

    @BeforeEach
    void setUp() {
        EmployeeTrainingDetailRequest detailRequest = new EmployeeTrainingDetailRequest(
                1,
                "isCreated",
                1001L,
                "COMPLETED",
                LocalDate.of(2030, 1, 2),
                "Done"
        );

        request = new EmployeeTrainingRequest();
        request.setCourseName("Safety Training");
        request.setPeriodFrom(LocalDate.of(2030, 1, 1));
        request.setPeriodTo(LocalDate.of(2030, 1, 2));
        request.setDurationDays(2);
        request.setTrainingType("INHOUSE");
        request.setInstitution("ASG Academy");
        request.setTrainingCost(BigDecimal.TEN);
        request.setTrainingLocation("Doha");
        request.setRemarks("Mandatory");
        request.setTransactionDate(LocalDate.of(2030, 1, 1));
        request.setEmployeePoid("1001");
        request.setDetails(List.of(detailRequest));

        header = EmployeeTrainingHeaderEntity.builder()
                .transactionPoid(1L)
                .courseName("Safety Training")
                .periodFrom(LocalDate.of(2030, 1, 1))
                .periodTo(LocalDate.of(2030, 1, 2))
                .durationDays(2)
                .trainingType("INHOUSE")
                .institution("ASG Academy")
                .trainingCost(BigDecimal.TEN)
                .trainingLocation("Doha")
                .remarks("Mandatory")
                .transactionDate(LocalDate.of(2030, 1, 1))
                .employeePoid("1001")
                .deleted("N")
                .build();

        detailEntities = List.of();

        response = new EmployeeTrainingResponse();
        response.setTransactionPoid(1L);
        response.setCourseName("Safety Training");
        response.setEmployeePoid("1");
        response.setTrainingType("INTERNAL");
        response.setActive("Y");
    }

    @Test
    void listTrainings_ReturnsPaginatedMap() {
        FilterDto filter = new FilterDto("COURSE_NAME", "Safety");
        FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", List.of(filter));
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of("COURSE_NAME", "Safety Training")),
                Map.of("COURSE_NAME", "Course Name"),
                1L
        );

        when(documentSearchService.resolveOperator(filterRequest)).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentSearchService.resolveFilters(filterRequest)).thenReturn(List.of(filter));
        when(documentSearchService.search(eq("800-108"), any(), eq("AND"), eq(pageable), eq("N"), eq("COURSE_NAME"), eq("TRANSACTION_POID")))
                .thenReturn(raw);

        Map<String, Object> result = service.listTrainings("800-108", filterRequest, pageable);

        assertNotNull(result);
        verify(documentSearchService).search(eq("800-108"), any(), eq("AND"), eq(pageable), eq("N"),
                eq("COURSE_NAME"), eq("TRANSACTION_POID"));
    }

    @Test
    void getTrainingById_WhenFound_ReturnsResponse() {
        when(headerRepository.findByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(header));
        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowIdAsc(1L)).thenReturn(detailEntities);
        when(procedureRepository.getEmployeeLovByIds(any()))
                .thenReturn(List.of(new LovGetListDto(1L, "E001", "John", 1L, "John", 1, null)));
        when(procedureRepository.getTrainingTypeByCode(anyString()))
                .thenReturn(new LovGetListDto(1L, "INTERNAL", "Internal", 1L, "Internal", 1, null));
        when(procedureRepository.getTrainingStatusByCodes(any()))
                .thenReturn(List.of(new LovGetListDto(1L, "PLANNED", "PLANNED", 1L, "PLANNED", 1, null)));
        when(mapper.toResponse(header, detailEntities)).thenReturn(response);

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class);
             MockedStatic<ASGHelperUtils> helperUtils = Mockito.mockStatic(ASGHelperUtils.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("800-108");
            userContext.when(UserContext::getUserPoid).thenReturn(99L);
            userContext.when(UserContext::getGroupPoid).thenReturn(10L);
            helperUtils.when(ASGHelperUtils::getCompanyId).thenReturn(1L);

            EmployeeTrainingResponse result = service.getTrainingById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getTransactionPoid());
            assertNotNull(result.getEmployeeDet());
            assertEquals("E001", result.getEmployeeDet().getCode());
            assertNotNull(result.getTrainingTypeDet());
            assertEquals("INTERNAL", result.getTrainingTypeDet().getCode());
        }
    }

    @Test
    void getTrainingById_WhenNotFound_ThrowsException() {
        when(headerRepository.findByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getTrainingById(1L));
    }

    @Test
    void createTraining_WhenSuccess_CreatesHeaderAndDetails() {
        when(headerRepository.existsByCourseNameIgnoreCaseAndPeriodFromAndPeriodToAndDeletedNot(
                anyString(), any(), any(), anyString())).thenReturn(false);
        when(mapper.toHeaderEntity(any(), anyLong(), anyLong())).thenReturn(header);
        when(headerRepository.save(header)).thenReturn(header);
        when(mapper.toDetailEntities(eq(1L), any())).thenReturn(detailEntities);
        when(mapper.toResponse(header, detailEntities)).thenReturn(response);

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class);
             MockedStatic<ASGHelperUtils> helperUtils = Mockito.mockStatic(ASGHelperUtils.class)) {
            userContext.when(UserContext::getUserId).thenReturn("user1");
            userContext.when(UserContext::getGroupPoid).thenReturn(10L);
            userContext.when(UserContext::getDocumentId).thenReturn("800-108");
            helperUtils.when(ASGHelperUtils::getCompanyId).thenReturn(1L);

            EmployeeTrainingResponse result = service.createTraining(request);

            assertNotNull(result);
            verify(headerRepository).save(header);
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "800-108", "1");
        }
    }

    @Test
    void createTraining_WhenDuplicate_ThrowsValidationException() {
        when(headerRepository.existsByCourseNameIgnoreCaseAndPeriodFromAndPeriodToAndDeletedNot(
                anyString(), any(), any(), anyString())).thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.createTraining(request));
        assertEquals("Training already exists for this course and period", ex.getMessage());
    }

    @Test
    void createTraining_WhenThreeColumnDuplicate_ThrowsValidationException() {
        when(headerRepository.existsByCourseNameIgnoreCaseAndPeriodFromAndPeriodToAndDeletedNot(
                anyString(), any(), any(), anyString())).thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.createTraining(request));
        assertEquals("Training already exists for this course and period", ex.getMessage());
    }

    @Test
    void updateTraining_WhenSuccess_ReplacesDetailsAndLogs() {
        // update detail action is handled via detailRepository.findByIdTransactionPoidOrderByIdDetRowIdAsc
        EmployeeTrainingDetailEntity existingDetail = EmployeeTrainingDetailEntity.builder()
                .id(new com.asg.hr.employeetraining.entity.EmployeeTrainingDetailId(1L, 1))
                .empPoid(1001L)
                .trainingStatus("COMPLETED")
                .completedOn(LocalDate.of(2030, 1, 2))
                .otherRemarks("Done")
                .build();

        when(headerRepository.findByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(header));
        when(headerRepository.existsDuplicateOnUpdate(anyString(), any(), any(), anyString(), eq(1L))).thenReturn(false);
        when(headerRepository.save(any(EmployeeTrainingHeaderEntity.class))).thenReturn(header);
        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowIdAsc(1L)).thenReturn(List.of(existingDetail));
        // mapper response will be returned with detail list built in service
        when(mapper.toResponse(any(), any())).thenReturn(response);

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class);
             MockedStatic<ASGHelperUtils> helperUtils = Mockito.mockStatic(ASGHelperUtils.class)) {
            userContext.when(UserContext::getUserId).thenReturn("user1");
            userContext.when(UserContext::getGroupPoid).thenReturn(10L);
            userContext.when(UserContext::getDocumentId).thenReturn("800-108");
            helperUtils.when(ASGHelperUtils::getCompanyId).thenReturn(1L);

            EmployeeTrainingDetailRequest updatedDetail = new EmployeeTrainingDetailRequest(
                    1,
                    "isUpdated",
                    1001L,
                    "COMPLETED",
                    LocalDate.of(2030, 1, 2),
                    "Done"
            );
            request.setDetails(List.of(updatedDetail));

            EmployeeTrainingResponse result = service.updateTraining(1L, request);

            assertNotNull(result);
            verify(loggingService).logChanges(any(), eq(header), eq(EmployeeTrainingHeaderEntity.class),
                    eq("800-108"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
        }
    }

    @Test
    void updateTraining_WhenNotFound_ThrowsException() {
        when(headerRepository.findByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateTraining(1L, request));
    }

    @Test
    void updateTraining_WhenThreeColumnDuplicate_ThrowsValidationException() {
        when(headerRepository.findByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(header));
        when(headerRepository.existsDuplicateOnUpdate(anyString(), any(), any(), anyString(), eq(1L))).thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.updateTraining(1L, request));
        assertEquals("Training already exists for this course and period", ex.getMessage());
    }

    @Test
    void deleteTraining_WhenSuccess_SoftDeletes() {

        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        when(headerRepository.findById(1L)).thenReturn(Optional.of(header));

        when(documentDeleteService.deleteDocument(
                eq(1L),
                eq(EmployeeTrainingConstants.TABLE_NAME),
                eq(EmployeeTrainingConstants.KEY_FIELD),
                eq(deleteReasonDto),
                any(LocalDate.class)
        )).thenReturn("SUCCESS");

        assertDoesNotThrow(() -> service.deleteTraining(1L, deleteReasonDto));

        verify(documentDeleteService).deleteDocument(
                eq(1L),
                eq(EmployeeTrainingConstants.TABLE_NAME),
                eq(EmployeeTrainingConstants.KEY_FIELD),
                eq(deleteReasonDto),
                any(LocalDate.class)
        );
    }

    @Test
    void normalizeValidation_WhenCompletedWithoutDate_ThrowsValidationException() {
        EmployeeTrainingDetailRequest invalidDetail = new EmployeeTrainingDetailRequest(
                1,
                "isCreated",
                1001L,
                "COMPLETED",
                null,
                null
        );
        request.setDetails(List.of(invalidDetail));

        ValidationException ex = assertThrows(ValidationException.class, () -> service.createTraining(request));
        assertEquals("Completed On is required when status is COMPLETED in row 1", ex.getMessage());

        verify(headerRepository, never()).save(any());
    }
}
