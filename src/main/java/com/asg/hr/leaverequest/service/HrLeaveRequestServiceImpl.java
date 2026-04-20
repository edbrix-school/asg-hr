package com.asg.hr.leaverequest.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.exceptions.ResourceNotFoundException;
import com.asg.hr.exceptions.ValidationException;
import com.asg.hr.leaverequest.dto.LeaveCalculationResponseDto;
import com.asg.hr.leaverequest.dto.LeaveCreateRequestDto;
import com.asg.hr.leaverequest.dto.LeaveHistoryUpdateRequestDto;
import com.asg.hr.leaverequest.dto.LeaveRequestDetailDto;
import com.asg.hr.leaverequest.dto.LeaveResponseDto;
import com.asg.hr.leaverequest.dto.LeaveTicketUpdateRequestDto;
import com.asg.hr.leaverequest.dto.LeaveUpdateRequestDto;
import com.asg.hr.leaverequest.entity.HrLeaveRequestDtl;
import com.asg.hr.leaverequest.entity.HrLeaveRequestDtlId;
import com.asg.hr.leaverequest.entity.HrLeaveRequestHdrEntity;
import com.asg.hr.leaverequest.repository.HrLeaveProcedureRepository;
import com.asg.hr.leaverequest.repository.HrLeaveRequestDtlRepository;
import com.asg.hr.leaverequest.repository.HrLeaveRequestHdrRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class HrLeaveRequestServiceImpl implements HrLeaveRequestService {

    private static final DateTimeFormatter LEGACY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    private final HrLeaveRequestHdrRepository hdrRepo;
    private final HrLeaveRequestDtlRepository dtlRepo;
    private final HrLeaveProcedureRepository repository;
    private final LeaveFullValidationService validation;
    private final PrintService printService;
    private final DataSource dataSource;
    private final DocumentDeleteService documentDeleteService;
    private final DocumentSearchService documentService;


    public LeaveResponseDto create(LeaveCreateRequestDto req) {

        normalizeLeaveTypeFields(req);
        validation.validateBeforeSave(req);
        validateAgainstAccumLeave(req);
        validateAnnualProbation(req);
        validateNoDateOverlap(null, req.getEmployeePoid(), req.getLeaveStartDate(), req.getPlanedRejoinDate());

        Map<String, Object> validationResult =
                repository.validateLeave(
                        null,
                        req.getLeaveStartDate(),
                        req.getPlanedRejoinDate(),
                        req.getEmployeePoid(),
                        req.getLeaveType(),
                        getSubType(req),
                        UserContext.getUserPoid()
                );

        String status = (String) validationResult.get("status");
        handleValidationStatus(status, req.getLeaveType());

        BigDecimal leaveDays = getRequiredLeaveDays(validationResult);

        HrLeaveRequestHdrEntity entity = new HrLeaveRequestHdrEntity();

        entity.setGroupPoid(Optional.ofNullable(req.getGroupPoid()).orElse(UserContext.getGroupPoid()));
        entity.setCompanyPoid(req.getCompanyPoid());
        entity.setEmployeePoid(req.getEmployeePoid());
        entity.setTransactionDate(LocalDate.now());
        applyRequest(entity, req);
        entity.setLeaveDays(leaveDays);
        entity.setStatus("SUBMIT_FOR_APPROVAL");
        entity.setDeleted("N");

        entity = hdrRepo.save(entity);

        saveDetails(entity.getTransactionPoid(), req.getDetails());

        return getById(entity.getTransactionPoid());
    }

    private void saveDetails(Long tranId, List<LeaveRequestDetailDto> details) {
        if (details == null) return;

        Long maxRowId = dtlRepo.findMaxDetRowIdByTransactionPoid(tranId);
        long nextRowId = (maxRowId != null ? maxRowId : 0L);

        for (LeaveRequestDetailDto d : details) {
            HrLeaveRequestDtl dtl = new HrLeaveRequestDtl();
            HrLeaveRequestDtlId id = new HrLeaveRequestDtlId();
            id.setTransactionPoid(tranId);
            id.setDetRowId(d.getDetRowId() != null ? d.getDetRowId() : ++nextRowId);
            dtl.setId(id);
            dtl.setName(d.getName());
            dtl.setRelation(d.getRelation());
            dtl.setTicketAgeGroup(d.getTicketAgeGroup());
            dtl.setDateFrom(d.getDateFrom());
            dtl.setDateTo(d.getDateTo());
            dtl.setRemarks(d.getRemarks());
            dtlRepo.save(dtl);
        }
    }

    private void mergeDetails(Long tranId, List<LeaveRequestDetailDto> details) {
        if (details == null) return;

        Long maxRowId = dtlRepo.findMaxDetRowIdByTransactionPoid(tranId);
        long nextRowId = (maxRowId != null ? maxRowId : 0L);

        for (LeaveRequestDetailDto d : details) {
            String action = d.getActionType() != null ? d.getActionType().toUpperCase() : "ISCREATED";

            switch (action) {
                case "ISDELETED" -> {
                    if (d.getDetRowId() != null) {
                        HrLeaveRequestDtlId id = new HrLeaveRequestDtlId();
                        id.setTransactionPoid(tranId);
                        id.setDetRowId(d.getDetRowId());
                        dtlRepo.deleteById(id);
                    }
                }
                case "ISUPDATED" -> {
                    if (d.getDetRowId() == null) break;
                    HrLeaveRequestDtlId id = new HrLeaveRequestDtlId();
                    id.setTransactionPoid(tranId);
                    id.setDetRowId(d.getDetRowId());
                    HrLeaveRequestDtl dtl = dtlRepo.findById(id).orElse(new HrLeaveRequestDtl());
                    dtl.setId(id);
                    dtl.setName(d.getName());
                    dtl.setRelation(d.getRelation());
                    dtl.setTicketAgeGroup(d.getTicketAgeGroup());
                    dtl.setDateFrom(d.getDateFrom());
                    dtl.setDateTo(d.getDateTo());
                    dtl.setRemarks(d.getRemarks());
                    dtlRepo.save(dtl);
                }
                case "NOCHANGE" -> { /* nothing */ }
                default -> { // ISCREATED
                    HrLeaveRequestDtl dtl = new HrLeaveRequestDtl();
                    HrLeaveRequestDtlId id = new HrLeaveRequestDtlId();
                    id.setTransactionPoid(tranId);
                    id.setDetRowId(d.getDetRowId() != null ? d.getDetRowId() : ++nextRowId);
                    dtl.setId(id);
                    dtl.setName(d.getName());
                    dtl.setRelation(d.getRelation());
                    dtl.setTicketAgeGroup(d.getTicketAgeGroup());
                    dtl.setDateFrom(d.getDateFrom());
                    dtl.setDateTo(d.getDateTo());
                    dtl.setRemarks(d.getRemarks());
                    dtlRepo.save(dtl);
                }
            }
        }
    }


    private String getSubType(LeaveCreateRequestDto req) {


        String subType = null;

        if (contains(req.getLeaveType(), "ANNUAL")) {
            subType = req.getAnnualLeaveType();
        } else if (contains(req.getLeaveType(), "EMERGENCY")) {
            subType = req.getEmergencyLeaveType();
        } else {
            subType = req.getSplLeaveTypes();
        }

        //  FIX 4: guard
        if (subType == null || subType.trim().isEmpty()) {
            throw new ValidationException("Leave subtype is required");
        }

        return subType;
    }

    @Override
    public LeaveResponseDto update(LeaveUpdateRequestDto req) {

        HrLeaveRequestHdrEntity entity = hdrRepo.findById(req.getTransactionPoid())
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        normalizeLeaveTypeFields(req);
        LeaveCreateRequestDto createRequest = convertToCreate(req, entity);
        validation.validateBeforeSave(createRequest);
        validateAgainstAccumLeave(createRequest);
        validateAnnualProbation(createRequest);
        validateNoDateOverlap(req.getTransactionPoid(), entity.getEmployeePoid(), req.getLeaveStartDate(), req.getPlanedRejoinDate());

        Map<String, Object> validationResult =
                repository.validateLeave(
                        req.getTransactionPoid(),
                        req.getLeaveStartDate(),
                        req.getPlanedRejoinDate(),
                        entity.getEmployeePoid(),
                        req.getLeaveType(),
                        getSubType(req),
                        UserContext.getUserPoid()
                );

        String status = (String) validationResult.get("status");
        handleValidationStatus(status, req.getLeaveType());

        BigDecimal leaveDays = getRequiredLeaveDays(validationResult);

        applyRequest(entity, req);
        entity.setLeaveDays(leaveDays);

        hdrRepo.save(entity);

        mergeDetails(entity.getTransactionPoid(), req.getDetails());

        if (Boolean.TRUE.equals(req.getUpdateHistory())) {
            repository.updateLeaveHistory(entity.getTransactionPoid(), null, null, null);
        }

        if (Boolean.TRUE.equals(req.getCancelHistory())) {
            repository.unUpdateLeaveHistory(entity.getTransactionPoid());
        }

        return getById(entity.getTransactionPoid());
    }

    private LeaveCreateRequestDto convertToCreate(
            LeaveUpdateRequestDto req,
            HrLeaveRequestHdrEntity entity) {

        LeaveCreateRequestDto c = new LeaveCreateRequestDto();

        c.setEmployeePoid(entity.getEmployeePoid());
        c.setCompanyPoid(entity.getCompanyPoid());
        c.setLeaveDaysMethod(req.getLeaveDaysMethod());
        c.setAnnualEncashmentRight(req.getAnnualEncashmentRight());

        c.setLeaveType(req.getLeaveType());
        c.setAnnualLeaveType(req.getAnnualLeaveType());
        c.setEmergencyLeaveType(req.getEmergencyLeaveType());
        c.setSplLeaveTypes(req.getSplLeaveTypes());

        c.setLeaveStartDate(req.getLeaveStartDate());
        c.setPlanedRejoinDate(req.getPlanedRejoinDate());
        c.setUpdateRejoinDate(req.getUpdateRejoinDate());
        c.setLeaveDays(req.getLeaveDays());
        c.setEligibleLeaveDays(req.getEligibleLeaveDays());
        c.setBalanceTillRejoin(req.getBalanceTillRejoin());
        c.setCalendarDays(req.getCalendarDays());
        c.setHolidays(req.getHolidays());

        c.setContactNumber(req.getContactNumber());
        c.setMedicalRecordsAttached(req.getMedicalRecordsAttached());
        c.setOtherLeaveReason(req.getOtherLeaveReason());
        c.setHodRemarks(req.getHodRemarks());
        c.setHod(req.getHod());

        c.setTicketRequired(req.getTicketRequired());
        c.setTicketFromLocn(req.getTicketFromLocn());
        c.setTicketToLocn(req.getTicketToLocn());
        c.setTicketTravelDate(req.getTicketTravelDate());
        c.setTicketReturnDate(req.getTicketReturnDate());
        c.setTicketCount(req.getTicketCount());
        c.setTicketRemarks(req.getTicketRemarks());
        c.setTicketEligiblity(req.getTicketEligiblity());
        c.setTicketEarned(req.getTicketEarned());
        c.setTicketPeriod(req.getTicketPeriod());
        c.setSettlementPoid(req.getSettlementPoid());
        c.setBookedTicket(req.getBookedTicket());
        c.setAirSectorPoid(req.getAirSectorPoid());
        c.setPaidLeaves(req.getPaidLeaves());
        c.setMedicalEligible(req.getMedicalEligible());
        c.setMedicalTaken(req.getMedicalTaken());
        c.setMedicalBalance(req.getMedicalBalance());
        c.setLastLeaveDetails(req.getLastLeaveDetails());
        c.setLastTicketDetails(req.getLastTicketDetails());
        c.setHrTicketIssueType(req.getHrTicketIssueType());
        c.setHrTicketIssuedCount(req.getHrTicketIssuedCount());
        c.setHrTicketEncashment(req.getHrTicketEncashment());
        c.setHrTicketsEarned(req.getHrTicketsEarned());
        c.setHrTicketTillDate(req.getHrTicketTillDate());

        return c;
    }

    private String getSubType(LeaveUpdateRequestDto req) {

        if (contains(req.getLeaveType(), "ANNUAL"))
            return req.getAnnualLeaveType();

        if (contains(req.getLeaveType(), "EMERGENCY"))
            return req.getEmergencyLeaveType();

        return req.getSplLeaveTypes();
    }

    private void applyRequest(HrLeaveRequestHdrEntity entity, LeaveCreateRequestDto req) {
        entity.setLeaveType(req.getLeaveType());
        entity.setAnnualLeaveType(req.getAnnualLeaveType());
        entity.setEmergencyLeaveType(req.getEmergencyLeaveType());
        entity.setSplLeaveTypes(req.getSplLeaveTypes());

        entity.setLeaveStartDate(req.getLeaveStartDate());
        entity.setPlanedRejoinDate(req.getPlanedRejoinDate());
        entity.setUpdateRejoinDate(req.getUpdateRejoinDate() != null ? req.getUpdateRejoinDate() : "Y");
        entity.setEligibleLeaveDays(req.getEligibleLeaveDays());
        entity.setBalanceTillRejoin(req.getBalanceTillRejoin());
        entity.setCalendarDays(req.getCalendarDays());
        entity.setHolidays(req.getHolidays());

        entity.setContactNumber(req.getContactNumber());
        entity.setMedicalRecordsAttached(req.getMedicalRecordsAttached());
        entity.setOtherLeaveReason(req.getOtherLeaveReason());
        entity.setHodRemarks(req.getHodRemarks());
        entity.setHod(req.getHod());

        entity.setTicketRequired(req.getTicketRequired());
        entity.setTicketFromLocn(req.getTicketFromLocn());
        entity.setTicketToLocn(req.getTicketToLocn());
        entity.setTicketTravelDate(req.getTicketTravelDate());
        entity.setTicketReturnDate(req.getTicketReturnDate());
        entity.setTicketCount(req.getTicketCount());
        entity.setTicketRemarks(req.getTicketRemarks());
        entity.setTicketEligiblity(req.getTicketEligiblity());
        entity.setTicketEarned(req.getTicketEarned());
        entity.setTicketPeriod(req.getTicketPeriod());
        entity.setSettlementPoid(req.getSettlementPoid());
        entity.setBookedTicket(req.getBookedTicket());
        entity.setAirSectorPoid(req.getAirSectorPoid());
        entity.setPaidLeaves(req.getPaidLeaves());
        entity.setMedicalEligible(req.getMedicalEligible());
        entity.setMedicalTaken(req.getMedicalTaken());
        entity.setMedicalBalance(req.getMedicalBalance());
        entity.setLastLeaveDetails(req.getLastLeaveDetails());
        entity.setLastTicketDetails(req.getLastTicketDetails());
        entity.setHrTicketIssueType(req.getHrTicketIssueType());
        entity.setHrTicketIssuedCount(req.getHrTicketIssuedCount());
        entity.setHrTicketEncashment(req.getHrTicketEncashment());
        entity.setHrTicketsEarned(req.getHrTicketsEarned());
        entity.setHrTicketTillDate(req.getHrTicketTillDate());
    }

    private void applyRequest(HrLeaveRequestHdrEntity entity, LeaveUpdateRequestDto req) {
        LeaveCreateRequestDto create = convertToCreate(req, entity);
        applyRequest(entity, create);
    }

    private void handleValidationStatus(String status, String leaveType) {
        if (status == null) {
            throw new ValidationException("Leave validation did not return a status");
        }

        if (status.contains("ERROR")) {
            throw new ValidationException(status);
        }
    }

    private BigDecimal getRequiredLeaveDays(Map<String, Object> validationResult) {
        Object value = validationResult.get("leaveDays");
        if (value == null) {
            throw new ValidationException("Leave days is not calculated, please check the dates entered");
        }

        BigDecimal leaveDays = new BigDecimal(value.toString());
        if (leaveDays.signum() < 0) {
            throw new ValidationException("Leave days is not calculated, please check the dates entered");
        }
        return leaveDays;
    }

    private boolean contains(String value, String token) {
        return value != null && value.toUpperCase().contains(token);
    }

    private void validateAgainstAccumLeave(LeaveCreateRequestDto req) {
        if (!isLeaveHolidayRunning(req.getLeaveType(), req.getLeaveDaysMethod())) {
            return;
        }
        if (!contains(req.getLeaveType(), "ANNUAL")) {
            return;
        }
        if (Boolean.TRUE.equals(req.getAnnualEncashmentRight())) {
            return;
        }
        if (!"AGAINST_ACCUM_LEAVE".equalsIgnoreCase(req.getAnnualLeaveType())) {
            throw new ValidationException("Annual Leave must be AGAINST_ACCUM_LEAVE");
        }
    }

    private boolean isLeaveHolidayRunning(String leaveType, String leaveDaysMethod) {


        if (leaveDaysMethod == null || leaveDaysMethod.isBlank()) {
            leaveDaysMethod = "DEFAULT"; // fallback (same behavior as legacy param)
        }

        if ("START_END_DURATION".equalsIgnoreCase(leaveDaysMethod)) {
            return false;
        }

        if (contains(leaveType, "ANNUAL") || contains(leaveType, "EMERGENCY")) {
            return true;
        }

        return false;
    }

    private void normalizeLeaveTypeFields(LeaveCreateRequestDto req) {
        if (contains(req.getLeaveType(), "ANNUAL")) {
            req.setEmergencyLeaveType(null);
            req.setSplLeaveTypes(null);
            return;
        }

        if (contains(req.getLeaveType(), "EMERGENCY")) {
            req.setAnnualLeaveType(null);
            req.setSplLeaveTypes(null);
            return;
        }

        if (contains(req.getLeaveType(), "SPECIAL_LEAVE")) {
            req.setAnnualLeaveType(null);
            req.setEmergencyLeaveType(null);
            return;
        }

        if (contains(req.getLeaveType(), "MEDICAL")) {
            req.setAnnualLeaveType(null);
            req.setEmergencyLeaveType(null);
        }
    }

    private void normalizeLeaveTypeFields(LeaveUpdateRequestDto req) {
        if (contains(req.getLeaveType(), "ANNUAL")) {
            req.setEmergencyLeaveType(null);
            req.setSplLeaveTypes(null);
            return;
        }

        if (contains(req.getLeaveType(), "EMERGENCY")) {
            req.setAnnualLeaveType(null);
            req.setSplLeaveTypes(null);
            return;
        }

        if (contains(req.getLeaveType(), "SPECIAL_LEAVE")) {
            req.setAnnualLeaveType(null);
            req.setEmergencyLeaveType(null);
            return;
        }

        if (contains(req.getLeaveType(), "MEDICAL")) {
            req.setAnnualLeaveType(null);
            req.setEmergencyLeaveType(null);
        }
    }

    private void validateAnnualProbation(LeaveCreateRequestDto req) {
        if (!contains(req.getLeaveType(), "ANNUAL")) {
            return;
        }

        Map<String, Object> employeeDetails = repository.getEmployeeDetails(req.getEmployeePoid());
        Object data = employeeDetails.get("data");

        if (!(data instanceof List<?> rows) || rows.isEmpty()) {
            return; // no employee data found, skip probation check
        }

        Object firstRow = rows.get(0);
        Integer probationMonths = extractInteger(firstRow, "probation", 4);
        LocalDate joinDate = extractLocalDate(firstRow, "joinDate", 3);

        if (probationMonths == null || probationMonths <= 0 || joinDate == null) {
            return;
        }

        LocalDate probationEndDate = joinDate.plusDays((long) probationMonths * 30);
        if (probationEndDate.isAfter(req.getLeaveStartDate())) {
            throw new ValidationException("Annual Leave can not be requested for employees in probation, try without pay");
        }

        System.out.println("Employee Data Row: " + firstRow);
        System.out.println("Probation: " + probationMonths);
        System.out.println("JoinDate: " + joinDate);
    }

    private void validateNoDateOverlap(Long transactionPoid, Long employeePoid, LocalDate leaveStartDate, LocalDate planedRejoinDate) {
        long requestOverlaps = hdrRepo.countOverlappingLeaveRequests(
                employeePoid,
                transactionPoid,
                leaveStartDate,
                planedRejoinDate
        );

        if (requestOverlaps > 0) {
            throw new ValidationException("Leave request overlaps with an existing leave request");
        }

        long historyOverlaps = hdrRepo.countOverlappingLeaveHistory(
                employeePoid,
                transactionPoid,
                UserContext.getDocumentId(),
                leaveStartDate,
                planedRejoinDate
        );

        if (historyOverlaps > 0) {
            throw new ValidationException("Leave request overlaps with employee leave history");
        }
    }

    private Integer extractInteger(Object row, String key, int index) {
        Object value = extractValue(row, key, index);
        return value instanceof Number number ? number.intValue() : null;
    }

    private LocalDate extractLocalDate(Object row, String key, int index) {

        Object value = extractValue(row, key, index);

        if (value == null) return null;

        if (value instanceof LocalDate localDate) {
            return localDate;
        }

        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }

        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();   //
        }

        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        return null;
    }

    private Object extractValue(Object row, String key, int index) {
        if (row instanceof Map<?, ?> map) {
            return map.get(key);
        }
        if (row instanceof Object[] values && values.length > index) {
            return values[index];
        }
        return null;
    }

    @Override
    public LeaveResponseDto getById(Long transactionPoid) {

        HrLeaveRequestHdrEntity entity = hdrRepo.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        List<HrLeaveRequestDtl> dtls =
                dtlRepo.findByIdTransactionPoid(transactionPoid);

        LeaveResponseDto dto = new LeaveResponseDto();

        dto.setTransactionPoid(entity.getTransactionPoid());
        dto.setGroupPoid(entity.getGroupPoid());
        dto.setCompanyPoid(entity.getCompanyPoid());
        dto.setEmployeePoid(entity.getEmployeePoid());

        dto.setLeaveType(entity.getLeaveType());
        dto.setAnnualLeaveType(entity.getAnnualLeaveType());
        dto.setEmergencyLeaveType(entity.getEmergencyLeaveType());
        dto.setSplLeaveTypes(entity.getSplLeaveTypes());

        dto.setLeaveStartDate(entity.getLeaveStartDate());
        dto.setPlanedRejoinDate(entity.getPlanedRejoinDate());
        dto.setUpdateRejoinDate(entity.getUpdateRejoinDate());

        dto.setLeaveDays(entity.getLeaveDays());
        dto.setEligibleLeaveDays(entity.getEligibleLeaveDays());
        dto.setBalanceTillRejoin(entity.getBalanceTillRejoin());
        dto.setBalanceLeaveDays(entity.getBalanceLeaveDays());
        dto.setCalendarDays(entity.getCalendarDays());
        dto.setHolidays(entity.getHolidays());

        dto.setContactNumber(entity.getContactNumber());
        dto.setMedicalRecordsAttached(entity.getMedicalRecordsAttached());
        dto.setOtherLeaveReason(entity.getOtherLeaveReason());
        dto.setHodRemarks(entity.getHodRemarks());
        dto.setHod(entity.getHod());

        dto.setTicketRequired(entity.getTicketRequired());
        dto.setTicketFromLocn(entity.getTicketFromLocn());
        dto.setTicketToLocn(entity.getTicketToLocn());
        dto.setTicketTravelDate(entity.getTicketTravelDate());
        dto.setTicketReturnDate(entity.getTicketReturnDate());
        dto.setTicketCount(entity.getTicketCount());
        dto.setTicketRemarks(entity.getTicketRemarks());
        dto.setTicketEligiblity(entity.getTicketEligiblity());
        dto.setTicketEarned(entity.getTicketEarned());
        dto.setTicketPeriod(entity.getTicketPeriod());
        dto.setSettlementPoid(entity.getSettlementPoid());
        dto.setBookedTicket(entity.getBookedTicket());
        dto.setAirSectorPoid(entity.getAirSectorPoid());
        dto.setTicketBookBy(entity.getTicketBookBy());
        dto.setTicketProcessed(entity.getTicketProcessed());
        dto.setTicketsIssued(entity.getTicketsIssued());
        dto.setPjDocRef(entity.getPjDocRef());
        dto.setPaidLeaves(entity.getPaidLeaves());
        dto.setMedicalEligible(entity.getMedicalEligible());
        dto.setMedicalTaken(entity.getMedicalTaken());
        dto.setMedicalBalance(entity.getMedicalBalance());
        dto.setLastLeaveDetails(entity.getLastLeaveDetails());
        dto.setLastTicketDetails(entity.getLastTicketDetails());
        dto.setHrTicketIssueType(entity.getHrTicketIssueType());
        dto.setHrTicketIssuedCount(entity.getHrTicketIssuedCount());
        dto.setHrTicketEncashment(entity.getHrTicketEncashment());
        dto.setHrTicketsEarned(entity.getHrTicketsEarned());
        dto.setHrTicketTillDate(entity.getHrTicketTillDate());

        dto.setStatus(entity.getStatus());

        dto.setDetails(
                dtls.stream().map(d -> {
                    LeaveRequestDetailDto dd = new LeaveRequestDetailDto();
                    dd.setDetRowId(d.getId().getDetRowId());
                    dd.setName(d.getName());
                    dd.setRelation(d.getRelation());
                    dd.setTicketAgeGroup(d.getTicketAgeGroup());
                    dd.setDateFrom(d.getDateFrom());
                    dd.setDateTo(d.getDateTo());
                    dd.setRemarks(d.getRemarks());
                    return dd;
                }).toList()
        );

        Map<String, Object> eligible =
                repository.getEligibleLeaveDays(
                        entity.getCompanyPoid(),
                        entity.getEmployeePoid(),
                        entity.getLeaveStartDate(),
                        entity.getSettlementPoid()
                );

        List<Map<String, Object>> data =
                (List<Map<String, Object>>) eligible.get("data");

        if (data != null && !data.isEmpty()) {
            Map<String, Object> row = data.get(0);

            dto.setTicketEarned(toBigDecimal(row.get("earnedTicket")));
            dto.setTicketEligiblity(toBigDecimal(row.get("eligibleTicket")));
            dto.setTicketPeriod(toBigDecimal(row.get("ticketPeriod")));
            dto.setPaidLeaves(toBigDecimal(row.get("paidLeavesTaken")));
            dto.setMedicalTaken(toBigDecimal(row.get("medicalTaken")));
            dto.setMedicalEligible(toBigDecimal(row.get("medicalEligible")));
            dto.setLastLeaveDetails(row.get("lastLeaveDetails") != null ? row.get("lastLeaveDetails").toString() : null);
            dto.setLastTicketDetails(row.get("lastTicketDetails") != null ? row.get("lastTicketDetails").toString() : null);

            BigDecimal leaveDays = dto.getLeaveDays() != null ? dto.getLeaveDays() : BigDecimal.ZERO;
            if (dto.getEligibleLeaveDays() != null) {
                dto.setBalanceTillRejoin(dto.getEligibleLeaveDays().subtract(leaveDays));
            }
        }

        return dto;
    }

    @Override
    public  Map<String, Object> list(String documentId, FilterRequestDto filters, LocalDate startDateValue, LocalDate endDateValue, Pageable pageable) {

        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDateValue, endDateValue);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "DOC_REF",   // label
                "TRANSACTION_POID");    // value

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private LeaveResponseDto mapHeaderToResponse(HrLeaveRequestHdrEntity entity) {
        LeaveResponseDto dto = new LeaveResponseDto();

        dto.setTransactionPoid(entity.getTransactionPoid());
        dto.setGroupPoid(entity.getGroupPoid());
        dto.setCompanyPoid(entity.getCompanyPoid());
        dto.setEmployeePoid(entity.getEmployeePoid());
        dto.setLeaveType(entity.getLeaveType());
        dto.setAnnualLeaveType(entity.getAnnualLeaveType());
        dto.setEmergencyLeaveType(entity.getEmergencyLeaveType());
        dto.setSplLeaveTypes(entity.getSplLeaveTypes());
        dto.setLeaveStartDate(entity.getLeaveStartDate());
        dto.setPlanedRejoinDate(entity.getPlanedRejoinDate());
        dto.setLeaveDays(entity.getLeaveDays());
        dto.setStatus(entity.getStatus());
        dto.setOtherLeaveReason(entity.getOtherLeaveReason());
        dto.setTicketRequired(entity.getTicketRequired());
        dto.setTicketCount(entity.getTicketCount());
        dto.setHod(entity.getHod());
        return dto;
    }

    public void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto) {

        HrLeaveRequestHdrEntity entity = hdrRepo.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));


        if ("APPROVED".equalsIgnoreCase(entity.getStatus())) {
            throw new ValidationException("Approved leave cannot be deleted");
        }

        documentDeleteService.deleteDocument(
                transactionPoid,
                "HR_LEAVE_REQUEST_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                entity.getTransactionDate()
        );

    }

    @Override
    public Map<String, Object> getEmployeeDetails(Long employeePoid) {

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> repoResponse =
                    repository.getEmployeeDetails(employeePoid);

            String status = (String) repoResponse.get("status");

            if (status != null && status.contains("SUCCESS")) {
                response.put("data", repoResponse.getOrDefault("data", Collections.emptyList()));
            } else {
                response.put("data", Collections.emptyList());
            }

            response.put("status", status);

        } catch (Exception ex) {
            response.put("status", "ERROR: " + ex.getMessage());
            response.put("data", Collections.emptyList());
        }

        return response;
    }

    @Override
    public Map<String, Object> getEmployeeHod(Long employeePoid) {

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> repoResponse =
                    repository.getEmployeeHod(employeePoid);

            String status = (String) repoResponse.get("status");

            if (status != null && status.contains("SUCCESS")) {
                response.put("hod", repoResponse.get("hod"));
            } else {
                response.put("hod", null);
            }

            response.put("status", status);

        } catch (Exception ex) {
            response.put("status", "ERROR: " + ex.getMessage());
            response.put("hod", null);
        }

        return response;
    }

    @Override
    public Map<String, Object> getEligibleLeaveDays(
            Long companyId,
            Long empPoid,
            LocalDate leaveStartDate,
            Long settlementPoid
    ) {

        Map<String, Object> response = new HashMap<>();

        try {

            Map<String, Object> repoResponse =
                    repository.getEligibleLeaveDays(
                            companyId,
                            empPoid,
                            leaveStartDate,
                            settlementPoid
                    );

            String status = (String) repoResponse.get("status");

            if (status != null && status.contains("SUCCESS")) {
                response.put("data", repoResponse.get("data"));
            } else {
                response.put("data", Collections.emptyMap());
            }

            response.put("status", status);

        } catch (Exception ex) {
            ex.printStackTrace();
            response.put("status", "ERROR: " + ex.getMessage());
            response.put("data", Collections.emptyMap());
        }

        return response;
    }

    @Override
    public Map<String, Object> getTicketFamilyDetails(Long empPoid) {

        Map<String, Object> response = new HashMap<>();

        try {

            List<Map<String, Object>> data =
                    repository.getTicketFamilyDetails(empPoid);

            if (data != null && !data.isEmpty()) {
                response.put("data", data);
                response.put("status", "SUCCESS");
            } else {
                response.put("data", Collections.emptyList());
                response.put("status", "NO_DATA");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            response.put("status", "ERROR: " + ex.getMessage());
            response.put("data", Collections.emptyList());
        }

        return response;
    }

    @Override
    public Map<String, Object> updateLeaveHistory(
            Long tranId,
            String ticketIssueType,
            String ticketTillDate,
            String ticketIssuedCount) {

        Map<String, Object> response = new HashMap<>();

        try {

            String status = repository.updateLeaveHistory(
                    tranId,
                    ticketIssueType,
                    ticketTillDate,
                    ticketIssuedCount
            );

            if (status != null && status.contains("SUCCESS")) {
                response.put("status", status);
                response.put("data", "UPDATED");
            } else {
                response.put("status", status);
                response.put("data", null);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            response.put("status", "ERROR: " + ex.getMessage());
            response.put("data", null);
        }

        return response;
    }

    @Override
    public Map<String, Object> updateLeaveHistory(LeaveHistoryUpdateRequestDto request) {
        if (request.getTransactionPoid() == null) {
            throw new ValidationException("Transaction poid is required");
        }

        HrLeaveRequestHdrEntity entity = hdrRepo.findById(request.getTransactionPoid())
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        String ticketTillDate = request.getHrTicketTillDate() != null
                ? request.getHrTicketTillDate().format(LEGACY_DATE_FORMAT)
                : null;
        String ticketIssuedCount = request.getHrTicketIssuedCount() != null
                ? request.getHrTicketIssuedCount().toPlainString()
                : null;

        String status = repository.updateLeaveHistory(
                request.getTransactionPoid(),
                request.getHrTicketIssueType(),
                ticketTillDate,
                ticketIssuedCount
        );

        if (status != null && status.contains("ERROR")) {
            throw new ValidationException(status);
        }

        entity.setHrTicketIssueType(request.getHrTicketIssueType());
        entity.setHrTicketTillDate(request.getHrTicketTillDate());
        entity.setHrTicketIssuedCount(request.getHrTicketIssuedCount());
        hdrRepo.save(entity);

        return Map.of(
                "status", status != null ? status : "SUCCESS",
                "message", "Leave history updated"
        );
    }

    @Override
    public Map<String, Object> cancelLeaveHistory(Long tranId) {
        if (tranId == null) {
            throw new ValidationException("Transaction poid is required");
        }

        hdrRepo.findById(tranId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        String status = repository.unUpdateLeaveHistory(tranId);

        if ("NO_DATA".equalsIgnoreCase(status)) {
            throw new ResourceNotFoundException("Please check, no record found");
        }
        if (status != null && status.contains("ERROR")) {
            throw new ValidationException(status);
        }

        return Map.of(
                "status", status != null ? status : "SUCCESS",
                "message", "This record is removed from history"
        );
    }

    @Override
    public Map<String, Object> updateTicketDetails(LeaveTicketUpdateRequestDto request) {

        if (request.getTransactionPoid() == null) {
            throw new ValidationException("Transaction poid is required");
        }

        HrLeaveRequestHdrEntity entity = hdrRepo.findById(request.getTransactionPoid())
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        String status = repository.updateTicketDetails(
                request.getTransactionPoid(),
                request.getTicketBookBy(),
                request.getTicketProcessed(),
                request.getTicketRemarks(),
                request.getTicketsIssued(),
                request.getPjDocRef()
        );

        if (status != null && status.contains("ERROR")) {
            throw new ValidationException(status);
        }

        entity.setTicketBookBy(request.getTicketBookBy());
        entity.setTicketProcessed(request.getTicketProcessed());
        entity.setTicketRemarks(request.getTicketRemarks());
        entity.setTicketsIssued(request.getTicketsIssued());
        entity.setPjDocRef(request.getPjDocRef());
        hdrRepo.save(entity);

        return Map.of(
                "status", status != null ? status : "SUCCESS",
                "message", "Ticket details updated"
        );
    }

    @Override
    public LeaveCalculationResponseDto calculateLeaveDays(
            Long transactionPoid,
            Long employeePoid,
            String leaveType,
            String annualLeaveType,
            String emergencyLeaveType,
            String splLeaveTypes,
            LocalDate leaveStartDate,
            LocalDate planedRejoinDate,
            BigDecimal eligibleLeaveDays,
            String leaveDaysMethod
    ) {
        if (employeePoid == null) {
            throw new ValidationException("Employee not selected");
        }
        if (leaveType == null || leaveType.trim().isEmpty()) {
            throw new ValidationException("Leave type required");
        }
        if (leaveStartDate == null || planedRejoinDate == null) {
            throw new ValidationException("Leave dates required");
        }
        if (planedRejoinDate.isBefore(leaveStartDate)) {
            throw new ValidationException("Planned rejoin date should be after leave start date");
        }

        Map<String, Object> validationResult = repository.validateLeave(
                transactionPoid,
                leaveStartDate,
                planedRejoinDate,
                employeePoid,
                leaveType,
                contains(leaveType, "ANNUAL") ? annualLeaveType : contains(leaveType, "EMERGENCY") ? emergencyLeaveType : splLeaveTypes,
                UserContext.getUserPoid()
        );

        String status = (String) validationResult.get("status");
        handleValidationStatus(status, leaveType);

        BigDecimal leaveDays = validationResult.get("leaveDays") != null
                ? new BigDecimal(validationResult.get("leaveDays").toString())
                : BigDecimal.ZERO;
        BigDecimal calendarDays = BigDecimal.valueOf(java.time.temporal.ChronoUnit.DAYS.between(leaveStartDate, planedRejoinDate));
        BigDecimal holidays = repository.getHolidayCount(leaveStartDate, planedRejoinDate);

        LeaveCalculationResponseDto response = new LeaveCalculationResponseDto();
        response.setLeaveHolidayRunning(isLeaveHolidayRunning(leaveType, leaveDaysMethod));
        response.setLeaveStartDate(leaveStartDate);
        response.setPlanedRejoinDate(planedRejoinDate);
        response.setCalendarDays(calendarDays);
        response.setHolidays(holidays);
        response.setLeaveDays(leaveDays);
        response.setBalanceTillRejoin(eligibleLeaveDays != null ? eligibleLeaveDays.subtract(leaveDays) : null);
        return response;
    }

    @Override
    public Map<String, Object> handleLeaveTypeChange(String leaveType, String leaveDaysMethod) {
        Map<String, Object> response = new HashMap<>();

        response.put("leaveHolidayRunning", isLeaveHolidayRunning(leaveType, leaveDaysMethod));
        response.put("leaveStartDate", null);
        response.put("planedRejoinDate", null);
        response.put("calendarDays", null);
        response.put("holidays", null);
        response.put("leaveDays", null);
        response.put("balanceTillRejoin", null);

        if (contains(leaveType, "ANNUAL")) {
            response.put("annualLeaveTypeVisible", true);
            response.put("emergencyLeaveType", null);
            response.put("splLeaveTypes", null);
        } else if (contains(leaveType, "EMERGENCY")) {
            response.put("emergencyLeaveTypeVisible", true);
            response.put("annualLeaveType", null);
            response.put("splLeaveTypes", null);
        } else if (contains(leaveType, "SPECIAL_LEAVE")) {
            response.put("specialLeaveTypeVisible", true);
            response.put("annualLeaveType", null);
            response.put("emergencyLeaveType", null);
        } else if (contains(leaveType, "MEDICAL")) {
            response.put("annualLeaveType", null);
            response.put("emergencyLeaveType", null);
        }

        return response;
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception  {

        Map<String, Object> params = printService.buildBaseParams(transactionPoid, UserContext.getDocumentId());
        params.put("SUBREPORT_GL", printService.load("HR/Emp_leave_Request_subreport1.jrxml"));
        JasperReport mainReport = printService.load("HR/Emp_leave_Request.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }


   /* private Long resolveEmployeePoid(Long requestEmployeePoid) {

        boolean hasEmpSelectionRight =
                commonService.isGrantedRights("000-219", "Edit"); // implement this

        if (hasEmpSelectionRight) {
            return requestEmployeePoid;
        }

        Long loginUserId = UserContext.getUserPoid();

        Long empId = repository.getLoginUserEmployeeId(loginUserId);

        if (empId == null) {
            throw new ValidationException("Employee not mapped with logged-in user");
        }

        return empId;
    }*/

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;

        String str = value.toString().trim();

        if (str.isEmpty() || str.equalsIgnoreCase("null")) {
            return null;
        }

        try {
            return new BigDecimal(str);
        } catch (Exception e) {
            return null;
        }
    }

}
