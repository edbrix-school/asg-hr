package com.asg.hr.competency.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.competency.dto.CompetencyScheduleRequestDto;
import com.asg.hr.competency.dto.CompetencyScheduleResponseDto;
import com.asg.hr.competency.dto.CreateBatchRequest;
import com.asg.hr.competency.service.CompetencyScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/competency-schedule")
@RequiredArgsConstructor
public class CompetencyScheduleController {
    
    private final CompetencyScheduleService competencyScheduleService;
    private final LoggingService loggingService;
    
    @Operation(
            summary = "Create Employee Performance Review Schedule",
            description = "Creates a new performance review schedule with evaluation period and date",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Schedule created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CompetencyScheduleResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input or validation error",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createSchedule(
            @Parameter(description = "Schedule details to be created", required = true)
            @Valid @RequestBody CompetencyScheduleRequestDto requestDto) {
        try {
            Long schedulePoid = competencyScheduleService.createSchedule(requestDto);
            return success("Schedule created successfully", Map.of("schedulePoid", schedulePoid));
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError(ex.getMessage());
        }
    }
    
    @Operation(
            summary = "Update Employee Performance Review Schedule",
            description = "Updates an existing performance review schedule",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Schedule updated successfully"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input or validation error"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Schedule not found"
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{schedulePoid}")
    public ResponseEntity<?> updateSchedule(
            @Parameter(description = "Schedule ID to update", required = true)
            @PathVariable Long schedulePoid,
            @Parameter(description = "Updated schedule details", required = true)
            @Valid @RequestBody CompetencyScheduleRequestDto requestDto) {
        try {
            Long updatedPoid = competencyScheduleService.updateSchedule(schedulePoid, requestDto);
            return success("Schedule updated successfully", Map.of("schedulePoid", updatedPoid));
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError(ex.getMessage());
        }
    }
    
    @Operation(
            summary = "Get Schedule by ID",
            description = "Retrieves schedule details by schedule POID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Schedule retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CompetencyScheduleResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Schedule not found"
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{schedulePoid}")
    public ResponseEntity<?> getScheduleById(
            @Parameter(description = "Schedule ID", required = true)
            @PathVariable Long schedulePoid) {
        CompetencyScheduleResponseDto response = competencyScheduleService.getScheduleById(schedulePoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), schedulePoid.toString());
        return success("Schedule fetched successfully", response);
    }
    
    @Operation(
            summary = "List Schedules with Filters & Pagination",
            description = """
                    Fetch Performance Review Schedules using pagination + dynamic filters.
                    
                    ### Allowed searchField values:
                    SCHEDULE_POID, SCHEDULE_DESCRIPTION, PERIOD_FROM, PERIOD_TO, 
                    ACTIVE, EVALUATION_DATE, CREATED_DATE
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            description = "Search Filters for Schedules",
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = @ExampleObject(
                            name = "Schedule Filters",
                            value = """
                                    {
                                      "operator": "AND",
                                      "isDeleted": "N",
                                      "filters": [
                                        { "searchField": "ACTIVE", "searchValue": "Y" },
                                        { "searchField": "SCHEDULE_DESCRIPTION", "searchValue": "Annual" }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listSchedules(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filterRequest
    ) {
        try {
            Map<String, Object> result = competencyScheduleService.listSchedules(filterRequest, pageable);
            return success("Schedules fetched successfully", result);
        } catch (Exception e) {
            return internalServerError("Unable to fetch schedules: " + e.getMessage());
        }
    }
    
    @Operation(
            summary = "Delete Schedule",
            description = "Soft deletes a performance review schedule",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Schedule deleted successfully"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Schedule not found"
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{schedulePoid}")
    public ResponseEntity<?> deleteSchedule(
            @Parameter(description = "Schedule ID to delete", required = true)
            @PathVariable Long schedulePoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        competencyScheduleService.deleteSchedule(schedulePoid, deleteReasonDto);
        return success("Schedule deleted successfully");
    }
    
    @Operation(
            summary = "Create Batch Evaluation Schedule",
            description = "Triggers generation of evaluation records for all employees based on the schedule",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Batch evaluation created successfully"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Schedule not found"
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{schedulePoid}/create-batch")
    public ResponseEntity<?> createBatchEvaluation(
            @Parameter(description = "Schedule ID", required = true)
            @PathVariable Long schedulePoid,
            @RequestBody CreateBatchRequest request) {
        try {
            competencyScheduleService.createBatchEvaluation(schedulePoid, request.getEvaluationDate(), request.getRecreate());
            return success("Batch evaluation created successfully");
        } catch (Exception ex) {
            return internalServerError(ex.getMessage());
        }
    }
}
