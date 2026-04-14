package com.asg.hr.personaldatasheet.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.exception.AsgException;
import com.asg.hr.personaldatasheet.dto.PersonalDataSheetRequestDto;
import com.asg.hr.personaldatasheet.entity.*;
import com.asg.hr.personaldatasheet.repository.*;
import com.asg.hr.personaldatasheet.util.PersonalDataSheetValidator;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.sql.DataSource;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalDataSheetServiceImplEdgeCaseTest {

    @Mock private HrPersonalDataHdrRepository repository;
    @Mock private HrPersonalDataDependentRepository dependentRepository;
    @Mock private HrPersonalDataEmergencyRepository emergencyRepository;
    @Mock private HrPersonalDataNomineeRepository nomineeRepository;
    @Mock private HrPersonalDataPoliciesRepository policiesRepository;
    @Mock private DocumentSearchService documentSearchService;
    @Mock private DocumentDeleteService documentDeleteService;
    @Mock private LoggingService loggingService;
    @Mock private PersonalDataSheetValidator validator;
    @Mock private PersonalDataSheetProcedureRepository procedureRepository;
    @Mock private PrintService printService;
    @Mock private DataSource dataSource;

    @InjectMocks private PersonalDataSheetServiceImpl service;

    private PersonalDataSheetRequestDto validRequest;
    private HrPersonalDataHdr validEntity;

    @BeforeEach
    void setUp() {
        validRequest = createValidRequest();
        validEntity = createValidEntity();
    }

    // Validation Edge Cases
    @Test
    void create_WithValidationException_ShouldPropagateException() {
        // Arrange
        doThrow(new ValidationException("Validation failed")).when(validator).validateRequest(any());

        // Act & Assert
        assertThatThrownBy(() -> service.create(validRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Validation failed");
        
        verify(repository, never()).save(any());
    }

    @Test
    void update_WithValidationException_ShouldPropagateException() {
        // Arrange
        when(repository.findByTransactionPoidAndNotDeleted(1L)).thenReturn(Optional.of(validEntity));
        doThrow(new ValidationException("Validation failed")).when(validator).validateRequest(any());

        // Act & Assert
        assertThatThrownBy(() -> service.update(1L, validRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Validation failed");
        
        verify(repository, never()).save(any());
    }

    // Database Transaction Edge Cases
    @Test
    void create_WithDatabaseException_ShouldPropagateException() {
        // Arrange
        when(repository.save(any(HrPersonalDataHdr.class))).thenThrow(new RuntimeException("Database error"));

        try (MockedStatic<UserContext> uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(1L);
            uc.when(UserContext::getCompanyPoid).thenReturn(1L);
            uc.when(UserContext::getUserId).thenReturn("testUser");

            // Act & Assert
            assertThatThrownBy(() -> service.create(validRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }
    }

    @Test
    void update_WithDatabaseException_ShouldPropagateException() {
        // Arrange
        when(repository.findByTransactionPoidAndNotDeleted(1L)).thenReturn(Optional.of(validEntity));
        when(repository.save(any(HrPersonalDataHdr.class))).thenThrow(new RuntimeException("Database error"));

        try (MockedStatic<UserContext> uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserId).thenReturn("testUser");

            // Act & Assert
            assertThatThrownBy(() -> service.update(1L, validRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }
    }

    // UserContext Edge Cases
    @Test
    void create_WithNullUserContext_ShouldHandleGracefully() {
        // Arrange
        when(repository.save(any(HrPersonalDataHdr.class))).thenReturn(validEntity);
        when(dependentRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(emergencyRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(nomineeRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(policiesRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());

        try (MockedStatic<UserContext> uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(null);
            uc.when(UserContext::getCompanyPoid).thenReturn(null);
            uc.when(UserContext::getUserId).thenReturn(null);
            uc.when(UserContext::getDocumentId).thenReturn(null);

            // Act
            var result = service.create(validRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(repository).save(any(HrPersonalDataHdr.class));
        }
    }

    // Large Data Set Edge Cases
    @Test
    void create_WithLargeNumberOfChildEntities_ShouldHandleCorrectly() {
        // Arrange - Create 100 dependents
        List<PersonalDataSheetRequestDto.DependentDto> dependents = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            PersonalDataSheetRequestDto.DependentDto dependent = new PersonalDataSheetRequestDto.DependentDto();
            dependent.setNamePassport("Dependent " + i);
            dependent.setRelation("Child");
            dependent.setDateOfBirth(LocalDate.of(2000, 1, 1));
            dependents.add(dependent);
        }
        validRequest.setDependents(dependents);

        when(repository.save(any(HrPersonalDataHdr.class))).thenReturn(validEntity);
        when(dependentRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(emergencyRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(nomineeRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(policiesRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());

        try (MockedStatic<UserContext> uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(1L);
            uc.when(UserContext::getCompanyPoid).thenReturn(1L);
            uc.when(UserContext::getUserId).thenReturn("testUser");
            uc.when(UserContext::getDocumentId).thenReturn("800-112");

            // Act
            var result = service.create(validRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(dependentRepository).saveAll(argThat(list -> {
                if (list instanceof Collection) {
                    return ((Collection<?>) list).size() == 100;
                }
                return false;
            }));
        }
    }

    // Search and List Edge Cases
    @Test
    void list_WithNullFilters_ShouldHandleGracefully() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(documentSearchService.resolveOperator(null)).thenReturn("OR");
        when(documentSearchService.resolveIsDeleted(null)).thenReturn("N");
        when(documentSearchService.resolveFilters(null)).thenReturn(Collections.emptyList());
        
        RawSearchResult mockResult = mock(RawSearchResult.class);
        when(mockResult.records()).thenReturn(Collections.emptyList());
        when(mockResult.totalRecords()).thenReturn(0L);
        when(mockResult.displayFields()).thenReturn(Collections.emptyMap());
        
        when(documentSearchService.search(anyString(), anyList(), anyString(), any(Pageable.class), 
                anyString(), anyString(), anyString())).thenReturn(mockResult);

        // Act
        Map<String, Object> result = service.list("800-112", null, pageable);

        // Assert
        assertThat(result).isNotNull();
        verify(documentSearchService).search(eq("800-112"), eq(Collections.emptyList()), eq("OR"), 
                eq(pageable), eq("N"), eq("EMPLOYEE_NAME_PASSPORT"), eq("TRANSACTION_POID"));
    }

    @Test
    void list_WithSearchServiceException_ShouldPropagateException() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        FilterRequestDto filters = mock(FilterRequestDto.class);
        
        when(documentSearchService.resolveOperator(filters)).thenReturn("OR");
        when(documentSearchService.resolveIsDeleted(filters)).thenReturn("N");
        when(documentSearchService.resolveFilters(filters)).thenReturn(Collections.emptyList());
        when(documentSearchService.search(anyString(), anyList(), anyString(), any(Pageable.class), 
                anyString(), anyString(), anyString())).thenThrow(new RuntimeException("Search failed"));

        // Act & Assert
        assertThatThrownBy(() -> service.list("800-112", filters, pageable))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Search failed");
    }

    // Delete Edge Cases
    @Test
    void delete_WithNullDeleteReason_ShouldHandleGracefully() {
        // Arrange
        when(repository.findByTransactionPoidAndNotDeleted(1L)).thenReturn(Optional.of(validEntity));

        // Act
        service.delete(1L, null);

        // Assert
        verify(documentDeleteService).deleteDocument(eq(1L), eq("HR_PERSONAL_DATA_HDR"), 
                eq("TRANSACTION_POID"), isNull(), any(LocalDate.class));
    }

    @Test
    void delete_WithDeleteServiceException_ShouldPropagateException() {
        // Arrange
        when(repository.findByTransactionPoidAndNotDeleted(1L)).thenReturn(Optional.of(validEntity));
        doThrow(new RuntimeException("Delete failed")).when(documentDeleteService)
                .deleteDocument(anyLong(), anyString(), anyString(), any(), any(LocalDate.class));

        // Act & Assert
        assertThatThrownBy(() -> service.delete(1L, new DeleteReasonDto()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Delete failed");
    }

    // Print Edge Cases
    @Test
    void print_WithNonExistentRecord_ShouldThrowException() {
        // Arrange
        when(repository.findByTransactionPoidAndNotDeleted(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.print(999L))
                .isInstanceOf(AsgException.class)
                .hasMessageContaining("Personal data sheet not found with ID: 999");
    }

    @Test
    void print_WithPrintServiceException_ShouldPropagateException() throws Exception {
        // Arrange
        when(repository.findByTransactionPoidAndNotDeleted(1L)).thenReturn(Optional.of(validEntity));
        when(printService.buildBaseParams(anyLong(), anyString())).thenReturn(Collections.emptyMap());
        when(printService.load(anyString())).thenThrow(new RuntimeException("Print failed"));

        try (MockedStatic<UserContext> uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getDocumentId).thenReturn("800-112");

            // Act & Assert
            assertThatThrownBy(() -> service.print(1L))
                    .isInstanceOf(AsgException.class)
                    .hasMessage("Failed to generate PDF report");
        }
    }

    @Test
    void print_WithValidData_ShouldReturnPdfBytes() throws Exception {
        // Arrange
        byte[] expectedPdf = "PDF_CONTENT".getBytes();
        JasperReport mockReport = mock(JasperReport.class);
        
        when(repository.findByTransactionPoidAndNotDeleted(1L)).thenReturn(Optional.of(validEntity));
        when(printService.buildBaseParams(1L, "800-112")).thenReturn(Collections.emptyMap());
        when(printService.load("HR_JRXML/Emp_Personal_Data.jrxml")).thenReturn(mockReport);
        when(printService.fillReportToPdf(eq(mockReport), anyMap(), eq(dataSource))).thenReturn(expectedPdf);

        try (MockedStatic<UserContext> uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getDocumentId).thenReturn("800-112");

            // Act
            byte[] result = service.print(1L);

            // Assert
            assertThat(result).isEqualTo(expectedPdf);
            verify(printService).fillReportToPdf(eq(mockReport), anyMap(), eq(dataSource));
        }
    }

    // Procedure Repository Edge Cases
    @Test
    void getLoginUserEmployee_WithProcedureException_ShouldPropagateException() {
        // Arrange
        when(procedureRepository.getLoginUserEmployeeId(anyLong())).thenThrow(new RuntimeException("Procedure failed"));

        try (MockedStatic<UserContext> uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserPoid).thenReturn(123L);

            // Act & Assert
            assertThatThrownBy(() -> service.getLoginUserEmployee())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Procedure failed");
        }
    }

    @Test
    void loadUserPolicies_WithProcedureException_ShouldPropagateException() {
        // Arrange
        when(procedureRepository.loadUserPolicies(anyLong(), anyLong(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Procedure failed"));

        try (MockedStatic<UserContext> uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(1L);
            uc.when(UserContext::getCompanyPoid).thenReturn(1L);
            uc.when(UserContext::getUserPoid).thenReturn(123L);
            uc.when(UserContext::getDocumentId).thenReturn("800-112");

            // Act & Assert
            assertThatThrownBy(() -> service.loadUserPolicies(456L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Procedure failed");
        }
    }

    @Test
    void loadUserPolicies_WithNullEmployeePoid_ShouldHandleGracefully() {
        // Arrange
        when(procedureRepository.loadUserPolicies(anyLong(), anyLong(), anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());

        try (MockedStatic<UserContext> uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(1L);
            uc.when(UserContext::getCompanyPoid).thenReturn(1L);
            uc.when(UserContext::getUserPoid).thenReturn(123L);
            uc.when(UserContext::getDocumentId).thenReturn("800-112");

            // Act
            List<Map<String, Object>> result = service.loadUserPolicies(null);

            // Assert
            assertThat(result).isEmpty();
            verify(procedureRepository).loadUserPolicies(1L, 1L, "123", "800-112", null);
        }
    }

    // Concurrent Access Edge Cases
    @Test
    void update_WithConcurrentModification_ShouldHandleOptimisticLocking() {
        // Arrange
        HrPersonalDataHdr staleEntity = createValidEntity();
        staleEntity.setLastmodifiedDate(LocalDateTime.now().minusDays(1));
        
        when(repository.findByTransactionPoidAndNotDeleted(1L)).thenReturn(Optional.of(staleEntity));
        when(repository.save(any(HrPersonalDataHdr.class)))
                .thenThrow(new RuntimeException("Optimistic locking failure"));

        try (MockedStatic<UserContext> uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserId).thenReturn("testUser");

            // Act & Assert
            assertThatThrownBy(() -> service.update(1L, validRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Optimistic locking failure");
        }
    }

    // Memory and Performance Edge Cases
    @Test
    void getById_WithLargeChildEntityCollections_ShouldHandleEfficiently() {
        // Arrange
        when(repository.findByTransactionPoidAndNotDeleted(1L)).thenReturn(Optional.of(validEntity));
        
        // Create mock entities instead of using Collections.nCopies with mock()
        List<HrPersonalDataDependent> mockDependents = new ArrayList<>();
        List<HrPersonalDataEmergency> mockEmergencies = new ArrayList<>();
        List<HrPersonalDataNominee> mockNominees = new ArrayList<>();
        List<HrPersonalDataPolicies> mockPolicies = new ArrayList<>();
        
        for (int i = 0; i < 1000; i++) {
            mockDependents.add(new HrPersonalDataDependent());
        }
        for (int i = 0; i < 500; i++) {
            mockEmergencies.add(new HrPersonalDataEmergency());
        }
        for (int i = 0; i < 100; i++) {
            mockNominees.add(new HrPersonalDataNominee());
        }
        for (int i = 0; i < 50; i++) {
            mockPolicies.add(new HrPersonalDataPolicies());
        }
        
        when(dependentRepository.findByTransactionPoid(1L)).thenReturn(mockDependents);
        when(emergencyRepository.findByTransactionPoid(1L)).thenReturn(mockEmergencies);
        when(nomineeRepository.findByTransactionPoid(1L)).thenReturn(mockNominees);
        when(policiesRepository.findByTransactionPoid(1L)).thenReturn(mockPolicies);

        // Act
        var result = service.getById(1L);

        // Assert
        assertThat(result).isNotNull();
        // Verify all repositories were called
        verify(dependentRepository).findByTransactionPoid(1L);
        verify(emergencyRepository).findByTransactionPoid(1L);
        verify(nomineeRepository).findByTransactionPoid(1L);
        verify(policiesRepository).findByTransactionPoid(1L);
    }

    // Helper methods
    private PersonalDataSheetRequestDto createValidRequest() {
        PersonalDataSheetRequestDto request = new PersonalDataSheetRequestDto();
        request.setEmployeePoid(123L);
        request.setEmployeeNamePassport("John Doe");
        request.setResidentStatus("Resident");
        request.setCurrentFlat("123");
        request.setCurrentBldg("Building A");
        request.setCurrentRoad("Main Street");
        request.setCurrentBlock("Block 1");
        request.setCurrentArea("Downtown");
        request.setCurrentMobile("12345678");
        request.setPermanentAddress("123 Main Street");
        return request;
    }

    private HrPersonalDataHdr createValidEntity() {
        HrPersonalDataHdr entity = new HrPersonalDataHdr();
        entity.setTransactionPoid(1L);
        entity.setGroupPoid(1L);
        entity.setCompanyPoid(1L);
        entity.setTransactionDate(LocalDate.now());
        entity.setEmployeePoid(123L);
        entity.setEmployeeNamePassport("John Doe");
        entity.setResidentStatus("Resident");
        entity.setCurrentFlat("123");
        entity.setCurrentBldg("Building A");
        entity.setCurrentRoad("Main Street");
        entity.setCurrentBlock("Block 1");
        entity.setCurrentArea("Downtown");
        entity.setCurrentMobile("12345678");
        entity.setPermanentAddress("123 Main Street");
        entity.setStatus("DRAFT");
        entity.setDeleted("N");
        entity.setCreatedBy("testUser");
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastmodifiedDate(LocalDateTime.now());
        return entity;
    }
}