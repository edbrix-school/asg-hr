package com.asg.hr.employeeinduction.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.hr.employeeinduction.dto.EmployeeInductionRequestDto;
import com.asg.hr.employeeinduction.dto.EmployeeInductionResponseDto;
import com.asg.hr.employeeinduction.entity.HrEmployeeInductionDtl;
import com.asg.hr.employeeinduction.entity.HrEmployeeInductionHdr;
import com.asg.hr.employeeinduction.repository.EmployeeInductionProcRepository;
import com.asg.hr.employeeinduction.repository.HrEmployeeInductionDtlRepository;
import com.asg.hr.employeeinduction.repository.HrEmployeeInductionHdrRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmployeeInductionServiceImplTest {

    @Mock
    private HrEmployeeInductionHdrRepository hdrRepository;

    @Mock
    private HrEmployeeInductionDtlRepository dtlRepository;

    @Mock
    private LoggingService loggingService;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private LovDataService lovDataService;

    @Mock
    private EmployeeInductionProcRepository procRepository;

    @Mock
    private PrintService printService;

    @Mock
    private javax.sql.DataSource dataSource;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private EmployeeInductionServiceImpl employeeInductionService;

    private EmployeeInductionRequestDto requestDto;
    private HrEmployeeInductionHdr headerEntity;
    private List<HrEmployeeInductionDtl> detailEntities;

    @BeforeEach
    void setUp() {
        // Setup test data
        requestDto = EmployeeInductionRequestDto.builder()
                .employeePoid(1L)
                .remarks("Test induction")
                .details(createTestDetails())
                .build();

        headerEntity = HrEmployeeInductionHdr.builder()
                .transactionPoid(1L)
                .docRef("IND-001")
                .employeePoid(1L)
                .remarks("Test induction")
                .deleted("N")
                .build();

        detailEntities = new ArrayList<>();
        
        // Setup LOV service mock to return empty details
        when(lovDataService.getDetailsByPoidAndLovName(anyLong(), anyString()))
                .thenReturn(new LovGetListDto());
        when(lovDataService.getDetailsByCodeAndLovName(anyString(), anyString()))
                .thenReturn(new LovGetListDto());
        
        // Setup EntityManager mocks
        doNothing().when(entityManager).flush();
        doNothing().when(entityManager).refresh(any());
        
        // Setup detail repository mocks
        when(dtlRepository.findMaxDetRowIdByHdrPoid(anyLong())).thenReturn(null);
    }

    @Test
    void createInduction_Success() {
        // Given
        when(hdrRepository.save(any(HrEmployeeInductionHdr.class))).thenReturn(headerEntity);
        when(dtlRepository.save(any(HrEmployeeInductionDtl.class))).thenReturn(new HrEmployeeInductionDtl());
        when(dtlRepository.findByHdrPoidAndNotDeleted(anyLong())).thenReturn(detailEntities);

        // When
        EmployeeInductionResponseDto result = employeeInductionService.createInduction(requestDto);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getPoid());
        assertEquals("IND-001", result.getDocId());
        assertEquals(1L, result.getEmployeePoid());
        verify(hdrRepository).save(any(HrEmployeeInductionHdr.class));
        verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.CREATED), isNull(), eq("1"));
    }

    @Test
    void createInduction_InvalidEmployee_ThrowsException() {
        // Given
        requestDto.setEmployeePoid(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            employeeInductionService.createInduction(requestDto));
    }

    @Test
    void updateInduction_Success() {
        // Given
        HrEmployeeInductionDtl existingDetail = HrEmployeeInductionDtl.builder()
                .transactionPoid(1L)
                .detRowId(1L)
                .inductionCatgPoid(1L)
                .status("N")
                .remarks("Existing detail")
                .build();

        when(hdrRepository.findByPoidAndNotDeleted(1L)).thenReturn(Optional.of(headerEntity));
        when(hdrRepository.save(any(HrEmployeeInductionHdr.class))).thenReturn(headerEntity);
        when(dtlRepository.findByHdrPoidAndNotDeleted(1L)).thenReturn(List.of(existingDetail));
        when(dtlRepository.findById(any())).thenReturn(Optional.of(existingDetail));
        when(dtlRepository.save(any(HrEmployeeInductionDtl.class))).thenReturn(existingDetail);
        when(dtlRepository.saveAll(anyList())).thenReturn(List.of(existingDetail));

        // When
        EmployeeInductionResponseDto result = employeeInductionService.updateInduction(1L, requestDto);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getPoid());
        verify(hdrRepository).save(any(HrEmployeeInductionHdr.class));
        verify(loggingService, times(2))
                .logChanges(any(), any(), any(), any(), any(), any(LogDetailsEnum.class), any());
    }

    @Test
    void updateInduction_NotFound_ThrowsException() {
        // Given
        when(hdrRepository.findByPoidAndNotDeleted(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> 
            employeeInductionService.updateInduction(1L, requestDto));
    }

    @Test
    void getInductionById_Success() {
        // Given
        when(hdrRepository.findByPoidAndNotDeleted(1L)).thenReturn(Optional.of(headerEntity));
        when(dtlRepository.findByHdrPoidAndNotDeleted(1L)).thenReturn(detailEntities);

        // When
        EmployeeInductionResponseDto result = employeeInductionService.getInductionById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getPoid());
        assertEquals("IND-001", result.getDocId());
    }

    @Test
    void getInductionById_NotFound_ThrowsException() {
        // Given
        when(hdrRepository.findByPoidAndNotDeleted(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> 
            employeeInductionService.getInductionById(1L));
    }

    @Test
    void getInductionsByEmployee_Success() {
        // Given
        List<HrEmployeeInductionHdr> headers = List.of(headerEntity);
        when(hdrRepository.findByEmployeePoidAndNotDeleted(1L)).thenReturn(headers);
        when(dtlRepository.findByHdrPoidAndNotDeleted(1L)).thenReturn(detailEntities);

        // When
        Map<String, Object> result = employeeInductionService.getInductionsByEmployee(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("inductions"));
        assertTrue(result.containsKey("totalCount"));
        assertEquals(1, result.get("totalCount"));
        assertEquals(1, ((List<?>) result.get("inductions")).size());
    }

    @Test
    void deleteInduction_Success() {
        // Given
        when(hdrRepository.findByPoidAndNotDeleted(1L)).thenReturn(Optional.of(headerEntity));
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();

        // When
        employeeInductionService.deleteInduction(1L, deleteReasonDto);

        // Then
        verify(documentDeleteService).deleteDocument(eq(1L), eq("HR_EMPLOYEE_INDUCTION_HDR"), 
                eq("TRANSACTION_POID"), eq(deleteReasonDto), isNull());
    }

    @Test
    void sendOverdueNotifications_Success() {
        HrEmployeeInductionDtl overdueDetail = HrEmployeeInductionDtl.builder()
                .transactionPoid(1L)
                .detRowId(1L)
                .header(headerEntity)
                .sheduledDate(LocalDate.now().minusDays(1))
                .inductionCatgPoid(1L)
                .build();
        List<HrEmployeeInductionDtl> overdueInductions = List.of(overdueDetail);
        when(dtlRepository.findOverdueInductions(any(LocalDate.class))).thenReturn(overdueInductions);

        employeeInductionService.sendOverdueNotifications();

        verify(dtlRepository).findOverdueInductions(any(LocalDate.class));
    }

    @Test
    void list_Success() {
        // Given
        FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", List.of());
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult rawResult = new RawSearchResult(
                List.of(Map.of("DOC_ID", "IND-001")),
                Map.of("DOC_ID", "Document ID"),
                1L
        );
        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveDateFilters(any(), any(), any(), any())).thenReturn(List.of());
        when(documentService.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(rawResult);

        // When
        Map<String, Object> result = employeeInductionService.list(filterRequest, null, null, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("content"));
        verify(documentService).search(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void validateRequest_CompletedDateBeforeScheduled_ThrowsException() {
        // Given
        EmployeeInductionRequestDto.EmployeeInductionDetailRequestDto detail = 
                EmployeeInductionRequestDto.EmployeeInductionDetailRequestDto.builder()
                        .sn(1)
                        .scheduledDate(LocalDate.now().plusDays(1))
                        .completedDate(LocalDate.now())
                        .build();
        
        requestDto.setDetails(List.of(detail));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            employeeInductionService.createInduction(requestDto));
    }

    private List<EmployeeInductionRequestDto.EmployeeInductionDetailRequestDto> createTestDetails() {
        return List.of(
                EmployeeInductionRequestDto.EmployeeInductionDetailRequestDto.builder()
                        .sn(1)
                        .inductionCategory("1")
                        .assigneePoid(2L)
                        .scheduledDate(LocalDate.now().plusDays(1))
                        .completedDate(null)
                        .status("N")
                        .remarks("Test detail")
                        .build()
        );
    }
}
