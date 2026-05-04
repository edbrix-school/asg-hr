package com.asg.hr.leaverejoin.repository.impl;

import com.asg.common.lib.security.util.UserContext;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinEmployeeDetailsResponse;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinLeaveDetailsResponse;
import com.asg.hr.leaverejoin.repository.EmployeeLeaveRejoinProcRepository;
import com.asg.hr.leaverejoin.util.EmployeeLeaveRejoinConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.dialect.OracleTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Repository
@RequiredArgsConstructor
@Slf4j
public class EmployeeLeaveRejoinProcRepositoryImpl implements EmployeeLeaveRejoinProcRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public EmployeeLeaveRejoinEmployeeDetailsResponse getEmployeeDetails(Long employeePoid) {
        if (employeePoid == null) {
            return EmployeeLeaveRejoinEmployeeDetailsResponse.builder()
                    .status("ERROR : EMPLOYEE_POID is mandatory")
                    .build();
        }

        String sql = "BEGIN " + EmployeeLeaveRejoinConstants.PROC_GET_EMPLOYEE_DETAILS + "(?,?,?,?,?); END;";
        log.info("Executing {} for employeePoid={}", EmployeeLeaveRejoinConstants.PROC_GET_EMPLOYEE_DETAILS, employeePoid);

        return jdbcTemplate.execute((Connection con) -> {
            try (CallableStatement cs = con.prepareCall(sql)) {
                cs.setObject(1, UserContext.getCompanyPoid(), Types.NUMERIC);
                cs.setObject(2, UserContext.getUserPoid(), Types.NUMERIC);
                cs.setLong(3, employeePoid);
                cs.registerOutParameter(4, OracleTypes.CURSOR);
                cs.registerOutParameter(5, Types.VARCHAR);
                cs.execute();

                String status = cs.getString(5);
                String departmentName = null;
                String designationName = null;

                try (ResultSet rs = (ResultSet) cs.getObject(4)) {
                    if (rs != null && rs.next()) {
                        departmentName = getString(rs, "DEPARTMENT_NAME", "DEPT_NAME", "DEPARTMENT");
                        designationName = getString(rs, "DESIGNATION_NAME", "DESIG_NAME", "DESIGNATION");
                    }
                }

                return EmployeeLeaveRejoinEmployeeDetailsResponse.builder()
                        .employeePoid(employeePoid)
                        .departmentName(departmentName)
                        .designationName(designationName)
                        .status(status != null ? status : EmployeeLeaveRejoinConstants.STATUS_SUCCESS)
                        .build();
            }
        });
    }

    @Override
    public EmployeeLeaveRejoinLeaveDetailsResponse getLeaveDetails(Long employeePoid, Long leaveRequestPoid) {
        if (employeePoid == null || leaveRequestPoid == null) {
            return EmployeeLeaveRejoinLeaveDetailsResponse.builder()
                    .employeePoid(employeePoid)
                    .leaveRequestPoid(leaveRequestPoid)
                    .status("ERROR : EMPLOYEE_POID and LEAVE_REQUEST_POID are mandatory")
                    .build();
        }

        String sql = "BEGIN " + EmployeeLeaveRejoinConstants.PROC_GET_LEAVE_DETAILS + "(?,?,?,?,?); END;";
        log.info("Executing {} for employeePoid={} leaveRequestPoid={}",
                EmployeeLeaveRejoinConstants.PROC_GET_LEAVE_DETAILS, employeePoid, leaveRequestPoid);

        return jdbcTemplate.execute((Connection con) -> {
            try (CallableStatement cs = con.prepareCall(sql)) {
                cs.setObject(1, UserContext.getCompanyPoid(), Types.NUMERIC);
                cs.setObject(2, UserContext.getUserPoid(), Types.NUMERIC);
                cs.setLong(3, leaveRequestPoid);
                cs.registerOutParameter(4, OracleTypes.CURSOR);
                cs.registerOutParameter(5, Types.VARCHAR);
                cs.execute();

                String status = cs.getString(5);
                Long resolvedEmployeePoid = null;
                LocalDate dateProceededOnLeave = null;
                LocalDate plannedRejoinDate = null;

                try (ResultSet rs = (ResultSet) cs.getObject(4)) {
                    if (rs != null && rs.next()) {
                        resolvedEmployeePoid = getLong(rs, "EMPLOYEE_POID");
                        dateProceededOnLeave = getLocalDate(
                                rs,
                                "DATE_PROCEEDED_ON_LEAVE",
                                "LEAVE_START_DATE",
                                "DATE_OF_LEAVE",
                                "LEAVE_DATE"
                        );
                        plannedRejoinDate = getLocalDate(
                                rs,
                                "PLANED_REJOIN_DATE",
                                "PLANNED_REJOIN_DATE",
                                "DATE_OF_REJOINING"
                        );
                    }
                }

                return EmployeeLeaveRejoinLeaveDetailsResponse.builder()
                        .employeePoid(resolvedEmployeePoid != null ? resolvedEmployeePoid : employeePoid)
                        .leaveRequestPoid(leaveRequestPoid)
                        .dateProceededOnLeave(dateProceededOnLeave)
                        .plannedRejoinDate(plannedRejoinDate)
                        .status(status != null ? status : EmployeeLeaveRejoinConstants.STATUS_SUCCESS)
                        .build();
            }
        });
    }

    private Long getLong(ResultSet rs, String... columnNames) throws java.sql.SQLException {
        Set<String> availableColumns = getAvailableColumns(rs);
        for (String columnName : columnNames) {
            if (availableColumns.contains(columnName.toUpperCase(Locale.ROOT))) {
                Object value = rs.getObject(columnName);
                if (value instanceof Number number) {
                    return number.longValue();
                }
            }
        }
        return null;
    }

    private String getString(ResultSet rs, String... columnNames) throws java.sql.SQLException {
        Set<String> availableColumns = getAvailableColumns(rs);
        for (String columnName : columnNames) {
            if (availableColumns.contains(columnName.toUpperCase(Locale.ROOT))) {
                return rs.getString(columnName);
            }
        }
        return null;
    }

    private LocalDate getLocalDate(ResultSet rs, String... columnNames) throws java.sql.SQLException {
        Set<String> availableColumns = getAvailableColumns(rs);
        for (String columnName : columnNames) {
            if (availableColumns.contains(columnName.toUpperCase(Locale.ROOT))) {
                java.sql.Date value = rs.getDate(columnName);
                return value != null ? value.toLocalDate() : null;
            }
        }
        return null;
    }

    private Set<String> getAvailableColumns(ResultSet rs) throws java.sql.SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        Set<String> columns = new HashSet<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            columns.add(metaData.getColumnLabel(i).toUpperCase(Locale.ROOT));
        }
        return columns;
    }
}
