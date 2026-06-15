package com.asg.hr.attendencerequest.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.attendencerequest.dto.AttendanceRequestDto;
import com.asg.hr.attendencerequest.service.AttendanceSpecialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("/v1/attendance-special")
@RequiredArgsConstructor
public class AttendanceSpecialController {

    private final AttendanceSpecialService service;
    private final LoggingService loggingService;

    // ================= LIST =================
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> list(@ParameterObject Pageable pageable,
                                  @RequestBody(required = false) FilterRequestDto filters) {

        return success("Attendance requests retrieved successfully",
                service.list(UserContext.getDocumentId(), filters, pageable));
    }

    // ================= CREATE =================
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AttendanceRequestDto dto) {
        return success("Attendance request created successfully", service.create(dto));
    }

    // ================= UPDATE =================
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody AttendanceRequestDto dto) {
        return success("Attendance request updated successfully", service.update(id, dto));
    }

    // ================= GET BY ID =================
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {

        var response = service.getById(id);

        loggingService.createLogSummaryEntry(
                LogDetailsEnum.VIEWED,
                UserContext.getDocumentId(),
                id.toString()
        );

        return success("Attendance request retrieved successfully", response);
    }

    // ================= DELETE =================
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @Valid @RequestBody DeleteReasonDto reason) {

        service.delete(id, reason);
        return success("Attendance request deleted successfully", null);
    }
}