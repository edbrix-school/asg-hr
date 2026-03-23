package com.asg.hr.religion.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.competency.dto.CompetencyScheduleResponseDto;
import com.asg.hr.religion.dto.ReligionDtoRequest;
import com.asg.hr.religion.dto.ReligionDtoResponse;
import com.asg.hr.religion.service.ReligionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("/v1/religion")
@RequiredArgsConstructor
@Slf4j
public class ReligionController {

    private final LoggingService loggingService;
    private final ReligionService service;

    @Operation(
            summary = "Create Religion",
            description = "Creates a new religion with religion code and religion description",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Religion created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ReligionDtoResponse.class)
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
    public ResponseEntity<?> createReligion(
            @Parameter(description = "Religion details to be created", required = true)
            @Valid @RequestBody ReligionDtoRequest requestDto) {
        Long religionPoid = service.createReligion(requestDto);
        return success("Religion created successfully", Map.of("religionPoid", religionPoid));
    }

    @Operation(
            summary = "Update Religion",
            description = "Updates an existing religion",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Religion updated successfully"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input or validation error"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Religion not found"
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{religionPoid}")
    public ResponseEntity<?> updateReligion(
            @Parameter(description = "Religion Id to update", required = true)
            @PathVariable(name = "religionPoid") Long religionPoid,
            @Parameter(description = "Updated religion details", required = true)
            @Valid @RequestBody ReligionDtoRequest requestDto) {
        ReligionDtoResponse response = service.updateReligion(requestDto, religionPoid);
        return success("Religion updated successfully", response);
    }

    @Operation(
            summary = "Get Religion by ID",
            description = "Retrieves religion details by religion POID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Religion detail retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CompetencyScheduleResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Religion not found"
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{religionPoid}")
    public ResponseEntity<?> getReligionById(
            @Parameter(description = "Religion ID", required = true)
            @PathVariable(name = "religionPoid") Long religionPoid) {
        ReligionDtoResponse response = service.getReligionById(religionPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), religionPoid.toString());
        return success("Religion detail fetched successfully", response);
    }

    @Operation(
            summary = "List Religion with Filters & Pagination",
            description = """
                    Fetch Religion details using pagination + dynamic filters.
                    
                    ### Allowed searchField values:
                    RELIGION_POID, RELIGION_CODE, RELIGION_DESCRIPTION
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            description = "Search Filters for Schedules",
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = @ExampleObject(
                            name = "Religion Filters",
                            value = """
                                    {
                                      "operator": "AND",
                                      "isDeleted": "N",
                                      "filters": [
                                        { "searchField": "ACTIVE", "searchValue": "Y" },
                                        { "searchField": "RELIGION_DESCRIPTION", "searchValue": "Test" }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/search")
    public ResponseEntity<?> listReligion(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filterRequest
    ) {
        Map<String, Object> result = service.listReligion(filterRequest, pageable);
        return success("Religions fetched successfully", result);
    }

    @Operation(
            summary = "Delete Religion",
            description = "Soft deletes a religion",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Religion deleted successfully"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Religion not found"
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{religionPoid}")
    public ResponseEntity<?> deleteReligion(
            @Parameter(description = "Religion ID to delete", required = true)
            @PathVariable(name = "religionPoid") Long religionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        service.deleteReligion(religionPoid, deleteReasonDto);
        return success("Religion deleted successfully");
    }
}
