package com.asg.hr.competencyevaluation.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.competencyevaluation.dto.CompetencyEvaluationCalculateScoresRequestDto;
import com.asg.hr.competencyevaluation.dto.CompetencyEvaluationCalculateScoresResponseDto;
import com.asg.hr.competencyevaluation.dto.CompetencyEvaluationRequestDto;
import com.asg.hr.competencyevaluation.dto.CompetencyEvaluationResponseDto;
import com.asg.hr.competencyevaluation.service.CompetencyEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.badRequest;
import static com.asg.common.lib.dto.response.ApiResponse.internalServerError;
import static com.asg.common.lib.dto.response.ApiResponse.notFound;
import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("/v1/competency-evaluation")
@RequiredArgsConstructor
public class CompetencyEvaluationController {

    private final CompetencyEvaluationService competencyEvaluationService;
    private final LoggingService loggingService;

    @Operation(summary = "Create employee performance review",
            description = "Creates a competency evaluation header and detail lines (transaction 800-110).",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Created",
                            content = @Content(schema = @Schema(implementation = CompetencyEvaluationResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error")
            })
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CompetencyEvaluationRequestDto request) {
        try {
            CompetencyEvaluationResponseDto data = competencyEvaluationService.create(request);
            return success("Employee performance review created successfully", data);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError(ex.getMessage());
        }
    }

    @Operation(summary = "Update employee performance review",
            security = @SecurityRequirement(name = "bearerAuth"))
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> update(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
            @Valid @RequestBody CompetencyEvaluationRequestDto request) {
        try {
            CompetencyEvaluationResponseDto data = competencyEvaluationService.update(transactionPoid, request);
            return success("Employee performance review updated successfully", data);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError(ex.getMessage());
        }
    }

    @Operation(summary = "Get employee performance review by id",
            security = @SecurityRequirement(name = "bearerAuth"))
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getById(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        try {
            CompetencyEvaluationResponseDto data = competencyEvaluationService.getById(transactionPoid);
            loggingService.createLogSummaryEntry(
                    UserContext.getDocumentId(),
                    transactionPoid.toString(),
                    String.format("%s %s", LogDetailsEnum.VIEWED.getDescription(), data.getDocRef()));
            return success("Employee performance review fetched successfully", data);
        } catch (ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError(ex.getMessage());
        }
    }

    @Operation(summary = "List employee performance reviews",
            description = "Uses document search with filters and pagination.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestBody(required = false) FilterRequestDto filterRequest) {
        if ((startDate == null) != (endDate == null)) {
            return badRequest("Both startDate and endDate must be provided together");
        }
        try {
            Map<String, Object> result = competencyEvaluationService.list(filterRequest, startDate, endDate, pageable);
            return success("Employee performance reviews fetched successfully", result);
        } catch (Exception e) {
            return internalServerError("Unable to fetch reviews: " + e.getMessage());
        }
    }

    @Operation(summary = "Delete employee performance review",
            security = @SecurityRequirement(name = "bearerAuth"))
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> delete(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        try {
            competencyEvaluationService.delete(transactionPoid, deleteReasonDto);
            return success("Employee performance review deleted successfully");
        } catch (ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError(ex.getMessage());
        }
    }

    @Operation(summary = "Calculate scores from detail ratings in request body",
            description = "Calculates scores from detail ratings in request body",
            security = @SecurityRequirement(name = "bearerAuth"))
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/calculate-scores")
    public ResponseEntity<?> calculateScoresFromDetails(
            @Valid @RequestBody CompetencyEvaluationCalculateScoresRequestDto request) {
        try {
            CompetencyEvaluationCalculateScoresResponseDto data =
                    competencyEvaluationService.calculateScoresFromDetails(request);
            return success("Scores calculated successfully", data);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError(ex.getMessage());
        }
    }

    @Operation(summary = "Calculate HOD total score and percentages from saved records",
            description = "Reads detail lines from database for the transaction and persists scores to header.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/{transactionPoid}/calculate-scores")
    public ResponseEntity<?> calculateScores(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        try {
            CompetencyEvaluationResponseDto data = competencyEvaluationService.calculateScores(transactionPoid);
            return success("Scores calculated successfully", data);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError(ex.getMessage());
        }
    }
}
