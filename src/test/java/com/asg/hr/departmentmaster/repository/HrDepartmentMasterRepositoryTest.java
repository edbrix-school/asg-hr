package com.asg.hr.departmentmaster.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrDepartmentMasterRepositoryTest {

    @Mock
    private HrDepartmentMasterRepository repository;

    @Test
    @DisplayName("existsByDeptPoid returns true when department exists")
    void existsByDeptPoid_returnsTrue() {
        when(repository.existsByDeptPoid(1L)).thenReturn(true);

        assertThat(repository.existsByDeptPoid(1L)).isTrue();
    }

    @Test
    @DisplayName("existsByDeptPoid returns false when department does not exist")
    void existsByDeptPoid_returnsFalse() {
        when(repository.existsByDeptPoid(999L)).thenReturn(false);

        assertThat(repository.existsByDeptPoid(999L)).isFalse();
    }

    @Test
    @DisplayName("existsByDeptNameIgnoreCase returns true when name exists")
    void existsByDeptNameIgnoreCase_returnsTrue() {
        when(repository.existsByDeptNameIgnoreCase("finance")).thenReturn(true);
        when(repository.existsByDeptNameIgnoreCase("FINANCE")).thenReturn(true);
        when(repository.existsByDeptNameIgnoreCase("Finance")).thenReturn(true);

        assertThat(repository.existsByDeptNameIgnoreCase("finance")).isTrue();
        assertThat(repository.existsByDeptNameIgnoreCase("FINANCE")).isTrue();
        assertThat(repository.existsByDeptNameIgnoreCase("Finance")).isTrue();
    }

    @Test
    @DisplayName("existsByDeptNameIgnoreCase returns false when name does not exist")
    void existsByDeptNameIgnoreCase_returnsFalse() {
        when(repository.existsByDeptNameIgnoreCase("NonExistent")).thenReturn(false);

        assertThat(repository.existsByDeptNameIgnoreCase("NonExistent")).isFalse();
    }

    @Test
    @DisplayName("existsByDeptNameIgnoreCaseAndDeptPoidNot returns true when duplicate exists")
    void existsByDeptNameIgnoreCaseAndDeptPoidNot_returnsTrue() {
        when(repository.existsByDeptNameIgnoreCaseAndDeptPoidNot("HR", 2L)).thenReturn(true);

        assertThat(repository.existsByDeptNameIgnoreCaseAndDeptPoidNot("HR", 2L)).isTrue();
    }

    @Test
    @DisplayName("existsByDeptNameIgnoreCaseAndDeptPoidNot returns false when no duplicate")
    void existsByDeptNameIgnoreCaseAndDeptPoidNot_returnsFalse() {
        when(repository.existsByDeptNameIgnoreCaseAndDeptPoidNot("Marketing", 1L)).thenReturn(false);
        when(repository.existsByDeptNameIgnoreCaseAndDeptPoidNot("NonExistent", 1L)).thenReturn(false);

        assertThat(repository.existsByDeptNameIgnoreCaseAndDeptPoidNot("Marketing", 1L)).isFalse();
        assertThat(repository.existsByDeptNameIgnoreCaseAndDeptPoidNot("NonExistent", 1L)).isFalse();
    }
}
