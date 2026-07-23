package com.asg.hr.employeeinduction.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.sql.Types;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
@Slf4j
public class EmployeeInductionProcRepositoryImpl implements EmployeeInductionProcRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> getInductionCategories() {
        log.info("Calling stored procedure PROC_HR_INDUCTION_CATG_CUR");
        
        try {
            SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("PROC_HR_INDUCTION_CATG_CUR")
                    .declareParameters(
                            new SqlParameter("P_TRN_POID", Types.NUMERIC),
                            new SqlOutParameter("P_RESULT", Types.VARCHAR),
                            new SqlOutParameter("ATT_REC", Types.REF_CURSOR)
                    );

            Map<String, Object> result = jdbcCall.execute(new MapSqlParameterSource("P_TRN_POID", 0));
            
            String procedureResult = (String) result.get("P_RESULT");
            log.info("Stored procedure result: {}", procedureResult);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> categories = (List<Map<String, Object>>) result.get("ATT_REC");
            
            log.info("Retrieved {} induction categories", categories != null ? categories.size() : 0);
            return categories;
            
        } catch (Exception e) {
            log.error("Error calling stored procedure PROC_HR_INDUCTION_CATG_CUR", e);
            throw new RuntimeException("Failed to retrieve induction categories", e);
        }
    }
}