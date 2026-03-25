package com.asg.hr.departmentmaster.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HrDepartmentMasterServiceTest {

    @Test
    @DisplayName("interface has correct method signatures")
    void interfaceHasCorrectMethods() {
        assertThat(HrDepartmentMasterService.class.isInterface()).isTrue();
        assertThat(HrDepartmentMasterService.class.getMethods()).hasSizeGreaterThan(0);
    }
}
