package com.asg.hr.religion.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.religion.dto.ReligionDtoRequest;
import com.asg.hr.religion.dto.ReligionDtoResponse;
import com.asg.hr.religion.service.ReligionService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReligionControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private MockedStatic<UserContext> mockedUserContext;

    @Mock
    private ReligionService service;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private ReligionController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        mockedUserContext = mockStatic(UserContext.class);
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

    private ReligionDtoRequest createMockRequest() {
        ReligionDtoRequest dto = new ReligionDtoRequest();
        dto.setReligionCode("HINDU");
        dto.setDescription("Hindu Religion");
        dto.setActive("Y");
        dto.setSeqNo(1L);
        return dto;
    }

    private ReligionDtoResponse createMockResponse() {
        ReligionDtoResponse dto = new ReligionDtoResponse();
        dto.setReligionPoid(1L);
        dto.setReligionCode("HINDU");
        dto.setDescription("Hindu Religion");
        dto.setActive("Y");
        dto.setSeqNo(1L);
        return dto;
    }

    // ================= CREATE TESTS =================

    @Test
    void createReligion_Success() throws Exception {
        ReligionDtoRequest request = createMockRequest();

        when(service.createReligion(any(ReligionDtoRequest.class))).thenReturn(1L);

        mockMvc.perform(post("/v1/religion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Religion created successfully"))
                .andExpect(jsonPath("$.result.data.religionPoid").value(1));

        verify(service).createReligion(any(ReligionDtoRequest.class));
    }

    @Test
    void updateReligion_Success() throws Exception {
        ReligionDtoRequest request = createMockRequest();
        ReligionDtoResponse response = createMockResponse();

        when(service.updateReligion(any(ReligionDtoRequest.class), eq(1L))).thenReturn(response);

        mockMvc.perform(put("/v1/religion/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Religion updated successfully"))
                .andExpect(jsonPath("$.result.data.religionPoid").value(1));

        verify(service).updateReligion(any(ReligionDtoRequest.class), eq(1L));
    }

    @Test
    void getReligionById_Success() throws Exception {
        ReligionDtoResponse response = createMockResponse();

        when(service.getReligionById(eq(1L))).thenReturn(response);
        doNothing().when(loggingService).createLogSummaryEntry(
                any(LogDetailsEnum.class), anyString(), anyString());

        mockMvc.perform(get("/v1/religion/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Religion detail fetched successfully"))
                .andExpect(jsonPath("$.result.data.religionPoid").value(1))
                .andExpect(jsonPath("$.result.data.religionCode").value("HINDU"));

        verify(service).getReligionById(eq(1L));
        verify(loggingService).createLogSummaryEntry(
                eq(LogDetailsEnum.VIEWED), eq("DOC123"), eq("1"));
    }

    @Test
    void listReligion_Success() throws Exception {
        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", List.of(createMockResponse()));
        responseMap.put("totalElements", 1);
        responseMap.put("totalPages", 1);

        when(service.listReligion(any(FilterRequestDto.class), any())).thenReturn(responseMap);

        mockMvc.perform(post("/v1/religion/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Religions fetched successfully"));

        verify(service).listReligion(any(FilterRequestDto.class), any());
    }

    @Test
    void listReligion_WithNullFilters_Success() throws Exception {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", List.of(createMockResponse()));
        responseMap.put("totalElements", 1);

        when(service.listReligion(any(), any())).thenReturn(responseMap);

        mockMvc.perform(post("/v1/religion/search")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(service).listReligion(any(), any());
    }

    @Test
    void deleteReligion_Success() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("No longer needed");

        doNothing().when(service).deleteReligion(eq(1L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/religion/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Religion deleted successfully"));

        verify(service).deleteReligion(eq(1L), any(DeleteReasonDto.class));
    }

    @Test
    void deleteReligion_WithNullDeleteReason_Success() throws Exception {
        doNothing().when(service).deleteReligion(eq(1L), any());

        mockMvc.perform(delete("/v1/religion/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Religion deleted successfully"));

        verify(service).deleteReligion(eq(1L), any());
    }

    // ================= VALIDATION TESTS =================

    @Test
    void createReligion_MissingReligionCode_BadRequest() throws Exception {
        ReligionDtoRequest request = createMockRequest();
        request.setReligionCode(""); // Invalid

        mockMvc.perform(post("/v1/religion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReligion_ReligionCodeTooLong_BadRequest() throws Exception {
        ReligionDtoRequest request = createMockRequest();
        request.setReligionCode("A".repeat(21)); // Max 20

        mockMvc.perform(post("/v1/religion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReligion_MissingActive_BadRequest() throws Exception {
        ReligionDtoRequest request = createMockRequest();
        request.setActive(""); // Invalid

        mockMvc.perform(post("/v1/religion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReligion_DescriptionTooLong_BadRequest() throws Exception {
        ReligionDtoRequest request = createMockRequest();
        request.setDescription("A".repeat(101)); // Max 100

        mockMvc.perform(post("/v1/religion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}