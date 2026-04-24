package com.asg.hr.competency.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import com.asg.common.lib.exception.ValidationException;

import java.time.LocalDate;

import static com.asg.common.lib.security.util.UserContext.getUserPoid;
import static com.asg.common.lib.utility.ASGHelperUtils.getCompanyId;

@Repository
@Slf4j
public class CompetencyScheduleProcRepositoryImpl implements CompetencyScheduleProcRepository {
    
    private static final String PROC_NAME = "PROC_HR_COMP_CREATE_BATCH";
    private static final String P_LOGIN_COMPANY_POID = "P_LOGIN_COMPANY_POID";
    private static final String P_LOGIN_USER_POID = "P_LOGIN_USER_POID";
    private static final String P_SCHEDULE_POID = "P_SCHEDULE_POID";
    private static final String P_EVALUATION_DATE = "P_EVALUATION_DATE";
    private static final String P_RECREATE = "P_RECREATE";
    private static final String P_STATUS = "P_STATUS";
    private static final String ERROR_KEYWORD = "ERROR";
    private static final String HTML_TAG_REGEX = "<[^>]*>";
    private static final String BATCH_EVALUATION_STATUS_MSG = "Batch evaluation status: {}";
    private static final String ERROR_CREATING_BATCH_MSG = "Error creating batch evaluation: {}";
    private static final String FAILED_TO_CREATE_BATCH_MSG = "Failed to create batch evaluation: ";
    private static final String DEFAULT_SUCCESS_MSG = "Batch evaluation created successfully";
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public String createBatchEvaluation(Long schedulePoid, Long groupPoid, Boolean recreate, LocalDate evaluationDate) {
        try {
            if (evaluationDate == null) {
                throw new ValidationException("Please enter Evaluation Date ...");
            }

            StoredProcedureQuery query = entityManager.createStoredProcedureQuery(PROC_NAME);
            
            query.registerStoredProcedureParameter(P_LOGIN_COMPANY_POID, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(P_LOGIN_USER_POID, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(P_SCHEDULE_POID, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(P_EVALUATION_DATE, java.sql.Date.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(P_RECREATE, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(P_STATUS, String.class, ParameterMode.OUT);
            
            query.setParameter(P_LOGIN_COMPANY_POID, getCompanyId());
            query.setParameter(P_LOGIN_USER_POID, getUserPoid());
            query.setParameter(P_SCHEDULE_POID, schedulePoid);
            query.setParameter(P_EVALUATION_DATE, java.sql.Date.valueOf(evaluationDate));
            query.setParameter(P_RECREATE, recreate != null && recreate ? "Y" : "N");
            
            query.execute();
            
            String rawStatus = (String) query.getOutputParameterValue(P_STATUS);
            String status = sanitizeStatus(rawStatus);
            log.info(BATCH_EVALUATION_STATUS_MSG, status);
            
            if (rawStatus != null && rawStatus.toUpperCase().contains(ERROR_KEYWORD)) {
                throw new ValidationException(status);
            }
            return status;
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception e) {
            log.error(ERROR_CREATING_BATCH_MSG, e.getMessage(), e);
            throw new ValidationException(FAILED_TO_CREATE_BATCH_MSG + e.getMessage());
        }
    }

    private String sanitizeStatus(String status) {
        if (status == null) {
            return DEFAULT_SUCCESS_MSG;
        }

        String sanitized = status.replaceAll(HTML_TAG_REGEX, "").trim();
        return sanitized.isEmpty() ? DEFAULT_SUCCESS_MSG : sanitized;
    }
}
