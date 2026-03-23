package com.asg.hr.religion.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.exceptions.ValidationException;
import com.asg.hr.religion.dto.ReligionDtoRequest;
import com.asg.hr.religion.dto.ReligionDtoResponse;
import com.asg.hr.religion.entity.HrReligionMaster;
import com.asg.hr.religion.repository.ReligionRepository;
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

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReligionServiceImplTest {

    @Mock
    private ReligionRepository repository;

    @Mock
    private LoggingService loggingService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private DocumentSearchService documentSearchService;

    @InjectMocks
    private ReligionServiceImpl service;

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

    private HrReligionMaster createMockEntity() {
        HrReligionMaster entity = new HrReligionMaster();
        entity.setReligionPoid(1L);
        entity.setGroupPoid(1L);
        entity.setReligionCode("HINDU");
        entity.setReligionDescription("Hindu Religion");
        entity.setSeqNo(1L);
        entity.setActive("Y");
        entity.setDeleted("N");
        entity.setCreatedBy("admin");
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy("admin");
        entity.setLastModifiedDate(LocalDateTime.now());
        return entity;
    }

    private ReligionDtoRequest createMockRequest() {
        ReligionDtoRequest dto = new ReligionDtoRequest();
        dto.setReligionCode("HINDU");
        dto.setDescription("Hindu Religion");
        dto.setSeqNo(1L);
        dto.setActive("Y");
        return dto;
    }

    // ================= GET BY ID TESTS =================

    @Test
    void getReligionById_Success() {
        HrReligionMaster entity = createMockEntity();

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        ReligionDtoResponse result = service.getReligionById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getReligionPoid());
        assertEquals("HINDU", result.getReligionCode());
        assertEquals("Hindu Religion", result.getDescription());
        assertEquals("Y", result.getActive());
        assertEquals("admin", result.getCreatedBy());
        assertNotNull(result.getCreatedDate());
        assertEquals("admin", result.getLastModifiedBy());
        assertNotNull(result.getLastModifiedDate());

        verify(repository).findById(1L);
    }

    @Test
    void getReligionById_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.getReligionById(1L));

        assertTrue(exception.getMessage().contains("Religion"));
        verify(repository, never()).save(any());
    }

    // ================= CREATE TESTS =================

    @Test
    void createReligion_Success() {
        ReligionDtoRequest request = createMockRequest();

        when(repository.findByReligionCode("HINDU")).thenReturn(Optional.empty());
        when(repository.findByReligionDescription("Hindu Religion")).thenReturn(Optional.empty());
        when(repository.save(any(HrReligionMaster.class))).thenAnswer(invocation -> {
            HrReligionMaster savedEntity = invocation.getArgument(0);
            savedEntity.setReligionPoid(1L);
            return savedEntity;
        });
        doNothing().when(loggingService).createLogSummaryEntry(
                any(LogDetailsEnum.class), anyString(), anyString());

        Long result = service.createReligion(request);

        assertNotNull(result);
        assertEquals(1L, result);

        verify(repository).findByReligionCode("HINDU");
        verify(repository).findByReligionDescription("Hindu Religion");
        verify(repository).save(any(HrReligionMaster.class));
        verify(loggingService).createLogSummaryEntry(
                LogDetailsEnum.CREATED, "DOC123", "1");
    }

    @Test
    void createReligion_NullReligionCode_ThrowsValidationException() {
        ReligionDtoRequest request = createMockRequest();
        request.setReligionCode(null);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.createReligion(request));

        assertEquals("Religion Code is required", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void createReligion_EmptyReligionCode_ThrowsValidationException() {
        ReligionDtoRequest request = createMockRequest();
        request.setReligionCode("   ");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.createReligion(request));

        assertEquals("Religion Code is required", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void createReligion_NullDescription_ThrowsValidationException() {
        ReligionDtoRequest request = createMockRequest();
        request.setDescription(null);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.createReligion(request));

        assertEquals("Religion Description is required", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void createReligion_EmptyDescription_ThrowsValidationException() {
        ReligionDtoRequest request = createMockRequest();
        request.setDescription("");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.createReligion(request));

        assertEquals("Religion Description is required", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void createReligion_DuplicateReligionCode_ThrowsResourceAlreadyExistsException() {
        ReligionDtoRequest request = createMockRequest();
        HrReligionMaster existingEntity = createMockEntity();

        when(repository.findByReligionCode("HINDU")).thenReturn(Optional.of(existingEntity));

        DuplicateKeyException exception = assertThrows(DuplicateKeyException.class,
                () -> service.createReligion(request));

        assertEquals("Religion Code already exists: HINDU", exception.getMessage());
        verify(repository).findByReligionCode("HINDU");
        verify(repository, never()).save(any());
    }

    @Test
    void createReligion_DuplicateDescription_ThrowsResourceAlreadyExistsException() {
        ReligionDtoRequest request = createMockRequest();
        HrReligionMaster existingEntity = createMockEntity();

        when(repository.findByReligionCode("HINDU")).thenReturn(Optional.empty());
        when(repository.findByReligionDescription("Hindu Religion")).thenReturn(Optional.of(existingEntity));

        DuplicateKeyException exception = assertThrows(DuplicateKeyException.class,
                () -> service.createReligion(request));

        assertEquals("Religion Description already exists: Hindu Religion", exception.getMessage());
        verify(repository).findByReligionCode("HINDU");
        verify(repository).findByReligionDescription("Hindu Religion");
        verify(repository, never()).save(any());
    }

    // ================= UPDATE TESTS =================

    @Test
    void updateReligion_Success() {
        ReligionDtoRequest request = createMockRequest();
        request.setReligionCode("MUSLIM");
        request.setDescription("Muslim Religion");

        HrReligionMaster entity = createMockEntity();

        when(repository.findByReligionPoidDeleted(1L)).thenReturn(Optional.of(entity));
        when(repository.findByReligionCode("MUSLIM")).thenReturn(Optional.empty());
        when(repository.findByReligionDescription("Muslim Religion")).thenReturn(Optional.empty());
        when(repository.save(any(HrReligionMaster.class))).thenReturn(entity);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        doNothing().when(loggingService).logChanges(
                any(), any(), any(), anyString(), anyString(), any(LogDetailsEnum.class), anyString());

        ReligionDtoResponse result = service.updateReligion(request, 1L);

        assertNotNull(result);

        verify(repository).findByReligionPoidDeleted(1L);
        verify(repository).findByReligionCode("MUSLIM");
        verify(repository).findByReligionDescription("Muslim Religion");
        verify(repository).save(any(HrReligionMaster.class));
        verify(loggingService).logChanges(
                any(HrReligionMaster.class), any(HrReligionMaster.class),
                eq(HrReligionMaster.class), eq("DOC123"), eq("1"),
                eq(LogDetailsEnum.MODIFIED), eq("RELIGION_POID"));
    }

    @Test
    void updateReligion_NotFound_ThrowsResourceNotFoundException() {
        ReligionDtoRequest request = createMockRequest();

        when(repository.findByReligionPoidDeleted(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.updateReligion(request, 1L));

        assertTrue(exception.getMessage().contains("Religion"));
        verify(repository).findByReligionPoidDeleted(1L);
        verify(repository, never()).save(any());
    }

    @Test
    void updateReligion_SameReligionCode_Success() {
        ReligionDtoRequest request = createMockRequest();
        HrReligionMaster entity = createMockEntity();

        when(repository.findByReligionPoidDeleted(1L)).thenReturn(Optional.of(entity));
        when(repository.findByReligionCode("HINDU")).thenReturn(Optional.of(entity));
        when(repository.findByReligionDescription("Hindu Religion")).thenReturn(Optional.of(entity));
        when(repository.save(any(HrReligionMaster.class))).thenReturn(entity);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        doNothing().when(loggingService).logChanges(
                any(), any(), any(), anyString(), anyString(), any(LogDetailsEnum.class), anyString());

        ReligionDtoResponse result = service.updateReligion(request, 1L);

        assertNotNull(result);

        verify(repository).save(any(HrReligionMaster.class));
    }

    @Test
    void updateReligion_DuplicateReligionCode_ThrowsResourceAlreadyExistsException() {
        ReligionDtoRequest request = createMockRequest();
        HrReligionMaster entity = createMockEntity();
        HrReligionMaster duplicateEntity = createMockEntity();
        duplicateEntity.setReligionPoid(2L);

        when(repository.findByReligionPoidDeleted(1L)).thenReturn(Optional.of(entity));
        when(repository.findByReligionCode("HINDU")).thenReturn(Optional.of(duplicateEntity));

        DuplicateKeyException exception = assertThrows(DuplicateKeyException.class,
                () -> service.updateReligion(request, 1L));

        assertEquals("Religion Code already exists: HINDU", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void updateReligion_DuplicateDescription_ThrowsResourceAlreadyExistsException() {
        ReligionDtoRequest request = createMockRequest();
        HrReligionMaster entity = createMockEntity();
        HrReligionMaster duplicateEntity = createMockEntity();
        duplicateEntity.setReligionPoid(2L);

        when(repository.findByReligionPoidDeleted(1L)).thenReturn(Optional.of(entity));
        when(repository.findByReligionCode("HINDU")).thenReturn(Optional.empty());
        when(repository.findByReligionDescription("Hindu Religion")).thenReturn(Optional.of(duplicateEntity));

        DuplicateKeyException exception = assertThrows(DuplicateKeyException.class,
                () -> service.updateReligion(request, 1L));

        assertEquals("Religion Description already exists: Hindu Religion", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void updateReligion_NullReligionCode_ThrowsValidationException() {
        ReligionDtoRequest request = createMockRequest();
        request.setReligionCode(null);
        HrReligionMaster entity = createMockEntity();

        when(repository.findByReligionPoidDeleted(1L)).thenReturn(Optional.of(entity));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.updateReligion(request, 1L));

        assertEquals("Religion Code is required", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void updateReligion_NullDescription_ThrowsValidationException() {
        ReligionDtoRequest request = createMockRequest();
        request.setDescription(null);
        HrReligionMaster entity = createMockEntity();

        when(repository.findByReligionPoidDeleted(1L)).thenReturn(Optional.of(entity));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.updateReligion(request, 1L));

        assertEquals("Religion Description is required", exception.getMessage());
        verify(repository, never()).save(any());
    }

    // ================= DELETE TESTS =================

    @Test
    void deleteReligion_Success() {
        HrReligionMaster entity = createMockEntity();

        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("No longer needed");

        when(repository.findByReligionPoidDeleted(1L)).thenReturn(Optional.of(entity));

        assertDoesNotThrow(() -> service.deleteReligion(1L, deleteReasonDto));

        verify(repository).findByReligionPoidDeleted(1L);
        verify(documentDeleteService).deleteDocument(eq(1L), eq("HR_RELIGION_MASTER"),
                eq("RELIGION_POID"), eq(deleteReasonDto), isNull());
    }

    @Test
    void deleteReligion_NotFound_ThrowsResourceNotFoundException() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("No longer needed");

        when(repository.findByReligionPoidDeleted(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.deleteReligion(1L, deleteReasonDto));

        assertTrue(exception.getMessage().contains("Religion"));
        verify(repository).findByReligionPoidDeleted(1L);
        verify(documentDeleteService, never()).deleteDocument(
                anyLong(), anyString(), anyString(), any(), any());
    }

    // ================= LIST TESTS =================

    @Test
    void listReligion_Success() {
        FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", new ArrayList<>());
        Pageable pageable = PageRequest.of(0, 10);

        List<Map<String, Object>> records = new ArrayList<>();
        Map<String, Object> data = new HashMap<>();
        data.put("RELIGION_POID", 1L);
        data.put("RELIGION_CODE", "HINDU");
        data.put("RELIGION_DESCRIPTION", "Hindu Religion");
        records.add(data);

        Map<String, String> displayFields = new HashMap<>();
        displayFields.put("RELIGION_POID", "Religion ID");
        displayFields.put("RELIGION_CODE", "Religion Code");

        RawSearchResult rawResult = new RawSearchResult(records, displayFields, 1L);

        when(documentSearchService.resolveOperator(filterRequest)).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentSearchService.resolveFilters(filterRequest)).thenReturn(new ArrayList<>());
        when(documentSearchService.search(
                anyString(), anyList(), anyString(), any(Pageable.class),
                anyString(), anyString(), anyString())).thenReturn(rawResult);

        Map<String, Object> result = service.listReligion(filterRequest, pageable);

        assertNotNull(result);
        assertTrue(result.containsKey("content"));
        assertTrue(result.containsKey("totalElements"));

        verify(documentSearchService).resolveOperator(filterRequest);
        verify(documentSearchService).resolveIsDeleted(filterRequest);
        verify(documentSearchService).resolveFilters(filterRequest);
        verify(documentSearchService).search(
                eq("DOC123"), anyList(), eq("AND"), eq(pageable),
                eq("N"), eq("RELIGION_DESCRIPTION"), eq("RELIGION_POID"));
    }

    @Test
    void listReligion_WithNullFilterRequest_Success() {
        Pageable pageable = PageRequest.of(0, 10);

        List<Map<String, Object>> records = new ArrayList<>();
        Map<String, String> displayFields = new HashMap<>();
        RawSearchResult rawResult = new RawSearchResult(records, displayFields, 0L);

        when(documentSearchService.resolveOperator(null)).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(null)).thenReturn("N");
        when(documentSearchService.resolveFilters(null)).thenReturn(new ArrayList<>());
        when(documentSearchService.search(
                anyString(), anyList(), anyString(), any(Pageable.class),
                anyString(), anyString(), anyString())).thenReturn(rawResult);

        Map<String, Object> result = service.listReligion(null, pageable);

        assertNotNull(result);
        verify(documentSearchService).search(
                eq("DOC123"), anyList(), anyString(), eq(pageable),
                anyString(), eq("RELIGION_DESCRIPTION"), eq("RELIGION_POID"));
    }
}
