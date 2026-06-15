package com.asg.hr.employeetraining.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.employeetraining.dto.EmployeeTrainingRequest;
import com.asg.hr.employeetraining.dto.EmployeeTrainingResponse;
import com.asg.hr.employeetraining.service.EmployeeTrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeTrainingControllerTest {

    @Mock
    private EmployeeTrainingService employeeTrainingService;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private EmployeeTrainingController controller;

    private EmployeeTrainingRequest request;
    private EmployeeTrainingResponse response;

    @BeforeEach
    void setUp() {
        request = new EmployeeTrainingRequest();
        request.setCourseName("Safety Training");
        request.setPeriodFrom(LocalDate.of(2030, 1, 1));
        request.setPeriodTo(LocalDate.of(2030, 1, 2));
        request.setTrainingType("INHOUSE");
        request.setInstitution("ASG Academy");
        request.setTrainingLocation("Doha");
        request.setTransactionDate(LocalDate.of(2030, 1, 1));
        request.setEmployeePoid("1001");

        response = new EmployeeTrainingResponse();
        response.setTransactionPoid(1L);
        response.setCourseName("Safety Training");
        response.setActive("Y");
    }

    @Test
    void listTrainings_ReturnsSuccess() {
        Pageable pageable = PageRequest.of(0, 10);
        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of(new FilterDto("COURSE_NAME", "Safety")));

        when(employeeTrainingService.listTrainings("800-108", filters, pageable))
                .thenReturn(Map.of("items", List.of(), "total", 0));

        try (MockedStatic<UserContext> userContext = org.mockito.Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("800-108");

            ResponseEntity<?> entity = controller.listTrainings(pageable, filters);

            assertNotNull(entity);
            assertEquals(200, entity.getStatusCode().value());
            verify(employeeTrainingService).listTrainings("800-108", filters, pageable);
        }
    }

    @Test
    void getById_ReturnsSuccessResponse() {
        when(employeeTrainingService.getTrainingById(1L)).thenReturn(response);

        try (MockedStatic<UserContext> userContext = org.mockito.Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("800-108");

            doNothing().when(loggingService).createLogSummaryEntry(
                    LogDetailsEnum.VIEWED,
                    "800-108",
                    "1"
            );

            ResponseEntity<?> entity = controller.getById(1L);

            assertNotNull(entity);
            assertEquals(200, entity.getStatusCode().value());
            verify(employeeTrainingService).getTrainingById(1L);
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, "800-108", "1");
        }
    }

    @Test
    void createTraining_ReturnsSuccess() {
        when(employeeTrainingService.createTraining(request)).thenReturn(response);

        ResponseEntity<?> entity = controller.createTraining(request);

        assertNotNull(entity);
        assertEquals(200, entity.getStatusCode().value());
        verify(employeeTrainingService).createTraining(request);
    }

    @Test
    void updateTraining_ReturnsSuccess() {
        when(employeeTrainingService.updateTraining(1L, request)).thenReturn(response);

        ResponseEntity<?> entity = controller.updateTraining(1L, request);

        assertNotNull(entity);
        assertEquals(200, entity.getStatusCode().value());
        verify(employeeTrainingService).updateTraining(1L, request);
    }

    @Test
    void delete_ReturnsSuccessResponse() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        doNothing().when(employeeTrainingService).deleteTraining(1L, deleteReasonDto);

        ResponseEntity<?> entity = controller.deleteTraining(1L, deleteReasonDto);

        assertNotNull(entity);
        assertEquals(200, entity.getStatusCode().value());
        verify(employeeTrainingService).deleteTraining(1L, deleteReasonDto);
    }

}
