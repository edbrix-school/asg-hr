package com.asg.hr.common.controller;

import com.asg.common.lib.dto.LovGetListDto;
import com.asg.hr.common.dto.CurrentUserEmployeeDto;
import com.asg.hr.common.dto.EmployeeLovDto;
import com.asg.hr.common.dto.EmployeeLovQuery;
import com.asg.hr.common.service.CurrentUserEmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HrCommonControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CurrentUserEmployeeService currentUserEmployeeService;

    @InjectMocks
    private HrCommonController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private CurrentUserEmployeeDto.CurrentUserEmployeeDtoBuilder linkedEmployee() {
        return CurrentUserEmployeeDto.builder()
                .userId("ksharma")
                .userPoid(5L)
                .documentId("800-106")
                .employeePoid(77L)
                .employeeCode("EMP-77")
                .employeeName("Kamal Sharma")
                .active("Y")
                .linkedToEmployee(true)
                .canSelectAnyEmployee(false);
    }

    @Test
    void returnsTheCurrentUserEmployee() throws Exception {
        when(currentUserEmployeeService.getCurrentUserEmployee(any())).thenReturn(linkedEmployee().build());

        mockMvc.perform(get("/v1/common/current-employee").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.employeePoid").value(77))
                .andExpect(jsonPath("$.result.data.employeeCode").value("EMP-77"))
                .andExpect(jsonPath("$.result.data.documentId").value("800-106"))
                .andExpect(jsonPath("$.result.data.linkedToEmployee").value(true))
                .andExpect(jsonPath("$.result.data.canSelectAnyEmployee").value(false))
                .andExpect(jsonPath("$.result.data.employeeLov").isEmpty());
    }

    @Test
    void returnsANullEmployeeForAnUnlinkedLogin() throws Exception {
        when(currentUserEmployeeService.getCurrentUserEmployee(any())).thenReturn(CurrentUserEmployeeDto.builder()
                .userId("admin")
                .userPoid(1L)
                .documentId("800-106")
                .linkedToEmployee(false)
                .canSelectAnyEmployee(true)
                .build());

        mockMvc.perform(get("/v1/common/current-employee").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.employeePoid").isEmpty())
                .andExpect(jsonPath("$.result.data.linkedToEmployee").value(false))
                .andExpect(jsonPath("$.result.data.canSelectAnyEmployee").value(true));
    }

    @Test
    void returnsTheEmployeeLovWhenOneIsNamed() throws Exception {
        LovGetListDto row = new LovGetListDto();
        row.setPoid(77L);
        row.setCode("EMP-77");
        row.setLabel("Kamal Sharma");
        row.setValue(77L);

        when(currentUserEmployeeService.getCurrentUserEmployee(any())).thenReturn(linkedEmployee()
                .employeeLov(EmployeeLovDto.builder()
                        .lovName("EMPLOYEE_NAME")
                        .restrictedToOwnEmployee(true)
                        .totalRecords(1)
                        .data(List.of(row))
                        .build())
                .build());

        mockMvc.perform(get("/v1/common/current-employee")
                        .param("lovName", "EMPLOYEE_NAME")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.employeeLov.lovName").value("EMPLOYEE_NAME"))
                .andExpect(jsonPath("$.result.data.employeeLov.restrictedToOwnEmployee").value(true))
                .andExpect(jsonPath("$.result.data.employeeLov.totalRecords").value(1))
                .andExpect(jsonPath("$.result.data.employeeLov.data[0].poid").value(77));
    }

    @Test
    void passesTheLovParametersThrough() throws Exception {
        when(currentUserEmployeeService.getCurrentUserEmployee(any())).thenReturn(linkedEmployee().build());

        mockMvc.perform(get("/v1/common/current-employee")
                        .param("lovName", "EMPLOYEE_NAME")
                        .param("filter", "kam")
                        .param("pageNumber", "2")
                        .param("pageSize", "50")
                        .param("sortBy", "code")
                        .param("sortDir", "desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<EmployeeLovQuery> query = ArgumentCaptor.forClass(EmployeeLovQuery.class);
        verify(currentUserEmployeeService).getCurrentUserEmployee(query.capture());

        assertEquals("EMPLOYEE_NAME", query.getValue().lovName());
        assertEquals("kam", query.getValue().filter());
        assertEquals(2, query.getValue().pageNumber());
        assertEquals(50, query.getValue().pageSize());
        assertEquals("code", query.getValue().sortBy());
        assertEquals("desc", query.getValue().sortDir());
    }

    @Test
    void sendsNoLovNameWhenTheCallerOmitsIt() throws Exception {
        when(currentUserEmployeeService.getCurrentUserEmployee(any())).thenReturn(linkedEmployee().build());

        mockMvc.perform(get("/v1/common/current-employee").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<EmployeeLovQuery> query = ArgumentCaptor.forClass(EmployeeLovQuery.class);
        verify(currentUserEmployeeService).getCurrentUserEmployee(query.capture());

        assertNull(query.getValue().lovName());
    }
}
