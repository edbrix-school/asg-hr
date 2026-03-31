package com.asg.hr.designation.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.designation.dto.DesignationRequest;
import com.asg.hr.designation.dto.DesignationResponse;
import com.asg.hr.designation.service.DesignationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DesignationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private MockedStatic<UserContext> mockedUserContext;

    @Mock
    private DesignationService designationService;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private DesignationController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        mockedUserContext = org.mockito.Mockito.mockStatic(UserContext.class);
        mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

        PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(pageableResolver)
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @AfterEach
    void tearDown() {
        if (mockedUserContext != null) {
            mockedUserContext.close();
        }
    }

    private DesignationRequest createMockRequest() {
        return DesignationRequest.builder()
                .designationCode("DEV001")
                .designationName("Developer")
                .jobDescription("<p>Writes code</p>")
                .skillDescription("Java, Spring")
                .active("Y")
                .seqNo(1L)
                .build();
    }

    private DesignationResponse createMockResponse() {
        return DesignationResponse.builder()
                .designationPoid(1L)
                .designationCode("DEV001")
                .designationName("Developer")
                .jobDescription("<p>Writes code</p>")
                .skillDescription("Java, Spring")
                .active("Y")
                .seqNo(1L)
                .build();
    }

    @Test
    void createDesignation_Success() throws Exception {
        DesignationRequest request = createMockRequest();

        when(designationService.createDesignation(any(DesignationRequest.class))).thenReturn(1L);

        mockMvc.perform(post("/v1/designation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Designation created successfully"))
                .andExpect(jsonPath("$.result.data.designationPoid").value(1));

        verify(designationService).createDesignation(any(DesignationRequest.class));
    }

    @Test
    void updateDesignation_Success() throws Exception {
        DesignationRequest request = createMockRequest();
        DesignationResponse response = createMockResponse();

        when(designationService.updateDesignation(eq(1L), any(DesignationRequest.class))).thenReturn(response);

        mockMvc.perform(put("/v1/designation/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Designation updated successfully"))
                .andExpect(jsonPath("$.result.data.designationPoid").value(1));

        verify(designationService).updateDesignation(eq(1L), any(DesignationRequest.class));
    }

    @Test
    void getDesignationById_Success() throws Exception {
        DesignationResponse response = createMockResponse();

        when(designationService.getDesignationById(eq(1L))).thenReturn(response);
        doNothing().when(loggingService).createLogSummaryEntry(
                any(LogDetailsEnum.class), anyString(), anyString());

        mockMvc.perform(get("/v1/designation/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Designation retrieved successfully"))
                .andExpect(jsonPath("$.result.data.designationPoid").value(1))
                .andExpect(jsonPath("$.result.data.designationCode").value("DEV001"));

        verify(designationService).getDesignationById(eq(1L));
        verify(loggingService).createLogSummaryEntry(
                eq(LogDetailsEnum.VIEWED), eq("DOC123"), eq("1"));
    }

    @Test
    void listDesignations_Success() throws Exception {
        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", List.of(createMockResponse()));
        responseMap.put("totalElements", 1);
        responseMap.put("totalPages", 1);

        when(designationService.listDesignations(any(FilterRequestDto.class), any())).thenReturn(responseMap);

        mockMvc.perform(post("/v1/designation/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Designation list retrieved successfully"));

        verify(designationService).listDesignations(any(FilterRequestDto.class), any());
    }

    @Test
    void listDesignations_WithNullFilters_Success() throws Exception {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", List.of(createMockResponse()));
        responseMap.put("totalElements", 1);

        when(designationService.listDesignations(any(), any())).thenReturn(responseMap);

        mockMvc.perform(post("/v1/designation/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(designationService).listDesignations(any(), any());
    }

    @Test
    void deleteDesignation_Success() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("No longer needed");

        doNothing().when(designationService).deleteDesignation(eq(1L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/designation/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Designation deleted successfully"));

        verify(designationService).deleteDesignation(eq(1L), any(DeleteReasonDto.class));
    }

    @Test
    void deleteDesignation_WithNullDeleteReason_Success() throws Exception {
        doNothing().when(designationService).deleteDesignation(eq(1L), any());

        mockMvc.perform(delete("/v1/designation/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Designation deleted successfully"));

        verify(designationService).deleteDesignation(eq(1L), any());
    }

    @Test
    void createDesignation_MissingCode_BadRequest() throws Exception {
        DesignationRequest request = createMockRequest();
        request.setDesignationCode(""); // Invalid

        mockMvc.perform(post("/v1/designation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDesignation_MissingName_BadRequest() throws Exception {
        DesignationRequest request = createMockRequest();
        request.setDesignationName(""); // Invalid

        mockMvc.perform(post("/v1/designation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDesignation_MissingJobDescription_BadRequest() throws Exception {
        DesignationRequest request = createMockRequest();
        request.setJobDescription(""); // Invalid

        mockMvc.perform(post("/v1/designation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

