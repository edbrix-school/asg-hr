package com.asg.hr.personaldatasheet.service;

import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.personaldatasheet.dto.PersonalDataSheetRequestDto;
import com.asg.hr.personaldatasheet.entity.*;
import com.asg.hr.personaldatasheet.repository.*;
import com.asg.hr.personaldatasheet.util.PersonalDataSheetValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalDataSheetServiceImplChildEntityTest {

    @Mock private HrPersonalDataHdrRepository repository;
    @Mock private HrPersonalDataDependentRepository dependentRepository;
    @Mock private HrPersonalDataEmergencyRepository emergencyRepository;
    @Mock private HrPersonalDataNomineeRepository nomineeRepository;
    @Mock private HrPersonalDataPoliciesRepository policiesRepository;
    @Mock private LoggingService loggingService;
    @Mock private PersonalDataSheetValidator validator;

    @InjectMocks private PersonalDataSheetServiceImpl service;

    private HrPersonalDataHdr testHeader;
    private PersonalDataSheetRequestDto testRequest;

    @BeforeEach
    void setUp() {
        testHeader = createTestHeader();
        testRequest = createTestRequest();
    }

    @Test
    void create_WithDependents_ShouldSaveAllDependents() {
        PersonalDataSheetRequestDto.DependentDto dependent1 = createDependentDto(null, "John Doe", "Child");
        PersonalDataSheetRequestDto.DependentDto dependent2 = createDependentDto(null, "Jane Doe", "Spouse");
        testRequest.setDependents(Arrays.asList(dependent1, dependent2));

        when(repository.save(any(HrPersonalDataHdr.class))).thenReturn(testHeader);
        when(dependentRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(emergencyRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(nomineeRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(policiesRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());

        try (MockedStatic<UserContext> uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(1L);
            uc.when(UserContext::getCompanyPoid).thenReturn(1L);
            uc.when(UserContext::getUserId).thenReturn("testUser");
            uc.when(UserContext::getDocumentId).thenReturn("800-112");

            service.create(testRequest);

            ArgumentCaptor<List<HrPersonalDataDependent>> captor = ArgumentCaptor.forClass(List.class);
            verify(dependentRepository).saveAll(captor.capture());
            
            List<HrPersonalDataDependent> savedDependents = captor.getValue();
            assertThat(savedDependents).hasSize(2);
            assertThat(savedDependents.get(0).getNamePassport()).isEqualTo("John Doe");
            assertThat(savedDependents.get(1).getNamePassport()).isEqualTo("Jane Doe");
        }
    }

    @Test
    void update_WithDependentActions_ShouldHandleAllActionTypes() {
        PersonalDataSheetRequestDto.DependentDto newDependent = createDependentDto(null, "New Dependent", "Child");
        newDependent.setActionType("ISCREATED");
        
        PersonalDataSheetRequestDto.DependentDto updatedDependent = createDependentDto(1L, "Updated Dependent", "Spouse");
        updatedDependent.setActionType("ISUPDATED");
        
        PersonalDataSheetRequestDto.DependentDto deletedDependent = createDependentDto(2L, "Deleted Dependent", "Child");
        deletedDependent.setActionType("ISDELETED");
        
        testRequest.setDependents(Arrays.asList(newDependent, updatedDependent, deletedDependent));

        HrPersonalDataDependent existingDependent1 = createTestDependent(1L, "Old Name", "Child");
        HrPersonalDataDependent existingDependent2 = createTestDependent(2L, "To Delete", "Child");
        
        when(repository.findByTransactionPoidAndNotDeleted(1L)).thenReturn(Optional.of(testHeader));
        when(repository.save(any(HrPersonalDataHdr.class))).thenReturn(testHeader);
        when(dependentRepository.findByTransactionPoid(1L)).thenReturn(Arrays.asList(existingDependent1, existingDependent2));
        when(emergencyRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(nomineeRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(policiesRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());

        try (MockedStatic<UserContext> uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserId).thenReturn("testUser");
            uc.when(UserContext::getDocumentId).thenReturn("800-112");

            service.update(1L, testRequest);

            verify(dependentRepository, times(2)).saveAll(any());
            verify(dependentRepository).delete(existingDependent2);
        }
    }

    @Test
    void create_WithEmergencyContacts_ShouldSaveAllContacts() {
        PersonalDataSheetRequestDto.EmergencyContactDto contact1 = createEmergencyContactDto(null, "Emergency 1", "Friend");
        PersonalDataSheetRequestDto.EmergencyContactDto contact2 = createEmergencyContactDto(null, "Emergency 2", "Relative");
        testRequest.setEmergencyContacts(Arrays.asList(contact1, contact2));

        when(repository.save(any(HrPersonalDataHdr.class))).thenReturn(testHeader);
        when(dependentRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(emergencyRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(nomineeRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(policiesRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());

        try (MockedStatic<UserContext> uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(1L);
            uc.when(UserContext::getCompanyPoid).thenReturn(1L);
            uc.when(UserContext::getUserId).thenReturn("testUser");
            uc.when(UserContext::getDocumentId).thenReturn("800-112");

            service.create(testRequest);

            ArgumentCaptor<List<HrPersonalDataEmergency>> captor = ArgumentCaptor.forClass(List.class);
            verify(emergencyRepository).saveAll(captor.capture());
            
            List<HrPersonalDataEmergency> savedContacts = captor.getValue();
            assertThat(savedContacts).hasSize(2);
            assertThat(savedContacts.get(0).getName()).isEqualTo("Emergency 1");
            assertThat(savedContacts.get(1).getName()).isEqualTo("Emergency 2");
        }
    }

    @Test
    void create_WithNominees_ShouldSaveAllNominees() {
        PersonalDataSheetRequestDto.NomineeDto nominee1 = createNomineeDto(null, "Nominee 1", "PRIMARY", 60.0);
        PersonalDataSheetRequestDto.NomineeDto nominee2 = createNomineeDto(null, "Nominee 2", "PRIMARY", 40.0);
        testRequest.setNominees(Arrays.asList(nominee1, nominee2));

        when(repository.save(any(HrPersonalDataHdr.class))).thenReturn(testHeader);
        when(dependentRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(emergencyRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(nomineeRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(policiesRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());

        try (MockedStatic<UserContext> uc = mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(1L);
            uc.when(UserContext::getCompanyPoid).thenReturn(1L);
            uc.when(UserContext::getUserId).thenReturn("testUser");
            uc.when(UserContext::getDocumentId).thenReturn("800-112");

            service.create(testRequest);

            ArgumentCaptor<List<HrPersonalDataNominee>> captor = ArgumentCaptor.forClass(List.class);
            verify(nomineeRepository).saveAll(captor.capture());
            
            List<HrPersonalDataNominee> savedNominees = captor.getValue();
            assertThat(savedNominees).hasSize(2);
            assertThat(savedNominees.get(0).getNomineeName()).isEqualTo("Nominee 1");
            assertThat(savedNominees.get(0).getPercentage()).isEqualByComparingTo(BigDecimal.valueOf(60.0));
        }
    }

    private HrPersonalDataHdr createTestHeader() {
        HrPersonalDataHdr header = new HrPersonalDataHdr();
        header.setTransactionPoid(1L);
        header.setEmployeePoid(123L);
        header.setEmployeeNamePassport("Test Employee");
        return header;
    }

    private PersonalDataSheetRequestDto createTestRequest() {
        PersonalDataSheetRequestDto request = new PersonalDataSheetRequestDto();
        request.setEmployeePoid(123L);
        request.setEmployeeNamePassport("Test Employee");
        return request;
    }

    private PersonalDataSheetRequestDto.DependentDto createDependentDto(Long detRowId, String name, String relation) {
        PersonalDataSheetRequestDto.DependentDto dependent = new PersonalDataSheetRequestDto.DependentDto();
        dependent.setDetRowId(detRowId);
        dependent.setNamePassport(name);
        dependent.setRelation(relation);
        dependent.setDateOfBirth(LocalDate.of(2000, 1, 1));
        return dependent;
    }

    private PersonalDataSheetRequestDto.EmergencyContactDto createEmergencyContactDto(Long detRowId, String name, String relation) {
        PersonalDataSheetRequestDto.EmergencyContactDto contact = new PersonalDataSheetRequestDto.EmergencyContactDto();
        contact.setDetRowId(detRowId);
        contact.setName(name);
        contact.setRelation(relation);
        contact.setMobile("12345678");
        return contact;
    }

    private PersonalDataSheetRequestDto.NomineeDto createNomineeDto(Long detRowId, String name, String type, Double percentage) {
        PersonalDataSheetRequestDto.NomineeDto nominee = new PersonalDataSheetRequestDto.NomineeDto();
        nominee.setDetRowId(detRowId);
        nominee.setNomineeName(name);
        nominee.setNomineeType(type);
        nominee.setPercentage(percentage);
        return nominee;
    }

    private HrPersonalDataDependent createTestDependent(Long detRowId, String name, String relation) {
        HrPersonalDataDependent dependent = new HrPersonalDataDependent();
        dependent.setDetRowId(detRowId);
        dependent.setNamePassport(name);
        dependent.setRelation(relation);
        dependent.setTransactionPoid(1L);
        return dependent;
    }
}