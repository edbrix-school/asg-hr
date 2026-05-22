package com.asg.hr.attendancemonthly.repository.impl;

import com.asg.hr.attendancemonthly.repository.HrAttendanceMonthlyProcRepository;
import com.asg.hr.exceptions.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
@Slf4j
public class HrAttendanceMonthlyProcRepositoryImpl implements HrAttendanceMonthlyProcRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private static final String PROC_HR_ATTENDANCE_IMPORT = "PROC_HR_ATTENDANCE_IMPORT";
    private static final String  PROC_HR_ATTENDANCE_UNLOAD = "PROC_HR_ATTENDANCE_UNLOAD";

    @Override
    public String loadAttendance(Long transactionPoid, Long userPoid, LocalDate fromDate, LocalDate toDate, Long employeePoid, String lateDeductionCheck) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery(PROC_HR_ATTENDANCE_IMPORT);

            query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(3, java.sql.Date.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(4, java.sql.Date.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(5, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(6, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(7, String.class, ParameterMode.OUT);

            query.setParameter(1, transactionPoid);
            query.setParameter(2, userPoid);
            query.setParameter(3, java.sql.Date.valueOf(fromDate));
            query.setParameter(4, java.sql.Date.valueOf(toDate));
            query.setParameter(5, employeePoid);
            query.setParameter(6, lateDeductionCheck);

            query.execute();

            String status = (String) query.getOutputParameterValue(7);
            log.info("Procedure {} status: {}", PROC_HR_ATTENDANCE_IMPORT, status);

            if (status != null && status.toUpperCase().contains("ERROR")) {
                throw new ValidationException(status);
            }

            return status;
        } catch (Exception e) {
            log.error("Error executing procedure {}: {}", PROC_HR_ATTENDANCE_IMPORT, e.getMessage(), e);
            throw new ValidationException("Error calling attendance load procedure: " + e.getMessage());
        }
    }

    @Override
    public String unloadAttendance(Long transactionPoid) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery(PROC_HR_ATTENDANCE_UNLOAD);

            query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, String.class, ParameterMode.OUT);

            query.setParameter(1, transactionPoid);

            query.execute();

            String status = (String) query.getOutputParameterValue(2);
            if (status != null && status.toUpperCase().contains("ERROR")) {
                throw new ValidationException(status);
            }

            return status;
        } catch (Exception e) {
            log.error("Error executing procedure {}: {}", PROC_HR_ATTENDANCE_UNLOAD, e.getMessage(), e);
            throw new ValidationException("Error calling attendance unload procedure: " + e.getMessage());
        }
    }

}
