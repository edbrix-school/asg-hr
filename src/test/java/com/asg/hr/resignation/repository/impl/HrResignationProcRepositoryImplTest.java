package com.asg.hr.resignation.repository.impl;

import com.asg.hr.resignation.dto.HrResignationEmployeeDetailsResponse;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class HrResignationProcRepositoryImplTest {

    @Test
    void getEmployeeDetails_WhenEmployeeNull_ReturnsErrorStatus() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        HrResignationProcRepositoryImpl repository = new HrResignationProcRepositoryImpl(jdbcTemplate);

        HrResignationEmployeeDetailsResponse response = repository.getEmployeeDetails(null);
        assertTrue(response.getStatus().contains("ERROR"));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEmployeeDetails_WhenCursorHasData_ReturnsMappedDetails() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareCall(any())).thenReturn(cs);
        when(cs.getString(3)).thenReturn("SUCCESS");
        when(cs.getObject(2)).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong("DEPARTMENT_POID")).thenReturn(11L);
        when(rs.getLong("DESIGNATION_POID")).thenReturn(12L);
        when(rs.getLong("DIRECT_SUPERVISOR_POID")).thenReturn(13L);
        when(rs.wasNull()).thenReturn(false);
        when(rs.getDate("JOIN_DATE")).thenReturn(Date.valueOf(LocalDate.of(2024, 1, 1)));
        when(rs.getDate("RP_EXPIRY_DATE")).thenReturn(Date.valueOf(LocalDate.of(2026, 1, 1)));

        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenAnswer((Answer<Object>) invocation -> {
            ConnectionCallback<?> callback = invocation.getArgument(0);
            return callback.doInConnection(connection);
        });

        HrResignationProcRepositoryImpl repository = new HrResignationProcRepositoryImpl(jdbcTemplate);
        HrResignationEmployeeDetailsResponse response = repository.getEmployeeDetails(100L);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(11L, response.getDepartmentPoid());
        assertEquals(12L, response.getDesignationPoid());
        assertEquals(13L, response.getDirectSupervisorPoid());
        assertEquals(LocalDate.of(2024, 1, 1), response.getJoinDate());
        assertEquals(LocalDate.of(2026, 1, 1), response.getRpExpiryDate());
    }

    @Test
    @SuppressWarnings("unchecked")
    void beforeSaveValidation_WithNullInputs_ReturnsProcStatus() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        when(connection.prepareCall(any())).thenReturn(cs);
        when(cs.getString(7)).thenReturn("SUCCESS");

        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenAnswer((Answer<Object>) invocation -> {
            ConnectionCallback<?> callback = invocation.getArgument(0);
            return callback.doInConnection(connection);
        });

        HrResignationProcRepositoryImpl repository = new HrResignationProcRepositoryImpl(jdbcTemplate);
        String status = repository.beforeSaveValidation(null, null, null, null, null, null);
        assertEquals("SUCCESS", status);
        verify(cs, atLeastOnce()).setNull(anyInt(), anyInt());
    }

}
