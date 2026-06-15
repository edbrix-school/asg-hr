package com.asg.hr.employeeinduction.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeInductionProcRepositoryIntegrationTest {

    @Mock
    private EmployeeInductionProcRepository employeeInductionProcRepository;

    @Test
    void testGetInductionCategories() {
        // Given
        Map<String, Object> category = Map.of(
                "INDUCTION_CATG_POID", 1L,
                "STATUS", "A"
        );
        when(employeeInductionProcRepository.getInductionCategories()).thenReturn(List.of(category));

        // When
        List<Map<String, Object>> categories = employeeInductionProcRepository.getInductionCategories();

        // Then
        assertNotNull(categories);
        assertFalse(categories.isEmpty());
        Map<String, Object> firstCategory = categories.get(0);
        assertTrue(firstCategory.containsKey("INDUCTION_CATG_POID"));
        assertTrue(firstCategory.containsKey("STATUS"));
    }
}
