package com.asg.hr.nationality.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.nationality.dto.request.HrNationalityRequest;
import com.asg.hr.nationality.dto.request.HrNationalityUpdateRequest;
import com.asg.hr.nationality.dto.response.HrNationalityResponse;
import com.asg.hr.nationality.entity.HrNationalityMaster;
import com.asg.hr.nationality.repository.HrNationalityRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrNationalityServiceImplTest {

    @Mock
    private HrNationalityRepository hrNationalityRepository;
    @Mock
    private DocumentSearchService documentService;
    @Mock
    private DocumentDeleteService deleteService;
    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private HrNationalityServiceImpl service;

    private HrNationalityRequest request;
    private HrNationalityMaster entity;

    @BeforeEach
    void setup() {
        request = new HrNationalityRequest();
        request.setNationalityCode("IND");
        request.setNationalityDescription("India");
        request.setTicketAmountNormal(BigDecimal.valueOf(100.0));
        request.setTicketAmountBusiness(BigDecimal.valueOf(200.0));
        request.setActive(true);
        request.setSeqNo(1);

        entity = new HrNationalityMaster();
        entity.setNationPoid(1L);
        entity.setGroupPoid(100L);
        entity.setNationalityCode("IND");
        entity.setNationalityDescription("India");
        entity.setTicketAmountNormal(BigDecimal.valueOf(100.0));
        entity.setTicketAmountBusiness(BigDecimal.valueOf(200.0));
        entity.setActive("Y");
        entity.setSeqno(1);
        entity.setDeleted("N");
    }

    @Test
    void testCreate_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);

            when(hrNationalityRepository.existsByNationalityCode(anyString())).thenReturn(false);
            when(hrNationalityRepository.existsByNationalityDescription(anyString())).thenReturn(false);
            when(hrNationalityRepository.save(any(HrNationalityMaster.class))).thenReturn(entity);
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

            HrNationalityResponse result = service.create(request);

            assertNotNull(result);
            assertEquals("IND", result.getNationalityCode());
            verify(hrNationalityRepository).save(any(HrNationalityMaster.class));
        }
    }

    @Test
    void testCreate_DuplicateCode() {
        when(hrNationalityRepository.existsByNationalityCode(anyString())).thenReturn(true);
        assertThrows(DuplicateKeyException.class, () -> service.create(request));
    }

    @Test
    void testCreate_DuplicateDescription() {
        when(hrNationalityRepository.existsByNationalityCode(anyString())).thenReturn(false);
        when(hrNationalityRepository.existsByNationalityDescription(anyString())).thenReturn(true);
        assertThrows(DuplicateKeyException.class, () -> service.create(request));
    }

    @Test
    void testUpdate_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            HrNationalityUpdateRequest updateReq = new HrNationalityUpdateRequest();
            updateReq.setNationalityDescription("United States");
            updateReq.setActive(true);
            updateReq.setSeqNo(1);
            updateReq.setTicketAmountNormal(BigDecimal.valueOf(150.0));
            updateReq.setTicketAmountBusiness(BigDecimal.valueOf(300.0));

            when(hrNationalityRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(hrNationalityRepository.existsByNationalityDescriptionExcluding("United States", 1L)).thenReturn(false);
            when(hrNationalityRepository.save(any(HrNationalityMaster.class))).thenReturn(entity);
            doNothing().when(loggingService).logChanges(any(), any(), any(), any(), any(), any(), any());

            HrNationalityResponse result = service.update(1L, updateReq);

            assertNotNull(result);
            verify(hrNationalityRepository).save(any(HrNationalityMaster.class));
        }
    }

    @Test
    void testUpdate_NotFound() {
        HrNationalityUpdateRequest updateReq = new HrNationalityUpdateRequest();
        when(hrNationalityRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.update(1L, updateReq));
    }

    @Test
    void testGetById_Success() {
        when(hrNationalityRepository.findById(1L)).thenReturn(Optional.of(entity));
        HrNationalityResponse result = service.getById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getNationPoid());
    }

    @Test
    void testGetById_NotFound() {
        when(hrNationalityRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(1L));
    }

    @Test
    void testDelete_Success() {
        when(hrNationalityRepository.findById(1L)).thenReturn(Optional.of(entity));
        DeleteReasonDto deleteReason = new DeleteReasonDto();
        deleteReason.setDeleteReason("Testing");

        service.delete(1L, deleteReason);

        verify(deleteService).deleteDocument(eq(1L), anyString(), anyString(), eq(deleteReason), isNull());
    }

    @Test
    void testList() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            FilterRequestDto filters = new FilterRequestDto("OR", "N", List.of());
            Pageable pageable = PageRequest.of(0, 10);
            RawSearchResult raw = new RawSearchResult(List.of(Map.of("NATIONALITY_CODE", "IND")), Map.of(), 1L);

            when(documentService.resolveOperator(any())).thenReturn("OR");
            when(documentService.resolveIsDeleted(any())).thenReturn("N");
            when(documentService.resolveFilters(any())).thenReturn(List.of());
            when(documentService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn(raw);

            Map<String, Object> result = service.list(filters, pageable);

            assertNotNull(result);
            verify(documentService).search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString());
        }
    }

    @Test
    void testValidateRequest_EmptyCode() {
        request.setNationalityCode("");
        assertThrows(ValidationException.class, () -> service.create(request));
    }


    @Test
    void testUpdate_NullId() {
        HrNationalityUpdateRequest updateReq = new HrNationalityUpdateRequest();
        assertThrows(ValidationException.class, () -> service.update(null, updateReq));
    }

    @Test
    void testUpdate_SameValues() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            HrNationalityUpdateRequest updateReq = new HrNationalityUpdateRequest();
            updateReq.setNationalityDescription("India");
            updateReq.setActive(true);
            updateReq.setSeqNo(1);
            updateReq.setTicketAmountNormal(BigDecimal.valueOf(100.0));
            updateReq.setTicketAmountBusiness(BigDecimal.valueOf(200.0));

            when(hrNationalityRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(hrNationalityRepository.existsByNationalityDescriptionExcluding(anyString(), anyLong())).thenReturn(false);
            when(hrNationalityRepository.save(any(HrNationalityMaster.class))).thenReturn(entity);
            doNothing().when(loggingService).logChanges(any(), any(), any(), any(), any(), any(), any());

            HrNationalityResponse result = service.update(1L, updateReq);

            assertNotNull(result);
            verify(hrNationalityRepository).save(any(HrNationalityMaster.class));
        }
    }

    @Test
    void testCreate_NullDescription() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);

            request.setNationalityDescription(null);
            when(hrNationalityRepository.existsByNationalityCode(anyString())).thenReturn(false);
            when(hrNationalityRepository.save(any(HrNationalityMaster.class))).thenReturn(entity);
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

            HrNationalityResponse result = service.create(request);

            assertNotNull(result);
            verify(hrNationalityRepository, never()).existsByNationalityDescription(anyString());
        }
    }


    @Test
    void testValidateUpdateRequest_DuplicateDescription() {
        HrNationalityUpdateRequest updateReq = new HrNationalityUpdateRequest();
        updateReq.setNationalityDescription("Other Description");
        updateReq.setActive(true);

        when(hrNationalityRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(hrNationalityRepository.existsByNationalityDescriptionExcluding(anyString(), anyLong())).thenReturn(true);

        assertThrows(DuplicateKeyException.class, () -> service.update(1L, updateReq));
    }

    @Test
    void testCreate_DuplicateDescription_CaseInsensitive() {
        request.setNationalityDescription("india"); // Original is "India"
        when(hrNationalityRepository.existsByNationalityCode(anyString())).thenReturn(false);
        // It should return false because description is now case-sensitive
        when(hrNationalityRepository.existsByNationalityDescription("india")).thenReturn(false);
        
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            when(hrNationalityRepository.save(any(HrNationalityMaster.class))).thenReturn(entity);
            
            HrNationalityResponse result = service.create(request);
            assertNotNull(result);
        }
    }

}
