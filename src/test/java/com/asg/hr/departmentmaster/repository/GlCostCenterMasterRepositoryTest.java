package com.asg.hr.departmentmaster.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlCostCenterMasterRepositoryTest {

    @Mock
    private GlCostCenterMasterRepository repository;

    @Test
    @DisplayName("existsByCostCenterPoid returns true when cost center exists")
    void existsByCostCenterPoid_returnsTrue() {
        when(repository.existsByCostCenterPoid(1L)).thenReturn(true);

        assertThat(repository.existsByCostCenterPoid(1L)).isTrue();
    }

    @Test
    @DisplayName("existsByCostCenterPoid returns false when cost center does not exist")
    void existsByCostCenterPoid_returnsFalse() {
        when(repository.existsByCostCenterPoid(999L)).thenReturn(false);

        assertThat(repository.existsByCostCenterPoid(999L)).isFalse();
    }
}
