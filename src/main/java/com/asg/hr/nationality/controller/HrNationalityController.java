package com.asg.hr.nationality.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.nationality.dto.request.HrNationalityRequest;
import com.asg.hr.nationality.dto.request.HrNationalityUpdateRequest;
import com.asg.hr.nationality.dto.response.HrNationalityResponse;
import com.asg.hr.nationality.service.HrNationalityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("v1/nationality-master")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "hr-nationality-controller", description = "Manage HR Nationality Master records")
public class HrNationalityController {

    private final HrNationalityService hrNationalityService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(summary = "Create HR Nationality", description = "Create a new HR Nationality record")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "HR Nationality Request",
                            value = """
                                {
                                  "nationalityCode": "IND",
                                  "nationalityDescription": "INDIA",
                                  "active": true,
                                  "seqNo": 1,
                                  "ticketAmountNormal": 100.0,
                                  "ticketAmountBusiness": 250.0
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "200",
            description = "Nationality created successfully",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "HR Nationality Response",
                            value = """
                                {
                                  "status": "success",
                                  "message": "Nationality created successfully",
                                  "data": {
                                    "nationPoid": 1,
                                    "nationalityCode": "IND",
                                    "nationalityDescription": "INDIA",
                                    "active": true,
                                    "seqno": 1,
                                    "ticketAmountNormal": 100.0,
                                    "ticketAmountBusiness": 250.0,
                                    "createdBy": "ADMIN",
                                    "createdDate": "2024-03-12T10:00:00",
                                    "lastModifiedBy": "ADMIN",
                                    "lastModifiedDate": "2024-03-12T10:00:00"
                                  }
                                }
                                """
                    )
            )
    )
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody HrNationalityRequest request) {
            HrNationalityResponse response = hrNationalityService.create(request);
            return success("Nationality created successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Update HR Nationality", description = "Update an existing HR Nationality record")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "HR Nationality Update Request",
                            value = """
                                {
                                  "nationalityDescription": "INDIA (Updated)",
                                  "active": true,
                                  "seqNo": 1,
                                  "ticketAmountNormal": 150.0,
                                  "ticketAmountBusiness": 300.0
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "200",
            description = "Nationality updated successfully",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "HR Nationality Response",
                            value = """
                                {
                                  "status": "success",
                                  "message": "Nationality updated successfully",
                                  "data": {
                                    "nationPoid": 1,
                                    "nationalityCode": "IND",
                                    "nationalityDescription": "INDIA (Updated)",
                                    "active": true,
                                    "seqno": 1,
                                    "ticketAmountNormal": 150.0,
                                    "ticketAmountBusiness": 300.0,
                                    "createdBy": "ADMIN",
                                    "createdDate": "2024-03-12T10:00:00",
                                    "lastModifiedBy": "ADMIN",
                                    "lastModifiedDate": "2024-03-12T12:00:00"
                                  }
                                }
                                """
                    )
            )
    )
    @PutMapping("/{nationPoid}")
    public ResponseEntity<?> update(
            @Parameter(description = "Nation POID", required = true) @PathVariable Long nationPoid,
            @Valid @RequestBody HrNationalityUpdateRequest request) {
            HrNationalityResponse response = hrNationalityService.update(nationPoid, request);
            return success("Nationality updated successfully", response);

    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get HR Nationality by ID", description = "Fetch a single HR Nationality record by its POID")
    @ApiResponse(
            responseCode = "200",
            description = "Nationality retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "HR Nationality Response",
                            value = """
                                {
                                  "status": "success",
                                  "message": "Nationality retrieved successfully",
                                  "data": {
                                    "nationPoid": 1,
                                    "nationalityCode": "IND",
                                    "nationalityDescription": "INDIA",
                                    "active": true,
                                    "seqno": 1,
                                    "ticketAmountNormal": 100.0,
                                    "ticketAmountBusiness": 250.0,
                                    "createdBy": "ADMIN",
                                    "createdDate": "2024-03-12T10:00:00",
                                    "lastModifiedBy": "ADMIN",
                                    "lastModifiedDate": "2024-03-12T10:00:00"
                                  }
                                }
                                """
                    )
            )
    )
    @GetMapping("/{nationPoid}")
    public ResponseEntity<?> getById(
            @Parameter(description = "Nation POID", required = true) @PathVariable Long nationPoid) {
        HrNationalityResponse response = hrNationalityService.getById(nationPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), nationPoid.toString());
        return success("Nationality retrieved successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @Operation(summary = "Delete HR Nationality", description = "Delete an existing HR Nationality record")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Delete Reason",
                            value = """
                                {
                                  "reason": "Record is no longer required"
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "200",
            description = "Nationality deleted successfully",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Success Response",
                            value = """
                                {
                                  "status": "success",
                                  "message": "Nationality deleted successfully",
                                  "data": null
                                }
                                """
                    )
            )
    )
    @DeleteMapping("/{nationPoid}")
    public ResponseEntity<?> delete(
            @Parameter(description = "Nation POID", required = true) @PathVariable Long nationPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
            hrNationalityService.delete(nationPoid, deleteReasonDto);
            return success("Nationality deleted successfully", null);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "List HR Nationalities", description = """
            Fetch HR Nationality Masters using filters and pagination.

            Valid `searchField` values: NATIONALITY_CODE, NATIONALITY_DESCRIPTION, ACTIVE
            """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody( content = @Content(examples = @ExampleObject(name = "HR Nationality Filters", value = """
            {
              "operator": "AND",
              "isDeleted": "N",
              "filters": [
                {
                  "searchField": "NATIONALITY_CODE",
                  "searchValue": "IND"
                },
                {
                  "searchField": "NATIONALITY_DESCRIPTION",
                  "searchValue": "India"
                },
                {
                  "searchField": "ACTIVE",
                  "searchValue": "Y"
                }
              ]
            }
            """)))
    @ApiResponse(
            responseCode = "200",
            description = "HR Nationalities list retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "HR Nationality List Response",
                            value = """
                                {
                                  "status": "success",
                                  "message": "HR Nationalities list retrieved successfully",
                                  "data": {
                                    "records": [
                                      {
                                        "nationPoid": 1,
                                        "nationalityCode": "IND",
                                        "nationalityDescription": "INDIA",
                                        "active": true,
                                        "seqno": 1,
                                        "ticketAmountNormal": 100.0,
                                        "ticketAmountBusiness": 250.0,
                                        "createdBy": "ADMIN",
                                        "createdDate": "2024-03-12T10:00:00",
                                        "lastModifiedBy": "ADMIN",
                                        "lastModifiedDate": "2024-03-12T10:00:00"
                                      }
                                    ],
                                    "totalRecords": 1
                                  }
                                }
                                """
                    )
            )
    )
    @PostMapping("/list")
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters) {
            Map<String, Object> response = hrNationalityService.list(filters, pageable);
            return success("HR Nationalities list retrieved successfully", response);
    }
}
