package com.asg.hr.common.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.response.ApiResponse;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.hr.common.dto.CurrentUserEmployeeDto;
import com.asg.hr.common.dto.EmployeeLovQuery;
import com.asg.hr.common.service.CurrentUserEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints shared by every HR screen rather than belonging to one document.
 */
@RestController
@RequestMapping("/v1/common")
@Tag(name = "HR Common", description = "Endpoints shared across HR screens")
@RequiredArgsConstructor
public class HrCommonController {

    private final CurrentUserEmployeeService currentUserEmployeeService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Employee of the logged-in user, and the employees they may pick",
            description = "Returns the employee the logged-in user is linked to, plus whether they hold Edit "
                    + "on the calling screen's document (the X-Document-Id sent with the request), read with "
                    + "PROC_GLOB_USR_RIGHTS_APPSTART. Without that right the user may only work on their own "
                    + "employee, so the screen should prefill this employee and lock the field; with it they "
                    + "may pick any employee. employeePoid is null when the login is not linked to an "
                    + "employee record.\n\n"
                    + "Pass lovName to have the picker's list returned in the same call: a user who may "
                    + "select any employee gets that LOV's list, honouring filter, paging and sorting, while a "
                    + "restricted user gets only their own employee — one lookup instead of the whole list. "
                    + "Leave lovName out and no LOV is read at all."
    )
    @GetMapping("/current-employee")
    public ResponseEntity<?> getCurrentUserEmployee(
            @Parameter(description = "Employee LOV to load the picker from, e.g. EMPLOYEE_NAME. "
                    + "Omit to skip the list entirely.")
            @RequestParam(required = false) String lovName,
            @Parameter(description = "Search text, applied only when the user may select any employee")
            @RequestParam(required = false) String filter,
            @Parameter(description = "Page number for pagination (0-based)")
            @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination")
            @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value, seqno, poid)")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)")
            @RequestParam(required = false) String sortDir) {

        CurrentUserEmployeeDto response = currentUserEmployeeService.getCurrentUserEmployee(
                new EmployeeLovQuery(lovName, filter, pageNumber, pageSize, sortBy, sortDir));

        return ApiResponse.success("Current user employee retrieved successfully", response);
    }
}
