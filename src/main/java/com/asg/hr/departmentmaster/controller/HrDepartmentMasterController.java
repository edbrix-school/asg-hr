package com.asg.hr.departmentmaster.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.response.ApiResponse;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.departmentmaster.dto.HrDepartmentMasterRequest;
import com.asg.hr.departmentmaster.dto.HrDepartmentMasterResponse;
import com.asg.hr.departmentmaster.service.HrDepartmentMasterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/department")
@Tag(name = "Department Master", description = "APIs for managing Department Master records")
public class HrDepartmentMasterController {

    private final HrDepartmentMasterService departmentService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> getDepartmentList(@RequestBody(required = false) FilterRequestDto filterRequest,
                                               @ParameterObject Pageable pageable
    ) {
        Map<String, Object> departmentsPage = departmentService.getAllDepartmentsWithFilters(UserContext.getDocumentId(), filterRequest, pageable);
        return ApiResponse.success("Department list retrieved successfully", departmentsPage);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{deptPoid}")
    public ResponseEntity<?> getDepartmentById(@PathVariable @NotNull @Positive Long deptPoid
    ) {
        HrDepartmentMasterResponse response = departmentService.getDepartmentById(deptPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED,UserContext.getDocumentId(),deptPoid.toString());
        return ApiResponse.success("Department retrieved successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createDepartment(@Valid @RequestBody HrDepartmentMasterRequest request
    ) {
        HrDepartmentMasterResponse response = departmentService.createDepartment(request, UserContext.getGroupPoid(), UserContext.getUserId());
        return ApiResponse.success("Department created successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{deptPoid}")
    public ResponseEntity<?> updateDepartment(@PathVariable @NotNull @Positive Long deptPoid,
                                              @Valid @RequestBody HrDepartmentMasterRequest request
    ) {
        HrDepartmentMasterResponse response = departmentService.updateDepartment(deptPoid, request, UserContext.getGroupPoid(), UserContext.getUserId());
        return ApiResponse.success("Department updated successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{deptPoid}")
    public ResponseEntity<?> deleteDepartment(@PathVariable @NotNull @Positive Long deptPoid,
                                              @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        departmentService.deleteDepartment(deptPoid, UserContext.getGroupPoid(), UserContext.getUserId(), deleteReasonDto);
        return ApiResponse.success("Department deleted successfully");
    }
}

