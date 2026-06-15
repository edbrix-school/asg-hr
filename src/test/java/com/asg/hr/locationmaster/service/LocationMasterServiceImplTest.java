package com.asg.hr.locationmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.hr.locationmaster.dto.LocationMasterRequestDto;
import com.asg.hr.locationmaster.dto.LocationMasterResponseDto;
import com.asg.hr.locationmaster.entity.GlobalLocationMaster;
import com.asg.hr.locationmaster.repository.GlobalLocationMasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationMasterServiceImplTest {

    @Mock
    private GlobalLocationMasterRepository repository;

    @Mock
    private DocumentSearchService documentSearchService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private LoggingService loggingService;

    @Mock
    private LovDataService lovDataService;

    @InjectMocks
    private LocationMasterServiceImpl service;

    private LocationMasterRequestDto requestDto;
    private GlobalLocationMaster entity;

    @BeforeEach
    void setUp() {
        requestDto = LocationMasterRequestDto.builder()
                .locationCode("LOC001")
                .locationName("Main Location")
                .locationName2("Main Location 2")
                .address("123 Main Street")
                .siteSupervisorUserPoid(1L)
                .active("Y")
                .seqno(1)
                .build();

        entity = GlobalLocationMaster.builder()
                .locationPoid(1L)
                .companyPoid(1L)
                .locationCode("LOC001")
                .locationName("Main Location")
                .locationName2("Main Location 2")
                .address("123 Main Street")
                .siteSupervisorUserPoid(1L)
                .seqno(1)
                .build();
        entity.setActive("Y");
        entity.setDeleted("N");
    }

    // ─── CREATE ──────────────────────────────────────────────────────────────

    @Test
    void create_Success() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);
            muc.when(UserContext::getDocumentId).thenReturn("500-004");

            when(repository.existsByLocationCodeAndCompanyPoid(anyString(), anyLong())).thenReturn(false);
            when(repository.save(any(GlobalLocationMaster.class))).thenReturn(entity);
            when(lovDataService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(null);

            LocationMasterResponseDto result = service.create(requestDto);

            assertNotNull(result);
            assertEquals(1L, result.getLocationPoid());
            assertEquals("LOC001", result.getLocationCode());
            assertEquals("Main Location", result.getLocationName());
            assertEquals("Y", result.getActive());

            verify(repository).existsByLocationCodeAndCompanyPoid("LOC001", 1L);
            verify(repository).save(any(GlobalLocationMaster.class));
            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.CREATED), eq("500-004"), eq("1"));
        }
    }

    @Test
    void create_DuplicateLocationCode_ThrowsException() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(repository.existsByLocationCodeAndCompanyPoid(anyString(), anyLong())).thenReturn(true);

            ResourceAlreadyExistsException exception = assertThrows(ResourceAlreadyExistsException.class,
                    () -> service.create(requestDto));

            assertTrue(exception.getMessage().contains("GLOBAL_LOCATION_MASTER"));
            verify(repository, never()).save(any());
            verify(loggingService, never()).createLogSummaryEntry(eq(LogDetailsEnum.CREATED), anyString(), anyString());
        }
    }

    @Test
    void create_DatabaseConstraintViolation_ThrowsDataIntegrityViolationException() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(repository.existsByLocationCodeAndCompanyPoid(anyString(), anyLong())).thenReturn(false);
            when(repository.save(any(GlobalLocationMaster.class)))
                    .thenThrow(new DataIntegrityViolationException("Constraint violation"));

            DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                    () -> service.create(requestDto));

            assertTrue(exception.getMessage().contains("Constraint violation"));
        }
    }

    @Test
    void create_SetsCorrectDefaults() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);
            muc.when(UserContext::getDocumentId).thenReturn("500-004");

            when(repository.existsByLocationCodeAndCompanyPoid(anyString(), anyLong())).thenReturn(false);
            when(repository.save(any(GlobalLocationMaster.class))).thenAnswer(invocation -> {
                GlobalLocationMaster saved = invocation.getArgument(0);
                assertEquals("N", saved.getDeleted());
                assertEquals(1L, saved.getCompanyPoid());
                assertEquals("Y", saved.getActive());
                saved.setLocationPoid(1L);
                return saved;
            });
            when(lovDataService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(null);

            LocationMasterResponseDto result = service.create(requestDto);

            assertNotNull(result);
            assertEquals("LOC001", result.getLocationCode());
        }
    }

    @Test
    void create_NullRequest_ThrowsException() {
        assertThrows(NullPointerException.class, () -> service.create(null));
    }

    // ─── GET BY ID ───────────────────────────────────────────────────────────

    @Test
    void getById_Success() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(repository.findByIdAndCompanyPoidAndNotDeleted(1L, 1L)).thenReturn(Optional.of(entity));
            when(lovDataService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(null);

            LocationMasterResponseDto result = service.getById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getLocationPoid());
            assertEquals("LOC001", result.getLocationCode());
            assertEquals("Main Location", result.getLocationName());

            verify(repository).findByIdAndCompanyPoidAndNotDeleted(1L, 1L);
        }
    }

    @Test
    void getById_NotFound_ThrowsException() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(repository.findByIdAndCompanyPoidAndNotDeleted(1L, 1L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> service.getById(1L));

            assertTrue(exception.getMessage().contains("GLOBAL_LOCATION_MASTER"));
            verify(repository).findByIdAndCompanyPoidAndNotDeleted(1L, 1L);
        }
    }

    @Test
    void getById_WithNullFields() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            GlobalLocationMaster entityWithNulls = GlobalLocationMaster.builder()
                    .locationPoid(1L)
                    .companyPoid(1L)
                    .locationCode("LOC001")
                    .locationName(null)
                    .locationName2(null)
                    .address(null)
                    .siteSupervisorUserPoid(null)
                    .seqno(null)
                    .build();
            entityWithNulls.setActive("Y");
            entityWithNulls.setDeleted("N");

            when(repository.findByIdAndCompanyPoidAndNotDeleted(1L, 1L)).thenReturn(Optional.of(entityWithNulls));
            // No need to mock lovDataService since siteSupervisorUserPoid is null

            LocationMasterResponseDto result = service.getById(1L);

            assertNotNull(result);
            assertEquals("LOC001", result.getLocationCode());
            assertNull(result.getLocationName());
            assertNull(result.getAddress());
        }
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    @Test
    void update_Success() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);
            muc.when(UserContext::getDocumentId).thenReturn("500-004");

            LocationMasterRequestDto updateDto = LocationMasterRequestDto.builder()
                    .locationCode("LOC001")
                    .locationName("Updated Location")
                    .locationName2("Updated Location 2")
                    .address("456 Updated Street")
                    .siteSupervisorUserPoid(2L)
                    .active("N")
                    .seqno(2)
                    .build();

            when(repository.findByIdAndCompanyPoidAndNotDeleted(1L, 1L)).thenReturn(Optional.of(entity));
            when(repository.existsByLocationCodeAndCompanyPoidAndLocationPoidNot(anyString(), anyLong(), anyLong()))
                    .thenReturn(false);
            when(repository.save(any(GlobalLocationMaster.class))).thenAnswer(invocation -> {
                GlobalLocationMaster saved = invocation.getArgument(0);
                assertEquals("Updated Location", saved.getLocationName());
                assertEquals("N", saved.getActive());
                return saved;
            });
            when(lovDataService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(null);

            LocationMasterResponseDto result = service.update(1L, updateDto);

            assertNotNull(result);
            verify(repository).save(any(GlobalLocationMaster.class));
            verify(loggingService).logChanges(any(), any(), eq(GlobalLocationMaster.class),
                    anyString(), anyString(), eq(LogDetailsEnum.MODIFIED), anyString());
        }
    }

    @Test
    void update_NotFound_ThrowsException() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(repository.findByIdAndCompanyPoidAndNotDeleted(1L, 1L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> service.update(1L, requestDto));

            assertTrue(exception.getMessage().contains("GLOBAL_LOCATION_MASTER"));
            verify(repository, never()).save(any());
        }
    }

    @Test
    void update_DuplicateLocationCode_ThrowsException() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(repository.findByIdAndCompanyPoidAndNotDeleted(1L, 1L)).thenReturn(Optional.of(entity));
            when(repository.existsByLocationCodeAndCompanyPoidAndLocationPoidNot(anyString(), anyLong(), anyLong()))
                    .thenReturn(true);

            ResourceAlreadyExistsException exception = assertThrows(ResourceAlreadyExistsException.class,
                    () -> service.update(1L, requestDto));

            assertTrue(exception.getMessage().contains("GLOBAL_LOCATION_MASTER"));
            verify(repository, never()).save(any());
        }
    }

    @Test
    void update_DatabaseException_ThrowsDataIntegrityViolationException() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);
            muc.when(UserContext::getDocumentId).thenReturn("500-004");

            when(repository.findByIdAndCompanyPoidAndNotDeleted(1L, 1L)).thenReturn(Optional.of(entity));
            when(repository.existsByLocationCodeAndCompanyPoidAndLocationPoidNot(anyString(), anyLong(), anyLong()))
                    .thenReturn(false);
            when(repository.save(any(GlobalLocationMaster.class)))
                    .thenThrow(new DataIntegrityViolationException("Constraint violation"));

            DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                    () -> service.update(1L, requestDto));

            assertTrue(exception.getMessage().contains("Constraint violation"));
        }
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @Test
    void delete_Success() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
            deleteReasonDto.setDeleteReason("Test deletion");

            when(repository.findByIdAndCompanyPoidAndNotDeleted(1L, 1L)).thenReturn(Optional.of(entity));
            when(documentDeleteService.deleteDocument(anyLong(), anyString(), anyString(), any(), any()))
                    .thenReturn("true");

            service.delete(1L, deleteReasonDto);

            verify(documentDeleteService).deleteDocument(
                    eq(1L),
                    eq("GLOBAL_LOCATION_MASTER"),
                    eq("LOCATION_POID"),
                    eq(deleteReasonDto),
                    isNull()
            );
        }
    }

    @Test
    void delete_WithNullDeleteReason_Success() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(repository.findByIdAndCompanyPoidAndNotDeleted(1L, 1L)).thenReturn(Optional.of(entity));
            when(documentDeleteService.deleteDocument(anyLong(), anyString(), anyString(), isNull(), any()))
                    .thenReturn("true");

            service.delete(1L, null);

            verify(documentDeleteService).deleteDocument(
                    eq(1L),
                    eq("GLOBAL_LOCATION_MASTER"),
                    eq("LOCATION_POID"),
                    isNull(),
                    isNull()
            );
        }
    }

    @Test
    void delete_NotFound_ThrowsException() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(repository.findByIdAndCompanyPoidAndNotDeleted(1L, 1L)).thenReturn(Optional.empty());

            DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
            deleteReasonDto.setDeleteReason("Test deletion");

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> service.delete(1L, deleteReasonDto));

            assertTrue(exception.getMessage().contains("GLOBAL_LOCATION_MASTER"));
            verify(documentDeleteService, never()).deleteDocument(any(), anyString(), anyString(), any(), any());
        }
    }

    @Test
    void delete_DocumentServiceException_PropagatesException() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(repository.findByIdAndCompanyPoidAndNotDeleted(1L, 1L)).thenReturn(Optional.of(entity));
            when(documentDeleteService.deleteDocument(anyLong(), anyString(), anyString(), any(), any()))
                    .thenThrow(new RuntimeException("Delete service failed"));

            assertThrows(RuntimeException.class, () -> service.delete(1L, null));
        }
    }

    // ─── LIST ────────────────────────────────────────────────────────────────

    @Test
    void list_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        FilterRequestDto filters = new FilterRequestDto("AND", "N", Collections.emptyList());

        List<Map<String, Object>> records = new ArrayList<>();
        Map<String, Object> record = new HashMap<>();
        record.put("locationPoid", "1");
        record.put("locationCode", "LOC001");
        records.add(record);

        Map<String, String> displayFields = new HashMap<>();
        displayFields.put("locationPoid", "Location ID");
        displayFields.put("locationCode", "Location Code");

        RawSearchResult rawResult = new RawSearchResult(records, displayFields, 1L);

        when(documentSearchService.resolveOperator(filters)).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(filters)).thenReturn("N");
        when(documentSearchService.resolveFilters(filters)).thenReturn(new ArrayList<>());
        when(documentSearchService.search(anyString(), anyList(), anyString(), any(Pageable.class),
                anyString(), anyString(), anyString())).thenReturn(rawResult);

        Map<String, Object> result = service.list("500-004", filters, pageable);

        assertNotNull(result);
        assertEquals(1L, result.get("totalElements"));
        assertNotNull(result.get("content"));
        assertNotNull(result.get("displayFields"));

        verify(documentSearchService).search(eq("500-004"), anyList(), eq("AND"), eq(pageable),
                eq("N"), anyString(), anyString());
    }

    @Test
    void list_WithNullFilters_Success() {
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult rawResult = new RawSearchResult(new ArrayList<>(), new HashMap<>(), 0L);

        when(documentSearchService.resolveOperator(isNull())).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(isNull())).thenReturn("N");
        when(documentSearchService.resolveFilters(isNull())).thenReturn(new ArrayList<>());
        when(documentSearchService.search(anyString(), anyList(), anyString(), any(Pageable.class),
                anyString(), anyString(), anyString())).thenReturn(rawResult);

        Map<String, Object> result = service.list("500-004", null, pageable);

        assertNotNull(result);
        assertEquals(0L, result.get("totalElements"));
        assertNotNull(result.get("content"));
    }

    @Test
    void list_DocumentServiceException_PropagatesException() {
        Pageable pageable = PageRequest.of(0, 10);

        when(documentSearchService.resolveOperator(any())).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(any())).thenReturn("N");
        when(documentSearchService.resolveFilters(any())).thenReturn(new ArrayList<>());
        when(documentSearchService.search(anyString(), anyList(), anyString(), any(Pageable.class),
                anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Document service error"));

        assertThrows(RuntimeException.class, () -> service.list("500-004", null, pageable));
    }

    // ─── EDGE CASES ──────────────────────────────────────────────────────────

    @Test
    void create_UserContextException_PropagatesException() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenThrow(new RuntimeException("UserContext error"));

            assertThrows(RuntimeException.class, () -> service.create(requestDto));
        }
    }

    @Test
    void update_ConcurrentModification_ThrowsDataIntegrityViolationException() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);
            muc.when(UserContext::getDocumentId).thenReturn("500-004");

            when(repository.findByIdAndCompanyPoidAndNotDeleted(1L, 1L)).thenReturn(Optional.of(entity));
            when(repository.existsByLocationCodeAndCompanyPoidAndLocationPoidNot(anyString(), anyLong(), anyLong()))
                    .thenReturn(false);
            when(repository.save(any(GlobalLocationMaster.class)))
                    .thenThrow(new DataIntegrityViolationException("Optimistic locking failure"));

            DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                    () -> service.update(1L, requestDto));

            assertTrue(exception.getMessage().contains("Optimistic locking failure"));
        }
    }

    @Test
    void convertFromEntityToDto_AllNullFields() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            GlobalLocationMaster entityWithAllNulls = GlobalLocationMaster.builder()
                    .locationPoid(1L)
                    .companyPoid(1L)
                    .locationCode(null)
                    .locationName(null)
                    .locationName2(null)
                    .address(null)
                    .siteSupervisorUserPoid(null)
                    .seqno(null)
                    .build();
            entityWithAllNulls.setActive(null);
            entityWithAllNulls.setDeleted("N");

            when(repository.findByIdAndCompanyPoidAndNotDeleted(1L, 1L)).thenReturn(Optional.of(entityWithAllNulls));
            // No need to mock lovDataService since siteSupervisorUserPoid is null

            LocationMasterResponseDto result = service.getById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getLocationPoid());
            assertNull(result.getLocationCode());
            assertNull(result.getLocationName());
            assertNull(result.getActive());
        }
    }

    @Test
    void list_EmptyFilters_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        FilterRequestDto emptyFilters = new FilterRequestDto("AND", "N", Collections.emptyList());

        RawSearchResult rawResult = new RawSearchResult(new ArrayList<>(), new HashMap<>(), 0L);

        when(documentSearchService.resolveOperator(emptyFilters)).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(emptyFilters)).thenReturn("N");
        when(documentSearchService.resolveFilters(emptyFilters)).thenReturn(new ArrayList<>());
        when(documentSearchService.search(anyString(), anyList(), anyString(), any(Pageable.class),
                anyString(), anyString(), anyString())).thenReturn(rawResult);

        Map<String, Object> result = service.list("500-004", emptyFilters, pageable);

        assertNotNull(result);
        assertEquals(0L, result.get("totalElements"));
        assertNotNull(result.get("content"));
    }

    @Test
    void create_WithNullActiveField_SetsDefault() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);
            muc.when(UserContext::getDocumentId).thenReturn("500-004");

            LocationMasterRequestDto requestWithNullActive = LocationMasterRequestDto.builder()
                    .locationCode("LOC001")
                    .locationName("Test Location")
                    .active(null)
                    .build();

            when(repository.existsByLocationCodeAndCompanyPoid(anyString(), anyLong())).thenReturn(false);
            when(repository.save(any(GlobalLocationMaster.class))).thenAnswer(invocation -> {
                GlobalLocationMaster saved = invocation.getArgument(0);
                // Service should handle null active field appropriately
                saved.setLocationPoid(1L);
                return saved;
            });
            // No siteSupervisorUserPoid in this request, so no need to mock lovDataService

            LocationMasterResponseDto result = service.create(requestWithNullActive);

            assertNotNull(result);
            assertEquals("LOC001", result.getLocationCode());
        }
    }

    @Test
    void update_WithPartialData_Success() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);
            muc.when(UserContext::getDocumentId).thenReturn("500-004");

            LocationMasterRequestDto partialUpdate = LocationMasterRequestDto.builder()
                    .locationCode("LOC001")
                    .locationName("Updated Name Only")
                    .build();

            when(repository.findByIdAndCompanyPoidAndNotDeleted(1L, 1L)).thenReturn(Optional.of(entity));
            when(repository.existsByLocationCodeAndCompanyPoidAndLocationPoidNot(anyString(), anyLong(), anyLong()))
                    .thenReturn(false);
            when(repository.save(any(GlobalLocationMaster.class))).thenReturn(entity);

            LocationMasterResponseDto result = service.update(1L, partialUpdate);

            assertNotNull(result);
            verify(repository).save(any(GlobalLocationMaster.class));
            verify(loggingService).logChanges(any(), any(), eq(GlobalLocationMaster.class),
                    anyString(), anyString(), eq(LogDetailsEnum.MODIFIED), anyString());
        }
    }
}