package com.asg.hr.personaldatasheet.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PersonalDataSheetProcedureRepository {

    private final DataSource dataSource;

    /**
     * PROC_GET_LOGIN_USER_EMP_ID - Gets the employee mapped with the logged-in user
     * This will make sure that logged in user will be able to see only his/her records
     * Also, it auto set the value of the Employee dropdown
     */
    public Map<String, Object> getLoginUserEmployeeId(Long userPoid) {
        String sql = "BEGIN PROC_GET_LOGIN_USER_EMP_ID(?, ?); END;";

        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setLong(1, userPoid);
            cs.registerOutParameter(2, Types.NUMERIC); // Employee POID

            cs.execute();

            Long employeePoid = cs.getLong(2);

            Map<String, Object> result = new HashMap<>();
            result.put("employeePoid", employeePoid);

            log.info("Login user employee ID retrieved: employeePoid={}", employeePoid);

            return result;

        } catch (Exception e) {
            log.error("Error calling PROC_GET_LOGIN_USER_EMP_ID", e);
            throw new RuntimeException("Failed to get login user employee ID", e);
        }
    }

    /**
     * PROC_HR_LOAD_USER_POLICIES - Loads all policies the employee has accepted under the Policies tab
     * When User clicks on New record, this procedure will get executed
     */
    public List<Map<String, Object>> loadUserPolicies(Long groupPoid, Long companyPoid, String loginUser, String docId, Long docKeyPoid) {
        String sql = "BEGIN PROC_HR_LOAD_USER_POLICIES(?, ?, ?, ?, ?, ?, ?); END;";
        
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            
            cs.setLong(1, groupPoid);
            cs.setLong(2, companyPoid);
            cs.setString(3, loginUser);
            cs.setString(4, docId);
            if (docKeyPoid != null) {
                cs.setLong(5, docKeyPoid);
            } else {
                cs.setNull(5, Types.NUMERIC);
            }
            cs.registerOutParameter(6, Types.VARCHAR); // P_STATUS
            cs.registerOutParameter(7, Types.REF_CURSOR); // P_OUTDATA
            
            cs.execute();
            
            String status = cs.getString(6);
            List<Map<String, Object>> policies = new ArrayList<>();
            
            try (ResultSet rs = (ResultSet) cs.getObject(7)) {
                while (rs.next()) {
                    Map<String, Object> policy = new HashMap<>();
                    policy.put("docPoid", rs.getLong("DOC_POID"));
                    policy.put("docName", rs.getString("DOC_NAME"));
                    // Handle the case where remarks column might be missing in the first query
                    try {
                        policy.put("remarks", rs.getString("REMARKS"));
                    } catch (Exception ex) {
                        policy.put("remarks", ""); // Default empty string if column doesn't exist
                    }
                    policy.put("drilldownLinkInfo", rs.getString("DRILLDOWN_LINK_INFO"));
                    policy.put("policyAccepted", rs.getString("POLICY_ACCEPTED"));
                    policy.put("policyAcceptedOn", rs.getDate("POLICY_ACCEPTED_ON"));
                    policies.add(policy);
                }
            }
            
            log.info("User policies loaded: {} policies found, status={}", policies.size(), status);
            
            return policies;
            
        } catch (Exception e) {
            log.error("Error calling PROC_HR_LOAD_USER_POLICIES for docKeyPoid={}", docKeyPoid, e);
            throw new RuntimeException("Failed to load user policies", e);
        }
    }
}