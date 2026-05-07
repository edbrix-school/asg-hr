package com.asg.hr.employeeinduction.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmployeeInductionProcRepositoryIntegrationTest {

    @Autowired
    private EmployeeInductionProcRepository employeeInductionProcRepository;

    @Test
    void testGetInductionCategories() {
        // When
        List<Map<String, Object>> categories = employeeInductionProcRepository.getInductionCategories();

        // Then
        assertNotNull(categories);
        // Note: The actual assertions will depend on your test data
        // This test verifies that the stored procedure call doesn't throw exceptions
        
        if (!categories.isEmpty()) {
            Map<String, Object> firstCategory = categories.get(0);
            assertTrue(firstCategory.containsKey("INDUCTION_CATG_POID"));
            assertTrue(firstCategory.containsKey("STATUS"));
        }
    }
}