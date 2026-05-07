package com.asg.hr.locationmaster.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.locationmaster.dto.LocationMasterRequestDto;
import com.asg.hr.locationmaster.dto.LocationMasterResponseDto;
import com.asg.hr.locationmaster.service.LocationMasterService;
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
@RequestMapping("/v1/location/master")
@RequiredArgsConstructor
public class LocationMasterController {

    private final LocationMasterService service;
    private final LoggingService loggingService;

    @Operation(
            summary = "List locations with filters",
            description = "Retrieves paginated list of locations with optional filters",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Locations retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            description = "Filter criteria for locations",
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = @ExampleObject(
                            name = "Location Filters",
                            value = """
                                    {
                                      "operator": "AND",
                                      "isDeleted": "N",
                                      "filters": [
                                        { "searchField": "ACTIVE", "searchValue": "Y" },
                                        { "searchField": "LOCATION_NAME", "searchValue": "Main" }
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
        return success("Locations retrieved successfully", service.list(UserContext.getDocumentId(), filters, pageable));
    }

    @Operation(
            summary = "Create a new location",
            description = "Creates a new location with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Location created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = LocationMasterResponseDto.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "409", description = "Location code already exists")
            }
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> create(
            @Parameter(description = "Location details to be created", required = true)
            @Valid @RequestBody LocationMasterRequestDto requestDto) {
        return success("Location created successfully", service.create(requestDto));
    }

    @Operation(
            summary = "Update location details",
            description = "Updates an existing location identified by its ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Location updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = LocationMasterResponseDto.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Location not found")
            }
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{locationPoid}")
    public ResponseEntity<?> update(
            @Parameter(description = "ID of the location to update", required = true)
            @PathVariable Long locationPoid,
            @Parameter(description = "Updated location details", required = true)
            @Valid @RequestBody LocationMasterRequestDto requestDto) {
        return success("Location updated successfully", service.update(locationPoid, requestDto));
    }

    @Operation(
            summary = "Get location by ID",
            description = "Retrieves location details by ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Location retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = LocationMasterResponseDto.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Location not found")
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{locationPoid}")
    public ResponseEntity<?> getById(
            @Parameter(description = "Location ID", required = true)
            @PathVariable Long locationPoid) {
        LocationMasterResponseDto response = service.getById(locationPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), locationPoid.toString());
        return success("Location retrieved successfully", response);
    }

    @Operation(
            summary = "Soft delete a location",
            description = "Marks a location as deleted without permanently removing it",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Location deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Location not found")
            }
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{locationPoid}")
    public ResponseEntity<?> delete(
            @Parameter(description = "Location ID", required = true)
            @PathVariable Long locationPoid,
            @Parameter(description = "Reason for deletion", required = true)
            @Valid @RequestBody DeleteReasonDto deleteReasonDto) {
        service.delete(locationPoid, deleteReasonDto);
        return success("Location deleted successfully", null);
    }

}