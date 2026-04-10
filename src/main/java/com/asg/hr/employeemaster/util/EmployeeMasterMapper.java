package com.asg.hr.employeemaster.util;

import com.asg.common.lib.security.util.UserContext;
import com.asg.hr.employeemaster.dto.*;
import com.asg.hr.employeemaster.entity.*;
import com.asg.hr.employeemaster.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EmployeeMasterMapper {

    private final HrEmployeeDependentRepository dependentRepository;
    private final HrEmpDepndtsLmraDtlsRepository lmraRepository;
    private final HrEmployeeDocumentDtlRepository documentRepository;
    private final HrEmployeeExperienceDtlRepository experienceRepository;
    private final HrEmployeeLeaveHistoryRepository leaveHistoryRepository;

    public EmployeeMasterResponseDto toResponseDto(HrEmployeeMaster entity) {
        Long employeePoid = entity.getEmployeePoid();

        List<HrEmployeeDependentsDtl> dependents = dependentRepository.findByEmployeePoid(employeePoid);
        List<HrEmpDepndtsLmraDtls> lmraDetails = lmraRepository.findByEmployeePoid(employeePoid);
        List<HrEmployeeExperienceDtl> experience = experienceRepository.findByEmployeePoid(employeePoid);
        List<HrEmployeeDocumentDtl> documents = documentRepository.findByEmployeePoid(employeePoid);
        List<HrEmployeeLeaveHistory> leaveHistory = leaveHistoryRepository.findByEmployeePoid(employeePoid);

        return EmployeeMasterResponseDto.builder()
                .employeePoid(entity.getEmployeePoid())
                .groupPoid(entity.getGroupPoid())

                .employeeCode(entity.getEmployeeCode())
                .employeeName(entity.getEmployeeName())
                .employeeName2(entity.getEmployeeName2())
                .baseGroup(entity.getBaseGroup())

                .companyPoid(entity.getCompanyPoid())
                .locationPoid(entity.getLocationPoid())
                .departmentPoid(entity.getDepartmentPoid())
                .designationPoid(entity.getDesignationPoid())

                .photo(entity.getPhoto())
                .joinDate(entity.getJoinDate())
                .nationalityPoid(entity.getNationalityPoid())
                .gender(entity.getGender())
                .maritalStatus(entity.getMaritalStatus())
                .religionPoid(entity.getReligionPoid())
                .dateOfBirth(entity.getDateOfBirth())
                .presentAddress(entity.getPresentAddress())
                .permanentAddress(entity.getPermanentAddress())
                .postalAddress(entity.getPostalAddress())
                .homeCountryPhone(entity.getHomeCountryPhone())

                .mobile(entity.getMobile())
                .personalEmail(entity.getPersonalEmail())
                .businessEmail(entity.getBusinessEmail())
                .bloodGroup(entity.getBloodGroup())
                .emergencyContactPerson(entity.getEmergencyContactPerson())
                .emergencyContactNo(entity.getEmergencyContactNo())

                .serviceStartDate(entity.getServiceStartDate())
                .serviceType(entity.getServiceType())
                .contractStart(entity.getContractStart())
                .contractEnd(entity.getContractEnd())
                .probation(entity.getProbation())
                .noticePeriod(entity.getNoticePeriod())
                .directSupervisorPoid(entity.getDirectSupervisorPoid())
                .loginUserPoid(entity.getLoginUserPoid())
                .jobDescription(entity.getJobDescription())
                .airSectorPoid(entity.getAirSectorPoid())
                .ticketPeriod(entity.getTicketPeriod())
                .noOfTickets(entity.getNoOfTickets())
                .accessCardIssued(entity.getAccessCardIssued())
                .discontinued(entity.getDiscontinued())
                .discontinuedDate(entity.getDiscontinuedDate())
                .reason(entity.getReason())
                .otherReasons(entity.getOtherReasons())

                .basicSalary(entity.getBasicSalary())
                .netSalary(entity.getNetSalary())
                .currencyPoid(entity.getCurrencyPoid())
                .paymentMethod(entity.getPaymentMethod())

                .bankPoid(entity.getBankPoid())
                .accountNo(entity.getAccountNo())
                .iban(entity.getIban())
                .registeredSalary(entity.getRegisteredSalary())
                .lastIncrementDate(entity.getLastIncrementDate())
                .nextIncrementDate(entity.getNextIncrementDate())
                .holdSalary(entity.getHoldSalary())
                .holdReason(entity.getHoldReason())
                .crPoid(entity.getCrPoid())
                .passportNo(entity.getPassportNo())
                .issuedDate(entity.getIssuedDate())
                .expiryDate(entity.getExpiryDate())
                .placeOfIssue(entity.getPlaceOfIssue())
                .passportPossessedBy(entity.getPassportPossessedBy())
                .gosiNo(entity.getGosiNo())
                .gosiReturnAmount(entity.getGosiReturnAmount())
                .cprNo(entity.getCprNo())
                .cprExpiryDate(entity.getCprExpiryDate())
                .cprOccupation(entity.getCprOccupation())
                .medicalInsurance(entity.getMedicalInsurance())
                .medicalInsuranceExpiryDate(entity.getMedicalInsuranceExpiryDate())
                .rpNo(entity.getRpNo())
                .rpStartDate(entity.getRpStartDate())
                .rpExpiryDate(entity.getRpExpiryDate())

                .active(entity.getActive())
                .seqNo(entity.getSeqNo())
                .employeeBiomatrixId(entity.getEmployeeBiomatrixId())
                .otApplicable(entity.getOtApplicable())
                .insuranceNominee(entity.getInsuranceNominee())
                .insuranceNomineeRelation(entity.getInsuranceNomineeRelation())
                .shiftPoid(entity.getShiftPoid())
                .currencyCode(entity.getCurrencyCode())
                .deleted(entity.getDeleted())
                .drivingLicExp(entity.getDrivingLicExp())
                .nomineeContactDtl(entity.getNomineeContactDtl())
                .displayName(entity.getDisplayName())
                .empGlPoid(entity.getEmpGlPoid())
                .insuNominee2(entity.getInsuNominee2())
                .bankRegisterEmployeeNo(entity.getBankRegisterEmployeeNo())
                .probationBenefits(entity.getProbationBenefits())
                .probationCompletedOn(entity.getProbationCompletedOn())
                .lifeInsurance(entity.getLifeInsurance())
                .attendanceFromLog(entity.getAttendanceFromLog())
                .managementStaff(entity.getManagementStaff())
                .extNo(entity.getExtNo())
                .attendanceLateCheck(entity.getAttendanceLateCheck())
                .attendanceLunchCheck(entity.getAttendanceLunchCheck())
                .passportRemarks(entity.getPassportRemarks())
                .placeOfHire(entity.getPlaceOfHire())
                .pettyCashGlPoid(entity.getPettyCashGlPoid())
                .attendanceCheck(entity.getAttendanceCheck())
                .recruitmentSource(entity.getRecruitmentSource())
                .recruitmentDetails(entity.getRecruitmentDetails())
                .recruitmentBudgeted(entity.getRecruitmentBudgeted())
                .recruitmentReason(entity.getRecruitmentReason())
                .ot1Limit(entity.getOt1Limit())
                .ot2Limit(entity.getOt2Limit())
                .shortHours(entity.getShortHours())
                .codeOfConductSigned(entity.getCodeOfConductSigned())
                .orientation(entity.getOrientation())
                .orientationRemarks(entity.getOrientationRemarks())
                .freeLunch(entity.getFreeLunch())
                .staffAccommodation(entity.getStaffAccommodation())
                .actualDob(entity.getActualDob())

                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())

                .dependentsDetails(dependents.stream().map(this::toDependentsResponseDto).toList())
                .lmraDetails(lmraDetails.stream().map(this::toLmraResponseDto).toList())
                .experienceDetails(experience.stream().map(this::toExperienceResponseDto).toList())
                .documentDetails(documents.stream().map(this::toDocumentResponseDto).toList())
                .leaveHistoryDetails(leaveHistory.stream().map(this::toLeaveHistoryResponseDto).toList())
                .build();
    }

    public void applyHeaderFields(HrEmployeeMaster entity, EmployeeMasterRequestDto requestDto) {

        if (requestDto.getEmployeeCode() != null) {
            entity.setEmployeeCode(requestDto.getEmployeeCode());
        }

        entity.setLocationPoid(requestDto.getLocationPoid());
        entity.setDepartmentPoid(requestDto.getDepartmentPoid());
        entity.setDesignationPoid(requestDto.getDesignationPoid());

        entity.setEmployeeName(requestDto.getFirstName());
        entity.setEmployeeName2(requestDto.getLastName());
        entity.setBaseGroup(requestDto.getBaseGroup());

        entity.setJoinDate(requestDto.getJoinDate());
        entity.setNationalityPoid(requestDto.getNationalityPoid());
        entity.setReligionPoid(requestDto.getReligionPoid());
        entity.setGender(requestDto.getGender());
        entity.setMaritalStatus(requestDto.getMaritalStatus());
        entity.setDateOfBirth(requestDto.getDateOfBirth());
        entity.setPresentAddress(requestDto.getPresentAddress());
        entity.setPermanentAddress(requestDto.getPermanentAddress());
        entity.setPostalAddress(requestDto.getPostalAddress());
        entity.setHomeCountryPhone(requestDto.getHomeCountryPhone());

        entity.setMobile(requestDto.getMobile());
        entity.setPersonalEmail(requestDto.getPersonalEmail());
        entity.setBusinessEmail(requestDto.getBusinessEmail());
        entity.setBloodGroup(requestDto.getBloodGroup());
        entity.setEmergencyContactPerson(requestDto.getEmergencyContactPerson());
        entity.setEmergencyContactNo(requestDto.getEmergencyContactNo());

        entity.setServiceStartDate(requestDto.getServiceStartDate());
        entity.setServiceType(requestDto.getServiceType());
        entity.setContractStart(requestDto.getContractStart());
        entity.setContractEnd(requestDto.getContractEnd());
        entity.setProbation(requestDto.getProbation());
        entity.setNoticePeriod(requestDto.getNoticePeriod());
        entity.setDirectSupervisorPoid(requestDto.getHod());
        entity.setLoginUserPoid(UserContext.getUserPoid());
        entity.setJobDescription(requestDto.getJobDescription());
        entity.setAirSectorPoid(requestDto.getAirSectorPoid());
        entity.setTicketPeriod(requestDto.getTicketPeriod());
        entity.setNoOfTickets(requestDto.getNoOfTickets());
        entity.setAccessCardIssued(requestDto.getAccessCardIssued());
        entity.setDiscontinued(requestDto.getDiscontinued());
        entity.setDiscontinuedDate(requestDto.getDiscontinuedDate());
        entity.setReason(requestDto.getReason());
        entity.setOtherReasons(requestDto.getOtherReasons());

        entity.setBasicSalary(requestDto.getBasicSalary());
        entity.setNetSalary(requestDto.getNetSalary());
        entity.setCurrencyPoid(requestDto.getCurrencyPoid());
        entity.setPaymentMethod(requestDto.getPaymentMethod());
        entity.setBankPoid(requestDto.getBankPoid());
        entity.setAccountNo(requestDto.getAccountNo());
        entity.setIban(requestDto.getIban());
        entity.setRegisteredSalary(requestDto.getRegisteredSalary());
        entity.setLastIncrementDate(requestDto.getLastIncrementDate());
        entity.setNextIncrementDate(requestDto.getNextIncrementDate());
        entity.setHoldSalary(requestDto.getHoldSalary());
        entity.setHoldReason(requestDto.getHoldReason());

        entity.setCrPoid(requestDto.getCrPoid());
        entity.setPassportNo(requestDto.getPassportNo());
        entity.setIssuedDate(requestDto.getIssuedDate());
        entity.setExpiryDate(requestDto.getExpiryDate());
        entity.setPlaceOfIssue(requestDto.getPlaceOfIssue());
        entity.setPassportPossessedBy(requestDto.getPassportPossessedBy());
        entity.setGosiNo(requestDto.getGosiNo());
        entity.setGosiReturnAmount(requestDto.getGosiReturnAmount());
        entity.setCprNo(requestDto.getCprNo());
        entity.setCprExpiryDate(requestDto.getCprExpiryDate());
        entity.setCprOccupation(requestDto.getCprOccupation());
        entity.setMedicalInsurance(requestDto.getMedicalInsurance());
        entity.setMedicalInsuranceExpiryDate(requestDto.getMedicalInsuranceExpiryDate());
        entity.setRpNo(requestDto.getRpNo());
        entity.setRpStartDate(requestDto.getRpStartDate());
        entity.setRpExpiryDate(requestDto.getRpExpiryDate());

        entity.setActive(requestDto.getActive());
        entity.setSeqNo(requestDto.getSeqNo());
        entity.setEmployeeBiomatrixId(requestDto.getEmployeeBiomatrixId());
        entity.setOtApplicable(requestDto.getOtApplicable());
        entity.setInsuranceNominee(requestDto.getInsuranceNominee());
        entity.setInsuranceNomineeRelation(requestDto.getInsuranceNomineeRelation());
        entity.setShiftPoid(requestDto.getShiftPoid());
        entity.setCurrencyCode(requestDto.getCurrencyCode());
        entity.setDeleted(requestDto.getDeleted() != null ? requestDto.getDeleted() : "N");
        entity.setDrivingLicExp(requestDto.getDrivingLicExp());
        entity.setNomineeContactDtl(requestDto.getNomineeContactDtl());
        entity.setDisplayName(requestDto.getDisplayName());
        entity.setEmpGlPoid(requestDto.getEmpGlPoid());
        entity.setInsuNominee2(requestDto.getInsuNominee2());
        entity.setBankRegisterEmployeeNo(requestDto.getBankRegisterEmployeeNo());
        entity.setProbationBenefits(requestDto.getProbationBenefits());
        entity.setProbationCompletedOn(requestDto.getProbationCompletedOn());
        entity.setLifeInsurance(requestDto.getLifeInsurance());
        entity.setAttendanceFromLog(requestDto.getAttendanceFromLog());
        entity.setManagementStaff(requestDto.getManagementStaff());
        entity.setExtNo(requestDto.getExtNo());
        entity.setAttendanceLateCheck(requestDto.getAttendanceLateCheck());
        entity.setAttendanceLunchCheck(requestDto.getAttendanceLunchCheck());
        entity.setPassportRemarks(requestDto.getPassportRemarks());
        entity.setPlaceOfHire(requestDto.getPlaceOfHire());
        entity.setPettyCashGlPoid(requestDto.getPettyCashGlPoid());
        entity.setAttendanceCheck(requestDto.getAttendanceCheck());
        entity.setRecruitmentSource(requestDto.getRecruitmentSource());
        entity.setRecruitmentDetails(requestDto.getRecruitmentDetails());
        entity.setRecruitmentBudgeted(requestDto.getRecruitmentBudgeted());
        entity.setRecruitmentReason(requestDto.getRecruitmentReason());
        entity.setOt1Limit(requestDto.getOt1Limit());
        entity.setOt2Limit(requestDto.getOt2Limit());
        entity.setShortHours(requestDto.getShortHours());
        entity.setCodeOfConductSigned(requestDto.getCodeOfConductSigned());
        entity.setOrientation(requestDto.getOrientation());
        entity.setOrientationRemarks(requestDto.getOrientationRemarks());
        entity.setFreeLunch(requestDto.getFreeLunch());
        entity.setStaffAccommodation(requestDto.getStaffAccommodation());
        entity.setActualDob(requestDto.getActualDob());
    }

    private EmployeeDependentsDtlResponseDto toDependentsResponseDto(HrEmployeeDependentsDtl entity) {
        return EmployeeDependentsDtlResponseDto.builder()
                .employeePoid(entity.getEmployeePoid())
                .detRowId(entity.getDetRowId())
                .name(entity.getName())
                .dateOfBirth(entity.getDateOfBirth())
                .relation(entity.getRelation())
                .gender(entity.getGender())
                .nationality(entity.getNationality())
                .passportNo(entity.getPassportNo())
                .ppExpiryDate(entity.getPpExpiryDate())
                .cprNo(entity.getCprNo())
                .cprExpiry(entity.getCprExpiry())
                .insuDetails(entity.getInsuDetails())
                .insuStartDt(entity.getInsuStartDt())
                .sponsor(entity.getSponsor())
                .rpExpiry(entity.getRpExpiry())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    private EmployeeDepndtsLmraDtlsResponseDto toLmraResponseDto(HrEmpDepndtsLmraDtls entity) {
        return EmployeeDepndtsLmraDtlsResponseDto.builder()
                .employeePoid(entity.getEmployeePoid())
                .detRowId(entity.getDetRowId())
                .expatCpr(entity.getExpatCpr())
                .expatPp(entity.getExpatPp())
                .nationality(entity.getNationality())
                .primaryCpr(entity.getPrimaryCpr())
                .wpType(entity.getWpType())
                .permitMonths(entity.getPermitMonths())
                .expatName(entity.getExpatName())
                .expatGender(entity.getExpatGender())
                .wpExpiryDate(entity.getWpExpiryDate())
                .ppExpiryDate(entity.getPpExpiryDate())
                .expatCurrentStatus(entity.getExpatCurrentStatus())
                .wpStatus(entity.getWpStatus())
                .inOutStatus(entity.getInOutStatus())
                .offenseClassification(entity.getOffenseClassification())
                .offenceCode(entity.getOffenceCode())
                .offenceDescription(entity.getOffenceDescription())
                .intention(entity.getIntention())
                .allowMobility(entity.getAllowMobility())
                .mobilityInProgress(entity.getMobilityInProgress())
                .rpCancelled(entity.getRpCancelled())
                .rpCancellationReason(entity.getRpCancellationReason())
                .photo(entity.getPhoto())
                .signature(entity.getSignature())
                .fingerPrint(entity.getFingerPrint())
                .healthCheckResult(entity.getHealthCheckResult())
                .additionalBhPermit(entity.getAdditionalBhPermit())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    private EmployeeExperienceDtlResponseDto toExperienceResponseDto(HrEmployeeExperienceDtl entity) {
        return EmployeeExperienceDtlResponseDto.builder()
                .employeePoid(entity.getEmployeePoid())
                .detRowId(entity.getDetRowId())
                .employer(entity.getEmployer())
                .countryLocation(entity.getCountryLocation())
                .fromDate(entity.getFromDate())
                .toDate(entity.getToDate())
                .months(entity.getMonths())
                .designation(entity.getDesignation())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    private EmployeeDocumentDtlResponseDto toDocumentResponseDto(HrEmployeeDocumentDtl entity) {
        return EmployeeDocumentDtlResponseDto.builder()
                .employeePoid(entity.getEmployeePoid())
                .detRowId(entity.getDetRowId())
                .docName(entity.getDocName())
                .expiryDate(entity.getExpiryDate())
                .remarks(entity.getRemarks())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    private EmployeeLeaveHistoryResponseDto toLeaveHistoryResponseDto(HrEmployeeLeaveHistory entity) {
        return EmployeeLeaveHistoryResponseDto.builder()
                .leaveHistPoid(entity.getLeaveHistPoid())
                .detRowId(entity.getDetRowId())
                .companyPoid(entity.getCompanyPoid())
                .groupPoid(entity.getGroupPoid())
                .deptPoid(entity.getDeptPoid())
                .employeePoid(entity.getEmployeePoid())
                .employeeName(entity.getEmployeeName())
                .leaveStartDate(entity.getLeaveStartDate())
                .rejoinDate(entity.getRejoinDate())
                .reffNo(entity.getReffNo())
                .remarks(entity.getRemarks())
                .deleted(entity.getDeleted())
                .leaveType(entity.getLeaveType())
                .leaveDays(entity.getLeaveDays())
                .annualLeaveType(entity.getAnnualLeaveType())
                .sourceDocId(entity.getSourceDocId())
                .sourceDocPoid(entity.getSourceDocPoid())
                .emergencyLeaveType(entity.getEmergencyLeaveType())
                .expRejoinDate(entity.getExpRejoinDate())
                .splLeaveTypes(entity.getSplLeaveTypes())
                .eligibleLeaveDays(entity.getEligibleLeaveDays())
                .ticketIssuedCount(entity.getTicketIssuedCount())
                .ticketTillDate(entity.getTicketTillDate())
                .ticketIssueType(entity.getTicketIssueType())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }
}
