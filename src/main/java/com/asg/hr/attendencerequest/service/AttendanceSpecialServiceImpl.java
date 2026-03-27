package com.asg.hr.attendencerequest.service;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.attendencerequest.dto.AttendanceRequestDto;
import com.asg.hr.attendencerequest.dto.AttendanceResponseDto;
import com.asg.hr.attendencerequest.entity.AttendanceEntity;
import com.asg.hr.attendencerequest.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttendanceSpecialServiceImpl implements AttendanceSpecialService {

    private static final String RESOURCE_NAME = "Attendance";
    private static final String ATTENDANCE_DATE_FIELD = "ATTENDANCE_DATE";
    private static final String ATTENDANCE_POID_FIELD = "TRANSACTION_POID";

    private final AttendanceRepository repository;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    private final JdbcTemplate jdbcTemplate;

    // ================= CREATE =================
    @Override
    @Transactional
    public AttendanceResponseDto create(AttendanceRequestDto dto) {

        validate(dto);
        callValidationSP(dto);

        AttendanceEntity entity = AttendanceEntity.builder()
                .employeePoid(dto.getEmployeePoid())
                .attendanceDate(dto.getAttendanceDate())
                .exceptionType(dto.getExceptionType())
                .reason(dto.getReason())
                .hodRemarks(dto.getHodRemarks())
                .groupPoid(UserContext.getGroupPoid())
                .status(dto.getStatus() != null ? dto.getStatus() : "IN_PROGRESS")
                .deleted("N")
                .build();

        entity = repository.save(entity);

        loggingService.createLogSummaryEntry(
                LogDetailsEnum.CREATED,
                UserContext.getDocumentId(),
                entity.getAttendancePoid().toString()
        );

        return map(entity);
    }

    // ================= LIST =================
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> list(String docId, FilterRequestDto request, Pageable pageable) {

        String operator = documentSearchService.resolveOperator(request);
        String isDeleted = documentSearchService.resolveIsDeleted(request);
        List<FilterDto> filters = documentSearchService.resolveFilters(request);

        RawSearchResult raw = documentSearchService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                ATTENDANCE_DATE_FIELD,
                ATTENDANCE_POID_FIELD
        );

        Page<Map<String, Object>> page =
                new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    // ================= GET =================
    @Override
    @Transactional(readOnly = true)
    public AttendanceResponseDto getById(Long id) {

        AttendanceEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, "id", id));

        return map(entity);
    }

    // ================= UPDATE =================
    @Override
    @Transactional
    public AttendanceResponseDto update(Long id, AttendanceRequestDto dto) {

        validate(dto);
        callValidationSP(dto);

        AttendanceEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, "id", id));

        AttendanceEntity old = AttendanceEntity.builder()
                .attendancePoid(entity.getAttendancePoid())
                .employeePoid(entity.getEmployeePoid())
                .attendanceDate(entity.getAttendanceDate())
                .exceptionType(entity.getExceptionType())
                .reason(entity.getReason())
                .hodRemarks(entity.getHodRemarks())
                .status(entity.getStatus())
                .build();

        entity.setEmployeePoid(dto.getEmployeePoid());
        entity.setAttendanceDate(dto.getAttendanceDate());
        entity.setExceptionType(dto.getExceptionType());
        entity.setReason(dto.getReason());
        entity.setHodRemarks(dto.getHodRemarks());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : entity.getStatus());

        entity = repository.save(entity);

        loggingService.logChanges(
                old,
                entity,
                AttendanceEntity.class,
                UserContext.getDocumentId(),
                id.toString(),
                LogDetailsEnum.MODIFIED,
                ATTENDANCE_POID_FIELD
        );

        return map(entity);
    }

    // ================= DELETE =================
    @Override
    @Transactional
    public void delete(Long id, DeleteReasonDto reason) {

        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, "id", id));

        documentDeleteService.deleteDocument(
                id,
                "HR_ATTENDANCE_SPECIAL_REQ",
                ATTENDANCE_POID_FIELD,
                reason,
                null
        );
    }

    // ================= VALIDATION =================
    private void validate(AttendanceRequestDto dto) {
        if (dto.getEmployeePoid() == null) throw new ValidationException("Employee required");
        if (dto.getAttendanceDate() == null) throw new ValidationException("Date required");
        if (dto.getExceptionType() == null) throw new ValidationException("Exception type required");
        if (dto.getReason() == null) throw new ValidationException("Reason required");
    }

    private void callValidationSP(AttendanceRequestDto dto) {

        String result = jdbcTemplate.execute((Connection conn) -> {
            CallableStatement cs = conn.prepareCall(
                    "BEGIN PROC_HR_ATT_SPECIAL_VALIDATE(?,?,?,?,?,?); END;"
            );

            cs.setString(1, UserContext.getDocumentId());
            cs.setNull(2, Types.NUMERIC);

            cs.setLong(3, dto.getEmployeePoid());
            cs.setDate(4, Date.valueOf(dto.getAttendanceDate()));
            cs.setString(5, dto.getExceptionType());
            cs.registerOutParameter(6, Types.VARCHAR);

            cs.execute();

            return cs.getString(6);
        });

        if (result.startsWith("ERROR")) {
            throw new ValidationException(result);
        }
    }

    // ================= MAPPER =================
    private AttendanceResponseDto map(AttendanceEntity e) {
        return AttendanceResponseDto.builder()
                .attendancePoid(e.getAttendancePoid())
                .employeePoid(e.getEmployeePoid())
                .attendanceDate(e.getAttendanceDate())
                .exceptionType(e.getExceptionType())
                .reason(e.getReason())
                .hodRemarks(e.getHodRemarks())
                .status(e.getStatus())
                .build();
    }
}