package com.asg.hr.holidaymaster.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.dto.response.ApiResponse;
import com.asg.hr.holidaymaster.dto.HolidayBatchCreateRequest;
import com.asg.hr.holidaymaster.dto.HolidayMasterRequest;
import com.asg.hr.holidaymaster.dto.HolidayMasterResponse;
import com.asg.hr.holidaymaster.service.HolidayMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/holiday-master")
@RequiredArgsConstructor
@Validated
@Slf4j
public class HolidayMasterController {

    private final HolidayMasterService holidayMasterService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "List Holidays with Search and Sort",
            description = """
                    Provide search filters via DocumentSearchService (doc_master).
                    Common filters:
                    - GLOBALSEARCH
                    - HOLIDAY_DATE
                    - HOLIDAY_REASON

                    Sorting will be applied as configured in list_of_records_sql
                    for docId 800-011 (Holiday Master).
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields
                      3. operator can be AND / OR (default OR)
                      4. isDeleted:
                         - N or null → non-deleted
                         - Y → deleted records
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Holiday Master Filters",
                                    value = """
                                            {
                                              "operator": "OR",
                                              "isDeleted": "N",
                                              "filters": [
                                                { "searchField": "GLOBALSEARCH", "searchValue": "Eid" },
                                                { "searchField": "HOLIDAY_DATE", "searchValue": "2026-01-01" },
                                                { "searchField": "HOLIDAY_REASON", "searchValue": "New Year" }
                                              ]
                                            }
                                            """
                            )
                    }
            )
    )
    @PostMapping("/list")
    public ResponseEntity<?> listHolidays(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestHeader("X-Document-Id") String docId
    ) {
        log.info("List Holidays request | page={}, size={}, docId={}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                docId);

        Map<String, Object> result =
                holidayMasterService.listHolidays(docId, filters, pageable);

        return ApiResponse.success("Holiday list fetched successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{holidayPoid}")
    @Operation(
            summary = "Get Holiday by ID",
            description = "Retrieve holiday details using HOLIDAY_POID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getById(
            @PathVariable @NotNull @Positive Long holidayPoid
    ) {
        log.info("Get Holiday request | poid={}", holidayPoid);
        HolidayMasterResponse response = holidayMasterService.getById(holidayPoid);
        return ApiResponse.success("Holiday retrieved successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create Holiday",
            description = "Creates a single holiday record (non-batch)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> create(
            @Valid @RequestBody HolidayMasterRequest request
    ) {
        log.info("Create Holiday request | date={}, userId={}",
                request.getHolidayDate(),
                UserContext.getUserId());
        HolidayMasterResponse response = holidayMasterService.create(request);
        return ApiResponse.success("Holiday created successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{holidayPoid}")
    @Operation(
            summary = "Update Holiday",
            description = "Updates an existing holiday record",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> update(
            @PathVariable @NotNull @Positive Long holidayPoid,
            @Valid @RequestBody HolidayMasterRequest request
    ) {
        log.info("Update Holiday request | poid={}, date={}, userId={}",
                holidayPoid,
                request.getHolidayDate(),
                UserContext.getUserId());
        HolidayMasterResponse response =
                holidayMasterService.update(holidayPoid, request);
        return ApiResponse.success("Holiday updated successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{holidayPoid}/activate")
    @Operation(
            summary = "Toggle Holiday Active Status",
            description = "Toggles the active status of a holiday between Y and N",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> toggleActiveStatus(
            @PathVariable @NotNull @Positive Long holidayPoid
    ) {
        log.info("Toggle Holiday Active Status request | poid={}, userId={}",
                holidayPoid,
                UserContext.getUserId());

        holidayMasterService.toggleActiveStatus(holidayPoid);

        return ApiResponse.success("Holiday status toggled successfully");
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{holidayPoid}")
    @Operation(
            summary = "Delete Holiday",
            description = "Soft deletes a holiday record",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> delete(
            @PathVariable @NotNull @Positive Long holidayPoid
    ) {
        log.info("Delete Holiday request | poid={}, userId={}",
                holidayPoid,
                UserContext.getUserId());

        holidayMasterService.delete(holidayPoid);
        return ApiResponse.success("Holiday deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/batch")
    @Operation(
            summary = "Batch Create Holidays",
            description = """
                    Batch create holidays using legacy PROC_HR_CREATE_HOLIDAYS.

                    - startDate: First holiday date
                    - days: Number of consecutive days to create
                    - reason: Reason text to reuse for all created holidays

                    This wraps the legacy ADF button behavior into a single REST call.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> batchCreate(
            @Valid @RequestBody HolidayBatchCreateRequest request
    ) {
        log.info("Batch Create Holidays request | startDate={}, days={}, userPoid={}",
                request.getStartDate(),
                request.getDays(),
                UserContext.getUserPoid());

        String status = holidayMasterService.batchCreateHolidays(request);
        return ApiResponse.success("Batch holiday creation completed", Map.of("status", status));
    }
}

