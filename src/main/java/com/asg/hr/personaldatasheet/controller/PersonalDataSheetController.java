package com.asg.hr.personaldatasheet.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.personaldatasheet.dto.PersonalDataSheetRequestDto;
import com.asg.hr.personaldatasheet.dto.PersonalDataSheetResponseDto;
import com.asg.hr.personaldatasheet.service.PersonalDataSheetService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/personal-data-sheet")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class PersonalDataSheetController {

    private final PersonalDataSheetService personalDataSheetService;
    private final LoggingService loggingService;

    @Operation(
            summary = "Create employee personal data sheet",
            description = "Creates a new employee personal data sheet with header and detail tabs. Company defaults to session company (read-only).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully created personal data sheet",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PersonalDataSheetResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input parameters or validation error",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @PostMapping
    @AllowedAction(UserRolesRightsEnum.CREATE)
    public ResponseEntity<?> createPersonalDataSheet(
            @Parameter(description = "Personal data sheet creation request", required = true)
            @Valid @RequestBody PersonalDataSheetRequestDto request) {

        log.info("createPersonalDataSheet started for groupPoid={} userId={}", 
                UserContext.getGroupPoid()
                , UserContext.getUserId());
        
        PersonalDataSheetResponseDto response = personalDataSheetService.create(request);
        
        log.info("createPersonalDataSheet completed for transactionPoid={}", 
                response != null ? response.getTransactionPoid() : null);
        
        return success("Personal data sheet created successfully", response);
    }

    @Operation(
            summary = "Get personal data sheet by ID",
            description = "Retrieves personal data sheet details including header and all detail tabs",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved personal data sheet",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PersonalDataSheetResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Personal data sheet not found",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @GetMapping("/{transactionPoid}")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    public ResponseEntity<?> getPersonalDataSheetById(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid) {

        log.info("getPersonalDataSheetById started for transactionPoid={} groupPoid={}", 
                transactionPoid, UserContext.getGroupPoid());
        
        PersonalDataSheetResponseDto response = personalDataSheetService.getById(transactionPoid);
        
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        
        log.info("getPersonalDataSheetById completed for transactionPoid={}", transactionPoid);
        return success("Personal data sheet fetched successfully", response);
    }

    @Operation(
            summary = "Update personal data sheet",
            description = "Updates an existing personal data sheet. Cannot update if document is approved.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated personal data sheet",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PersonalDataSheetResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input parameters or validation error",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @PutMapping("/{transactionPoid}")
    @AllowedAction(UserRolesRightsEnum.EDIT)
    public ResponseEntity<?> updatePersonalDataSheet(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Parameter(description = "Personal data sheet update request", required = true)
            @Valid @RequestBody PersonalDataSheetRequestDto request
           ) {

        log.info("updatePersonalDataSheet started for transactionPoid={} groupPoid={}", 
                transactionPoid, UserContext.getGroupPoid());
        
        PersonalDataSheetResponseDto response = personalDataSheetService.update(transactionPoid, request);
        
        log.info("updatePersonalDataSheet completed for transactionPoid={}", transactionPoid);
        return success("Personal data sheet updated successfully", response);
    }

    @Operation(
            summary = "Delete personal data sheet",
            description = "Deletes a personal data sheet. Cannot delete if document is approved.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully deleted personal data sheet"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Cannot delete approved personal data sheet",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @DeleteMapping("/{transactionPoid}")
    @AllowedAction(UserRolesRightsEnum.DELETE)
    public ResponseEntity<?> deletePersonalDataSheet(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Parameter(description = "Delete reason details", required = false)
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {

        log.info("deletePersonalDataSheet started for transactionPoid={} groupPoid={}", 
                transactionPoid, UserContext.getGroupPoid());
        
        personalDataSheetService.delete(transactionPoid, deleteReasonDto);
        
        log.info("deletePersonalDataSheet completed for transactionPoid={}", transactionPoid);
        return success("Personal data sheet deleted successfully", null);
    }

    @Operation(
            summary = "List Personal Data Sheets with Search and Sort (DocId: 800-112)",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (TRANSACTION_POID, DOC_REF, EMPLOYEE_NAME_PASSPORT). Sorting default on transactionPoid, desc." +
                    "Will be searched in all available fields given in list_of_records_sql or main_table field in doc_master table." +
                    "Sorting will be applied as specified in list_of_records_sql in doc_master table." +
                    "Display fields for showing columns can be customized through list_of_display_columns_and_types field in doc_master.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    description = """
                        - ### Filters:
                          Use either:
                          1. A single `GLOBALSEARCH` filter, OR
                          2. Any combination of specific fields (TRANSACTION_POID, DOC_REF, EMPLOYEE_NAME_PASSPORT).
                          3. operator field will either have "AND" or "OR", if not given will be considered as "OR",
                             not required for GLOBALSEARCH, for non GLOBALSEARCH need to give only 1 time
                          4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                          5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                        
                        - #### Global Search:
                          Apply one search term across multiple fields.
                          No operator need to send in this case.
                          • { "searchField": "GLOBALSEARCH", "searchValue": "2024" }
                        
                        - #### Single Field, Single Value:
                          Search one field with one value.
                          • { "searchField": "TRANSACTION_POID", "searchValue": "12345" },
                        
                        - #### Single Field, Multiple Values:
                          Provide multiple values for the same field, separated by `|`.
                          • { "searchField": "TRANSACTION_POID", "searchValue": "12345|12346" }
                        
                        - #### Multiple Different Fields, Single or Multiple Value:
                          Provide one or multiple search values across multiple fields.
                          • { "searchField": "TRANSACTION_POID", "searchValue": "12345" },
                          • { "searchField": "DOC_REF", "searchValue": "PDS-2024-001|PDS-2024-002" }
                          • "operator" :  "AND"/"OR"
                        
                        - ### Sorting:
                          Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                          To override, pass `sort=<field>,ASC|DESC` in query params.
                          Examples:
                          • sort=TRANSACTION_POID,ASC
                          • sort=DOC_REF,DESC
                        
                        - ### Authorization Parameters (handled by interceptor)
                            - **documentId:** Unique identifier for the document (`800-112`)
                            - **actionRequested:** Action being performed (`VIEW`)
                        """,
                    content = @Content(
                            schema = @Schema(implementation = FilterRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Personal Data Sheet Filters",
                                            value = """
                                                {
                                                  "operator": "AND",
                                                  "isDeleted": "N",
                                                  "filters": [
                                                     { "searchField": "GLOBALSEARCH", "searchValue": "2024" },
                                                     { "searchField": "DOC_REF", "searchValue": "PDS-2024-001|PDS-2024-002" },
                                                     { "searchField": "TRANSACTION_POID", "searchValue": "12345" }
                                                  ]
                                                }
                                                """
                                    )
                            }
                    )
            )
    )
    @PostMapping("/list")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    public ResponseEntity<?> listPersonalDataSheets(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters) {
        try {
            Map<String, Object> data = personalDataSheetService.list(UserContext.getDocumentId(), filters, pageable);
            return success("Personal data sheets fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch Personal Data Sheet list: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get login user employee ID",
            description = "Gets the employee mapped with the logged-in user and custom where clause for filtering records",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved login user employee information",
                            content = @Content(
                                    mediaType = "application/json"
                            )
                    )
            }
    )
    @GetMapping("/login-user-employee")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    public ResponseEntity<?> getLoginUserEmployee() {

        log.info("getLoginUserEmployee started for groupPoid={} userId={}", 
                UserContext.getGroupPoid(), UserContext.getUserId());
        
        Map<String, Object> response = personalDataSheetService.getLoginUserEmployee();
        
        log.info("getLoginUserEmployee completed");
        return success("Login user employee information retrieved successfully", response);
    }

    @Operation(
            summary = "Load user policies",
            description = "Loads all policies that the employee has accepted under the Policies tab (equivalent to calling PROC_HR_LOAD_USER_POLICIES)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully loaded user policies",
                            content = @Content(
                                    mediaType = "application/json"
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation error or employee not found",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @PostMapping("/load-user-policies")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    public ResponseEntity<?> loadUserPolicies(
            @Parameter(description = "Employee POID", required = true)
            @RequestParam Long employeePoid) {

        log.info("loadUserPolicies started for employeePoid={}", employeePoid);
        
        List<Map<String, Object>> response = personalDataSheetService.loadUserPolicies(employeePoid);
        
        log.info("loadUserPolicies completed for employeePoid={}", employeePoid);
        return success("User policies loaded successfully", response);
    }

    @Operation(
            summary = "Generate PDF for Personal Data Sheet",
            description = "Generate PDF report for a specific Personal Data Sheet transaction",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Personal Data Sheet not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "21")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = personalDataSheetService.print(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=personal-data-sheet-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Personal Data Sheet: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }
}