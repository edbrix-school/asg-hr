package com.asg.hr.designation.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.designation.dto.DesignationRequest;
import com.asg.hr.designation.dto.DesignationResponse;
import com.asg.hr.designation.entity.HrDesignationMaster;
import com.asg.hr.designation.repository.DesignationRepository;
import com.asg.hr.exceptions.ValidationException;

@ExtendWith(MockitoExtension.class)
class DesignationServiceImplTest {

    @Mock
    private DesignationRepository repository;

    @Mock
    private LoggingService loggingService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private DocumentSearchService documentSearchService;

    @InjectMocks
    private DesignationServiceImpl service;

    private MockedStatic<UserContext> mockedUserContext;

    @BeforeEach
    void setUp() {
        mockedUserContext = mockStatic(UserContext.class);
        mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
        mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");
    }

    @AfterEach
    void tearDown() {
        if (mockedUserContext != null) {
            mockedUserContext.close();
        }
    }

    private HrDesignationMaster createMockEntity() {
        HrDesignationMaster entity = new HrDesignationMaster();
        entity.setDesignationPoid(1L);
        entity.setGroupPoid(1L);
        entity.setDesignationCode("DEV001");
        entity.setDesignationName("Developer");
        entity.setJobDescription("<p>Writes code</p>");
        entity.setSkillDescription("Java, Spring");
        entity.setReportingToPoid("MGR001");
        entity.setSeqNo(1L);
        entity.setActive("Y");
        entity.setDeleted("N");
        entity.setCreatedBy("admin");
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy("admin");
        entity.setLastModifiedDate(LocalDateTime.now());
        return entity;
    }

    private DesignationRequest createMockRequest() {
        return DesignationRequest.builder()
                .designationCode("DEV001")
                .designationName("Developer")
                .jobDescription("<p>Writes code</p>")
                .skillDescription("Java, Spring")
                .reportingToPoid("MGR001")
                .seqNo(1L)
                .active("Y")
                .build();
    }

    @Test
    void getDesignationById_Success() {
        HrDesignationMaster entity = createMockEntity();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        DesignationResponse result = service.getDesignationById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getDesignationPoid());
        assertEquals("DEV001", result.getDesignationCode());
        assertEquals("Developer", result.getDesignationName());
        verify(repository).findById(1L);
    }

    @Test
    void getDesignationById_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getDesignationById(1L));
        verify(repository).findById(1L);
    }

    @Test
    void createDesignation_Success() {
        DesignationRequest request = createMockRequest();
        when(repository.existsByDesignationCodeIgnoreCase("DEV001")).thenReturn(false);
        when(repository.existsByDesignationNameIgnoreCase("Developer")).thenReturn(false);
        when(repository.save(any(HrDesignationMaster.class))).thenAnswer(invocation -> {
            HrDesignationMaster saved = invocation.getArgument(0);
            saved.setDesignationPoid(1L);
            return saved;
        });

        Long result = service.createDesignation(request);

        assertEquals(1L, result);
        verify(repository).save(any(HrDesignationMaster.class));
        verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "DOC123", "1");
    }

    @Test
    void createDesignation_DuplicateCode_ThrowsDuplicateKeyException() {
        DesignationRequest request = createMockRequest();
        when(repository.existsByDesignationCodeIgnoreCaseAndDesignationPoidNot("DEV001", null)).thenReturn(true);

        DuplicateKeyException exception = assertThrows(DuplicateKeyException.class,
                () -> service.createDesignation(request));

        assertEquals("Designation Code already exists: DEV001", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void createDesignation_DuplicateName_ThrowsDuplicateKeyException() {
        DesignationRequest request = createMockRequest();
        when(repository.existsByDesignationCodeIgnoreCaseAndDesignationPoidNot("DEV001", null)).thenReturn(false);
        when(repository.existsByDesignationNameIgnoreCaseAndDesignationPoidNot("Developer", null)).thenReturn(true);

        DuplicateKeyException exception = assertThrows(DuplicateKeyException.class,
                () -> service.createDesignation(request));

        assertEquals("Designation Name already exists: Developer", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void createDesignation_NegativeSeqNo_ThrowsValidationException() {
        DesignationRequest request = createMockRequest();
        request.setSeqNo(-1L);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.createDesignation(request));

        assertEquals("Sequence number must be numeric and non-negative", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void updateDesignation_Success() {
        HrDesignationMaster entity = createMockEntity();
        DesignationRequest request = createMockRequest();
        request.setDesignationCode("DEV002");
        request.setDesignationName("Senior Developer");

        when(repository.findByDesignationPoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));
        when(repository.existsByDesignationCodeIgnoreCaseAndDesignationPoidNot("DEV002", 1L)).thenReturn(false);
        when(repository.existsByDesignationNameIgnoreCaseAndDesignationPoidNot("Senior Developer", 1L)).thenReturn(false);
        when(repository.save(any(HrDesignationMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DesignationResponse result = service.updateDesignation(1L, request);

        assertNotNull(result);
        assertEquals("DEV002", result.getDesignationCode());
        assertEquals("Senior Developer", result.getDesignationName());
        verify(loggingService).logChanges(any(HrDesignationMaster.class), any(HrDesignationMaster.class),
                eq(HrDesignationMaster.class), eq("DOC123"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("DESIG_POID"));
    }

    @Test
    void updateDesignation_NotFound() {
        DesignationRequest request = createMockRequest();
        when(repository.findByDesignationPoidAndDeleted(1L, "N")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateDesignation(1L, request));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteDesignation_Success() {
        HrDesignationMaster entity = createMockEntity();
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("No longer required");

        when(repository.findByDesignationPoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));

        assertDoesNotThrow(() -> service.deleteDesignation(1L, deleteReasonDto));
        verify(documentDeleteService).deleteDocument(eq(1L), eq("HR_DESIGNATION_MASTER"),
                eq("DESIG_POID"), eq(deleteReasonDto), isNull());
    }

    @Test
    void deleteDesignation_NotFound() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("No longer required");
        when(repository.findByDesignationPoidAndDeleted(1L, "N")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteDesignation(1L, deleteReasonDto));
        verify(documentDeleteService, never()).deleteDocument(any(), any(), any(), any(), any());
    }

    @Test
    void listDesignations_Success() {
        FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", new ArrayList<>());
        Pageable pageable = PageRequest.of(0, 10);

        List<Map<String, Object>> records = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("DESIG_POID", 1L);
        row.put("DESIGNATION_NAME", "Developer");
        records.add(row);

        Map<String, String> displayFields = new HashMap<>();
        displayFields.put("DESIG_POID", "Designation ID");
        displayFields.put("DESIGNATION_NAME", "Designation Name");

        RawSearchResult raw = new RawSearchResult(records, displayFields, 1L);

        when(documentSearchService.resolveOperator(filterRequest)).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentSearchService.resolveFilters(filterRequest)).thenReturn(new ArrayList<>());
        when(documentSearchService.search(anyString(), anyList(), anyString(), any(Pageable.class),
                anyString(), anyString(), anyString())).thenReturn(raw);

        Map<String, Object> result = service.listDesignations(filterRequest, pageable);

        assertNotNull(result);
        assertTrue(result.containsKey("content"));
        verify(documentSearchService).search(eq("DOC123"), anyList(), eq("AND"), eq(pageable),
                eq("N"), eq("DESIGNATION_NAME"), eq("DESIG_POID"));
    }
}
