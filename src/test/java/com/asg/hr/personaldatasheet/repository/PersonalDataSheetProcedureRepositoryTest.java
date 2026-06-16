package com.asg.hr.personaldatasheet.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalDataSheetProcedureRepositoryTest {

    @Mock
    private DataSource dataSource;
    
    @Mock
    private Connection connection;
    
    @Mock
    private CallableStatement callableStatement;
    
    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private PersonalDataSheetProcedureRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        // Common setup will be done in individual tests as needed
    }

    // Tests for getLoginUserEmployeeId method
    @Test
    void getLoginUserEmployeeId_Success_ReturnsEmployeePoid() throws SQLException {
        // Arrange
        Long userPoid = 123L;
        Long expectedEmployeePoid = 456L;
        
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall(anyString())).thenReturn(callableStatement);
        when(callableStatement.getLong(2)).thenReturn(expectedEmployeePoid);

        // Act
        Map<String, Object> result = repository.getLoginUserEmployeeId(userPoid);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("employeePoid")).isEqualTo(expectedEmployeePoid);
        
        verify(callableStatement).setLong(1, userPoid);
        verify(callableStatement).registerOutParameter(2, Types.NUMERIC);
        verify(callableStatement).execute();
        verify(callableStatement).getLong(2);
    }

    @Test
    void getLoginUserEmployeeId_WithZeroUserPoid_ShouldReturnZero() throws SQLException {
        // Arrange
        Long userPoid = 0L;
        Long expectedEmployeePoid = 0L;
        
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall(anyString())).thenReturn(callableStatement);
        when(callableStatement.getLong(2)).thenReturn(expectedEmployeePoid);

        // Act
        Map<String, Object> result = repository.getLoginUserEmployeeId(userPoid);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("employeePoid")).isEqualTo(expectedEmployeePoid);
    }

    @Test
    void getLoginUserEmployeeId_DatabaseConnectionError_ThrowsRuntimeException() throws SQLException {
        // Arrange
        Long userPoid = 123L;
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> repository.getLoginUserEmployeeId(userPoid))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to get login user employee ID")
                .hasCauseInstanceOf(SQLException.class);
    }

    @Test
    void getLoginUserEmployeeId_CallableStatementError_ThrowsRuntimeException() throws SQLException {
        // Arrange
        Long userPoid = 123L;
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall(anyString())).thenThrow(new SQLException("Statement preparation failed"));

        // Act & Assert
        assertThatThrownBy(() -> repository.getLoginUserEmployeeId(userPoid))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to get login user employee ID")
                .hasCauseInstanceOf(SQLException.class);
    }

    @Test
    void getLoginUserEmployeeId_ExecutionError_ThrowsRuntimeException() throws SQLException {
        // Arrange
        Long userPoid = 123L;
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall(anyString())).thenReturn(callableStatement);
        when(callableStatement.execute()).thenThrow(new SQLException("Execution failed"));

        // Act & Assert
        assertThatThrownBy(() -> repository.getLoginUserEmployeeId(userPoid))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to get login user employee ID")
                .hasCauseInstanceOf(SQLException.class);
    }

    // Tests for loadUserPolicies method
    @Test
    void loadUserPolicies_Success_ReturnsPoliciesList() throws SQLException {
        // Arrange
        Long groupPoid = 1L;
        Long companyPoid = 2L;
        String loginUser = "testUser";
        String docId = "800-112";
        Long docKeyPoid = 123L;
        
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall(anyString())).thenReturn(callableStatement);
        when(callableStatement.getString(6)).thenReturn("SUCCESS");
        when(callableStatement.getObject(7)).thenReturn(resultSet);
        
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getLong("DOC_POID")).thenReturn(1L, 2L);
        when(resultSet.getString("DOC_NAME")).thenReturn("Policy 1", "Policy 2");
        when(resultSet.getString("REMARKS")).thenReturn("Remark 1", "Remark 2");
        when(resultSet.getString("DRILLDOWN_LINK_INFO")).thenReturn("Link 1", "Link 2");
        when(resultSet.getString("POLICY_ACCEPTED")).thenReturn("Y", "N");
        when(resultSet.getDate("POLICY_ACCEPTED_ON")).thenReturn(Date.valueOf("2024-01-01"), null);

        // Act
        List<Map<String, Object>> result = repository.loadUserPolicies(groupPoid, companyPoid, loginUser, docId, docKeyPoid);

        // Assert
        assertThat(result).hasSize(2);
        
        Map<String, Object> policy1 = result.get(0);
        assertThat(policy1.get("docPoid")).isEqualTo(1L);
        assertThat(policy1.get("docName")).isEqualTo("Policy 1");
        assertThat(policy1.get("remarks")).isEqualTo("Remark 1");
        assertThat(policy1.get("drilldownLinkInfo")).isEqualTo("Link 1");
        assertThat(policy1.get("policyAccepted")).isEqualTo("Y");
        assertThat(policy1.get("policyAcceptedOn")).isEqualTo(Date.valueOf("2024-01-01"));
        
        Map<String, Object> policy2 = result.get(1);
        assertThat(policy2.get("docPoid")).isEqualTo(2L);
        assertThat(policy2.get("docName")).isEqualTo("Policy 2");
        assertThat(policy2.get("policyAccepted")).isEqualTo("N");
        assertThat(policy2.get("policyAcceptedOn")).isNull();
        
        verify(callableStatement).setLong(1, groupPoid);
        verify(callableStatement).setLong(2, companyPoid);
        verify(callableStatement).setString(3, loginUser);
        verify(callableStatement).setString(4, docId);
        verify(callableStatement).setLong(5, docKeyPoid);
        verify(callableStatement).registerOutParameter(6, Types.VARCHAR);
        verify(callableStatement).registerOutParameter(7, Types.REF_CURSOR);
    }

    @Test
    void loadUserPolicies_WithNullDocKeyPoid_SetsNullParameter() throws SQLException {
        // Arrange
        Long groupPoid = 1L;
        Long companyPoid = 2L;
        String loginUser = "testUser";
        String docId = "800-112";
        Long docKeyPoid = null;
        
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall(anyString())).thenReturn(callableStatement);
        when(callableStatement.getString(6)).thenReturn("SUCCESS");
        when(callableStatement.getObject(7)).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // Act
        List<Map<String, Object>> result = repository.loadUserPolicies(groupPoid, companyPoid, loginUser, docId, docKeyPoid);

        // Assert
        assertThat(result).isEmpty();
        verify(callableStatement).setNull(5, Types.NUMERIC);
    }

    @Test
    void loadUserPolicies_WithEmptyResultSet_ReturnsEmptyList() throws SQLException {
        // Arrange
        Long groupPoid = 1L;
        Long companyPoid = 2L;
        String loginUser = "testUser";
        String docId = "800-112";
        Long docKeyPoid = 123L;
        
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall(anyString())).thenReturn(callableStatement);
        when(callableStatement.getString(6)).thenReturn("SUCCESS");
        when(callableStatement.getObject(7)).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // Act
        List<Map<String, Object>> result = repository.loadUserPolicies(groupPoid, companyPoid, loginUser, docId, docKeyPoid);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void loadUserPolicies_WithMissingRemarksColumn_SetsEmptyString() throws SQLException {
        // Arrange
        Long groupPoid = 1L;
        Long companyPoid = 2L;
        String loginUser = "testUser";
        String docId = "800-112";
        Long docKeyPoid = 123L;
        
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall(anyString())).thenReturn(callableStatement);
        when(callableStatement.getString(6)).thenReturn("SUCCESS");
        when(callableStatement.getObject(7)).thenReturn(resultSet);
        
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("DOC_POID")).thenReturn(1L);
        when(resultSet.getString("DOC_NAME")).thenReturn("Policy 1");
        when(resultSet.getString("REMARKS")).thenThrow(new SQLException("Column not found"));
        when(resultSet.getString("DRILLDOWN_LINK_INFO")).thenReturn("Link 1");
        when(resultSet.getString("POLICY_ACCEPTED")).thenReturn("Y");
        when(resultSet.getDate("POLICY_ACCEPTED_ON")).thenReturn(Date.valueOf("2024-01-01"));

        // Act
        List<Map<String, Object>> result = repository.loadUserPolicies(groupPoid, companyPoid, loginUser, docId, docKeyPoid);

        // Assert
        assertThat(result).hasSize(1);
        Map<String, Object> policy = result.get(0);
        assertThat(policy.get("remarks")).isEqualTo("");
    }

    @Test
    void loadUserPolicies_DatabaseConnectionError_ThrowsRuntimeException() throws SQLException {
        // Arrange
        Long groupPoid = 1L;
        Long companyPoid = 2L;
        String loginUser = "testUser";
        String docId = "800-112";
        Long docKeyPoid = 123L;
        
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> repository.loadUserPolicies(groupPoid, companyPoid, loginUser, docId, docKeyPoid))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to load user policies")
                .hasCauseInstanceOf(SQLException.class);
    }

    @Test
    void loadUserPolicies_ExecutionError_ThrowsRuntimeException() throws SQLException {
        // Arrange
        Long groupPoid = 1L;
        Long companyPoid = 2L;
        String loginUser = "testUser";
        String docId = "800-112";
        Long docKeyPoid = 123L;
        
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall(anyString())).thenReturn(callableStatement);
        when(callableStatement.execute()).thenThrow(new SQLException("Execution failed"));

        // Act & Assert
        assertThatThrownBy(() -> repository.loadUserPolicies(groupPoid, companyPoid, loginUser, docId, docKeyPoid))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to load user policies")
                .hasCauseInstanceOf(SQLException.class);
    }
}