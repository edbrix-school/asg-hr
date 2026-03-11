package com.asg.hr.competency.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.competency.dto.CompetencyMasterRequestDto;
import com.asg.hr.competency.dto.CompetencyMasterResponseDto;
import com.asg.hr.competency.service.CompetencyMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/competency/master")
@RequiredArgsConstructor
public class CompetencyMasterController {

    private final CompetencyMasterService service;
    private final LoggingService loggingService;

    @Operation(
            summary = "List competencies with filters",
            description = "Retrieves paginated list of competencies with optional filters",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Competencies retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            description = "Filter criteria for competencies",
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = @ExampleObject(
                            name = "Competency Filters",
                            value = """
                                    {
                                      "operator": "AND",
                                      "isDeleted": "N",
                                      "filters": [
                                        { "searchField": "ACTIVE", "searchValue": "Y" },
                                        { "searchField": "COMPETENCY_DESCRIPTION", "searchValue": "Work" }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> list(@ParameterObject Pageable pageable,
                                   @RequestBody(required = false) FilterRequestDto filters) {
        return success("Competencies retrieved successfully", service.list(UserContext.getDocumentId(), filters, pageable));
    }

    @Operation(
            summary = "Create a new competency",
            description = "Creates a new competency with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Competency created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CompetencyMasterResponseDto.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "409", description = "Competency code already exists")
            }
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> create(
            @Parameter(description = "Competency details to be created", required = true)
            @Valid @RequestBody CompetencyMasterRequestDto requestDto) {
        return success("Competency created successfully", service.create(requestDto));
    }

    @Operation(
            summary = "Update competency details",
            description = "Updates an existing competency identified by its ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Competency updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CompetencyMasterResponseDto.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Competency not found")
            }
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{competencyPoid}")
    public ResponseEntity<?> update(
            @Parameter(description = "ID of the competency to update", required = true)
            @PathVariable Long competencyPoid,
            @Parameter(description = "Updated competency details", required = true)
            @Valid @RequestBody CompetencyMasterRequestDto requestDto) {
        return success("Competency updated successfully", service.update(competencyPoid, requestDto));
    }

    @Operation(
            summary = "Get competency by ID",
            description = "Retrieves competency details by ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Competency retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CompetencyMasterResponseDto.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Competency not found")
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{competencyPoid}")
    public ResponseEntity<?> getById(
            @Parameter(description = "Competency ID", required = true)
            @PathVariable Long competencyPoid) {
        return success("Competency retrieved successfully", service.getById(competencyPoid));
    }

    @Operation(
            summary = "Soft delete a competency",
            description = "Marks a competency as deleted without permanently removing it",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Competency deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Competency not found")
            }
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{competencyPoid}")
    public ResponseEntity<?> delete(
            @Parameter(description = "Competency ID", required = true)
            @PathVariable Long competencyPoid,
            @Parameter(description = "Reason for deletion", required = true)
            @Valid @RequestBody DeleteReasonDto deleteReasonDto) {
        service.delete(competencyPoid, deleteReasonDto);
        return success("Competency deleted successfully", null);
    }
}
