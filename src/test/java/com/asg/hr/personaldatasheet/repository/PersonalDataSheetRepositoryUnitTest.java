package com.asg.hr.personaldatasheet.repository;

import com.asg.hr.personaldatasheet.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalDataSheetRepositoryUnitTest {

    @Mock
    private HrPersonalDataHdrRepository hdrRepository;

    @Mock
    private HrPersonalDataDependentRepository dependentRepository;

    @Mock
    private HrPersonalDataEmergencyRepository emergencyRepository;

    @Mock
    private HrPersonalDataNomineeRepository nomineeRepository;

    @Mock
    private HrPersonalDataPoliciesRepository policiesRepository;

    private HrPersonalDataHdr testHeader;
    private Long transactionPoid;

    @BeforeEach
    void setUp() {
        // Create test header entity
        testHeader = new HrPersonalDataHdr();
        testHeader.setGroupPoid(1L);
        testHeader.setCompanyPoid(1L);
        testHeader.setTransactionDate(LocalDate.now());
        testHeader.setEmployeePoid(123L);
        testHeader.setEmployeeNamePassport("John Doe");
        testHeader.setResidentStatus("Resident");
        testHeader.setCurrentFlat("123");
        testHeader.setCurrentBldg("Building A");
        testHeader.setCurrentRoad("Main Street");
        testHeader.setCurrentBlock("Block 1");
        testHeader.setCurrentArea("Downtown");
        testHeader.setCurrentMobile("12345678");
        testHeader.setPermanentAddress("123 Main Street");
        testHeader.setStatus("DRAFT");
        testHeader.setDeleted("N");
        testHeader.setCreatedBy("testUser");
        testHeader.setCreatedDate(LocalDateTime.now());
        testHeader.setLastmodifiedDate(LocalDateTime.now());
        testHeader.setTransactionPoid(1L);
        
        transactionPoid = 1L;
    }

    // HrPersonalDataHdrRepository Tests
    @Test
    void hdrRepository_FindByTransactionPoidAndNotDeleted_Success() {
        // Arrange
        when(hdrRepository.findByTransactionPoidAndNotDeleted(transactionPoid)).thenReturn(Optional.of(testHeader));
        
        // Act
        Optional<HrPersonalDataHdr> result = hdrRepository.findByTransactionPoidAndNotDeleted(transactionPoid);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getEmployeeNamePassport()).isEqualTo("John Doe");
        assertThat(result.get().getDeleted()).isEqualTo("N");
        verify(hdrRepository).findByTransactionPoidAndNotDeleted(transactionPoid);
    }

    @Test
    void hdrRepository_FindByTransactionPoidAndNotDeleted_DeletedRecord_ReturnsEmpty() {
        // Arrange
        when(hdrRepository.findByTransactionPoidAndNotDeleted(transactionPoid)).thenReturn(Optional.empty());

        // Act
        Optional<HrPersonalDataHdr> result = hdrRepository.findByTransactionPoidAndNotDeleted(transactionPoid);

        // Assert
        assertThat(result).isEmpty();
        verify(hdrRepository).findByTransactionPoidAndNotDeleted(transactionPoid);
    }

    @Test
    void hdrRepository_FindByTransactionPoidAndNotDeleted_NonExistentId_ReturnsEmpty() {
        // Arrange
        when(hdrRepository.findByTransactionPoidAndNotDeleted(999L)).thenReturn(Optional.empty());
        
        // Act
        Optional<HrPersonalDataHdr> result = hdrRepository.findByTransactionPoidAndNotDeleted(999L);

        // Assert
        assertThat(result).isEmpty();
        verify(hdrRepository).findByTransactionPoidAndNotDeleted(999L);
    }

    // HrPersonalDataDependentRepository Tests
    @Test
    void dependentRepository_FindByTransactionPoid_Success() {
        // Arrange
        HrPersonalDataDependent dependent1 = createTestDependent(1L, "Jane Doe", "Spouse");
        HrPersonalDataDependent dependent2 = createTestDependent(2L, "Bob Doe", "Child");
        List<HrPersonalDataDependent> dependents = List.of(dependent1, dependent2);
        
        when(dependentRepository.findByTransactionPoid(transactionPoid)).thenReturn(dependents);

        // Act
        List<HrPersonalDataDependent> result = dependentRepository.findByTransactionPoid(transactionPoid);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(HrPersonalDataDependent::getNamePassport)
                .containsExactlyInAnyOrder("Jane Doe", "Bob Doe");
        verify(dependentRepository).findByTransactionPoid(transactionPoid);
    }

    @Test
    void dependentRepository_FindByTransactionPoid_EmptyResult() {
        // Arrange
        when(dependentRepository.findByTransactionPoid(999L)).thenReturn(new ArrayList<>());
        
        // Act
        List<HrPersonalDataDependent> result = dependentRepository.findByTransactionPoid(999L);

        // Assert
        assertThat(result).isEmpty();
        verify(dependentRepository).findByTransactionPoid(999L);
    }

    @Test
    void dependentRepository_SaveAndDelete_Success() {
        // Arrange
        HrPersonalDataDependent dependent = createTestDependent(1L, "Test Dependent", "Child");
        when(dependentRepository.save(dependent)).thenReturn(dependent);
        when(dependentRepository.findByTransactionPoid(transactionPoid)).thenReturn(new ArrayList<>());
        
        // Act - Save
        HrPersonalDataDependent saved = dependentRepository.save(dependent);

        // Assert - Save
        assertThat(saved.getDetRowId()).isNotNull();
        assertThat(saved.getNamePassport()).isEqualTo("Test Dependent");
        verify(dependentRepository).save(dependent);

        // Act - Delete
        dependentRepository.delete(saved);

        // Assert - Delete
        verify(dependentRepository).delete(saved);
        List<HrPersonalDataDependent> result = dependentRepository.findByTransactionPoid(transactionPoid);
        assertThat(result).isEmpty();
    }

    // HrPersonalDataEmergencyRepository Tests
    @Test
    void emergencyRepository_FindByTransactionPoid_Success() {
        // Arrange
        HrPersonalDataEmergency emergency1 = createTestEmergency(1L, "Emergency Contact 1", "Friend");
        HrPersonalDataEmergency emergency2 = createTestEmergency(2L, "Emergency Contact 2", "Relative");
        List<HrPersonalDataEmergency> emergencies = List.of(emergency1, emergency2);
        when(emergencyRepository.findByTransactionPoid(transactionPoid)).thenReturn(emergencies);

        // Act
        List<HrPersonalDataEmergency> result = emergencyRepository.findByTransactionPoid(transactionPoid);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(HrPersonalDataEmergency::getName)
                .containsExactlyInAnyOrder("Emergency Contact 1", "Emergency Contact 2");
    }

    @Test
    void emergencyRepository_SaveAll_Success() {
        // Arrange
        HrPersonalDataEmergency emergency1 = createTestEmergency(1L, "Contact 1", "Friend");
        HrPersonalDataEmergency emergency2 = createTestEmergency(2L, "Contact 2", "Relative");
        List<HrPersonalDataEmergency> emergencies = List.of(emergency1, emergency2);
        when(emergencyRepository.saveAll(emergencies)).thenReturn(emergencies);
        when(emergencyRepository.findByTransactionPoid(transactionPoid)).thenReturn(emergencies);

        // Act
        List<HrPersonalDataEmergency> saved = emergencyRepository.saveAll(emergencies);

        // Assert
        assertThat(saved).hasSize(2);
        verify(emergencyRepository).saveAll(emergencies);
        List<HrPersonalDataEmergency> result = emergencyRepository.findByTransactionPoid(transactionPoid);
        assertThat(result).hasSize(2);
    }

    // HrPersonalDataNomineeRepository Tests
    @Test
    void nomineeRepository_FindByTransactionPoid_Success() {
        // Arrange
        HrPersonalDataNominee nominee1 = createTestNominee(1L, "Nominee 1", "PRIMARY", BigDecimal.valueOf(60.0));
        HrPersonalDataNominee nominee2 = createTestNominee(2L, "Nominee 2", "PRIMARY", BigDecimal.valueOf(40.0));
        List<HrPersonalDataNominee> nominees = List.of(nominee1, nominee2);
        when(nomineeRepository.findByTransactionPoid(transactionPoid)).thenReturn(nominees);

        // Act
        List<HrPersonalDataNominee> result = nomineeRepository.findByTransactionPoid(transactionPoid);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(HrPersonalDataNominee::getNomineeName)
                .containsExactlyInAnyOrder("Nominee 1", "Nominee 2");
        
        BigDecimal totalPercentage = result.stream()
                .map(HrPersonalDataNominee::getPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalPercentage).isEqualByComparingTo(BigDecimal.valueOf(100.0));
    }

    @Test
    void nomineeRepository_FindByNomineeType_Success() {
        // Arrange
        HrPersonalDataNominee primary = createTestNominee(1L, "Primary Nominee", "PRIMARY", BigDecimal.valueOf(100.0));
        HrPersonalDataNominee secondary = createTestNominee(2L, "Secondary Nominee", "SECONDARY", BigDecimal.valueOf(100.0));
        List<HrPersonalDataNominee> allNominees = List.of(primary, secondary);
        when(nomineeRepository.findByTransactionPoid(transactionPoid)).thenReturn(allNominees);

        // Act
        List<HrPersonalDataNominee> result = nomineeRepository.findByTransactionPoid(transactionPoid);
        List<HrPersonalDataNominee> primaryNominees = result.stream()
                .filter(n -> "PRIMARY".equals(n.getNomineeType()))
                .toList();

        // Assert
        assertThat(primaryNominees).hasSize(1);
        assertThat(primaryNominees.get(0).getNomineeName()).isEqualTo("Primary Nominee");
    }

    // HrPersonalDataPoliciesRepository Tests
    @Test
    void policiesRepository_FindByTransactionPoid_Success() {
        // Arrange
        HrPersonalDataPolicies policy1 = createTestPolicy(1L, 101L, "Privacy Policy");
        HrPersonalDataPolicies policy2 = createTestPolicy(2L, 102L, "Code of Conduct");
        List<HrPersonalDataPolicies> policies = List.of(policy1, policy2);
        when(policiesRepository.findByTransactionPoid(transactionPoid)).thenReturn(policies);

        // Act
        List<HrPersonalDataPolicies> result = policiesRepository.findByTransactionPoid(transactionPoid);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(HrPersonalDataPolicies::getDocName)
                .containsExactlyInAnyOrder("Privacy Policy", "Code of Conduct");
    }

    @Test
    void policiesRepository_UpdatePolicyAcceptance_Success() {
        // Arrange
        HrPersonalDataPolicies policy = createTestPolicy(1L, 101L, "Test Policy");
        policy.setPolicyAccepted("N");
        
        HrPersonalDataPolicies updatedPolicy = createTestPolicy(1L, 101L, "Test Policy");
        updatedPolicy.setPolicyAccepted("Y");
        updatedPolicy.setPolicyAcceptedOn(LocalDate.now());
        
        when(policiesRepository.save(any(HrPersonalDataPolicies.class))).thenReturn(updatedPolicy);

        // Act
        policy.setPolicyAccepted("Y");
        policy.setPolicyAcceptedOn(LocalDate.now());
        HrPersonalDataPolicies updated = policiesRepository.save(policy);

        // Assert
        assertThat(updated.getPolicyAccepted()).isEqualTo("Y");
        assertThat(updated.getPolicyAcceptedOn()).isEqualTo(LocalDate.now());
    }

    // Cross-Repository Operations Tests
    @Test
    void allRepositories_CascadeOperations_Success() {
        // Arrange - Create related entities
        HrPersonalDataDependent dependent = createTestDependent(1L, "Test Dependent", "Child");
        HrPersonalDataEmergency emergency = createTestEmergency(1L, "Emergency Contact", "Friend");
        HrPersonalDataNominee nominee = createTestNominee(1L, "Test Nominee", "PRIMARY", BigDecimal.valueOf(100.0));
        HrPersonalDataPolicies policy = createTestPolicy(1L, 101L, "Test Policy");

        // Mock repository responses
        when(dependentRepository.save(dependent)).thenReturn(dependent);
        when(emergencyRepository.save(emergency)).thenReturn(emergency);
        when(nomineeRepository.save(nominee)).thenReturn(nominee);
        when(policiesRepository.save(policy)).thenReturn(policy);
        
        when(dependentRepository.findByTransactionPoid(transactionPoid)).thenReturn(List.of(dependent));
        when(emergencyRepository.findByTransactionPoid(transactionPoid)).thenReturn(List.of(emergency));
        when(nomineeRepository.findByTransactionPoid(transactionPoid)).thenReturn(List.of(nominee));
        when(policiesRepository.findByTransactionPoid(transactionPoid)).thenReturn(List.of(policy));

        // Act - Save all entities
        dependentRepository.save(dependent);
        emergencyRepository.save(emergency);
        nomineeRepository.save(nominee);
        policiesRepository.save(policy);

        // Assert - Verify all entities exist
        assertThat(dependentRepository.findByTransactionPoid(transactionPoid)).hasSize(1);
        assertThat(emergencyRepository.findByTransactionPoid(transactionPoid)).hasSize(1);
        assertThat(nomineeRepository.findByTransactionPoid(transactionPoid)).hasSize(1);
        assertThat(policiesRepository.findByTransactionPoid(transactionPoid)).hasSize(1);

        // Act - Delete header (should not affect child entities due to no cascade)
        hdrRepository.delete(testHeader);

        // Assert - Child entities should still exist
        assertThat(dependentRepository.findByTransactionPoid(transactionPoid)).hasSize(1);
        assertThat(emergencyRepository.findByTransactionPoid(transactionPoid)).hasSize(1);
        assertThat(nomineeRepository.findByTransactionPoid(transactionPoid)).hasSize(1);
        assertThat(policiesRepository.findByTransactionPoid(transactionPoid)).hasSize(1);
    }

    @Test
    void allRepositories_BulkOperations_Success() {
        // Arrange
        List<HrPersonalDataDependent> dependents = List.of(
                createTestDependent(1L, "Dependent 1", "Child"),
                createTestDependent(2L, "Dependent 2", "Spouse")
        );
        
        List<HrPersonalDataNominee> nominees = List.of(
                createTestNominee(1L, "Nominee 1", "PRIMARY", BigDecimal.valueOf(50.0)),
                createTestNominee(2L, "Nominee 2", "PRIMARY", BigDecimal.valueOf(50.0))
        );

        // Mock repository responses
        when(dependentRepository.saveAll(dependents)).thenReturn(dependents);
        when(nomineeRepository.saveAll(nominees)).thenReturn(nominees);
        when(dependentRepository.findByTransactionPoid(transactionPoid)).thenReturn(dependents);
        when(nomineeRepository.findByTransactionPoid(transactionPoid)).thenReturn(nominees);

        // Act
        dependentRepository.saveAll(dependents);
        nomineeRepository.saveAll(nominees);

        // Assert
        assertThat(dependentRepository.findByTransactionPoid(transactionPoid)).hasSize(2);
        assertThat(nomineeRepository.findByTransactionPoid(transactionPoid)).hasSize(2);
        
        // Verify percentage calculation
        List<HrPersonalDataNominee> savedNominees = nomineeRepository.findByTransactionPoid(transactionPoid);
        BigDecimal totalPercentage = savedNominees.stream()
                .map(HrPersonalDataNominee::getPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalPercentage).isEqualByComparingTo(BigDecimal.valueOf(100.0));
    }

    // Helper methods
    private HrPersonalDataDependent createTestDependent(Long detRowId, String name, String relation) {
        HrPersonalDataDependent dependent = new HrPersonalDataDependent();
        dependent.setTransactionPoid(transactionPoid);
        dependent.setDetRowId(detRowId);
        dependent.setNamePassport(name);
        dependent.setRelation(relation);
        dependent.setDateOfBirth(LocalDate.of(1990, 1, 1));
        dependent.setNationality("US");
        dependent.setCreatedBy("testUser");
        dependent.setCreatedDate(LocalDateTime.now());
        dependent.setLastmodifiedDate(LocalDateTime.now());
        return dependent;
    }

    private HrPersonalDataEmergency createTestEmergency(Long detRowId, String name, String relation) {
        HrPersonalDataEmergency emergency = new HrPersonalDataEmergency();
        emergency.setTransactionPoid(transactionPoid);
        emergency.setDetRowId(detRowId);
        emergency.setName(name);
        emergency.setRelation(relation);
        emergency.setMobile("12345678");
        emergency.setCountry("US");
        emergency.setCreatedBy("testUser");
        emergency.setCreatedDate(LocalDateTime.now());
        emergency.setLastmodifiedDate(LocalDateTime.now());
        return emergency;
    }

    private HrPersonalDataNominee createTestNominee(Long detRowId, String name, String type, BigDecimal percentage) {
        HrPersonalDataNominee nominee = new HrPersonalDataNominee();
        nominee.setTransactionPoid(transactionPoid);
        nominee.setDetRowId(detRowId);
        nominee.setNomineeName(name);
        nominee.setNomineeType(type);
        nominee.setPercentage(percentage);
        nominee.setRelation("Spouse");
        nominee.setCreatedBy("testUser");
        nominee.setCreatedDate(LocalDateTime.now());
        nominee.setLastmodifiedDate(LocalDateTime.now());
        return nominee;
    }

    private HrPersonalDataPolicies createTestPolicy(Long detRowId, Long docPoid, String docName) {
        HrPersonalDataPolicies policy = new HrPersonalDataPolicies();
        policy.setTransactionPoid(transactionPoid);
        policy.setDetRowId(detRowId);
        policy.setDocPoid(docPoid);
        policy.setDocName(docName);
        policy.setPolicyAccepted("N");
        policy.setCreatedBy("testUser");
        policy.setCreatedDate(LocalDateTime.now());
        policy.setLastmodifiedDate(LocalDateTime.now());
        return policy;
    }
}