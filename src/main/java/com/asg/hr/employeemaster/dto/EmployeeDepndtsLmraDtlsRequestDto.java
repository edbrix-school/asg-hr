package com.asg.hr.employeemaster.dto;

import com.asg.hr.employeemaster.enums.ActionType;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDepndtsLmraDtlsRequestDto {

    private ActionType actionType;
    private Long employeePoid;

    // Required for isUpdated/isDeleted. If null for isCreated, backend will generate it.
    private Long detRowId;

    @Size(max = 20, message = "Expat Cpr Must Be At Most 20 Characters")
    private String expatCpr;

    @Size(max = 20, message = "Expat Pp Must Be At Most 20 Characters")
    private String expatPp;

    @Size(max = 20, message = "Nationality Must Be At Most 20 Characters")
    private String nationality;

    @Size(max = 20, message = "Primary Cpr Must Be At Most 20 Characters")
    private String primaryCpr;

    @Size(max = 20, message = "Wp Type Must Be At Most 20 Characters")
    private String wpType;
    private Integer permitMonths;

    @Size(max = 100, message = "Expat Name Must Be At Most 100 Characters")
    private String expatName;

    @Size(max = 10, message = "Expat Gender Must Be At Most 10 Characters")
    private String expatGender;

    private LocalDate wpExpiryDate;
    private LocalDate ppExpiryDate;

    @Size(max = 100, message = "Expat Current Status Must Be At Most 100 Characters")
    private String expatCurrentStatus;

    @Size(max = 100, message = "Wp Status Must Be At Most 100 Characters")
    private String wpStatus;

    @Size(max = 20, message = "In Out Status Must Be At Most 20 Characters")
    private String inOutStatus;

    @Size(max = 100, message = "Offense Classification Must Be At Most 100 Characters")
    private String offenseClassification;

    @Size(max = 50, message = "Offence Code Must Be At Most 50 Characters")
    private String offenceCode;

    @Size(max = 500, message = "Offence Description Must Be At Most 500 Characters")
    private String offenceDescription;

    @Size(max = 100, message = "Intention Must Be At Most 100 Characters")
    private String intention;

    @Size(max = 10, message = "Allow Mobility Must Be At Most 10 Characters")
    private String allowMobility;

    @Size(max = 100, message = "Mobility In Progress Must Be At Most 100 Characters")
    private String mobilityInProgress;

    @Size(max = 20, message = "Rp Cancelled Must Be At Most 20 Characters")
    private String rpCancelled;

    @Size(max = 100, message = "Rp Cancellation Reason Must Be At Most 100 Characters")
    private String rpCancellationReason;

    @Size(max = 10, message = "Photo Must Be At Most 10 Characters")
    private String photo;

    @Size(max = 10, message = "Signature Must Be At Most 10 Characters")
    private String signature;

    @Size(max = 10, message = "Finger Print Must Be At Most 10 Characters")
    private String fingerPrint;

    @Size(max = 100, message = "Health Check Result Must Be At Most 100 Characters")
    private String healthCheckResult;

    @Size(max = 50, message = "Additional Bh Permit Must Be At Most 50 Characters")
    private String additionalBhPermit;
}

