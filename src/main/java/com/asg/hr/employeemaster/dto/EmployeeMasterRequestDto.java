package com.asg.hr.employeemaster.dto;

import com.asg.hr.employeemaster.annotation.ValidLifeInsurance;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidLifeInsurance
public class EmployeeMasterRequestDto {

    @NotBlank(message = "First Name Is Required")
    @Size(max = 100, message = "First Name Must Be At Most 100 Characters")
    private String firstName;

    @NotBlank(message = "Last Name Is Required")
    @Size(max = 100, message = "Last Name Must Be At Most 100 Characters")
    private String lastName;

    @Size(max = 20, message = "Base Group Must Be At Most 20 Characters")
    private String baseGroup;

    @NotNull(message = "Location Is Required")
    private Long locationPoid;

    @NotNull(message = "Department Is Required")
    private Long departmentPoid;

    @NotNull(message = "Designation Is Required")
    private Long designationPoid;

    @NotNull(message = "Join Date Is Required")
    private LocalDate joinDate;

    @NotNull(message = "Nationality Is Required")
    private Long nationalityPoid;

    @Size(max = 20, message = "Gender Must Be At Most 20 Characters")
    private String gender;

    @Size(max = 20, message = "Marital Status Must Be At Most 20 Characters")
    private String maritalStatus;

    @NotNull(message = "Religion Is Required")
    private Long religionPoid;
    private LocalDate dateOfBirth;

    @Size(max = 500, message = "Present Address Must Be At Most 500 Characters")
    private String presentAddress;

    @Size(max = 500, message = "Permanent Address Must Be At Most 500 Characters")
    private String permanentAddress;

    @Size(max = 500, message = "Postal Address Must Be At Most 500 Characters")
    private String postalAddress;

    @Size(max = 20, message = "Home Country Phone Must Be At Most 20 Characters")
    private String homeCountryPhone;

    @Size(max = 20, message = "Mobile Must Be At Most 20 Characters")
    private String mobile;

    @Size(max = 100, message = "Personal Email Must Be At Most 100 Characters")
    private String personalEmail;

    @Size(max = 100, message = "Business Email Must Be At Most 100 Characters")
    private String businessEmail;

    @Size(max = 20, message = "Blood Group Must Be At Most 20 Characters")
    private String bloodGroup;

    @Size(max = 100, message = "Emergency Contact Person Must Be At Most 100 Characters")
    private String emergencyContactPerson;

    @Size(max = 20, message = "Emergency Contact No Must Be At Most 20 Characters")
    private String emergencyContactNo;

    @NotNull(message = "Service Start Date Is Required")
    private LocalDate serviceStartDate;

    @NotBlank(message = "Service Type Is Required")
    @Size(max = 20, message = "Service Type Must Be At Most 20 Characters")
    private String serviceType;
    private LocalDate contractStart;
    private LocalDate contractEnd;

    @Size(max = 20, message = "Probation Must Be At Most 20 Characters")
    private String probation;

    @Size(max = 20, message = "Notice Period Must Be At Most 20 Characters")
    private String noticePeriod;

    @NotNull(message = "HOD Is Required")
    private Long hod;
    private Long loginUserPoid;

    @Size(max = 500, message = "Job Description Must Be At Most 500 Characters")
    private String jobDescription;
    private Long airSectorPoid;

    @Size(max = 20, message = "Ticket Period Must Be At Most 20 Characters")
    private String ticketPeriod;

    @Size(max = 20, message = "No Of Tickets Must Be At Most 20 Characters")
    private String noOfTickets;

    @Size(max = 20, message = "Access Card Issued Must Be At Most 20 Characters")
    private String accessCardIssued;

    @Size(max = 20, message = "Discontinued Must Be At Most 20 Characters")
    private String discontinued;
    private LocalDate discontinuedDate;

    @Size(max = 200, message = "Reason Must Be At Most 200 Characters")
    private String reason;

    @Size(max = 200, message = "Other Reasons Must Be At Most 200 Characters")
    private String otherReasons;

    private BigDecimal basicSalary;
    private BigDecimal netSalary;
    private Long currencyPoid;

    @Size(max = 20, message = "Payment Method Must Be At Most 20 Characters")
    private String paymentMethod;

    private Long bankPoid;

    @Size(max = 50, message = "Account No Must Be At Most 50 Characters")
    private String accountNo;

    @Size(max = 50, message = "Iban Must Be At Most 50 Characters")
    private String iban;
    private BigDecimal registeredSalary;
    private LocalDate lastIncrementDate;
    private LocalDate nextIncrementDate;
    private BigDecimal holdSalary;

    @Size(max = 200, message = "Hold Reason Must Be At Most 200 Characters")
    private String holdReason;

    private Long crPoid;

    @NotBlank(message = "Passport No Is Required")
    @Size(max = 20, message = "Passport No Must Be At Most 20 Characters")
    private String passportNo;

    @NotNull(message = "Issued Date Is Required")
    private LocalDate issuedDate;

    @NotNull(message = "Expiry Date Is Required")
    private LocalDate expiryDate;

    @Size(max = 20, message = "Place Of Issue Must Be At Most 20 Characters")
    private String placeOfIssue;

    @Size(max = 50, message = "Passport Possessed By Must Be At Most 50 Characters")
    private String passportPossessedBy;

    @Size(max = 50, message = "Gosi No Must Be At Most 50 Characters")
    private String gosiNo;
    private BigDecimal gosiReturnAmount;

    @Size(max = 20, message = "Cpr No Must Be At Most 20 Characters")
    private String cprNo;
    private LocalDate cprExpiryDate;

    @Size(max = 50, message = "Cpr Occupation Must Be At Most 50 Characters")
    private String cprOccupation;

    @Size(max = 20, message = "Medical Insurance Must Be At Most 20 Characters")
    private String medicalInsurance;
    private LocalDate medicalInsuranceExpiryDate;

    @Size(max = 50, message = "Rp No Must Be At Most 50 Characters")
    private String rpNo;
    private LocalDate rpStartDate;
    private LocalDate rpExpiryDate;

    @Size(max = 1, message = "Active Must Be At Most 1 Character")
    @Pattern(regexp = "[YN]", message = "Active must be either 'Y' or 'N'")
    private String active;
    private Integer seqNo;

    @Size(max = 50, message = "Employee Biomatrix Id Must Be At Most 50 Characters")
    private String employeeBiomatrixId;

    @Size(max = 50, message = "Ot Applicable Must Be At Most 50 Characters")
    private String otApplicable;

    @Size(max = 100, message = "Insurance Nominee Must Be At Most 100 Characters")
    private String insuranceNominee;

    @Size(max = 20, message = "Insurance Nominee Relation Must Be At Most 20 Characters")
    private String insuranceNomineeRelation;
    private Long shiftPoid;

    @Size(max = 10, message = "Currency Code Must Be At Most 10 Characters")
    private String currencyCode;

    @Size(max = 1, message = "Deleted Must Be At Most 1 Character")
    @Pattern(regexp = "[YN]", message = "Deleted must be either 'Y' or 'N'")
    private String deleted;
    private LocalDate drivingLicExp;

    @Size(max = 200, message = "Nominee Contact Dtl Must Be At Most 200 Characters")
    private String nomineeContactDtl;

    @Size(max = 100, message = "Display Name Must Be At Most 100 Characters")
    private String displayName;

    @NotNull(message = "Employee Gl Is Required")
    private Long empGlPoid;

    @Size(max = 50, message = "Insu Nominee2 Must Be At Most 50 Characters")
    private String insuNominee2;

    @Size(max = 100, message = "Bank Register Employee No Must Be At Most 100 Characters")
    private String bankRegisterEmployeeNo;

    @Size(max = 1, message = "Probation Benefits Must Be At Most 1 Character")
    @Pattern(regexp = "[YN]", message = "Probation Benefits must be either 'Y' or 'N'")
    private String probationBenefits;
    private LocalDate probationCompletedOn;

    @Size(max = 1, message = "Life Insurance Must Be At Most 1 Character")
    @Pattern(regexp = "[YN]", message = "Life Insurance must be either 'Y' or 'N'")
    private String lifeInsurance;

    @Size(max = 1, message = "Attendance From Log Must Be At Most 1 Character")
    @Pattern(regexp = "[YN]", message = "Attendance From Log must be either 'Y' or 'N'")
    private String attendanceFromLog;

    @Size(max = 1, message = "Management Staff Must Be At Most 1 Character")
    @Pattern(regexp = "[YN]", message = "Management Staff must be either 'Y' or 'N'")
    private String managementStaff;

    @Size(max = 20, message = "Ext No Must Be At Most 20 Characters")
    private String extNo;

    @Size(max = 1, message = "Attendance Late Check Must Be At Most 1 Character")
    @Pattern(regexp = "[YN]", message = "Attendance Late Check must be either 'Y' or 'N'")
    private String attendanceLateCheck;

    @Size(max = 1, message = "Attendance Lunch Check Must Be At Most 1 Character")
    @Pattern(regexp = "[YN]", message = "Attendance Lunch Check must be either 'Y' or 'N'")
    private String attendanceLunchCheck;

    @Size(max = 100, message = "Passport Remarks Must Be At Most 100 Characters")
    private String passportRemarks;

    @Size(max = 100, message = "Place Of Hire Must Be At Most 100 Characters")
    private String placeOfHire;
    private Long pettyCashGlPoid;

    @Size(max = 1, message = "Attendance Check Must Be At Most 1 Character")
    @Pattern(regexp = "[YN]", message = "Attendance Check must be either 'Y' or 'N'")
    private String attendanceCheck;

    @Size(max = 100, message = "Recruitment Source Must Be At Most 100 Characters")
    private String recruitmentSource;

    @Size(max = 200, message = "Recruitment Details Must Be At Most 200 Characters")
    private String recruitmentDetails;

    @Size(max = 100, message = "Recruitment Budgeted Must Be At Most 100 Characters")
    private String recruitmentBudgeted;

    @Size(max = 100, message = "Recruitment Reason Must Be At Most 100 Characters")
    private String recruitmentReason;
    private BigDecimal ot1Limit;
    private BigDecimal ot2Limit;

    @Size(max = 1, message = "Short Hours Must Be At Most 1 Character")
    @Pattern(regexp = "[YN]", message = "Short Hours must be either 'Y' or 'N'")
    private String shortHours;

    @Size(max = 1, message = "Code Of Conduct Signed Must Be At Most 1 Character")
    @Pattern(regexp = "[YN]", message = "Code Of Conduct Signed must be either 'Y' or 'N'")
    private String codeOfConductSigned;

    @Size(max = 1, message = "Orientation Must Be At Most 1 Character")
    @Pattern(regexp = "[YN]", message = "Orientation must be either 'Y' or 'N'")
    private String orientation;

    @Size(max = 100, message = "Orientation Remarks Must Be At Most 100 Characters")
    private String orientationRemarks;

    @Size(max = 1, message = "Free Lunch Must Be At Most 1 Character")
    @Pattern(regexp = "[YN]", message = "Free Lunch must be either 'Y' or 'N'")
    private String freeLunch;

    @Size(max = 50, message = "Staff Accommodation Must Be At Most 50 Characters")
    private String staffAccommodation;

    @NotNull(message = "Actual Date of Birth Is Required")
    private LocalDate actualDob;

    // Child tables (frontend drives changes via actionType).
    @Valid
    private List<EmployeeDependentsDtlRequestDto> dependentsDetails;

    @Valid
    private List<EmployeeDepndtsLmraDtlsRequestDto> lmraDetails;

    @Valid
    private List<EmployeeExperienceDtlRequestDto> experienceDetails;

    @Valid
    private List<EmployeeDocumentDtlRequestDto> documentDetails;
}

