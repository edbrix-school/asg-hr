package com.asg.hr.attendancemonthly.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.response.ApiResponse;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.attendancemonthly.dto.*;
import com.asg.hr.attendancemonthly.service.HrAttendanceMonthlyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/attendance-monthly")
@Tag(name = "Attendance Monthly", description = "APIs for managing Attendance Monthly records")
public class HrAttendanceMonthlyController {

    private final HrAttendanceMonthlyService attendanceService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> getAttendanceList(@RequestBody(required = false) FilterRequestDto filterRequest,
                                               @ParameterObject Pageable pageable
    ) {
        Map<String, Object> attendancePage = attendanceService.getAllAttendanceWithFilters(UserContext.getDocumentId(), filterRequest, pageable);
        return ApiResponse.success("Attendance list retrieved successfully", attendancePage);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    public ResponseEntity<?> getAttendanceById(
            @PathVariable("id") @NotNull @Positive Long transactionPoid) {
        
        HrAttendanceMonthlyResponse response = attendanceService.getAttendanceSummary(transactionPoid);
        
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        
        return ApiResponse.success("Attendance record retrieved successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAttendance(@PathVariable @NotNull @Positive Long id,
                                               @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        attendanceService.deleteAttendance(id, deleteReasonDto);
        return ApiResponse.success("Attendance record deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createAttendance(@Valid @RequestBody HrAttendanceMonthlyRequest request) {
        HrAttendanceMonthlyResponse response = attendanceService.saveAttendance(request);
        return ApiResponse.success("Attendance record created successfully", response);
    }

    @Operation(summary = "Update Attendance record")
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updateAttendance(
            @PathVariable Long transactionPoid,
            @Valid @RequestBody HrAttendanceMonthlyUpdateRequest request
    ) {
        HrAttendanceMonthlyResponse response = attendanceService.updateAttendance(transactionPoid, request);
        return ApiResponse.success("Attendance record updated successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/{id}/load")
    public ResponseEntity<?> loadAttendance(@PathVariable Long id, @RequestParam(defaultValue = "Y") String lateDeductionCheck) {
        HrAttendanceMonthlyLoadAttendanceDto response = attendanceService.loadAndProcessAttendance(id, lateDeductionCheck);
        return ApiResponse.success("Attendance loaded and processed successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}/unload")
    public ResponseEntity<?> unloadAttendance(@PathVariable Long id) {
        attendanceService.unloadAttendance(id);
        return ApiResponse.success("Attendance details unloaded successfully");
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @GetMapping("/calculate-dates")
    public ResponseEntity<?> calculateDates(@RequestParam("fromDate") LocalDate fromDate) {
        HrAttendanceMonthlyDateParams result = attendanceService.calculateDateParams(fromDate);
        return ApiResponse.success("Dates calculated successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping(value = "/upload-ot-excel", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadOtExcel(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String result = attendanceService.uploadOtExcel(file);
        return ApiResponse.success(result);
    }
}
