package com.asg.hr.employeemaster.service;

import com.asg.hr.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;

@Service
@RequiredArgsConstructor
public class EmployeeGlCreationService {

    private static final String DOC_ID_EMPLOYEE_MASTER = "800-001";
    private static final String GL_TYPE_EMPLOYEE = "EMPLOYEE";
    private static final String ERROR = "ERROR";

    private final DataSource dataSource;

    public String createEmployeeGlIfMissing(Long employeePoid, Long groupPoid, Long companyPoid, String userId) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            String empCode;
            String empName;
            try (var ps = conn.prepareStatement(
                    "SELECT EMPLOYEE_CODE, EMPLOYEE_NAME, EMPLOYEE_NAME2, EMP_GL_POID FROM HR_EMPLOYEE_MASTER WHERE EMPLOYEE_POID = ?")) {
                ps.setLong(1, employeePoid);
                try (var rs = ps.executeQuery()) {
                    if (!rs.next()) throw new ValidationException("Employee not found: " + employeePoid);
                    if (rs.getObject("EMP_GL_POID") != null) return "GL already created";
                    empCode = rs.getString("EMPLOYEE_CODE");
                    empName = StringUtils.trimToEmpty(rs.getString("EMPLOYEE_NAME"));
                    String empName2 = rs.getString("EMPLOYEE_NAME2");
                    if (StringUtils.isNotBlank(empName2)) empName = (empName + " " + empName2).trim();
                }
            }

            try (CallableStatement cs = conn.prepareCall(
                    "{call PROC_GL_MASTER_CREATION(?,?,?,?,?,?,?,?,?)}")) {
                cs.setLong(1, employeePoid);
                cs.setLong(2, groupPoid);
                cs.setLong(3, companyPoid);
                cs.setString(4, userId);
                cs.setString(5, DOC_ID_EMPLOYEE_MASTER);
                cs.setString(6, empCode);
                cs.setString(7, empName);
                cs.setString(8, GL_TYPE_EMPLOYEE);
                cs.registerOutParameter(9, Types.VARCHAR);
                cs.execute();
                String status = cs.getString(9);
                if (status != null && status.toUpperCase().contains(ERROR)) {
                    throw new ValidationException(status);
                }
                return status;
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception ex) {
            throw new ValidationException("PROC_GL_MASTER_CREATION failed: " + ex.getMessage());
        }
    }
}
