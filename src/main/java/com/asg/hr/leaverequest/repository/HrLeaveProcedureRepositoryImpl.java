package com.asg.hr.leaverequest.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.util.*;

import org.hibernate.Session;
import oracle.jdbc.OracleTypes;

@Slf4j
@Repository
@RequiredArgsConstructor
public class HrLeaveProcedureRepositoryImpl implements HrLeaveProcedureRepository {

    private static final String KEY_STATUS = "status";
    private static final String KEY_DATA = "data";
    private static final String KEY_HOD = "hod";
    private static final String ERROR_PREFIX = "ERROR: ";
    private static final String PARAM_P_STATUS = "P_STATUS";
    private static final String PARAM_P_EMPLOYEE_POID = "P_EMPLOYEE_POID";
    private static final String PARAM_P_TRN_NO = "P_TRN_NO";

    private final EntityManager entityManager;

    @Override
    public Map<String, Object> getEmployeeDetails(Long employeePoid) {

        Map<String, Object> map = new HashMap<>();

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_HR_GET_EMP_DETAILS");

            query.registerStoredProcedureParameter(PARAM_P_EMPLOYEE_POID, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);
            query.registerStoredProcedureParameter(PARAM_P_STATUS, String.class, ParameterMode.OUT);

            query.setParameter(PARAM_P_EMPLOYEE_POID, employeePoid);

            query.execute();

            String status = (String) query.getOutputParameterValue(PARAM_P_STATUS);

            if (status != null && status.contains("SUCCESS")) {
                List<Object[]> result = query.getResultList();
                map.put(KEY_DATA, normalizeEmployeeDetails(result));
            } else {
                map.put(KEY_DATA, Collections.emptyList());
            }

            map.put(KEY_STATUS, status);

        } catch (Exception ex) {
            log.error("Error in getEmployeeDetails", ex);
            map.put(KEY_STATUS, ERROR_PREFIX + ex.getMessage());
            map.put(KEY_DATA, Collections.emptyList());
        }

        return map;
    }


    public Map<String, Object> getEmployeeHod(Long employeePoid) {

        Map<String, Object> map = new HashMap<>();

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_HR_GET_EMP_HOD");

            query.registerStoredProcedureParameter(PARAM_P_EMPLOYEE_POID, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);
            query.registerStoredProcedureParameter(PARAM_P_STATUS, String.class, ParameterMode.OUT);

            query.setParameter(PARAM_P_EMPLOYEE_POID, employeePoid);

            query.execute();

            String status = (String) query.getOutputParameterValue(PARAM_P_STATUS);

            if (status != null && status.contains("SUCCESS")) {

                List<?> result = query.getResultList();

                Long hod = null;

                if (result != null && !result.isEmpty()) {
                    Object value = result.get(0);
                    if (value != null) {
                        hod = ((Number) value).longValue();
                    }
                }

                map.put(KEY_HOD, hod);
            } else {
                map.put(KEY_HOD, null);
            }

            map.put(KEY_STATUS, status);

        } catch (Exception ex) {
            log.error("Error in getEmployeeHod", ex);
            map.put(KEY_STATUS, ERROR_PREFIX + ex.getMessage());
            map.put(KEY_HOD, null);
        }

        return map;
    }


    @Override
    public Map<String, Object> getEligibleLeaveDays(
            Long companyId,
            Long empPoid,
            LocalDate leaveStartDate,
            Long settlementPoid
    ) {

        Map<String, Object> map = new HashMap<>();

        Connection conn = entityManager.unwrap(Session.class)
                .doReturningWork(connection -> connection);

        try (CallableStatement stmt = conn.prepareCall("{call PROC_HR_ELIGIBLELEAVE_DAYS(?,?,?,?,?,?,?)}")) {

            stmt.setBigDecimal(1, BigDecimal.valueOf(companyId));
            stmt.setBigDecimal(2, BigDecimal.valueOf(empPoid));
            stmt.setDate(3, java.sql.Date.valueOf(leaveStartDate));
            stmt.setDate(4, null);

            if (settlementPoid != null && settlementPoid > 0) {
                stmt.setBigDecimal(5, BigDecimal.valueOf(settlementPoid));
            } else {
                stmt.setNull(5, Types.NUMERIC);
            }

            stmt.registerOutParameter(6, OracleTypes.CURSOR);
            stmt.registerOutParameter(7, Types.VARCHAR);

            stmt.execute();

            String status = stmt.getString(7);

            try (ResultSet rs = (ResultSet) stmt.getObject(6)) {
                List<Map<String, Object>> data = new ArrayList<>();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("eligibleLeave", rs.getObject(1));
                    row.put("eligibleTicket", rs.getObject(2));
                    row.put("ticketPeriod", rs.getObject(3));
                    row.put("earnedTicket", rs.getObject(4));
                    row.put("airSectorPoid", rs.getObject(5));
                    row.put("paidLeavesTaken", rs.getObject(6));
                    row.put("medicalTaken", rs.getObject(7));
                    row.put("medicalEligible", rs.getObject(8));
                    row.put("lastLeaveDetails", rs.getObject(9));
                    row.put("lastTicketDetails", rs.getObject(10));
                    data.add(row);
                }

                map.put(KEY_STATUS, status);
                map.put(KEY_DATA, data);
            }

        } catch (java.sql.SQLException ex) {
            log.error("Error in getEligibleLeaveDays", ex);
            map.put(KEY_STATUS, ERROR_PREFIX + ex.getMessage());
            map.put(KEY_DATA, Collections.emptyList());
        }

        return map;
    }




    @Override
    public List<Map<String, Object>> getTicketFamilyDetails(Long empPoid) {

        List<Map<String, Object>> list = new ArrayList<>();

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_HR_LEAVE_REQ_TICKET_DTL");


            query.registerStoredProcedureParameter("P_EMP_POID", Long.class, ParameterMode.IN);


            query.registerStoredProcedureParameter("ATT_REC", void.class, ParameterMode.REF_CURSOR);

            query.setParameter("P_EMP_POID", empPoid);

            query.execute();

            List<Object[]> result = query.getResultList();

            if (result != null) {

                for (Object[] r : result) {

                    Map<String, Object> row = new HashMap<>();

                    row.put("NAME", r[0]);
                    row.put("RELATION", r[1]);
                    row.put("AGE_GROUP", r[2]);

                    list.add(row);
                }
            }

        } catch (Exception ex) {
            log.error("Error in getTicketFamilyDetails", ex);
        }

        return list;
    }


    @Override
    public String updateLeaveHistory(Long tranId,
                                     String ticketIssueType,
                                     String ticketTillDate,
                                     String ticketIssuedCount) {


            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_HR_LEAVE_REQ_POST_IN_HIST");


            query.registerStoredProcedureParameter(PARAM_P_TRN_NO, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_P_STATUS, String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("pHrTicketIssueType", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("pHrTicketTillDate", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("pHrTicketIssuedCount", String.class, ParameterMode.IN);

            query.setParameter(PARAM_P_TRN_NO, tranId);
            query.setParameter("pHrTicketIssueType", ticketIssueType);
            query.setParameter("pHrTicketTillDate", ticketTillDate);
            query.setParameter("pHrTicketIssuedCount", ticketIssuedCount);

            query.execute();

            return (String) query.getOutputParameterValue(PARAM_P_STATUS);
    }

    @Override
    public String updateTicketDetails(Long tranId,
                                      String ticketBookBy,
                                      String ticketProcessed,
                                      String ticketRemarks,
                                      BigDecimal ticketsIssued,
                                      String pjDocRef) {

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery("PROC_HR_LEAVE_REQ_TICKET_V2");

        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, BigDecimal.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(6, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(7, String.class, ParameterMode.OUT);

        query.setParameter(1, tranId);
        query.setParameter(2, ticketBookBy);
        query.setParameter(3, ticketProcessed);
        query.setParameter(4, ticketRemarks);
        query.setParameter(5, ticketsIssued);
        query.setParameter(6, pjDocRef);
        query.execute();

        return (String) query.getOutputParameterValue(7);
    }

    public Map<String, Object> validateLeave(
            Long tranId,
            LocalDate startDate,
            LocalDate endDate,
            Long empId,
            String leaveType,
            String subType,
            Long userId
    ) {

        Map<String, Object> map = new HashMap<>();

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_HR_LEAVE_VALIDATE_V3");

            query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, LocalDate.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(3, LocalDate.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(4, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(6, String.class, ParameterMode.IN);

            query.registerStoredProcedureParameter(7, Integer.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter(8, String.class, ParameterMode.OUT);

            query.registerStoredProcedureParameter(9, Long.class, ParameterMode.IN);

            query.setParameter(1, tranId);
            query.setParameter(2, startDate);
            query.setParameter(3, endDate);
            query.setParameter(4, empId);
            query.setParameter(5, leaveType);
            query.setParameter(6, subType);
            query.setParameter(9, userId);

            query.execute();

            map.put("leaveDays", query.getOutputParameterValue(7));
            map.put(KEY_STATUS, query.getOutputParameterValue(8));

        } catch (Exception e) {
            log.error("Error in validateLeave", e);
            map.put(KEY_STATUS, ERROR_PREFIX + e.getMessage());
        }

        return map;
    }

    public String unUpdateLeaveHistory(Long tranId) {


        String result = "NO_DATA";

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_HR_LEV_REQ_UNUPDATE_HIST");

            query.registerStoredProcedureParameter(PARAM_P_TRN_NO, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            query.setParameter(PARAM_P_TRN_NO, tranId);
            query.execute();
            result = (String) query.getOutputParameterValue("P_RESULT");

        } catch (Exception ex) {
            log.error("Error in unUpdateLeaveHistory", ex);
            result = ERROR_PREFIX + ex.getMessage();
        }

        return result;
    }

    @Override
    public BigDecimal getHolidayCount(LocalDate leaveStartDate, LocalDate planedRejoinDate) {
        Connection conn = entityManager.unwrap(Session.class)
                .doReturningWork(connection -> connection);

        try (CallableStatement stmt = conn.prepareCall("begin ? := FUNC_RTN_HOLIDAY_COUNT(?,?); end;")) {
            stmt.registerOutParameter(1, Types.VARCHAR);
            stmt.setDate(2, java.sql.Date.valueOf(leaveStartDate));
            stmt.setDate(3, java.sql.Date.valueOf(planedRejoinDate.minusDays(1)));
            stmt.execute();

            Object value = stmt.getObject(1);
            return value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
        } catch (Exception ex) {
            log.error("Error in getHolidayCount", ex);
            return BigDecimal.ZERO;
        }
    }

    private List<Map<String, Object>> normalizeEmployeeDetails(List<Object[]> result) {

        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> rows = new ArrayList<>();

        for (Object[] values : result) {

            Map<String, Object> row = new LinkedHashMap<>();

            row.put("employeePoid", values.length > 0 ? values[0] : null);
            row.put("ticketDetails", values.length > 1 ? values[1] : null);
            row.put("designationName", values.length > 2 ? values[2] : null);


            row.put("joinDate", values.length > 3 ? values[3] : null);
            row.put("probation", values.length > 4 ? values[4] : null);
            row.put("hod", values.length > 5 ? values[5] : null);

            row.put("raw", values);

            rows.add(row);
        }

        return rows;
    }

    public Long getLoginUserEmployeeId(Long userId) {

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_GET_LOGIN_USER_EMP_ID");

            query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, Long.class, ParameterMode.OUT);

            query.setParameter(1, userId);

            query.execute();

            Object result = query.getOutputParameterValue(2);

            return result != null ? ((Number) result).longValue() : null;

        } catch (Exception e) {
            log.error("Error in getLoginUserEmployeeId", e);
            return null;
        }
    }



}
