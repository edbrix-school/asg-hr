package com.asg.hr.leaverequest.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.response.ApiResponse;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDownloadHeaderService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.leaverequest.dto.EarlierPendingLeaveCheckDto;
import com.asg.hr.leaverequest.dto.LeaveCalculationResponseDto;
import com.asg.hr.leaverequest.dto.LeaveCreateRequestDto;
import com.asg.hr.leaverequest.dto.LeaveResponseDto;
import com.asg.hr.leaverequest.dto.LeaveTicketUpdateRequestDto;
import com.asg.hr.leaverequest.dto.LeaveUpdateRequestDto;
import com.asg.hr.leaverequest.entity.HrLeaveRequestHdrEntity;
import com.asg.hr.leaverequest.service.HrLeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.badRequest;
import static com.asg.common.lib.dto.response.ApiResponse.internalServerError;


@RestController
@RequestMapping("/v1/leave-request")
@Tag(name = "Leave Request", description = "APIs for managing and retrieving Leave Request records")
@RequiredArgsConstructor
public class HrLeaveRequestController {

    private static final DateTimeFormatter LEGACY_REPORT_DATE =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    private final HrLeaveRequestService service;
    private final LoggingService loggingService;
    private final DocumentDownloadHeaderService downloadHeaderService;

    @PostMapping("/create")
    @AllowedAction(UserRolesRightsEnum.CREATE)
    public ResponseEntity<?> createLeave(
            @RequestBody LeaveCreateRequestDto request) {

        LeaveResponseDto response = service.create(request);

        return ApiResponse.success("Leave request created successfully", response);
    }

    @PutMapping("/update")
    @AllowedAction(UserRolesRightsEnum.EDIT)
    public ResponseEntity<?> updateLeave(
            @RequestBody LeaveUpdateRequestDto request) {

        LeaveResponseDto response = service.update(request);

        return ApiResponse.success("Leave request updated successfully", response);
    }

    @GetMapping("/id/{transactionPoid}")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    public ResponseEntity<?> getById(@PathVariable Long transactionPoid) {

        LeaveResponseDto response = service.getById(transactionPoid);

        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());

        return ApiResponse.success("Leave request retrieved successfully", response);
    }

    @PostMapping("/list")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        if ((startDate == null) != (endDate == null)) {
            return badRequest("Both startDate and endDate must be provided together");
        }

        Map<String, Object> response = service.list(UserContext.getDocumentId(), filters, startDate, endDate, pageable);

        return ApiResponse.success("Leave request list retrieved successfully", response);
    }

    @DeleteMapping("/{transactionPoid}")
    @AllowedAction(UserRolesRightsEnum.DELETE)
    public ResponseEntity<?> delete(@PathVariable Long transactionPoid,
                @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {

                service.delete(transactionPoid,deleteReasonDto);

        return ApiResponse.success("Leave request deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{employeePoid}")
    public ResponseEntity<?> getEmployeeDetails(
            @PathVariable Long employeePoid) {

        Map<String, Object> response = service.getEmployeeDetails(employeePoid);

        return ApiResponse.success("Leave request Employee Details retrieved successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{employeePoid}/hod")
    public ResponseEntity<?> getEmployeeHod(
            @PathVariable Long employeePoid) {

        Map<String, Object> response =
                service.getEmployeeHod(employeePoid);

        return ApiResponse.success("Get Hod Details retrieved successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/eligible-leave")
    public ResponseEntity<?> getEligibleLeaveDays(
            @RequestParam Long companyId,
            @RequestParam Long empId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate leaveStartDate,
            @RequestParam(required = false) Long settlementPoid
    ) {

        Map<String, Object> response =
                service.getEligibleLeaveDays(
                        companyId,
                        empId,
                        leaveStartDate,
                        settlementPoid
                );

        return ApiResponse.success("Get Employee eligible leave Details retrieved successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{employeePoid}/ticket-family")
    public ResponseEntity<?> getTicketFamilyDetails(
            @PathVariable Long employeePoid) {

        Map<String, Object> response =
                service.getTicketFamilyDetails(employeePoid);

        return ApiResponse.success("Get Employee Family Details retrieved successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/update-leave-history")
    public ResponseEntity<?> updateLeaveHistoryLegacyParams(
            @RequestParam Long tranId,
            @RequestParam (required = false)String ticketIssueType,
            @RequestParam(required = false) String ticketTillDate,
            @RequestParam (required = false)String ticketIssuedCount) {

        Map<String, Object> response =
                service.updateLeaveHistory(
                        tranId,
                        ticketIssueType,
                        ticketTillDate,
                        ticketIssuedCount
                );

        return ApiResponse.success("Employee update leave history successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/history/{transactionPoid}/cancel")
    public ResponseEntity<?> cancelLeaveHistory(
            @PathVariable Long transactionPoid) {

        Map<String, Object> response = service.cancelLeaveHistory(transactionPoid);

        return ApiResponse.success("Cancel leave history of employee  successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/update-ticket-details")
    public ResponseEntity<?> updateTicketDetails(
            @RequestBody LeaveTicketUpdateRequestDto request) {

        Map<String, Object> response = service.updateTicketDetails(request);

        return ApiResponse.success("Update employee ticket details successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/calculate-days")
    public ResponseEntity<?> calculateLeaveDays(
            @RequestParam(required = false) Long transactionPoid,
            @RequestParam Long employeePoid,
            @RequestParam String leaveType,
            @RequestParam(required = false) String annualLeaveType,
            @RequestParam(required = false) String emergencyLeaveType,
            @RequestParam(required = false) String splLeaveTypes,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate leaveStartDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planedRejoinDate,
            @RequestParam(required = false) java.math.BigDecimal eligibleLeaveDays,
            @RequestParam(required = false) String leaveDaysMethod) {

        LeaveCalculationResponseDto response = service.calculateLeaveDays(
                transactionPoid,
                employeePoid,
                leaveType,
                annualLeaveType,
                emergencyLeaveType,
                splLeaveTypes,
                leaveStartDate,
                planedRejoinDate,
                eligibleLeaveDays,
                leaveDaysMethod
        );

        return ApiResponse.success("Calculate leave days for employee successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Check for earlier pending leave requests",
            description = "Checks whether the employee has an earlier leave request that is still pending. "
                    + "Call this before Save so the warning can be shown up front; the same check is "
                    + "re-applied on create and update, where it rejects the save with HTTP 400."
    )
    @GetMapping("/earlier-pending-check")
    public ResponseEntity<?> checkEarlierPendingLeaveRequests(
            @RequestParam Long employeePoid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate leaveStartDate,
            @Parameter(description = "Transaction POID of the record being edited, so it is not checked against itself")
            @RequestParam(required = false) Long transactionPoid) {

        EarlierPendingLeaveCheckDto response =
                service.checkEarlierPendingLeaveRequests(transactionPoid, employeePoid, leaveStartDate);

        return ApiResponse.success("Earlier pending leave request check completed successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/leave-type-change")
    public ResponseEntity<?> handleLeaveTypeChange(
            @RequestParam String leaveType,
            @RequestParam(required = false) String leaveDaysMethod) {

        Map<String, Object> response = service.handleLeaveTypeChange(leaveType, leaveDaysMethod);

        return ApiResponse.success("Employee leave type change successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/reports/attendance")
    public ResponseEntity<Map<String, Object>> attendanceReport(
            @RequestParam Long employeePoid,
            @RequestParam(required = false) String employeeCode) {

        return ResponseEntity.ok(Map.of(
                "reportId", "800-216",
                "employeePoid", employeePoid,
                "parameters", "EMP_CODE=" + (employeeCode != null ? employeeCode : "")
        ));
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/reports/leave-history")
    public ResponseEntity<Map<String, Object>> leaveHistoryReport(
            @RequestParam Long employeePoid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodDate2) {

        LocalDate fromDate = periodDate != null ? periodDate : LocalDate.now().minusDays(730);
        LocalDate toDate = periodDate2 != null ? periodDate2 : LocalDate.now();

        String filters = "EMPLOYEE_POID=" + employeePoid + ";"
                + "PERIOD_DATE=" + fromDate.format(LEGACY_REPORT_DATE) + ";"
                + "PERIOD_DATE2=" + toDate.format(LEGACY_REPORT_DATE) + ";";

        return ResponseEntity.ok(Map.of(
                "reportId", "800-217",
                "filters", filters
        ));
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/reports/leave-schedule")
    public ResponseEntity<Map<String, Object>> leaveScheduleReport() {

        return ResponseEntity.ok(Map.of(
                "reportId", "800-220",
                "filters", ""
        ));
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for Leave Request",
            description = "Generate PDF report for a specific Leave Request transaction",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Leave Request not found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "71031")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = service.print(transactionPoid);
            return ResponseEntity.ok()
                    .headers(downloadHeaderService.buildAttachmentHeaders(
                            HrLeaveRequestHdrEntity.class,
                            transactionPoid,
                            "leave-request",
                            "pdf"))
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return internalServerError("Failed to generate PDF: " + e.getMessage());
        }
    }


}
