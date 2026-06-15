package com.asg.hr.designation.controller;

import static com.asg.common.lib.dto.response.ApiResponse.success;

import java.util.Map;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.designation.dto.DesignationRequest;
import com.asg.hr.designation.dto.DesignationResponse;
import com.asg.hr.designation.service.DesignationService;

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

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/designation")
@Tag(name = "Designation Master", description = "APIs for managing Designation Master records")
public class DesignationController {

    private final DesignationService designationService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listDesignations(@RequestBody(required = false) FilterRequestDto filterRequest,
                                              @ParameterObject Pageable pageable) {
        Map<String, Object> page = designationService.listDesignations(filterRequest, pageable);
        return success("Designation list retrieved successfully", page);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{designationPoid}")
    public ResponseEntity<?> getDesignationById(@PathVariable @NotNull @Positive Long designationPoid) {
        DesignationResponse response = designationService.getDesignationById(designationPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(),
                designationPoid.toString());
        return success("Designation retrieved successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createDesignation(@Valid @RequestBody DesignationRequest request) {
        Long designationPoid = designationService.createDesignation(request);
        return success("Designation created successfully", Map.of("designationPoid", designationPoid));
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{designationPoid}")
    public ResponseEntity<?> updateDesignation(@PathVariable @NotNull @Positive Long designationPoid,
                                               @Valid @RequestBody DesignationRequest request) {
        DesignationResponse response = designationService.updateDesignation(designationPoid, request);
        return success("Designation updated successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{designationPoid}")
    public ResponseEntity<?> deleteDesignation(@PathVariable @NotNull @Positive Long designationPoid,
                                               @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        designationService.deleteDesignation(designationPoid, deleteReasonDto);
        return success("Designation deleted successfully");
    }
}

