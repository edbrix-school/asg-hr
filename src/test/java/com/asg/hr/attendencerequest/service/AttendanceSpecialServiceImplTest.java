package com.asg.hr.attendencerequest.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.attendencerequest.dto.AttendanceRequestDto;
import com.asg.hr.attendencerequest.dto.AttendanceResponseDto;
import com.asg.hr.attendencerequest.entity.AttendanceEntity;
import com.asg.hr.attendencerequest.repository.AttendanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class AttendanceSpecialServiceImplTest {

    private static final String ATTENDANCE_DATE_FIELD = "ATTENDANCE_DATE";
    private static final String ATTENDANCE_POID_FIELD = "TRANSACTION_POID";
    private static final String HR_ATTENDANCE_SPECIAL_REQ = "HR_ATTENDANCE_SPECIAL_REQ";

    @Mock
    private AttendanceRepository repository;

    @Mock
    private DocumentSearchService documentSearchService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private LoggingService loggingService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AttendanceSpecialServiceImpl service;

    // ---------- CREATE ----------
    @Test
    void create_whenEmployeeMissing_throwsValidationException() {
        AttendanceRequestDto dto = new AttendanceRequestDto();
        dto.setAttendanceDate(LocalDate.of(2024, 1, 1));
        dto.setExceptionType("E1");
        dto.setReason("R1");
        dto.setHodRemarks("H1");

        assertThrows(ValidationException.class, () -> service.create(dto));
        verifyNoInteractions(repository, jdbcTemplate, loggingService);
    }

    @Test
    void create_whenAttendanceDateMissing_throwsValidationException() {
        AttendanceRequestDto dto = new AttendanceRequestDto();
        dto.setEmployeePoid(1L);
        dto.setExceptionType("E1");
        dto.setReason("R1");
        dto.setHodRemarks("H1");

        assertThrows(ValidationException.class, () -> service.create(dto));
        verifyNoInteractions(repository, jdbcTemplate, loggingService);
    }

    @Test
    void create_whenExceptionTypeMissing_throwsValidationException() {
        AttendanceRequestDto dto = new AttendanceRequestDto();
        dto.setEmployeePoid(1L);
        dto.setAttendanceDate(LocalDate.of(2024, 1, 1));
        dto.setReason("R1");
        dto.setHodRemarks("H1");

        assertThrows(ValidationException.class, () -> service.create(dto));
        verifyNoInteractions(repository, jdbcTemplate, loggingService);
    }

    @Test
    void create_whenReasonMissing_throwsValidationException() {
        AttendanceRequestDto dto = new AttendanceRequestDto();
        dto.setEmployeePoid(1L);
        dto.setAttendanceDate(LocalDate.of(2024, 1, 1));
        dto.setExceptionType("E1");
        dto.setHodRemarks("H1");

        assertThrows(ValidationException.class, () -> service.create(dto));
        verifyNoInteractions(repository, jdbcTemplate, loggingService);
    }

    @Test
    void create_whenSpReturnsError_throwsValidationException() {
        AttendanceRequestDto dto = new AttendanceRequestDto();
        dto.setEmployeePoid(1L);
        dto.setAttendanceDate(LocalDate.of(2024, 1, 1));
        dto.setExceptionType("E1");
        dto.setReason("R1");

        when(jdbcTemplate.execute((ConnectionCallback<String>) any(ConnectionCallback.class))).thenReturn("ERROR: invalid");

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(100L);
            uc.when(UserContext::getDocumentId).thenReturn("DOC1");

            assertThrows(ValidationException.class, () -> service.create(dto));
        }

        verifyNoInteractions(repository, loggingService);
    }

    @Test
    void create_success_setsDefaults_andLogs() {
        AttendanceRequestDto dto = new AttendanceRequestDto();
        dto.setEmployeePoid(11L);
        dto.setAttendanceDate(LocalDate.of(2024, 2, 2));
        dto.setExceptionType("E2");
        dto.setReason("R2");
        dto.setHodRemarks("H2");
        dto.setStatus(null); // cover default branch ("IN_PROGRESS")

        AttendanceEntity saved = AttendanceEntity.builder()
                .attendancePoid(99L)
                .employeePoid(11L)
                .attendanceDate(LocalDate.of(2024, 2, 2))
                .exceptionType("E2")
                .reason("R2")
                .hodRemarks("H2")
                .status("IN_PROGRESS")
                .deleted("N")
                .groupPoid(100L)
                .build();

        when(jdbcTemplate.execute((ConnectionCallback<String>) any(ConnectionCallback.class))).thenReturn("OK");
        when(repository.save(any(AttendanceEntity.class))).thenReturn(saved);

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(100L);
            uc.when(UserContext::getDocumentId).thenReturn("DOC1");

            var resp = service.create(dto);

            ArgumentCaptor<AttendanceEntity> captor = ArgumentCaptor.forClass(AttendanceEntity.class);
            verify(repository).save(captor.capture());
            AttendanceEntity toSave = captor.getValue();
            assertThat(toSave.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(toSave.getDeleted()).isEqualTo("N");
            assertThat(toSave.getGroupPoid()).isEqualTo(100L);

            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "DOC1", "99");
            assertThat(resp.getAttendancePoid()).isEqualTo(99L);
            assertThat(resp.getEmployeePoid()).isEqualTo(11L);
        }
    }

    // ---------- LIST ----------
    @Test
    void list_success_delegatesToDocumentSearch_andWraps() {
        FilterRequestDto request = mock(FilterRequestDto.class);
        Pageable pageable = PageRequest.of(0, 10);

        when(documentSearchService.resolveOperator(request)).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(request)).thenReturn("N");
        when(documentSearchService.resolveFilters(request)).thenReturn(List.of(mock(FilterDto.class)));

        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of(ATTENDANCE_POID_FIELD, 1L)),
                Map.of(ATTENDANCE_DATE_FIELD, "Attendance Date"),
                1L
        );

        when(documentSearchService.search(
                eq("DOC1"),
                anyList(),
                eq("AND"),
                eq(pageable),
                eq("N"),
                eq(ATTENDANCE_DATE_FIELD),
                eq(ATTENDANCE_POID_FIELD)
        )).thenReturn(raw);

        Map<String, Object> result = service.list("DOC1", request, pageable);

        assertThat(result).isNotNull();
        verify(documentSearchService).search(
                eq("DOC1"),
                anyList(),
                eq("AND"),
                eq(pageable),
                eq("N"),
                eq(ATTENDANCE_DATE_FIELD),
                eq(ATTENDANCE_POID_FIELD)
        );
    }

    // ---------- GET BY ID ----------
    @Test
    void getById_whenMissing_throws() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(1L));
    }

    @Test
    void getById_success_mapsAndDoesNotLogViewed() {
        AttendanceEntity entity = AttendanceEntity.builder()
                .attendancePoid(1L)
                .employeePoid(2L)
                .attendanceDate(LocalDate.of(2024, 1, 1))
                .exceptionType("E1")
                .reason("R1")
                .hodRemarks("H1")
                .status("IN_PROGRESS")
                .groupPoid(100L)
                .deleted("N")
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        AttendanceResponseDto resp = service.getById(1L);

        assertThat(resp.getAttendancePoid()).isEqualTo(1L);
        assertThat(resp.getEmployeePoid()).isEqualTo(2L);
        verifyNoInteractions(loggingService);
    }

    // ---------- UPDATE ----------
    @Test
    void update_whenEmployeeMissing_throwsValidationException() {
        AttendanceRequestDto dto = new AttendanceRequestDto();
        dto.setAttendanceDate(LocalDate.of(2024, 1, 1));
        dto.setExceptionType("E1");
        dto.setReason("R1");

        assertThrows(ValidationException.class, () -> service.update(1L, dto));
        verifyNoInteractions(repository, jdbcTemplate, loggingService);
    }

    @Test
    void update_whenAttendanceDateMissing_throwsValidationException() {
        AttendanceRequestDto dto = new AttendanceRequestDto();
        dto.setEmployeePoid(1L);
        dto.setExceptionType("E1");
        dto.setReason("R1");

        assertThrows(ValidationException.class, () -> service.update(1L, dto));
        verifyNoInteractions(repository, jdbcTemplate, loggingService);
    }

    @Test
    void update_whenExceptionTypeMissing_throwsValidationException() {
        AttendanceRequestDto dto = new AttendanceRequestDto();
        dto.setEmployeePoid(1L);
        dto.setAttendanceDate(LocalDate.of(2024, 1, 1));
        dto.setReason("R1");

        assertThrows(ValidationException.class, () -> service.update(1L, dto));
        verifyNoInteractions(repository, jdbcTemplate, loggingService);
    }

    @Test
    void update_whenReasonMissing_throwsValidationException() {
        AttendanceRequestDto dto = new AttendanceRequestDto();
        dto.setEmployeePoid(1L);
        dto.setAttendanceDate(LocalDate.of(2024, 1, 1));
        dto.setExceptionType("E1");

        assertThrows(ValidationException.class, () -> service.update(1L, dto));
        verifyNoInteractions(repository, jdbcTemplate, loggingService);
    }

    @Test
    void update_whenSpReturnsError_throwsValidationException() {
        AttendanceRequestDto dto = new AttendanceRequestDto();
        dto.setEmployeePoid(11L);
        dto.setAttendanceDate(LocalDate.of(2024, 2, 2));
        dto.setExceptionType("E2");
        dto.setReason("R2");

        when(jdbcTemplate.execute((ConnectionCallback<String>) any(ConnectionCallback.class))).thenReturn("ERROR: invalid");

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getDocumentId).thenReturn("DOC1");
            uc.when(UserContext::getGroupPoid).thenReturn(100L);

            assertThrows(ValidationException.class, () -> service.update(1L, dto));
        }

        verifyNoInteractions(repository, loggingService);
    }

    @Test
    void update_whenMissing_throws() {
        AttendanceRequestDto dto = new AttendanceRequestDto();
        dto.setEmployeePoid(11L);
        dto.setAttendanceDate(LocalDate.of(2024, 2, 2));
        dto.setExceptionType("E2");
        dto.setReason("R2");

        when(jdbcTemplate.execute((ConnectionCallback<String>) any(ConnectionCallback.class))).thenReturn("OK");
        when(repository.findById(1L)).thenReturn(Optional.empty());

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getDocumentId).thenReturn("DOC1");

            assertThrows(ResourceNotFoundException.class, () -> service.update(1L, dto));
        }
    }

    @Test
    void update_success_preservesActive_whenDtoActiveNull_andLogsChanges() {
        AttendanceRequestDto dto = new AttendanceRequestDto();
        dto.setEmployeePoid(11L);
        dto.setAttendanceDate(LocalDate.of(2024, 2, 2));
        dto.setExceptionType("E2");
        dto.setReason("R2");
        dto.setHodRemarks("H2");
        dto.setStatus(null); // cover preserve-status branch

        AttendanceEntity existing = AttendanceEntity.builder()
                .attendancePoid(1L)
                .employeePoid(10L)
                .attendanceDate(LocalDate.of(2024, 1, 1))
                .exceptionType("E1")
                .reason("R1")
                .hodRemarks("H1")
                .status("IN_PROGRESS")
                .groupPoid(100L)
                .deleted("N")
                .build();

        when(jdbcTemplate.execute((ConnectionCallback<String>) any(ConnectionCallback.class))).thenReturn("OK");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(AttendanceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getDocumentId).thenReturn("DOC1");

            AttendanceResponseDto resp = service.update(1L, dto);

            ArgumentCaptor<AttendanceEntity> captor = ArgumentCaptor.forClass(AttendanceEntity.class);
            verify(repository).save(captor.capture());
            AttendanceEntity saved = captor.getValue();
            assertThat(saved.getEmployeePoid()).isEqualTo(11L);
            assertThat(saved.getExceptionType()).isEqualTo("E2");
            assertThat(saved.getStatus()).isEqualTo("IN_PROGRESS"); // preserved

            verify(loggingService).logChanges(
                    any(AttendanceEntity.class),
                    any(AttendanceEntity.class),
                    eq(AttendanceEntity.class),
                    eq("DOC1"),
                    eq("1"),
                    eq(LogDetailsEnum.MODIFIED),
                    eq(ATTENDANCE_POID_FIELD)
            );

            assertThat(resp.getReason()).isEqualTo("R2");
        }
    }

    // ---------- DELETE ----------
    @Test
    void delete_whenMissing_throws() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(7L, new DeleteReasonDto()));
    }

    @Test
    void delete_success_callsDocumentDeleteService() {
        DeleteReasonDto reason = new DeleteReasonDto();
        reason.setDeleteReason("no longer needed");

        when(repository.findById(7L)).thenReturn(Optional.of(
                AttendanceEntity.builder().attendancePoid(7L).build()
        ));

        service.delete(7L, reason);

        verify(documentDeleteService).deleteDocument(
                eq(7L),
                eq(HR_ATTENDANCE_SPECIAL_REQ),
                eq(ATTENDANCE_POID_FIELD),
                eq(reason),
                isNull()
        );
    }
}

