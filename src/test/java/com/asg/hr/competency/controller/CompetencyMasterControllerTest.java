package com.asg.hr.competency.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.competency.dto.CompetencyMasterRequestDto;
import com.asg.hr.competency.dto.CompetencyMasterResponseDto;
import com.asg.hr.competency.service.CompetencyMasterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetencyMasterControllerTest {

    @Test
    void list_usesUserDocumentId_andReturnsSuccessResponse() {
        CompetencyMasterService service = mock(CompetencyMasterService.class);
        LoggingService loggingService = mock(LoggingService.class);
        CompetencyMasterController controller = new CompetencyMasterController(service, loggingService);

        Pageable pageable = mock(Pageable.class);
        FilterRequestDto filters = mock(FilterRequestDto.class);
        when(service.list(eq("DOC1"), same(filters), same(pageable))).thenReturn(Map.of("items", 1));

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getDocumentId).thenReturn("DOC1");

            ResponseEntity<?> resp = controller.list(pageable, filters);

            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            verify(service).list(eq("DOC1"), same(filters), same(pageable));
            verifyNoInteractions(loggingService);
        }
    }

    @Test
    void create_delegatesToService() {
        CompetencyMasterService service = mock(CompetencyMasterService.class);
        LoggingService loggingService = mock(LoggingService.class);
        CompetencyMasterController controller = new CompetencyMasterController(service, loggingService);

        CompetencyMasterRequestDto req = CompetencyMasterRequestDto.builder()
                .competencyCode("C1")
                .build();
        CompetencyMasterResponseDto created = CompetencyMasterResponseDto.builder()
                .competencyPoid(1L)
                .competencyCode("C1")
                .build();

        when(service.create(same(req))).thenReturn(created);

        ResponseEntity<?> resp = controller.create(req);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(service).create(same(req));
        verifyNoInteractions(loggingService);
    }

    @Test
    void update_delegatesToService() {
        CompetencyMasterService service = mock(CompetencyMasterService.class);
        LoggingService loggingService = mock(LoggingService.class);
        CompetencyMasterController controller = new CompetencyMasterController(service, loggingService);

        CompetencyMasterRequestDto req = CompetencyMasterRequestDto.builder()
                .competencyCode("C2")
                .build();
        CompetencyMasterResponseDto updated = CompetencyMasterResponseDto.builder()
                .competencyPoid(2L)
                .competencyCode("C2")
                .build();

        when(service.update(2L, req)).thenReturn(updated);

        ResponseEntity<?> resp = controller.update(2L, req);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(service).update(2L, req);
        verifyNoInteractions(loggingService);
    }

    @Test
    void getById_logsViewed_andDelegatesToService() {
        CompetencyMasterService service = mock(CompetencyMasterService.class);
        LoggingService loggingService = mock(LoggingService.class);
        CompetencyMasterController controller = new CompetencyMasterController(service, loggingService);

        CompetencyMasterResponseDto dto = CompetencyMasterResponseDto.builder()
                .competencyPoid(5L)
                .build();
        when(service.getById(5L)).thenReturn(dto);

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getDocumentId).thenReturn("DOC1");

            ResponseEntity<?> resp = controller.getById(5L);

            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, "DOC1", "5");
            verify(service).getById(5L);
        }
    }

    @Test
    void delete_delegatesToService_andReturnsSuccessResponse() {
        CompetencyMasterService service = mock(CompetencyMasterService.class);
        LoggingService loggingService = mock(LoggingService.class);
        CompetencyMasterController controller = new CompetencyMasterController(service, loggingService);

        DeleteReasonDto reason = mock(DeleteReasonDto.class);

        ResponseEntity<?> resp = controller.delete(9L, reason);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(service).delete(9L, reason);
        verifyNoInteractions(loggingService);
    }
}
