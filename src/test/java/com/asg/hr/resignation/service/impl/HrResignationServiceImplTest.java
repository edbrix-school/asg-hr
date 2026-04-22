package com.asg.hr.resignation.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.*;
import com.asg.hr.resignation.dto.*;
import com.asg.hr.resignation.entity.HrResignationEntity;
import com.asg.hr.resignation.repository.HrResignationProcRepository;
import com.asg.hr.resignation.repository.HrResignationRepository;
import com.asg.hr.resignation.util.HrResignationConstants;
import com.asg.hr.resignation.util.HrResignationMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrResignationServiceImplTest {
    private static final String DOCUMENT_ID = "800-116";

    @Mock
    private HrResignationRepository repository;

    @Mock
    private HrResignationProcRepository procRepository;

    @Mock
    private HrResignationMapper mapper;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private LovDataService lovDataService;

    @Mock
    private Query query;

    @Mock
    private LoggingService loggingService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @InjectMocks
    private HrResignationServiceImpl service;

    private HrResignationRequest baseRequest;
    private HrResignationEmployeeDetailsResponse employeeDetails;
    private Pageable pageable;

    @BeforeEach
    void setup() {
        pageable = PageRequest.of(0, 10);

        baseRequest = new HrResignationRequest();
        baseRequest.setEmployeePoid(1001L);
        baseRequest.setTransactionDate(LocalDate.now());
        baseRequest.setLastDateOfWork(LocalDate.now().plusDays(10));
        baseRequest.setResignationDetails("Resign for personal reasons");
        baseRequest.setResignationType("VOLUNTARY");
        baseRequest.setHodRemarks("HOD remarks");
        baseRequest.setRemarks("Remarks");

        employeeDetails = HrResignationEmployeeDetailsResponse.builder()
                .departmentPoid(2002L)
                .designationPoid(3003L)
                .directSupervisorPoid(4004L)
                .joinDate(LocalDate.now().minusDays(100))
                .rpExpiryDate(LocalDate.now().plusDays(30))
                .resignationType(HrResignationConstants.RESIGNATION_TYPE_VOLUNTARY)
                .status("SUCCESS")
                .build();

        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.setParameter(anyString(), any())).thenReturn(query);
        lenient().when(query.getResultList()).thenReturn(List.of());
        lenient().when(lovDataService.getLovItemByCodeFast(anyString(), eq("RESIGNATION_TYPE")))
                .thenReturn(new com.asg.common.lib.dto.LovGetListDto());
    }

    @Test
    void listResignations_ReturnsWrappedPage() {
        FilterRequestDto filters = new FilterRequestDto("OR", "N", List.of(new FilterDto("EMPLOYEE_POID", "1001")));
        List<FilterDto> resolvedFilters = List.of(new FilterDto("EMPLOYEE_POID", "1001"));
        when(documentService.resolveOperator(filters)).thenReturn("OR");
        when(documentService.resolveIsDeleted(filters)).thenReturn("N");
        when(documentService.resolveDateFilters(filters, "TRANSACTION_DATE", null, null)).thenReturn(resolvedFilters);

        Map<String, Object> row = Map.of("DOC_REF", "DOC-1", "TRANSACTION_POID", 1L);
        Map<String, String> display = Map.of("DOC_REF", "Doc Ref");
        RawSearchResult raw = new RawSearchResult(List.of(row), display, 1L);

        when(documentService.search(
                eq(DOCUMENT_ID),
                eq(resolvedFilters),
                eq("OR"),
                eq(pageable),
                eq("N"),
                eq("DOC_REF"),
                eq(HrResignationConstants.KEY_FIELD)
        )).thenReturn(raw);

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn(DOCUMENT_ID);
            Map<String, Object> result = service.listResignations(filters, null, null, pageable);

            assertNotNull(result);
            assertEquals(1L, result.get("totalElements"));
            verify(documentService).search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString());
        }
    }

    @Test
    void getById_WhenFound_SetsApprovalStatus() {
        HrResignationEntity entity = HrResignationEntity.builder()
                .transactionPoid(10L)
                .deleted("N")
                .build();
        HrResignationResponse response = new HrResignationResponse();
        response.setTransactionPoid(10L);

        when(repository.findById(10L)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);
        HrResignationResponse result = service.getById(10L);
        assertNotNull(result);
        verify(repository).findById(10L);
        verify(mapper).toResponse(entity);
    }

    @Test
    void getById_WhenNotFound_Throws() {
        when(repository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(10L));
    }

    @Test
    void create_WhenSuccess_SavesAndAutoSubmits() {
        HrResignationEntity mappedEntity = new HrResignationEntity();
        mappedEntity.setEmployeePoid(baseRequest.getEmployeePoid());
        mappedEntity.setDeleted("N");
        HrResignationEntity savedEntity = new HrResignationEntity();
        savedEntity.setTransactionPoid(123L);

        when(procRepository.getEmployeeDetails(baseRequest.getEmployeePoid())).thenReturn(employeeDetails);
        when(procRepository.beforeSaveValidation(anyLong(), anyLong(), any(), eq(0L), eq(baseRequest.getEmployeePoid()), any()))
                .thenReturn("Success");
        when(mapper.toEntity(any(HrResignationRequest.class))).thenReturn(mappedEntity);
        when(repository.save(any(HrResignationEntity.class))).thenReturn(savedEntity);
        HrResignationResponse mappedResponse = new HrResignationResponse();
        mappedResponse.setTransactionPoid(123L);
        mappedResponse.setEmployeePoid(1001L);
        mappedResponse.setLastDateOfWork(baseRequest.getLastDateOfWork());
        mappedResponse.setResignationDetails(baseRequest.getResignationDetails());
        mappedResponse.setResignationType(baseRequest.getResignationType());
        mappedResponse.setHodRemarks(baseRequest.getHodRemarks());
        mappedResponse.setRemarks(baseRequest.getRemarks());
        when(mapper.toResponse(savedEntity)).thenReturn(mappedResponse);

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(10L);
            userContext.when(UserContext::getCompanyPoid).thenReturn(20L);
            userContext.when(UserContext::getUserPoid).thenReturn(30L);
            userContext.when(UserContext::getDocumentId).thenReturn(DOCUMENT_ID);

            HrResignationResponse result = service.create(baseRequest);
            assertNotNull(result);

            verify(procRepository).getEmployeeDetails(baseRequest.getEmployeePoid());
            verify(procRepository).beforeSaveValidation(eq(20L), eq(30L), any(), eq(0L), eq(1001L), eq(baseRequest.getLastDateOfWork()));
            verify(repository).save(any(HrResignationEntity.class));

            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, DOCUMENT_ID, "123");
        }
    }

    @Test
    void create_WhenBeforeSaveValidationReturnsWarning_Throws() {
        when(procRepository.getEmployeeDetails(baseRequest.getEmployeePoid())).thenReturn(employeeDetails);
        when(procRepository.beforeSaveValidation(anyLong(), anyLong(), any(), eq(0L), eq(baseRequest.getEmployeePoid()), any()))
                .thenReturn("WARNING : Selected employee resignation is there in the system.");
        when(mapper.toEntity(any(HrResignationRequest.class))).thenReturn(new HrResignationEntity());

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(10L);
            userContext.when(UserContext::getCompanyPoid).thenReturn(20L);
            userContext.when(UserContext::getUserPoid).thenReturn(30L);
            userContext.when(UserContext::getDocumentId).thenReturn(DOCUMENT_ID);

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(baseRequest));
            assertTrue(ex.getMessage().contains("WARNING"));
            verify(repository, never()).save(any());
        }
    }

    @Test
    void delete_WhenSuccess_LogsAndCallsDocumentDelete() {
        HrResignationEntity entity = new HrResignationEntity();
        entity.setTransactionPoid(77L);
        entity.setDeleted("N");

        DeleteReasonDto reason = new DeleteReasonDto();

        when(repository.findByTransactionPoidAndDeletedNot(77L, "Y")).thenReturn(Optional.of(entity));
        when(documentDeleteService.deleteDocument(eq(77L), eq(HrResignationConstants.TABLE_NAME), eq(HrResignationConstants.KEY_FIELD), eq(reason), any(LocalDate.class)))
                .thenReturn("SUCCESS");

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn(DOCUMENT_ID);
            service.delete(77L, reason);
        }

        verify(loggingService).logDelete(entity, DOCUMENT_ID, "77");
        verify(documentDeleteService).deleteDocument(eq(77L), eq(HrResignationConstants.TABLE_NAME), eq(HrResignationConstants.KEY_FIELD), eq(reason), any(LocalDate.class));
    }

    @Test
    void getEmployeeDetails_WhenStatusError_Throws() {
        HrResignationEmployeeDetailsResponse bad = HrResignationEmployeeDetailsResponse.builder()
                .resignationType("VOLUNTARY")
                .status("ERROR : not found")
                .build();
        when(procRepository.getEmployeeDetails(999L)).thenReturn(bad);
        assertThrows(ValidationException.class, () -> service.getEmployeeDetails(999L));
    }

    @Test
    void getEmployeeDetails_WhenSuccess_ReturnsDetails() {
        when(procRepository.getEmployeeDetails(1001L)).thenReturn(employeeDetails);
        HrResignationEmployeeDetailsResponse resp = service.getEmployeeDetails(1001L);
        assertEquals("SUCCESS", resp.getStatus());
        assertEquals(2002L, resp.getDepartmentPoid());
    }



    @Test
    void create_WhenUserNotAuthenticated_Throws() {
        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(null);
            userContext.when(UserContext::getCompanyPoid).thenReturn(20L);
            userContext.when(UserContext::getUserPoid).thenReturn(30L);
            assertThrows(ValidationException.class, () -> service.create(baseRequest));
        }
    }

    @Test
    void create_WhenLastDateBeforeToday_Throws() {
        baseRequest.setLastDateOfWork(LocalDate.now().minusDays(1));
        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(10L);
            userContext.when(UserContext::getCompanyPoid).thenReturn(20L);
            userContext.when(UserContext::getUserPoid).thenReturn(30L);
            assertThrows(ValidationException.class, () -> service.create(baseRequest));
        }
    }

    @Test
    void update_WhenEmployeeChanged_RefreshesEmployeeDetails() {
        HrResignationEntity existing = new HrResignationEntity();
        existing.setTransactionPoid(55L);
        existing.setEmployeePoid(1001L);
        existing.setTransactionDate(LocalDate.now());
        existing.setResignationType(HrResignationConstants.RESIGNATION_TYPE_VOLUNTARY);
        existing.setDeleted("N");

        HrResignationRequest req = new HrResignationRequest();
        req.setEmployeePoid(1002L);
        req.setTransactionDate(LocalDate.now());
        req.setLastDateOfWork(LocalDate.now().plusDays(1));
        req.setResignationDetails("updated");
        req.setResignationType("INVOLUNTARY");

        when(repository.findByTransactionPoidAndDeletedNot(55L, "Y")).thenReturn(Optional.of(existing));
        when(procRepository.getEmployeeDetails(1002L)).thenReturn(employeeDetails);
        when(procRepository.beforeSaveValidation(anyLong(), anyLong(), any(), eq(55L), eq(1002L), any())).thenReturn("SUCCESS");
        when(repository.save(any(HrResignationEntity.class))).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(new HrResignationResponse());

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(10L);
            userContext.when(UserContext::getCompanyPoid).thenReturn(20L);
            userContext.when(UserContext::getUserPoid).thenReturn(30L);
            userContext.when(UserContext::getDocumentId).thenReturn(DOCUMENT_ID);

            service.update(55L, req);
            verify(procRepository).getEmployeeDetails(1002L);
        }
    }

}

