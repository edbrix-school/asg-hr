package com.asg.hr.resignation.repository.impl;

import com.asg.hr.resignation.dto.HrResignationEmployeeDetailsResponse;
import com.asg.hr.resignation.repository.HrResignationProcRepository;
import com.asg.hr.resignation.util.HrResignationConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.dialect.OracleTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
@Slf4j
public class HrResignationProcRepositoryImpl implements HrResignationProcRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public HrResignationEmployeeDetailsResponse getEmployeeDetails(Long employeePoid) {
        if (employeePoid == null) {
            log.warn("getEmployeeDetails called with null employeePoid");
            return HrResignationEmployeeDetailsResponse.builder()
                    .resignationType(HrResignationConstants.RESIGNATION_TYPE_VOLUNTARY)
                    .status("ERROR : EMPLOYEE_POID is mandatory")
                    .build();
        }

        log.info("Executing {} for employeePoid={}", HrResignationConstants.PROC_GET_EMP_DTLS, employeePoid);
        String sql = "BEGIN " + HrResignationConstants.PROC_GET_EMP_DTLS + "(?,?,?); END;";

        return jdbcTemplate.execute((Connection con) -> {
            try (CallableStatement cs = con.prepareCall(sql)) {
                cs.setLong(1, employeePoid);
                cs.registerOutParameter(2, OracleTypes.CURSOR);
                cs.registerOutParameter(3, Types.VARCHAR);
                cs.execute();

                String status = cs.getString(3);

                Long departmentPoid = null;
                Long designationPoid = null;
                Long directSupervisorPoid = null;
                java.time.LocalDate joinDate = null;
                java.time.LocalDate rpExpiryDate = null;

                try (ResultSet rs = (ResultSet) cs.getObject(2)) {
                    if (rs != null && rs.next()) {
                        departmentPoid = getLongOrNull(rs, "DEPARTMENT_POID");
                        designationPoid = getLongOrNull(rs, "DESIGNATION_POID");
                        directSupervisorPoid = getLongOrNull(rs, "DIRECT_SUPERVISOR_POID");
                        joinDate = rs.getDate("JOIN_DATE") != null ? rs.getDate("JOIN_DATE").toLocalDate() : null;
                        rpExpiryDate = rs.getDate("RP_EXPIRY_DATE") != null ? rs.getDate("RP_EXPIRY_DATE").toLocalDate() : null;
                    }
                }

                return HrResignationEmployeeDetailsResponse.builder()
                        .departmentPoid(departmentPoid)
                        .designationPoid(designationPoid)
                        .directSupervisorPoid(directSupervisorPoid)
                        .joinDate(joinDate)
                        .rpExpiryDate(rpExpiryDate)
                        .resignationType(HrResignationConstants.RESIGNATION_TYPE_VOLUNTARY)
                        .status(status)
                        .build();
            }
        });
    }

    @Override
    public String beforeSaveValidation(
            Long companyPoid,
            Long userPoid,
            LocalDate docDate,
            Long transactionPoid,
            Long employeePoid,
            LocalDate lastDateOfWork
    ) {
        log.info("Executing {} for employeePoid={} transactionPoid={}",
                HrResignationConstants.PROC_BEFORE_SAVE_VAL, employeePoid, transactionPoid);
        String sql = "BEGIN " + HrResignationConstants.PROC_BEFORE_SAVE_VAL + "(?,?,?,?,?,?,?); END;";

        return jdbcTemplate.execute((Connection con) -> {
            try (CallableStatement cs = con.prepareCall(sql)) {
                setNullableLong(cs, 1, companyPoid);
                setNullableLong(cs, 2, userPoid);
                setNullableDate(cs, 3, docDate);
                setNullableLong(cs, 4, transactionPoid);
                setNullableLong(cs, 5, employeePoid);
                setNullableDate(cs, 6, lastDateOfWork);

                cs.registerOutParameter(7, Types.VARCHAR);
                cs.executeUpdate();

                return cs.getString(7);
            }
        });
    }

    private static Long getLongOrNull(ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static void setNullableLong(CallableStatement cs, int index, Long value) throws java.sql.SQLException {
        if (value == null) {
            cs.setNull(index, Types.NUMERIC);
            return;
        }
        cs.setLong(index, value);
    }

    private static void setNullableDate(CallableStatement cs, int index, LocalDate value) throws java.sql.SQLException {
        if (value == null) {
            cs.setNull(index, Types.DATE);
            return;
        }
        cs.setDate(index, java.sql.Date.valueOf(value));
    }
}

