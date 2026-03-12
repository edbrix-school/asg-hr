package com.asg.hr.nationality.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.nationality.dto.request.HrNationalityRequest;
import com.asg.hr.nationality.dto.request.HrNationalityUpdateRequest;
import com.asg.hr.nationality.dto.response.HrNationalityResponse;
import com.asg.hr.nationality.service.HrNationalityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HrNationalityControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private MockedStatic<UserContext> mockedUserContext;

    @Mock
    private HrNationalityService hrNationalityService;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private HrNationalityController hrNationalityController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockedUserContext = mockStatic(UserContext.class);
        mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

        PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(hrNationalityController)
                .setCustomArgumentResolvers(pageableResolver)
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        mockedUserContext.close();
    }

    @Test
    void create_Success() throws Exception {
        HrNationalityRequest request = createMockRequest();
        HrNationalityResponse response = createMockResponse();

        when(hrNationalityService.create(any(HrNationalityRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/nationality-master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(hrNationalityService).create(any(HrNationalityRequest.class));
    }

    @Test
    void update_Success() throws Exception {
        HrNationalityUpdateRequest request = createMockUpdateRequest();
        HrNationalityResponse response = createMockResponse();
 
        when(hrNationalityService.update(eq(1L), any(HrNationalityUpdateRequest.class))).thenReturn(response);
 
        mockMvc.perform(put("/v1/nationality-master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
 
        verify(hrNationalityService).update(eq(1L), any(HrNationalityUpdateRequest.class));
    }

    @Test
    void getById_Success() throws Exception {
        HrNationalityResponse response = createMockResponse();

        when(hrNationalityService.getById(1L)).thenReturn(response);
        doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

        mockMvc.perform(get("/v1/nationality-master/1"))
                .andExpect(status().isOk());

        verify(hrNationalityService).getById(1L);
    }

    @Test
    void delete_Success() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Testing delete");

        doNothing().when(hrNationalityService).delete(eq(1L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/nationality-master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk());

        verify(hrNationalityService).delete(eq(1L), any(DeleteReasonDto.class));
    }

    @Test
    void list_Success() throws Exception {
        FilterRequestDto filters = new FilterRequestDto("OR", "N", List.of());
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", List.of(createMockResponse()));

        when(hrNationalityService.list(any(FilterRequestDto.class), any())).thenReturn(responseMap);

        mockMvc.perform(post("/v1/nationality-master/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isOk());

        verify(hrNationalityService).list(any(FilterRequestDto.class), any());
    }

    private HrNationalityRequest createMockRequest() {
        HrNationalityRequest request = new HrNationalityRequest();
        request.setNationalityCode("IND");
        request.setNationalityDescription("India");
        request.setTicketAmountNormal(BigDecimal.valueOf(100.0));
        request.setTicketAmountBusiness(BigDecimal.valueOf(200.0));
        request.setActive(true);
        request.setSeqNo(1);
        return request;
    }

    private HrNationalityUpdateRequest createMockUpdateRequest() {
        HrNationalityUpdateRequest request = new HrNationalityUpdateRequest();
        request.setNationalityDescription("India Updated");
        request.setTicketAmountNormal(BigDecimal.valueOf(150.0));
        request.setTicketAmountBusiness(BigDecimal.valueOf(250.0));
        request.setActive(true);
        request.setSeqNo(1);
        return request;
    }

    private HrNationalityResponse createMockResponse() {
        return HrNationalityResponse.builder()
                .nationPoid(1L)
                .nationalityCode("IND")
                .nationalityDescription("India")
                .ticketAmountNormal(BigDecimal.valueOf(100.0))
                .ticketAmountBusiness(BigDecimal.valueOf(200.0))
                .active(true)
                .seqNo(1)
                .build();
    }
}
