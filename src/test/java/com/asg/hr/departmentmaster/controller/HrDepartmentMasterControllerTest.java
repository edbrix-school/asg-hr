package com.asg.hr.departmentmaster.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.hr.departmentmaster.dto.HrDepartmentMasterRequest;
import com.asg.hr.departmentmaster.dto.HrDepartmentMasterResponse;
import com.asg.hr.departmentmaster.service.HrDepartmentMasterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HrDepartmentMasterControllerTest {

    private static final Long DEPT_POID = 1L;
    private static final Long GROUP_POID = 100L;
    private static final String USER_ID = "user1";
    private static final String DOCUMENT_ID = "doc1";

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private HrDepartmentMasterService departmentService;

    @Mock
    private com.asg.common.lib.service.LoggingService loggingService;

    private MockedStatic<UserContext> userContextMock;

    @BeforeEach
    void setUp() {
        HrDepartmentMasterController controller = new HrDepartmentMasterController(departmentService, loggingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        userContextMock = Mockito.mockStatic(UserContext.class);
        userContextMock.when(UserContext::getDocumentId).thenReturn(DOCUMENT_ID);
        userContextMock.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);
        userContextMock.when(UserContext::getUserId).thenReturn(USER_ID);
    }

    @AfterEach
    void tearDown() {
        if (userContextMock != null) {
            userContextMock.close();
        }
    }

    @Nested
    @DisplayName("POST /v1/department/list")
    class GetDepartmentList {

        @Test
        @DisplayName("returns 200 and department list")
        void returnsList() throws Exception {
            Map<String, Object> page = Map.of(
                    "content", Collections.emptyList(),
                    "totalElements", 0L,
                    "totalPages", 0,
                    "size", 10,
                    "page", 0
            );
            when(departmentService.getAllDepartmentsWithFilters(eq(DOCUMENT_ID), any(FilterRequestDto.class), any()))
                    .thenReturn(page);

            mockMvc.perform(post("/v1/department/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Department list retrieved successfully"));

            verify(departmentService).getAllDepartmentsWithFilters(eq(DOCUMENT_ID), any(FilterRequestDto.class), any());
        }

        @Test
        @DisplayName("returns 200 with null filter request")
        void returnsListWithNullFilter() throws Exception {
            Map<String, Object> page = Map.of("content", Collections.emptyList());
            when(departmentService.getAllDepartmentsWithFilters(eq(DOCUMENT_ID), eq(null), any())).thenReturn(page);

            mockMvc.perform(post("/v1/department/list"))
                    .andExpect(status().isOk());

            verify(departmentService).getAllDepartmentsWithFilters(eq(DOCUMENT_ID), eq(null), any());
        }
    }

    @Nested
    @DisplayName("GET /v1/department/{deptPoid}")
    class GetDepartmentById {

        @Test
        @DisplayName("returns 200 and department when found")
        void returnsDepartment() throws Exception {
            HrDepartmentMasterResponse response = HrDepartmentMasterResponse.builder()
                    .deptPoid(DEPT_POID)
                    .deptName("IT")
                    .groupPoid(GROUP_POID)
                    .build();
            when(departmentService.getDepartmentById(DEPT_POID)).thenReturn(response);

            mockMvc.perform(get("/v1/department/{deptPoid}", DEPT_POID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Department retrieved successfully"));

            verify(departmentService).getDepartmentById(DEPT_POID);
            verify(loggingService).createLogSummaryEntry(any(com.asg.common.lib.enums.LogDetailsEnum.class), eq(DOCUMENT_ID), eq(DEPT_POID.toString()));
        }

        @Test
        @DisplayName("returns 400 when deptPoid is invalid")
        void returns400WhenDeptPoidInvalid() throws Exception {
            mockMvc.perform(get("/v1/department/{deptPoid}", 0))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /v1/department")
    class CreateDepartment {

        @Test
        @DisplayName("returns 201 and created department")
        void createsDepartment() throws Exception {
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("Engineering")
                    .costCentrePoid(200L)
                    .build();
            HrDepartmentMasterResponse response = HrDepartmentMasterResponse.builder()
                    .deptPoid(DEPT_POID)
                    .deptName("Engineering")
                    .build();
            when(departmentService.createDepartment(any(HrDepartmentMasterRequest.class), eq(GROUP_POID), eq(USER_ID))).thenReturn(response);

            mockMvc.perform(post("/v1/department")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Department created successfully"));

            verify(departmentService).createDepartment(any(HrDepartmentMasterRequest.class), eq(GROUP_POID), eq(USER_ID));
        }

        @Test
        @DisplayName("returns 400 when request invalid")
        void returns400WhenInvalid() throws Exception {
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName(null)
                    .costCentrePoid(null)
                    .build();

            mockMvc.perform(post("/v1/department")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /v1/department/{deptPoid}")
    class UpdateDepartment {

        @Test
        @DisplayName("returns 200 and updated department")
        void updatesDepartment() throws Exception {
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("IT Updated")
                    .costCentrePoid(200L)
                    .build();
            HrDepartmentMasterResponse response = HrDepartmentMasterResponse.builder()
                    .deptPoid(DEPT_POID)
                    .deptName("IT Updated")
                    .build();
            when(departmentService.updateDepartment(eq(DEPT_POID), any(HrDepartmentMasterRequest.class), eq(GROUP_POID), eq(USER_ID)))
                    .thenReturn(response);

            mockMvc.perform(put("/v1/department/{deptPoid}", DEPT_POID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Department updated successfully"));

            verify(departmentService).updateDepartment(eq(DEPT_POID), any(HrDepartmentMasterRequest.class), eq(GROUP_POID), eq(USER_ID));
        }

        @Test
        @DisplayName("returns 400 when deptPoid is invalid")
        void returns400WhenDeptPoidInvalid() throws Exception {
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("IT Updated")
                    .costCentrePoid(200L)
                    .build();

            mockMvc.perform(put("/v1/department/{deptPoid}", 0)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /v1/department/{deptPoid}")
    class DeleteDepartment {

        @Test
        @DisplayName("returns 200 when deleted")
        void deletesDepartment() throws Exception {
            mockMvc.perform(delete("/v1/department/{deptPoid}", DEPT_POID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Department deleted successfully"));

            verify(departmentService).deleteDepartment(eq(DEPT_POID), eq(GROUP_POID), eq(USER_ID), any());
        }

        @Test
        @DisplayName("returns 200 when deleted without body")
        void deletesDepartmentWithoutBody() throws Exception {
            mockMvc.perform(delete("/v1/department/{deptPoid}", DEPT_POID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Department deleted successfully"));

            verify(departmentService).deleteDepartment(eq(DEPT_POID), eq(GROUP_POID), eq(USER_ID), eq(null));
        }

        @Test
        @DisplayName("returns 400 when deptPoid is invalid")
        void returns400WhenDeptPoidInvalid() throws Exception {
            mockMvc.perform(delete("/v1/department/{deptPoid}", 0))
                    .andExpect(status().isBadRequest());
        }
    }
}
