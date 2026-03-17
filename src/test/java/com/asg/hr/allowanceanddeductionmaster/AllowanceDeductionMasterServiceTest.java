package com.asg.hr.allowanceanddeductionmaster;

import com.asg.hr.allowanceanddeductionmaster.dto.AllowanceDeductionRequestDTO;
import com.asg.hr.allowanceanddeductionmaster.dto.AllowanceDeductionResponseDTO;
import com.asg.hr.allowanceanddeductionmaster.entity.HrAllowanceDeductionMaster;
import com.asg.hr.allowanceanddeductionmaster.mapper.AllowanceDeductionMasterMapper;
import com.asg.hr.allowanceanddeductionmaster.repository.AllowanceDeductionMasterRepository;
import com.asg.hr.allowanceanddeductionmaster.service.impl.AllowanceDeductionMasterServiceImpl;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.DocumentDeleteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private DocumentDeleteService documentDeleteService;

    @InjectMocks
    private AllowanceDeductionMasterServiceImpl service;

    private AllowanceDeductionRequestDTO requestDTO;
    private HrAllowanceDeductionMaster entity;
    private AllowanceDeductionResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = AllowanceDeductionRequestDTO.builder()
                .code("DA")
                .description("Dearness Allowance")
                .variableFixed("VARIABLE")
                .type("ALLOWANCE")
                .formula("salary * 0.10")
                .glPoid(1001L)
                .mandatory("N")
                .payrollFieldName("dearness_allowance")
                .seqno(1)
                .active("Y")
                .groupPoid(100L)
                .build();

        entity = HrAllowanceDeductionMaster.builder()
                .allowaceDeductionPoid(1L)
                .code("DA")
                .description("Dearness Allowance")
                .variableFixed("VARIABLE")
                .type("ALLOWANCE")
                .formula("salary * 0.10")
                .glPoid(1001L)
                .mandatory("N")
                .payrollFieldName("dearness_allowance")
                .seqno(1)
                .active("Y")
                .groupPoid(100L)
                .deleted("N")
                .build();

        responseDTO = AllowanceDeductionResponseDTO.builder()
                .allowaceDeductionPoid(1L)
                .code("DA")
                .description("Dearness Allowance")
                .variableFixed("VARIABLE")
                .type("ALLOWANCE")
                .formula("salary * 0.10")
                .glPoid(1001L)
                .mandatory("N")
                .payrollFieldName("dearness_allowance")
                .seqno(1)
                .active("Y")
                .groupPoid(100L)
                .deleted("N")
                .build();
    }

    @Test
    void testCreateSuccess() {
        when(repository.findByCodeAndDeletedNot("DA", "Y")).thenReturn(Optional.empty());
        when(repository.findByPayrollFieldName("dearness_allowance")).thenReturn(Optional.empty());
        when(mapper.toEntity(requestDTO)).thenReturn(entity);
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toResponseDTO(entity)).thenReturn(responseDTO);

        AllowanceDeductionResponseDTO result = service.create(requestDTO);

        assertNotNull(result);
        assertEquals("DA", result.getCode());
        verify(repository, times(1)).save(any());
    }

    @Test
    void testCreateDuplicateCode() {
        when(repository.findByCodeAndDeletedNot("DA", "Y")).thenReturn(Optional.of(entity));

        assertThrows(ResourceAlreadyExistsException.class, () -> service.create(requestDTO));
    }

    @Test
    void testCreateDuplicatePayrollFieldName() {
        when(repository.findByCodeAndDeletedNot("DA", "Y")).thenReturn(Optional.empty());
        when(repository.findByPayrollFieldName("dearness_allowance")).thenReturn(Optional.of(entity));

        assertThrows(ResourceAlreadyExistsException.class, () -> service.create(requestDTO));
    }

    @Test
    void testUpdateSuccess() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.findByPayrollFieldName("dearness_allowance")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toResponseDTO(entity)).thenReturn(responseDTO);

        AllowanceDeductionResponseDTO result = service.update(1L, requestDTO);

        assertNotNull(result);
        verify(repository, times(1)).save(any());
    }

    @Test
    void testUpdateNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(999L, requestDTO));
    }

    @Test
    void testGetByIdSuccess() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toResponseDTO(entity)).thenReturn(responseDTO);

        AllowanceDeductionResponseDTO result = service.getById(1L);

        assertNotNull(result);
        assertEquals("DA", result.getCode());
    }

    @Test
    void testGetByIdNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(999L));
    }

    @Test
    void testSoftDelete() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L, null);

        verify(repository, times(1)).findById(1L);
        verify(documentDeleteService, times(1)).deleteDocument(1L, "HR_ALLOWANCE_DEDUCTION_MASTER", "ALLOWACE_DEDUCTION_POID", null, null);
    }
}
