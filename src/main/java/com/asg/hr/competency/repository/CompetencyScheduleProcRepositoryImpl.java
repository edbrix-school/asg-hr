package com.asg.hr.competency.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

import static com.asg.common.lib.security.util.UserContext.getUserPoid;
import static com.asg.common.lib.utility.ASGHelperUtils.getCompanyId;

@Repository
@Slf4j
public class CompetencyScheduleProcRepositoryImpl implements CompetencyScheduleProcRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public void createBatchEvaluation(Long schedulePoid, Long groupPoid, Boolean recreate, LocalDate evaluationDate) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_HR_COMP_CREATE_BATCH");
            
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_SCHEDULE_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_EVALUATION_DATE", java.sql.Date.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RECREATE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);
            
            query.setParameter("P_LOGIN_COMPANY_POID", getCompanyId());
            query.setParameter("P_LOGIN_USER_POID", getUserPoid());
            query.setParameter("P_SCHEDULE_POID", schedulePoid);
            query.setParameter("P_EVALUATION_DATE", java.sql.Date.valueOf(evaluationDate != null ? evaluationDate : LocalDate.now()));
            query.setParameter("P_RECREATE", recreate != null && recreate ? "Y" : "N");
            
            query.execute();
            
            String status = (String) query.getOutputParameterValue("P_STATUS");
            log.info("Batch evaluation status: {}", status);
            
            if (status != null && status.contains("ERROR")) {
                throw new RuntimeException(status.replaceAll("<[^>]*>", ""));
            }
        } catch (Exception e) {
            log.error("Error creating batch evaluation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create batch evaluation: " + e.getMessage());
        }
    }
}
