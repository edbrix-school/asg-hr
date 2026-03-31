package com.asg.hr.employeetraining.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.employeetraining.dto.EmployeeTrainingRequest;
import com.asg.hr.employeetraining.dto.EmployeeTrainingResponse;
import com.asg.hr.employeetraining.service.EmployeeTrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import static com.asg.common.lib.dto.response.ApiResponse.success;
import static com.asg.common.lib.dto.response.ApiResponse.error;


@RestController
@RequestMapping("/v1/employee-training")
@RequiredArgsConstructor
@Validated
@Slf4j
public class EmployeeTrainingController {

    private final EmployeeTrainingService employeeTrainingService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "List Employee Training Records",
            description = "Returns paginated training list via document-search filters for docId 800-108",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            description = "Dynamic filters for training list",
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = @ExampleObject(
                            name = "Training Filters",
                            value = """
                                    {
                                      "operator": "AND",
                                      "isDeleted": "N",
                                      "filters": [
                                        { "searchField": "COURSE_NAME", "searchValue": "Safety" },
                                        { "searchField": "TRAINING_TYPE", "searchValue": "INHOUSE" }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/list")
    public ResponseEntity<?> listTrainings(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filterRequest
    ) {
        log.info("List Employee Training request | page={}, size={}",
                pageable.getPageNumber(),
                pageable.getPageSize());
        Map<String, Object> result = employeeTrainingService.listTrainings(UserContext.getDocumentId(), filterRequest, pageable);
        return success("Employee training list fetched successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    @Operation(
            summary = "Get Employee Training by ID",
            description = "Returns complete header and detail data",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getById(
            @PathVariable @NotNull @Positive Long transactionPoid
    ) {
        log.info("Get Employee Training request | poid={}", transactionPoid);
        EmployeeTrainingResponse response = employeeTrainingService.getTrainingById(transactionPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        return success("Employee training fetched successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create Employee Training",
            description = "Creates header + detail rows in one API",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> createTraining(
            @Valid @RequestBody EmployeeTrainingRequest request
    ) {
        log.info("Create Employee Training request | userId={}, employeePoid={}, courseName={}",
                UserContext.getUserId(),
                request.getEmployeePoid(),
                request.getCourseName());
        EmployeeTrainingResponse response = employeeTrainingService.createTraining(request);
        return success("Employee training created successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    @Operation(
            summary = "Update Employee Training",
            description = "Updates header + replaces detail rows in one API",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> updateTraining(
            @PathVariable @NotNull @Positive Long transactionPoid,
            @Valid @RequestBody EmployeeTrainingRequest request
    ) {
        log.info("Update Employee Training request | poid={}, userId={}, employeePoid={}, courseName={}",
                transactionPoid,
                UserContext.getUserId(),
                request.getEmployeePoid(),
                request.getCourseName());
        EmployeeTrainingResponse response = employeeTrainingService.updateTraining(transactionPoid, request);
        return success("Employee training updated successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    @Operation(
            summary = "Delete Employee Training",
            description = "Soft delete by setting DELETED = Y",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> deleteTraining(
            @PathVariable @NotNull @Positive Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        employeeTrainingService.deleteTraining(transactionPoid, deleteReasonDto);
        return success("Employee training deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for Employee Induction",
            description = "Generate PDF report for a Employee Induction",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Employee Induction Rpt not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "21")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = employeeTrainingService.print(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=hr-employee-induction-rpt-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Employee Induction: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

}
