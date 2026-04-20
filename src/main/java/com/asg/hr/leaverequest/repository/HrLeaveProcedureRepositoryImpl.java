package com.asg.hr.leaverequest.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
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

@Repository
@RequiredArgsConstructor
public class HrLeaveProcedureRepositoryImpl implements HrLeaveProcedureRepository {

    private final EntityManager entityManager;

    @Override
    public Map<String, Object> getEmployeeDetails(Long employeePoid) {

        Map<String, Object> map = new HashMap<>();

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_HR_GET_EMP_DETAILS");

            query.registerStoredProcedureParameter("P_EMPLOYEE_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);
            query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);

            query.setParameter("P_EMPLOYEE_POID", employeePoid);

            query.execute();

            String status = (String) query.getOutputParameterValue("P_STATUS");

            if (status != null && status.contains("SUCCESS")) {
                List<Object[]> result = query.getResultList();
                map.put("data", normalizeEmployeeDetails(result));
            } else {
                // ERROR case → no data
                map.put("data", Collections.emptyList());
            }

            map.put("status", status);

        } catch (Exception ex) {
            map.put("status", "ERROR: " + ex.getMessage());
            map.put("data", Collections.emptyList());
        }

        return map;
    }


    public Map<String, Object> getEmployeeHod(Long employeePoid) {

        Map<String, Object> map = new HashMap<>();

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_HR_GET_EMP_HOD");

            query.registerStoredProcedureParameter("P_EMPLOYEE_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);
            query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);

            query.setParameter("P_EMPLOYEE_POID", employeePoid);

            query.execute();

            String status = (String) query.getOutputParameterValue("P_STATUS");

            if (status != null && status.contains("SUCCESS")) {

                List<?> result = query.getResultList();

                Long hod = null;

                if (result != null && !result.isEmpty()) {
                    Object value = result.get(0);
                    if (value != null) {
                        hod = ((Number) value).longValue();
                    }
                }

                map.put("hod", hod);
            } else {
                map.put("hod", null);
            }

            map.put("status", status);

        } catch (Exception ex) {
            map.put("status", "ERROR: " + ex.getMessage());
            map.put("hod", null);
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

        Connection conn = null;
        CallableStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = entityManager.unwrap(Session.class)
                    .doReturningWork(connection -> connection);

            String sql = "{call PROC_HR_ELIGIBLELEAVE_DAYS(?,?,?,?,?,?,?)}";

            stmt = conn.prepareCall(sql);


            stmt.setBigDecimal(1, BigDecimal.valueOf(companyId));
            stmt.setBigDecimal(2, BigDecimal.valueOf(empPoid));
            stmt.setDate(3, java.sql.Date.valueOf(leaveStartDate));
            stmt.setDate(4, null);

            if (settlementPoid != null && settlementPoid > 0) {
                stmt.setBigDecimal(5, BigDecimal.valueOf(settlementPoid));
            } else {
                stmt.setNull(5, Types.NUMERIC); //  IMPORTANT
            }


            stmt.registerOutParameter(6, OracleTypes.CURSOR);
            stmt.registerOutParameter(7, Types.VARCHAR);

            stmt.execute();

            String status = stmt.getString(7);

            rs = (ResultSet) stmt.getObject(6);

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

            map.put("status", status);
            map.put("data", data);

        } catch (Exception ex) {
            ex.printStackTrace();
            map.put("status", "ERROR: " + ex.getMessage());
            map.put("data", Collections.emptyList());
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
            ex.printStackTrace();
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


            query.registerStoredProcedureParameter("P_TRN_NO", Long.class, ParameterMode.IN);


            query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);


            query.registerStoredProcedureParameter("pHrTicketIssueType", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("pHrTicketTillDate", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("pHrTicketIssuedCount", String.class, ParameterMode.IN);


            query.setParameter("P_TRN_NO", tranId);
            query.setParameter("pHrTicketIssueType", ticketIssueType);
            query.setParameter("pHrTicketTillDate", ticketTillDate);
            query.setParameter("pHrTicketIssuedCount", ticketIssuedCount);

            query.execute();

            String status = (String) query.getOutputParameterValue("P_STATUS");

            return status;
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
            map.put("status", query.getOutputParameterValue(8));

        } catch (Exception e) {
            map.put("status", "ERROR: " + e.getMessage());
        }

        return map;
    }

    public String unUpdateLeaveHistory(Long tranId) {


        String result = "NO_DATA";

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_HR_LEV_REQ_UNUPDATE_HIST");

            // IN parameter
            query.registerStoredProcedureParameter("P_TRN_NO", Long.class, ParameterMode.IN);

            // OUT parameter
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // set value
            query.setParameter("P_TRN_NO", tranId);

            // execute
            query.execute();

            // get result
            result = (String) query.getOutputParameterValue("P_RESULT");

        } catch (Exception ex) {
            ex.printStackTrace();
            result = "ERROR: " + ex.getMessage();
        }

        return result;
    }

    @Override
    public BigDecimal getHolidayCount(LocalDate leaveStartDate, LocalDate planedRejoinDate) {
        Connection conn = null;
        CallableStatement stmt = null;

        try {
            conn = entityManager.unwrap(Session.class)
                    .doReturningWork(connection -> connection);
            stmt = conn.prepareCall("begin ? := FUNC_RTN_HOLIDAY_COUNT(?,?); end;");
            stmt.registerOutParameter(1, Types.VARCHAR);
            stmt.setDate(2, java.sql.Date.valueOf(leaveStartDate));
            stmt.setDate(3, java.sql.Date.valueOf(planedRejoinDate.minusDays(1)));
            stmt.execute();

            Object value = stmt.getObject(1);
            return value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception ignored) {
            }
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
            return null;
        }
    }



}
