package com.asg.hr.allowanceanddeductionmaster.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.CustomException;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.hr.allowanceanddeductionmaster.dto.AllowanceDeductionRequestDTO;
import com.asg.hr.allowanceanddeductionmaster.dto.AllowanceDeductionResponseDTO;
import com.asg.hr.allowanceanddeductionmaster.entity.HrAllowanceDeductionMaster;
import com.asg.hr.allowanceanddeductionmaster.mapper.AllowanceDeductionMasterMapper;
import com.asg.hr.allowanceanddeductionmaster.repository.AllowanceDeductionMasterRepository;
import com.asg.hr.allowanceanddeductionmaster.service.impl.AllowanceDeductionMasterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllowanceDeductionMasterServiceTest {

    @Mock
    private AllowanceDeductionMasterRepository repository;

    @Mock
    private AllowanceDeductionMasterMapper mapper;

    @Mock
    private LoggingService loggingService;

    @Mock
    private DocumentSearchService documentSearchService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @InjectMocks
    private AllowanceDeductionMasterServiceImpl service;

    private AllowanceDeductionRequestDTO requestDTO;
    private AllowanceDeductionResponseDTO responseDTO;
    private HrAllowanceDeductionMaster entity;

    @BeforeEach
    void setUp() {
        requestDTO = AllowanceDeductionRequestDTO.builder()
                .code("BASIC_PAY")
                .description("Basic Salary")
                .variableFixed("FIXED")
                .type("ALLOWANCE")
                .formula("BASE_SALARY")
                .glPoid(1L)
                .mandatory("Y")
                .payrollFieldName("basic_pay")
                .seqno(1)
                .active("Y")
                .build();

        responseDTO = AllowanceDeductionResponseDTO.builder()
                .allowaceDeductionPoid(1L)
                .code("BASIC_PAY")
                .description("Basic Salary")
                .variableFixed("FIXED")
                .type("ALLOWANCE")
                .formula("BASE_SALARY")
                .glPoid(1L)
                .mandatory("Y")
                .payrollFieldName("basic_pay")
                .seqno(1)
                .active("Y")
                .groupPoid(1L)
                .deleted("N")
                .createdBy("admin")
                .createdDate(LocalDateTime.now())
                .build();

        entity = HrAllowanceDeductionMaster.builder()
                .allowaceDeductionPoid(1L)
                .code("BASIC_PAY")
                .description("Basic Salary")
                .variableFixed("FIXED")
                .type("ALLOWANCE")
                .formula("BASE_SALARY")
                .glPoid(1L)
                .mandatory("Y")
                .payrollFieldName("basic_pay")
                .seqno(1)
                .active("Y")
                .groupPoid(1L)
                .deleted("N")
                .build();
    }

    // ---------- CREATE ----------
    @Test
    void testCreateSuccess() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<ASGHelperUtils> mockedASGHelper = mockStatic(ASGHelperUtils.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-004");
            mockedASGHelper.when(ASGHelperUtils::getCurrentUser).thenReturn("admin");

            when(repository.findByCodeAndDeletedNot(requestDTO.getCode(), "Y")).thenReturn(Optional.empty());
            when(repository.findByPayrollFieldName(requestDTO.getPayrollFieldName())).thenReturn(Optional.empty());
            when(mapper.toEntity(requestDTO)).thenReturn(entity);
            when(repository.save(any(HrAllowanceDeductionMaster.class))).thenReturn(entity);
            when(mapper.toResponseDTO(entity)).thenReturn(responseDTO);
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

            AllowanceDeductionResponseDTO result = service.create(requestDTO);

            assertNotNull(result);
            assertEquals("BASIC_PAY", result.getCode());
            verify(repository).save(any(HrAllowanceDeductionMaster.class));
            verify(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());
        }
    }

    @Test
    void testCreateSuccessWithNullPayrollFieldName() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<ASGHelperUtils> mockedASGHelper = mockStatic(ASGHelperUtils.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-004");
            mockedASGHelper.when(ASGHelperUtils::getCurrentUser).thenReturn("admin");

            requestDTO.setPayrollFieldName(null);
            when(repository.findByCodeAndDeletedNot(requestDTO.getCode(), "Y")).thenReturn(Optional.empty());
            when(mapper.toEntity(requestDTO)).thenReturn(entity);
            when(repository.save(any(HrAllowanceDeductionMaster.class))).thenReturn(entity);
            when(mapper.toResponseDTO(entity)).thenReturn(responseDTO);
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

            AllowanceDeductionResponseDTO result = service.create(requestDTO);

            assertNotNull(result);
            verify(repository, never()).findByPayrollFieldName(any());
        }
    }

    @Test
    void testCreateSuccessWithEmptyPayrollFieldName() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<ASGHelperUtils> mockedASGHelper = mockStatic(ASGHelperUtils.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-004");
            mockedASGHelper.when(ASGHelperUtils::getCurrentUser).thenReturn("admin");

            requestDTO.setPayrollFieldName("");
            when(repository.findByCodeAndDeletedNot(requestDTO.getCode(), "Y")).thenReturn(Optional.empty());
            when(mapper.toEntity(requestDTO)).thenReturn(entity);
            when(repository.save(any(HrAllowanceDeductionMaster.class))).thenReturn(entity);
            when(mapper.toResponseDTO(entity)).thenReturn(responseDTO);
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

            AllowanceDeductionResponseDTO result = service.create(requestDTO);

            assertNotNull(result);
            verify(repository, never()).findByPayrollFieldName(any());
        }
    }

    @Test
    void testCreateDuplicateCode() {
        when(repository.findByCodeAndDeletedNot(requestDTO.getCode(), "Y")).thenReturn(Optional.of(entity));

        assertThrows(ResourceAlreadyExistsException.class, () -> service.create(requestDTO));
        verify(repository, never()).save(any());
    }

    @Test
    void testCreateDuplicatePayrollFieldName() {
        when(repository.findByCodeAndDeletedNot(requestDTO.getCode(), "Y")).thenReturn(Optional.empty());
        when(repository.findByPayrollFieldName(requestDTO.getPayrollFieldName())).thenReturn(Optional.of(entity));

        assertThrows(ResourceAlreadyExistsException.class, () -> service.create(requestDTO));
        verify(repository, never()).save(any());
    }

    // ---------- UPDATE ----------
    @Test
    void testUpdateSuccess() {
        Long id = 1L;
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<ASGHelperUtils> mockedASGHelper = mockStatic(ASGHelperUtils.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-004");
            mockedASGHelper.when(ASGHelperUtils::getCurrentUser).thenReturn("admin");

            when(repository.findById(id)).thenReturn(Optional.of(entity));
            when(repository.findByPayrollFieldName(requestDTO.getPayrollFieldName())).thenReturn(Optional.empty());
            when(repository.save(any(HrAllowanceDeductionMaster.class))).thenReturn(entity);
            when(mapper.toResponseDTO(entity)).thenReturn(responseDTO);
            doNothing().when(mapper).updateEntity(any(AllowanceDeductionRequestDTO.class), any(HrAllowanceDeductionMaster.class));
            doNothing().when(loggingService).logChanges(any(), any(), any(), anyString(), anyString(), any(), anyString());

            AllowanceDeductionResponseDTO result = service.update(id, requestDTO);

            assertNotNull(result);
            verify(repository).save(any(HrAllowanceDeductionMaster.class));
            verify(loggingService).logChanges(any(), any(), any(), anyString(), anyString(), any(), anyString());
        }
    }

    @Test
    void testUpdateSuccessWithSamePayrollFieldName() {
        Long id = 1L;
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<ASGHelperUtils> mockedASGHelper = mockStatic(ASGHelperUtils.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-004");
            mockedASGHelper.when(ASGHelperUtils::getCurrentUser).thenReturn("admin");

            HrAllowanceDeductionMaster existingEntity = HrAllowanceDeductionMaster.builder()
                    .allowaceDeductionPoid(id)
                    .payrollFieldName(requestDTO.getPayrollFieldName())
                    .build();

            when(repository.findById(id)).thenReturn(Optional.of(entity));
            when(repository.findByPayrollFieldName(requestDTO.getPayrollFieldName())).thenReturn(Optional.of(existingEntity));
            when(repository.save(any(HrAllowanceDeductionMaster.class))).thenReturn(entity);
            when(mapper.toResponseDTO(entity)).thenReturn(responseDTO);
            doNothing().when(mapper).updateEntity(any(AllowanceDeductionRequestDTO.class), any(HrAllowanceDeductionMaster.class));
            doNothing().when(loggingService).logChanges(any(), any(), any(), anyString(), anyString(), any(), anyString());

            AllowanceDeductionResponseDTO result = service.update(id, requestDTO);

            assertNotNull(result);
            verify(repository).save(any(HrAllowanceDeductionMaster.class));
        }
    }

    @Test
    void testUpdateSuccessWithNullPayrollFieldName() {
        Long id = 1L;
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<ASGHelperUtils> mockedASGHelper = mockStatic(ASGHelperUtils.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-004");
            mockedASGHelper.when(ASGHelperUtils::getCurrentUser).thenReturn("admin");

            requestDTO.setPayrollFieldName(null);
            when(repository.findById(id)).thenReturn(Optional.of(entity));
            when(repository.save(any(HrAllowanceDeductionMaster.class))).thenReturn(entity);
            when(mapper.toResponseDTO(entity)).thenReturn(responseDTO);
            doNothing().when(mapper).updateEntity(any(AllowanceDeductionRequestDTO.class), any(HrAllowanceDeductionMaster.class));
            doNothing().when(loggingService).logChanges(any(), any(), any(), anyString(), anyString(), any(), anyString());

            AllowanceDeductionResponseDTO result = service.update(id, requestDTO);

            assertNotNull(result);
            verify(repository, never()).findByPayrollFieldName(any());
        }
    }

    @Test
    void testUpdateSuccessWithEmptyPayrollFieldName() {
        Long id = 1L;
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<ASGHelperUtils> mockedASGHelper = mockStatic(ASGHelperUtils.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-004");
            mockedASGHelper.when(ASGHelperUtils::getCurrentUser).thenReturn("admin");

            requestDTO.setPayrollFieldName("");
            when(repository.findById(id)).thenReturn(Optional.of(entity));
            when(repository.save(any(HrAllowanceDeductionMaster.class))).thenReturn(entity);
            when(mapper.toResponseDTO(entity)).thenReturn(responseDTO);
            doNothing().when(mapper).updateEntity(any(AllowanceDeductionRequestDTO.class), any(HrAllowanceDeductionMaster.class));
            doNothing().when(loggingService).logChanges(any(), any(), any(), anyString(), anyString(), any(), anyString());

            AllowanceDeductionResponseDTO result = service.update(id, requestDTO);

            assertNotNull(result);
            verify(repository, never()).findByPayrollFieldName(any());
        }
    }

    @Test
    void testUpdateDuplicatePayrollFieldName() {
        Long id = 1L;
        HrAllowanceDeductionMaster existingEntity = HrAllowanceDeductionMaster.builder()
                .allowaceDeductionPoid(2L) // Different ID
                .payrollFieldName(requestDTO.getPayrollFieldName())
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.findByPayrollFieldName(requestDTO.getPayrollFieldName())).thenReturn(Optional.of(existingEntity));

        assertThrows(ResourceAlreadyExistsException.class, () -> service.update(id, requestDTO));
        verify(repository, never()).save(any());
    }

    @Test
    void testUpdateNotFound() {
        Long id = 999L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(id, requestDTO));
        verify(repository, never()).save(any());
    }

    @Test
    void testUpdateDeletedRecord() {
        Long id = 1L;
        entity.setDeleted("Y");
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        assertThrows(CustomException.class, () -> service.update(id, requestDTO));
        verify(repository, never()).save(any());
    }

    // ---------- GET BY ID ----------
    @Test
    void testGetByIdSuccess() {
        Long id = 1L;
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toResponseDTO(entity)).thenReturn(responseDTO);

        AllowanceDeductionResponseDTO result = service.getById(id);

        assertNotNull(result);
        assertEquals("BASIC_PAY", result.getCode());
        verify(repository).findById(id);
    }

    @Test
    void testGetByIdNotFound() {
        Long id = 999L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(id));
    }

    @Test
    void testGetByIdDeletedRecord() {
        Long id = 1L;
        entity.setDeleted("Y");
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        assertThrows(CustomException.class, () -> service.getById(id));
    }

    // ---------- DELETE ----------
    @Test
    void testDeleteSuccess() {
        Long id = 1L;
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(documentDeleteService.deleteDocument(id, "HR_ALLOWANCE_DEDUCTION_MASTER", "ALLOWACE_DEDUCTION_POID", null, null))
                .thenReturn("SUCCESS");

        service.delete(id, null);

        verify(documentDeleteService).deleteDocument(id, "HR_ALLOWANCE_DEDUCTION_MASTER", "ALLOWACE_DEDUCTION_POID", null, null);
    }

    @Test
    void testDeleteRecordNotFound() {
        Long id = 1L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(id, null));
        verify(documentDeleteService, never()).deleteDocument(anyLong(), anyString(), anyString(), any(), any());
    }

    // ---------- SEARCH ----------
    @Test
    void testSearchSuccess() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-004");

            Pageable pageable = PageRequest.of(0, 20);
            FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", new ArrayList<>());
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);
            List<FilterDto> filters = new ArrayList<>();
            List<Map<String, Object>> records = new ArrayList<>();
            Map<String, String> displayFields = new HashMap<>();

            when(documentSearchService.resolveOperator(filterRequest)).thenReturn("AND");
            when(documentSearchService.resolveIsDeleted(filterRequest)).thenReturn("N");
            when(documentSearchService.resolveDateFilters(filterRequest, "TRANSACTION_DATE", startDate, endDate)).thenReturn(filters);
            when(documentSearchService.search(anyString(), anyList(), anyString(), any(Pageable.class), anyString(), anyString(), anyString()))
                    .thenReturn(new RawSearchResult(records, displayFields, 0L));

            Map<String, Object> result = service.list(filterRequest, startDate, endDate, pageable);

            assertNotNull(result);
            verify(documentSearchService).resolveDateFilters(filterRequest, "TRANSACTION_DATE", startDate, endDate);
            verify(documentSearchService).search(anyString(), anyList(), anyString(), any(Pageable.class), anyString(), anyString(), anyString());
        }
    }

    @Test
    void testSearchWithNullDates() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-004");

            Pageable pageable = PageRequest.of(0, 20);
            FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", new ArrayList<>());
            List<FilterDto> filters = new ArrayList<>();
            List<Map<String, Object>> records = new ArrayList<>();
            Map<String, String> displayFields = new HashMap<>();

            when(documentSearchService.resolveOperator(filterRequest)).thenReturn("AND");
            when(documentSearchService.resolveIsDeleted(filterRequest)).thenReturn("N");
            when(documentSearchService.resolveDateFilters(filterRequest, "TRANSACTION_DATE", null, null)).thenReturn(filters);
            when(documentSearchService.search(anyString(), anyList(), anyString(), any(Pageable.class), anyString(), anyString(), anyString()))
                    .thenReturn(new RawSearchResult(records, displayFields, 0L));

            Map<String, Object> result = service.list(filterRequest, null, null, pageable);

            assertNotNull(result);
            verify(documentSearchService).resolveDateFilters(filterRequest, "TRANSACTION_DATE", null, null);
        }
    }
}
