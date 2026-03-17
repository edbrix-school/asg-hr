package com.asg.hr.holidaymaster.controller;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.holidaymaster.dto.HolidayBatchCreateRequest;
import com.asg.hr.holidaymaster.dto.HolidayMasterRequest;
import com.asg.hr.holidaymaster.dto.HolidayMasterResponse;
import com.asg.hr.holidaymaster.service.HolidayMasterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayMasterControllerTest {

    @Mock
    private HolidayMasterService holidayMasterService;

    @InjectMocks
    private HolidayMasterController controller;

    private HolidayMasterRequest request;
    private HolidayMasterResponse response;

    @BeforeEach
    void setUp() {
        request = new HolidayMasterRequest();
        request.setHolidayDate(LocalDate.of(2030, 1, 1));
        request.setHolidayReason("New Year");
        request.setSeqNo(1);
        request.setActive("Y");

        response = new HolidayMasterResponse();
        response.setHolidayPoid(1L);
        response.setHolidayDate(LocalDate.of(2030, 1, 1));
        response.setHolidayReason("New Year");
    }

    @Test
    void listHolidays_ReturnsSuccessResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        FilterRequestDto filters = new FilterRequestDto("OR", "N", List.of(new FilterDto("GLOBALSEARCH", "new")));

        when(holidayMasterService.listHolidays("800-011", filters, pageable))
                .thenReturn(Map.of("items", List.of(), "total", 0));

        ResponseEntity<?> entity = controller.listHolidays(pageable, filters, "800-011");

        assertNotNull(entity);
        assertEquals(200, entity.getStatusCode().value());
        verify(holidayMasterService).listHolidays("800-011", filters, pageable);
    }

    @Test
    void getById_ReturnsSuccessResponse() {
        when(holidayMasterService.getById(1L)).thenReturn(response);

        ResponseEntity<?> entity = controller.getById(1L);

        assertNotNull(entity);
        assertEquals(200, entity.getStatusCode().value());
        verify(holidayMasterService).getById(1L);
    }

    @Test
    void create_ReturnsSuccessResponse() {
        when(holidayMasterService.create(request)).thenReturn(response);

        ResponseEntity<?> entity = controller.create(request);

        assertNotNull(entity);
        assertEquals(200, entity.getStatusCode().value());
        verify(holidayMasterService).create(request);
    }

    @Test
    void update_ReturnsSuccessResponse() {
        when(holidayMasterService.update(1L, request)).thenReturn(response);

        ResponseEntity<?> entity = controller.update(1L, request);

        assertNotNull(entity);
        assertEquals(200, entity.getStatusCode().value());
        verify(holidayMasterService).update(1L, request);
    }

    @Test
    void toggleActiveStatus_ReturnsSuccessResponse() {
        doNothing().when(holidayMasterService).toggleActiveStatus(1L);

        ResponseEntity<?> entity = controller.toggleActiveStatus(1L);

        assertNotNull(entity);
        assertEquals(200, entity.getStatusCode().value());
        verify(holidayMasterService).toggleActiveStatus(1L);
    }

    @Test
    void delete_ReturnsSuccessResponse() {
        doNothing().when(holidayMasterService).delete(1L);

        ResponseEntity<?> entity = controller.delete(1L);

        assertNotNull(entity);
        assertEquals(200, entity.getStatusCode().value());
        verify(holidayMasterService).delete(1L);
    }

    @Test
    void batchCreate_ReturnsSuccessResponse() {
        HolidayBatchCreateRequest batchRequest = new HolidayBatchCreateRequest(
                LocalDate.of(2030, 1, 1),
                "Festival",
                3
        );
        when(holidayMasterService.batchCreateHolidays(any(HolidayBatchCreateRequest.class))).thenReturn("SUCCESS");

        ResponseEntity<?> entity = controller.batchCreate(batchRequest);

        assertNotNull(entity);
        assertEquals(200, entity.getStatusCode().value());
        verify(holidayMasterService).batchCreateHolidays(batchRequest);
    }
}
