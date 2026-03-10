package com.asg.hr.Employee.performance.review.master.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.Employee.performance.review.master.dto.EmployeePerformanceReviewRequestDto;
import com.asg.hr.Employee.performance.review.master.service.EmployeePerformanceReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/hr/competencies")
@RequiredArgsConstructor
public class EmployeePerformanceReviewController {

    private final EmployeePerformanceReviewService service;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> list(@ParameterObject Pageable pageable,
                                   @RequestBody(required = false) FilterRequestDto filters) {
        return success("Competencies retrieved successfully", service.list(UserContext.getDocumentId(), filters, pageable));
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody EmployeePerformanceReviewRequestDto requestDto) {
        return success("Competency created successfully", service.create(requestDto));
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{competencyPoid}")
    public ResponseEntity<?> update(@PathVariable Long competencyPoid,
                                    @Valid @RequestBody EmployeePerformanceReviewRequestDto requestDto) {
        return success("Competency updated successfully", service.update(competencyPoid, requestDto));
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{competencyPoid}")
    public ResponseEntity<?> getById(@PathVariable Long competencyPoid) {
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), competencyPoid.toString());
        return success("Competency retrieved successfully", service.getById(competencyPoid));
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{competencyPoid}")
    public ResponseEntity<?> delete(@PathVariable Long competencyPoid,
                                    @Valid @RequestBody DeleteReasonDto deleteReasonDto) {
        service.delete(competencyPoid, deleteReasonDto);
        return success("Competency deleted successfully", null);
    }
}
