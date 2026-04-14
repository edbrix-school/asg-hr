package com.asg.hr.personaldatasheet.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.AsgException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.personaldatasheet.dto.PersonalDataSheetRequestDto;
import com.asg.hr.personaldatasheet.entity.*;
import com.asg.hr.personaldatasheet.repository.*;
import com.asg.hr.personaldatasheet.util.PersonalDataSheetValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalDataSheetServiceImplTest {

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

    @InjectMocks private PersonalDataSheetServiceImpl service;

    @Test
    void create_success_savesEntity_setsFlags_andLogs() {
        PersonalDataSheetRequestDto req = createValidRequest();
        HrPersonalDataHdr saved = createValidEntity();

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(10L);
            uc.when(UserContext::getCompanyPoid).thenReturn(5L);
            uc.when(UserContext::getUserId).thenReturn("testUser");
            uc.when(UserContext::getDocumentId).thenReturn("800-112");

            when(repository.save(any(HrPersonalDataHdr.class))).thenReturn(saved);
            when(dependentRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
            when(emergencyRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
            when(nomineeRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
            when(policiesRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());

            var resp = service.create(req);

            ArgumentCaptor<HrPersonalDataHdr> captor = ArgumentCaptor.forClass(HrPersonalDataHdr.class);
            verify(repository).save(captor.capture());
            HrPersonalDataHdr toSave = captor.getValue();
            assertThat(toSave.getGroupPoid()).isEqualTo(10L);
            assertThat(toSave.getCompanyPoid()).isEqualTo(5L);
            assertThat(toSave.getEmployeeNamePassport()).isEqualTo("John Doe");
            assertThat(toSave.getStatus()).isEqualTo("DRAFT");

            verify(validator).validateRequest(req);
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "800-112", "1");

            assertThat(resp.getTransactionPoid()).isEqualTo(1L);
            assertThat(resp.getEmployeeNamePassport()).isEqualTo("John Doe");
        }
    }

    @Test
    void getById_whenMissing_throws() {
        when(repository.findByTransactionPoidAndNotDeleted(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(AsgException.class)
                .hasMessageContaining("Personal data sheet not found with ID: 1");
    }

    @Test
    void getById_success_mapsToResponse() {
        HrPersonalDataHdr entity = createValidEntity();
        when(repository.findByTransactionPoidAndNotDeleted(1L)).thenReturn(Optional.of(entity));
        when(dependentRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());
        when(emergencyRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());
        when(nomineeRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());
        when(policiesRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());

        var resp = service.getById(1L);
        assertThat(resp.getTransactionPoid()).isEqualTo(1L);
        assertThat(resp.getEmployeeNamePassport()).isEqualTo("John Doe");
        assertThat(resp.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    void update_whenMissing_throws() {
        PersonalDataSheetRequestDto req = createValidRequest();

        when(repository.findByTransactionPoidAndNotDeleted(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(5L, req))
                .isInstanceOf(AsgException.class)
                .hasMessageContaining("Personal data sheet not found with ID: 5");
    }

    @Test
    void update_success_savesAndLogsChanges() {
        PersonalDataSheetRequestDto req = createValidRequest();
        req.setEmployeeNamePassport("Jane Smith");

        HrPersonalDataHdr existing = createValidEntity();
        HrPersonalDataHdr saved = createValidEntity();
        saved.setEmployeeNamePassport("Jane Smith");

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserId).thenReturn("testUser");
            uc.when(UserContext::getDocumentId).thenReturn("800-112");

            when(repository.findByTransactionPoidAndNotDeleted(1L)).thenReturn(Optional.of(existing));
            when(repository.save(any(HrPersonalDataHdr.class))).thenReturn(saved);
            when(dependentRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());
            when(emergencyRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());
            when(nomineeRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());
            when(policiesRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());

            var resp = service.update(1L, req);

            verify(validator).validateRequest(req);
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.MODIFIED, "800-112", "1");

            assertThat(resp.getEmployeeNamePassport()).isEqualTo("Jane Smith");
        }
    }

    @Test
    void delete_whenMissing_throws() {
        DeleteReasonDto reason = mock(DeleteReasonDto.class);
        when(repository.findByTransactionPoidAndNotDeleted(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(7L, reason))
                .isInstanceOf(AsgException.class)
                .hasMessageContaining("Personal data sheet not found with ID: 7");
    }

    @Test
    void delete_success_callsDocumentDeleteService() {
        DeleteReasonDto reason = mock(DeleteReasonDto.class);
        HrPersonalDataHdr entity = createValidEntity();

        when(repository.findByTransactionPoidAndNotDeleted(1L)).thenReturn(Optional.of(entity));

        service.delete(1L, reason);

        verify(documentDeleteService).deleteDocument(
                eq(1L),
                eq("HR_PERSONAL_DATA_HDR"),
                eq("TRANSACTION_POID"),
                same(reason),
                any(LocalDate.class)
        );
    }

    @Test
    void list_delegatesToSearch_andWrapsPage() {
        FilterRequestDto request = mock(FilterRequestDto.class);
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult raw = mock(RawSearchResult.class);
        when(documentSearchService.resolveOperator(request)).thenReturn("OR");
        when(documentSearchService.resolveIsDeleted(request)).thenReturn("N");
        when(documentSearchService.resolveFilters(request)).thenReturn(List.of());

        when(raw.records()).thenReturn(List.of(Map.of("TRANSACTION_POID", 1L)));
        when(raw.totalRecords()).thenReturn(1L);
        when(raw.displayFields()).thenReturn(Map.of("EMPLOYEE_NAME_PASSPORT", "Employee Name"));

        when(documentSearchService.search(eq("800-112"), anyList(), eq("OR"), eq(pageable), eq("N"),
                eq("EMPLOYEE_NAME_PASSPORT"), eq("TRANSACTION_POID")))
                .thenReturn(raw);

        Map<String, Object> result = service.list("800-112", request, pageable);

        assertThat(result).isNotNull();
        verify(documentSearchService).search(eq("800-112"), anyList(), eq("OR"), eq(pageable), eq("N"),
                eq("EMPLOYEE_NAME_PASSPORT"), eq("TRANSACTION_POID"));
    }

    @Test
    void getLoginUserEmployee_delegatesToProcedureRepository() {
        Map<String, Object> expectedResult = Map.of("employeePoid", 123L, "employeeName", "John Doe");

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserPoid).thenReturn(456L);

            when(procedureRepository.getLoginUserEmployeeId(456L)).thenReturn(expectedResult);

            Map<String, Object> result = service.getLoginUserEmployee();

            assertThat(result).isEqualTo(expectedResult);
            verify(procedureRepository).getLoginUserEmployeeId(456L);
        }
    }

    @Test
    void loadUserPolicies_delegatesToProcedureRepository() {
        Long employeePoid = 123L;
        List<Map<String, Object>> expectedPolicies = List.of(
                Map.of("policyId", 1L, "policyName", "Privacy Policy")
        );

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(10L);
            uc.when(UserContext::getCompanyPoid).thenReturn(5L);
            uc.when(UserContext::getUserPoid).thenReturn(456L);
            uc.when(UserContext::getDocumentId).thenReturn("800-112");

            when(procedureRepository.loadUserPolicies(10L, 5L, "456", "800-112", null))
                    .thenReturn(expectedPolicies);

            List<Map<String, Object>> result = service.loadUserPolicies(employeePoid);

            assertThat(result).isEqualTo(expectedPolicies);
            verify(procedureRepository).loadUserPolicies(10L, 5L, "456", "800-112", null);
        }
    }

    private PersonalDataSheetRequestDto createValidRequest() {
        PersonalDataSheetRequestDto request = new PersonalDataSheetRequestDto();
        request.setEmployeePoid(1L);
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
        entity.setGroupPoid(10L);
        entity.setCompanyPoid(5L);
        entity.setTransactionDate(LocalDate.now());
        entity.setEmployeePoid(1L);
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
        return entity;
    }
}