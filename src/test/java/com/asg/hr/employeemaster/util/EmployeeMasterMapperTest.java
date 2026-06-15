package com.asg.hr.employeemaster.util;

import com.asg.common.lib.security.util.UserContext;
import com.asg.hr.employeemaster.dto.EmployeeMasterRequestDto;
import com.asg.hr.employeemaster.dto.EmployeeMasterResponseDto;
import com.asg.hr.employeemaster.entity.*;
import com.asg.hr.employeemaster.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeMasterMapperTest {

    private MockedStatic<UserContext> userContextMock;

    @Mock private HrEmployeeDependentRepository dependentRepository;
    @Mock private HrEmpDepndtsLmraDtlsRepository lmraRepository;
    @Mock private HrEmployeeDocumentDtlRepository documentRepository;
    @Mock private HrEmployeeExperienceDtlRepository experienceRepository;
    @Mock private HrEmployeeLeaveHistoryRepository leaveHistoryRepository;

    @InjectMocks
    private EmployeeMasterMapper mapper;

    @BeforeEach
    void setUp() {
        userContextMock = mockStatic(UserContext.class);
        userContextMock.when(UserContext::getUserPoid).thenReturn(99L);
    }

    @AfterEach
    void tearDown() {
        if (userContextMock != null) userContextMock.close();
    }

    @Test
    void toResponseDto_mapsHeaderAndChildLists() {
        HrEmployeeMaster master = new HrEmployeeMaster();
        master.setEmployeePoid(1L);
        master.setEmployeeCode("E1");
        master.setEmployeeName("A");
        master.setEmployeeName2("B");
        master.setGroupPoid(10L);
        master.setCompanyPoid(20L);
        master.setJoinDate(LocalDate.of(2026, 1, 1));
        master.setActive("Y");
        master.setDeleted("N");
        master.setCreatedBy("u");
        master.setCreatedDate(LocalDateTime.now());
        master.setLastModifiedBy("u2");
        master.setLastModifiedDate(LocalDateTime.now());

        HrEmployeeDependentsDtl dep = new HrEmployeeDependentsDtl();
        dep.setEmployeePoid(1L);
        dep.setDetRowId(1L);
        dep.setName("D");
        dep.setCreatedBy("x");
        dep.setCreatedDate(LocalDateTime.now());

        HrEmpDepndtsLmraDtls lm = new HrEmpDepndtsLmraDtls();
        lm.setEmployeePoid(1L);
        lm.setDetRowId(2L);
        lm.setPermitMonths(5);

        HrEmployeeExperienceDtl ex = new HrEmployeeExperienceDtl();
        ex.setEmployeePoid(1L);
        ex.setDetRowId(3L);
        ex.setEmployer("ACME");

        HrEmployeeDocumentDtl doc = new HrEmployeeDocumentDtl();
        doc.setEmployeePoid(1L);
        doc.setDetRowId(4L);
        doc.setDocName("Passport");

        HrEmployeeLeaveHistory lh = new HrEmployeeLeaveHistory();
        lh.setLeaveHistPoid(10L);
        lh.setDetRowId(20L);
        lh.setEmployeePoid(1L);
        lh.setLeaveStartDate(LocalDate.of(2026, 1, 10));

        when(dependentRepository.findByEmployeePoid(1L)).thenReturn(List.of(dep));
        when(lmraRepository.findByEmployeePoid(1L)).thenReturn(List.of(lm));
        when(experienceRepository.findByEmployeePoid(1L)).thenReturn(List.of(ex));
        when(documentRepository.findByEmployeePoid(1L)).thenReturn(List.of(doc));
        when(leaveHistoryRepository.findByEmployeePoid(1L)).thenReturn(List.of(lh));

        EmployeeMasterResponseDto dto = mapper.toResponseDto(master);
        assertThat(dto.getEmployeePoid()).isEqualTo(1L);
        assertThat(dto.getEmployeeCode()).isEqualTo("E1");
        assertThat(dto.getDependentsDetails()).hasSize(1);
        assertThat(dto.getLmraDetails()).hasSize(1);
        assertThat(dto.getExperienceDetails()).hasSize(1);
        assertThat(dto.getDocumentDetails()).hasSize(1);
        assertThat(dto.getLeaveHistoryDetails()).hasSize(1);
    }

    @Test
    void applyHeaderFields_setsDefaultsAndUsesUserContext() {
        HrEmployeeMaster entity = new HrEmployeeMaster();
        entity.setEmployeeCode("OLD");
        entity.setDeleted("Y");

        EmployeeMasterRequestDto req = new EmployeeMasterRequestDto();
        req.setEmployeeCode(null); // should not overwrite
        req.setFirstName("FN");
        req.setLastName("LN");
        req.setDeleted(null); // defaults to N
        req.setLifeInsurance("N");

        mapper.applyHeaderFields(entity, req);

        assertThat(entity.getEmployeeCode()).isEqualTo("OLD");
        assertThat(entity.getEmployeeName()).isEqualTo("FN");
        assertThat(entity.getEmployeeName2()).isEqualTo("LN");
        assertThat(entity.getDeleted()).isEqualTo("N");
        assertThat(entity.getLoginUserPoid()).isEqualTo(99L);
    }

    @Test
    void applyHeaderFields_overwritesEmployeeCodeAndKeepsDeletedWhenProvided() {
        HrEmployeeMaster entity = new HrEmployeeMaster();
        entity.setEmployeeCode("OLD");
        entity.setDeleted("N");

        EmployeeMasterRequestDto req = new EmployeeMasterRequestDto();
        req.setEmployeeCode("NEW");
        req.setFirstName("FN");
        req.setLastName("LN");
        req.setDeleted("Y");
        req.setLifeInsurance("N");

        mapper.applyHeaderFields(entity, req);

        assertThat(entity.getEmployeeCode()).isEqualTo("NEW");
        assertThat(entity.getDeleted()).isEqualTo("Y");
    }

    @Test
    void toResponseDto_handlesEmptyChildren() {
        HrEmployeeMaster master = new HrEmployeeMaster();
        master.setEmployeePoid(2L);
        when(dependentRepository.findByEmployeePoid(2L)).thenReturn(Collections.emptyList());
        when(lmraRepository.findByEmployeePoid(2L)).thenReturn(Collections.emptyList());
        when(experienceRepository.findByEmployeePoid(2L)).thenReturn(Collections.emptyList());
        when(documentRepository.findByEmployeePoid(2L)).thenReturn(Collections.emptyList());
        when(leaveHistoryRepository.findByEmployeePoid(2L)).thenReturn(Collections.emptyList());

        EmployeeMasterResponseDto dto = mapper.toResponseDto(master);
        assertThat(dto.getDependentsDetails()).isEmpty();
        assertThat(dto.getLmraDetails()).isEmpty();
        assertThat(dto.getExperienceDetails()).isEmpty();
        assertThat(dto.getDocumentDetails()).isEmpty();
        assertThat(dto.getLeaveHistoryDetails()).isEmpty();
    }
}

