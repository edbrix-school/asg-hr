package com.asg.hr.airsector.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.airsector.dto.HrAirsectorRequestDto;
import com.asg.hr.airsector.dto.HrAirsectorResponseDto;
import com.asg.hr.airsector.service.HrAirsectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/airsector")
@Tag(name = "AirSector Master", description = "APIs for managing and retrieving AirSector Master records")
@RequiredArgsConstructor
public class HrAirsectorMasterController {

    private final HrAirsectorService service;
    private final LoggingService loggingService;

    @Operation(
            summary = "Create a new AirSector Master",
            description = "Creates a new AirSector with the provided details",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully created the AirSector Master",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = HrAirsectorResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input or duplicate description",
                            content = @Content(mediaType = "application/json"))
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createAirsectorMaster(
            @Valid @RequestBody HrAirsectorRequestDto requestDTO) {

        try {

            HrAirsectorResponseDto response = service.create(requestDTO);

            return success("AirSector Master created successfully", response);

        } catch (ValidationException e) {

            return badRequest(e.getMessage());

        }
    }


    @Operation(
            summary = "Update an existing AirSector Master",
            description = "Updates the AirSector with the provided details",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully updated the AirSector Master",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = HrAirsectorResponseDto.class))),
                    @ApiResponse(responseCode = "404", description = "AirSector not found",
                            content = @Content(mediaType = "application/json"))
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{airsecPoid}")
    public ResponseEntity<?> updateAirsectorMaster(

            @Parameter(description = "AirSector POID", required = true)
            @PathVariable Long airsecPoid,

            @Valid @RequestBody HrAirsectorRequestDto requestDto) {

        try {

            HrAirsectorResponseDto response = service.update(airsecPoid, requestDto);

            return success("AirSector Master updated successfully", response);

        } catch (ValidationException ex) {

            return badRequest(ex.getMessage());

        } catch (Exception ex) {

            return internalServerError(ex.getMessage());

        }
    }


    @Operation(
            summary = "Soft delete an AirSector",
            description = "Marks an AirSector as deleted without permanently removing data",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully soft deleted the AirSector"),
                    @ApiResponse(responseCode = "404", description = "AirSector not found")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{airsecPoid}")
    public ResponseEntity<?> deleteAirsectorMaster(

            @Parameter(description = "AirSector POID", required = true)
            @PathVariable Long airsecPoid,

            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {

        service.deleteAirsectorMaster(
                airsecPoid,
                deleteReasonDto
        );

        return success("AirSector Master soft deleted successfully");

    }


    @Operation(
            summary = "Get AirSector by POID",
            description = "Retrieves AirSector details using the provided POID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved AirSector",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = HrAirsectorResponseDto.class))),
                    @ApiResponse(responseCode = "404", description = "AirSector not found")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{airsecPoid}")
    public ResponseEntity<?> getAirsectorById(

            @Parameter(description = "AirSector POID", required = true)
            @PathVariable Long airsecPoid) {

        HrAirsectorResponseDto response = service.findById(airsecPoid);

        loggingService.createLogSummaryEntry(
                LogDetailsEnum.VIEWED,
                UserContext.getDocumentId(),
                airsecPoid.toString()
        );

        return success("AirSector fetched successfully", response);

    }


    @Operation(
            summary = "List AirSectors with Search and Sort",
            description = "Returns paginated AirSector list with filter and sorting support"
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> getAirsectorList(

            @ParameterObject Pageable pageable,

            @RequestBody(required = false) FilterRequestDto filters,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        try {

            Map<String, Object> data =
                    service.listOfRecordsAndGenericSearch(
                            UserContext.getDocumentId(),
                            filters,
                            pageable,
                            startDate,
                            endDate
                    );

            return success("AirSector list fetched successfully", data);

        } catch (Exception ex) {

            return internalServerError(
                    "Unable to fetch AirSector list: " + ex.getMessage()
            );

        }

    }
}
