package com.asg.hr.employeeinduction.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.employeeinduction.dto.EmployeeInductionRequestDto;
import com.asg.hr.employeeinduction.dto.EmployeeInductionResponseDto;
import com.asg.hr.employeeinduction.dto.InductionCategoryDto;
import com.asg.hr.employeeinduction.service.EmployeeInductionService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/employee-induction")
@RequiredArgsConstructor
@Slf4j
public class EmployeeInductionController {

    private final EmployeeInductionService employeeInductionService;
    private final LoggingService loggingService;

    @Operation(
            summary = "Create Employee Induction",
            description = "Creates a new employee induction record",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully created"),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createInduction(@Valid @RequestBody EmployeeInductionRequestDto requestDto) {
        log.info("Creating employee induction for employee: {}", requestDto.getEmployeePoid());
        try {
            EmployeeInductionResponseDto response = employeeInductionService.createInduction(requestDto);
            return success("Employee Induction created successfully", response);
        } catch (Exception ex) {
            log.error("Failed to create employee induction", ex);
            return internalServerError(ex.getMessage());
        }
    }

    @Operation(
            summary = "Update Employee Induction",
            description = "Updates an existing employee induction record",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully updated"),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{poid}")
    public ResponseEntity<?> updateInduction(
            @Parameter(description = "Employee Induction ID", required = true)
            @PathVariable Long poid,
            @Valid @RequestBody EmployeeInductionRequestDto requestDto) {
        log.info("Updating employee induction with id: {}", poid);
        try {
            EmployeeInductionResponseDto response = employeeInductionService.updateInduction(poid, requestDto);
            return success("Employee Induction updated successfully", response);
        } catch (Exception ex) {
            log.error("Failed to update employee induction with id: {}", poid, ex);
            return internalServerError(ex.getMessage());
        }
    }

    @Operation(
            summary = "Get Employee Induction by ID",
            description = "Retrieves employee induction details by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{poid}")
    public ResponseEntity<?> getInductionById(
            @Parameter(description = "Employee Induction ID", required = true)
            @PathVariable Long poid) {
        log.info("Getting employee induction with id: {}", poid);
        try {
            EmployeeInductionResponseDto response = employeeInductionService.getInductionById(poid);
            loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), poid.toString());
            return success("Employee Induction fetched successfully", response);
        } catch (Exception ex) {
            log.error("Failed to get employee induction with id: {}", poid, ex);
            return notFound(ex.getMessage());
        }
    }

    @Operation(
            summary = "Search Employee Inductions",
            description = """
                    Fetch Employee Inductions using pagination + dynamic filters.
                    
                    ### Allowed searchField values:
                    POID, DOC_ID, EMPLOYEE_POID, CREATED_DATE
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            description = "Search Filters for Employee Inductions",
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = @ExampleObject(
                            name = "Employee Induction Filters",
                            value = """
                                    {
                                      "operator": "AND",
                                      "isDeleted": "N",
                                      "filters": [
                                        { "searchField": "EMPLOYEE_POID", "searchValue": "123" }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestBody(required = false) FilterRequestDto filterRequest) {
        log.info("Searching employee inductions with page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        try {
            Map<String, Object> result = employeeInductionService.list(filterRequest, startDate, endDate, pageable);
            return success("Employee Inductions retrieved successfully", result);
        } catch (Exception ex) {
            log.error("Failed to search employee inductions", ex);
            return internalServerError("Failed to search: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get Employee Inductions by Employee",
            description = "Retrieves all induction records for a specific employee",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/employee/{employeePoid}")
    public ResponseEntity<?> getInductionsByEmployee(
            @Parameter(description = "Employee ID", required = true)
            @PathVariable Long employeePoid) {
        log.info("Getting employee inductions for employee: {}", employeePoid);
        try {
            Map<String, Object> response = employeeInductionService.getInductionsByEmployee(employeePoid);
            return success("Employee Inductions fetched successfully", response);
        } catch (Exception ex) {
            log.error("Failed to get employee inductions for employee: {}", employeePoid, ex);
            return notFound(ex.getMessage());
        }
    }

    @Operation(
            summary = "Delete Employee Induction",
            description = "Soft deletes an employee induction record",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully deleted"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{poid}")
    public ResponseEntity<?> deleteInduction(
            @Parameter(description = "Employee Induction ID", required = true)
            @PathVariable Long poid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        log.info("Deleting employee induction with id: {}", poid);
        try {
            employeeInductionService.deleteInduction(poid, deleteReasonDto);
            return success("Employee Induction deleted successfully");
        } catch (Exception ex) {
            log.error("Failed to delete employee induction with id: {}", poid, ex);
            return notFound(ex.getMessage());
        }
    }

    @Operation(
            summary = "Send Overdue Notifications",
            description = "Sends email notifications for overdue induction activities",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Notifications sent successfully")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/send-overdue-notifications")
    public ResponseEntity<?> sendOverdueNotifications() {
        log.info("Sending overdue notifications for employee inductions");
        try {
            employeeInductionService.sendOverdueNotifications();
            return success("Overdue notifications sent successfully");
        } catch (Exception ex) {
            log.error("Failed to send overdue notifications", ex);
            return internalServerError(ex.getMessage());
        }
    }

    @Operation(
            summary = "Load Employee Induction",
            description = "Loads existing induction records for the selected employee",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully loaded"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/load/{employeePoid}")
    public ResponseEntity<?> loadInductionByEmployee(
            @Parameter(description = "Employee ID", required = true)
            @PathVariable Long employeePoid) {
        log.info("Loading employee induction for employee: {}", employeePoid);
        try {
            Map<String, Object> response = employeeInductionService.loadInductionByEmployee(employeePoid);
            return success("Employee Induction loaded successfully", response);
        } catch (Exception ex) {
            log.error("Failed to load employee induction for employee: {}", employeePoid, ex);
            return notFound(ex.getMessage());
        }
    }

    @Operation(
            summary = "Get Induction Categories",
            description = "Retrieves all available induction categories for dropdown selection",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/induction-categories")
    public ResponseEntity<?> getInductionCategories() {
        log.info("Getting induction categories");
        try {
            List<InductionCategoryDto> categories = employeeInductionService.getInductionCategories();
            return success("Induction categories retrieved successfully", categories);
        } catch (Exception ex) {
            log.error("Failed to get induction categories", ex);
            return internalServerError("Failed to retrieve induction categories: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Generate PDF for Employee Induction",
            description = "Generate PDF report for a specific Employee Induction transaction",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Employee Induction not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/print/{poid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Employee Induction POID", example = "1")
            @PathVariable Long poid) {
        log.info("Generating PDF for employee induction with id: {}", poid);
        try {
            byte[] pdf = employeeInductionService.print(poid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=employee-induction-" + poid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for employee induction with id: {}", poid, e);
            return internalServerError("Failed to generate PDF: " + e.getMessage());
        }
    }
}