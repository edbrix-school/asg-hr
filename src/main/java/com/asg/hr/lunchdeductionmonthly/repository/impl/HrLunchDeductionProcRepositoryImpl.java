package com.asg.hr.lunchdeductionmonthly.repository.impl;

import com.asg.hr.exceptions.ValidationException;
import com.asg.hr.lunchdeductionmonthly.repository.HrLunchDeductionProcRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
@Slf4j
public class HrLunchDeductionProcRepositoryImpl implements HrLunchDeductionProcRepository {

    private static final String PROC_HR_LUNCH_SUMM_IMPORT = "PROC_HR_LUNCH_SUMM_IMPORT";

    @PersistenceContext
    private EntityManager entityManager;

@Override
    public String loadLunchDetails(Long transactionPoid, Long userPoid, LocalDate payrollMonth) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery(PROC_HR_LUNCH_SUMM_IMPORT);

            query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);     // P_TRANSACTION_NO
            query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);     // P_LOGIN_USER_POID
            query.registerStoredProcedureParameter(3, java.sql.Date.class, ParameterMode.IN); // P_PAYROLL_MONTH
            query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);   // P_OTHER_PARAMETERS
            query.registerStoredProcedureParameter(5, String.class, ParameterMode.OUT);  // P_STATUS

            query.setParameter(1, transactionPoid);
            query.setParameter(2, userPoid);
            query.setParameter(3, java.sql.Date.valueOf(payrollMonth));
            query.setParameter(4, null);  // P_OTHER_PARAMETERS — not used in current proc logic

            query.execute();

            String status = (String) query.getOutputParameterValue(5);
            log.info("Procedure {} status: {}", PROC_HR_LUNCH_SUMM_IMPORT, status);

            if (status != null && status.toUpperCase().contains("ERROR")) {
                throw new ValidationException(status);
            }
            return status;
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing procedure {}: {}", PROC_HR_LUNCH_SUMM_IMPORT, e.getMessage(), e);
            throw new ValidationException("Error calling lunch load procedure: " + e.getMessage());
        }
    }
}
