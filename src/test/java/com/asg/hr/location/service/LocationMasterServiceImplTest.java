package com.asg.hr.location.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.location.dto.LocationMasterRequestDto;
import com.asg.hr.location.dto.LocationMasterResponseDto;
import com.asg.hr.location.entity.LocationMasterEntity;
import com.asg.hr.location.repository.LocationMasterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationMasterServiceImplTest {

    @Mock
    private LocationMasterRepository repository;

    @Mock
    private DocumentSearchService documentSearchService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private LocationMasterServiceImpl service;

    @Test
    void create_success() {
        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            userContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            LocationMasterRequestDto request = LocationMasterRequestDto.builder()
                    .locationCode("LOC001")
                    .locationName("Main Office")
                    .active("Y")
                    .build();

            LocationMasterEntity entity = LocationMasterEntity.builder()
                    .locationPoid(1L)
                    .company(1L)
                    .locationCode("LOC001")
                    .locationName("Main Office")
                    .build();
            entity.setActive("Y");

            when(repository.existsByLocationCode("LOC001")).thenReturn(false);
            when(repository.save(any(LocationMasterEntity.class))).thenReturn(entity);

            LocationMasterResponseDto result = service.create(request);

            assertNotNull(result);
            assertEquals("LOC001", result.getLocationCode());
            assertEquals("Main Office", result.getLocationName());
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "DOC001", "1");
        }
    }

    @Test
    void create_locationCodeAlreadyExists_throwsException() {
        LocationMasterRequestDto request = LocationMasterRequestDto.builder()
                .locationCode("LOC001")
                .locationName("Main Office")
                .build();

        when(repository.existsByLocationCode("LOC001")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> service.create(request));
        verify(repository, never()).save(any());
    }

    @Test
    void create_locationCodeWithSpaces_throwsValidationException() {
        LocationMasterRequestDto request = LocationMasterRequestDto.builder()
                .locationCode("LOC 001")
                .locationName("Main Office")
                .build();

        ValidationException exception = assertThrows(ValidationException.class, () -> service.create(request));
        assertEquals("Location code cannot contain spaces", exception.getMessage());
        verify(repository, never()).existsByLocationCode(any());
        verify(repository, never()).save(any());
    }

    @Test
    void list_success() {
        FilterRequestDto request = new FilterRequestDto("AND", "N", List.of());
        Pageable pageable = PageRequest.of(0, 10);
        List<FilterDto> filters = List.of();
        Map<String, String> displayFields = Map.of("LOCATION_NAME", "Location Name");
        RawSearchResult rawResult = new RawSearchResult(List.of(Map.of("LOCATION_NAME", "Office")), displayFields, 1L);

        when(documentSearchService.resolveOperator(request)).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(request)).thenReturn("N");
        when(documentSearchService.resolveFilters(request)).thenReturn(filters);
        when(documentSearchService.search(eq("DOC001"), eq(filters), eq("AND"), eq(pageable), eq("N"), eq("LOCATION_NAME"), eq("LOCATION_POID")))
                .thenReturn(rawResult);

        Map<String, Object> result = service.list("DOC001", request, pageable);

        assertNotNull(result);
        verify(documentSearchService).search(eq("DOC001"), eq(filters), eq("AND"), eq(pageable), eq("N"), eq("LOCATION_NAME"), eq("LOCATION_POID"));
    }

    @Test
    void getById_success() {
        LocationMasterEntity entity = LocationMasterEntity.builder()
                .locationPoid(1L)
                .locationCode("LOC001")
                .locationName("Main Office")
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        LocationMasterResponseDto result = service.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getLocationPoid());
        assertEquals("LOC001", result.getLocationCode());
    }

    @Test
    void getById_notFound_throwsException() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(1L));
    }

    @Test
    void update_success() {
        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            LocationMasterEntity existingEntity = LocationMasterEntity.builder()
                    .locationPoid(1L)
                    .locationCode("LOC001")
                    .locationName("Old Office")
                    .build();
            existingEntity.setActive("Y");

            LocationMasterRequestDto request = LocationMasterRequestDto.builder()
                    .locationCode("LOC002")
                    .locationName("New Office")
                    .active("Y")
                    .build();

            LocationMasterEntity updatedEntity = LocationMasterEntity.builder()
                    .locationPoid(1L)
                    .locationCode("LOC002")
                    .locationName("New Office")
                    .build();
            updatedEntity.setActive("Y");

            when(repository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(existingEntity));
            when(repository.existsByLocationCodeAndIdNot("LOC002", 1L)).thenReturn(false);
            when(repository.save(any(LocationMasterEntity.class))).thenReturn(updatedEntity);

            LocationMasterResponseDto result = service.update(1L, request);

            assertNotNull(result);
            assertEquals("LOC002", result.getLocationCode());
            assertEquals("New Office", result.getLocationName());
            verify(loggingService).logChanges(any(), any(), eq(LocationMasterEntity.class), eq("DOC001"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("LOCATION_POID"));
        }
    }

    @Test
    void update_locationNotFound_throwsException() {
        LocationMasterRequestDto request = LocationMasterRequestDto.builder()
                .locationCode("LOC002")
                .locationName("New Office")
                .build();

        when(repository.findByIdAndNotDeleted(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(1L, request));
        verify(repository, never()).save(any());
    }

    @Test
    void update_locationCodeWithSpaces_throwsValidationException() {
        LocationMasterEntity existingEntity = LocationMasterEntity.builder()
                .locationPoid(1L)
                .locationCode("LOC001")
                .build();

        LocationMasterRequestDto request = LocationMasterRequestDto.builder()
                .locationCode("LOC 002")
                .locationName("New Office")
                .build();

        when(repository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(existingEntity));

        ValidationException exception = assertThrows(ValidationException.class, () -> service.update(1L, request));
        assertEquals("Location code cannot contain spaces", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void update_locationCodeAlreadyExists_throwsException() {
        LocationMasterEntity existingEntity = LocationMasterEntity.builder()
                .locationPoid(1L)
                .locationCode("LOC001")
                .build();

        LocationMasterRequestDto request = LocationMasterRequestDto.builder()
                .locationCode("LOC002")
                .locationName("New Office")
                .build();

        when(repository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(existingEntity));
        when(repository.existsByLocationCodeAndIdNot("LOC002", 1L)).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> service.update(1L, request));
        verify(repository, never()).save(any());
    }

    @Test
    void delete_success() {
        LocationMasterEntity entity = LocationMasterEntity.builder()
                .locationPoid(1L)
                .build();

        when(repository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(entity));

        DeleteReasonDto deleteReason = new DeleteReasonDto();
        deleteReason.setDeleteReason("Test deletion");

        service.delete(1L, deleteReason);

        verify(documentDeleteService).deleteDocument(eq(1L), eq("GLOBAL_LOCATION_MASTER"), 
                eq("LOCATION_POID"), eq(deleteReason), isNull());
    }

    @Test
    void delete_locationNotFound_throwsException() {
        when(repository.findByIdAndNotDeleted(1L)).thenReturn(Optional.empty());

        DeleteReasonDto deleteReason = new DeleteReasonDto();
        deleteReason.setDeleteReason("Test deletion");

        assertThrows(ResourceNotFoundException.class, () -> service.delete(1L, deleteReason));
        verify(documentDeleteService, never()).deleteDocument(any(), any(), any(), any(), any());
    }
}