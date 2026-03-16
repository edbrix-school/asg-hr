package com.asg.hr.airsector.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.hr.airsector.dto.HrAirsectorRequestDto;
import com.asg.hr.airsector.dto.HrAirsectorResponseDto;
import com.asg.hr.airsector.entity.HrAirsectorMaster;
import com.asg.hr.airsector.repository.HrAirsectorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
 class HrAirsectorServiceTest {

    @Mock
    private HrAirsectorRepository repository;

    @Mock
    private LoggingService loggingService;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private LovDataService lovService;

    @InjectMocks
    private HrAirsectorServiceImpl service;

    @Test
    void testCreate_Success() {
        HrAirsectorRequestDto request = new HrAirsectorRequestDto();
        request.setAirsectorDescription("Test Sector");
        request.setActive("Y");
        request.setSeqno(1);
        request.setAverageTicketRate(new BigDecimal("1000.00"));
        request.setHrCountryPoid(1L);
        request.setBusinessFare("Y");

        HrAirsectorMaster savedEntity = HrAirsectorMaster.builder()
                .airsecPoid(1L)
                .airsectorDescription("TEST SECTOR")
                .active("Y")
                .seqno(1)
                .averageTicketRate(new BigDecimal("1000.00"))
                .hrCountryPoid(1L)
                .businessFare("Y")
                .deleted("N")
                .build();

        when(repository.existsByAirsectorDescription("TEST SECTOR")).thenReturn(false);
        when(repository.save(any(HrAirsectorMaster.class))).thenReturn(savedEntity);
        when(lovService.getDetailsByPoidAndLovNameFast(1L, "NATIONALITY")).thenReturn(new LovGetListDto());

        HrAirsectorResponseDto response = service.create(request);

        assertNotNull(response);
        assertEquals(1L, response.getAirsecPoid());
        assertEquals("TEST SECTOR", response.getAirsectorDescription());
        verify(repository).save(any(HrAirsectorMaster.class));
    }

    @Test
    void testCreate_DuplicateDescription() {
        HrAirsectorRequestDto request = new HrAirsectorRequestDto();
        request.setAirsectorDescription("Test Sector");

        when(repository.existsByAirsectorDescription("TEST SECTOR")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> service.create(request));
        verify(repository, never()).save(any());
    }

    @Test
    void testUpdate_Success() {
        Long poid = 1L;
        HrAirsectorRequestDto request = new HrAirsectorRequestDto();
        request.setAirsectorDescription("Updated Sector");
        request.setActive("Y");
        request.setSeqno(2);
        request.setAverageTicketRate(new BigDecimal("2000.00"));
        request.setHrCountryPoid(2L);
        request.setBusinessFare("N");

        HrAirsectorMaster existingEntity = HrAirsectorMaster.builder()
                .airsecPoid(poid)
                .airsectorDescription("OLD SECTOR")
                .active("Y")
                .build();

        HrAirsectorMaster updatedEntity = HrAirsectorMaster.builder()
                .airsecPoid(poid)
                .airsectorDescription("UPDATED SECTOR")
                .active("Y")
                .seqno(2)
                .averageTicketRate(new BigDecimal("2000.00"))
                .hrCountryPoid(2L)
                .businessFare("N")
                .build();

        when(repository.findById(poid)).thenReturn(Optional.of(existingEntity));
        when(repository.existsByAirsectorDescriptionAndAirsecPoidNot("UPDATED SECTOR", poid)).thenReturn(false);
        when(repository.save(any(HrAirsectorMaster.class))).thenReturn(updatedEntity);
        when(lovService.getDetailsByPoidAndLovNameFast(2L, "NATIONALITY")).thenReturn(new LovGetListDto());

        HrAirsectorResponseDto response = service.update(poid, request);

        assertNotNull(response);
        assertEquals("UPDATED SECTOR", response.getAirsectorDescription());
        verify(repository).save(any(HrAirsectorMaster.class));
    }

    @Test
    void testUpdate_NotFound() {
        Long poid = 1L;
        HrAirsectorRequestDto request = new HrAirsectorRequestDto();
        request.setAirsectorDescription("Test");

        when(repository.findById(poid)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(poid, request));
        verify(repository, never()).save(any());
    }

    @Test
    void testFindById_Success() {
        Long poid = 1L;
        HrAirsectorMaster entity = HrAirsectorMaster.builder()
                .airsecPoid(poid)
                .airsectorDescription("TEST SECTOR")
                .active("Y")
                .hrCountryPoid(1L)
                .build();

        when(repository.findById(poid)).thenReturn(Optional.of(entity));
        when(lovService.getDetailsByPoidAndLovNameFast(1L, "NATIONALITY")).thenReturn(new LovGetListDto());

        HrAirsectorResponseDto response = service.findById(poid);

        assertNotNull(response);
        assertEquals(poid, response.getAirsecPoid());
        assertEquals("TEST SECTOR", response.getAirsectorDescription());
    }

    @Test
    void testFindById_NotFound() {
        Long poid = 1L;
        when(repository.findById(poid)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(poid));
    }

    @Test
    void testDeleteAirsectorMaster_Success() {
        Long poid = 1L;
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        HrAirsectorMaster entity = HrAirsectorMaster.builder()
                .airsecPoid(poid)
                .airsectorDescription("TEST SECTOR")
                .active("Y")
                .deleted("N")
                .build();

        when(repository.findById(poid)).thenReturn(Optional.of(entity));

        service.deleteAirsectorMaster(poid, deleteReasonDto);

        assertEquals("Y", entity.getDeleted());
        assertEquals("N", entity.getActive());
        verify(documentDeleteService).deleteDocument(eq(poid), eq("HR_AIRSECTOR_MASTER"), 
                eq("AIRSEC_POID"), eq(deleteReasonDto), any());
    }

    @Test
    void testDeleteAirsectorMaster_NotFound() {
        Long poid = 1L;
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();

        when(repository.findById(poid)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> service.deleteAirsectorMaster(poid, deleteReasonDto));
        verify(documentDeleteService, never()).deleteDocument(anyLong(), anyString(), 
                anyString(), any(), any());
    }
}
