package com.asg.hr.competency.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.competency.dto.CompetencyMasterRequestDto;
import com.asg.hr.competency.entity.CompetencyMasterEntity;
import com.asg.hr.competency.repository.CompetencyMasterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetencyMasterServiceImplTest {

    @Mock private CompetencyMasterRepository repository;
    @Mock private DocumentSearchService documentSearchService;
    @Mock private DocumentDeleteService documentDeleteService;
    @Mock private LoggingService loggingService;

    @InjectMocks private CompetencyMasterServiceImpl service;

    @Test
    void create_whenCompetencyCodeAlreadyExists_throws() {
        CompetencyMasterRequestDto req = CompetencyMasterRequestDto.builder()
                .competencyCode("C1")
                .competencyDescription("Desc")
                .competencyNarration("Nar")
                .seqNo(1)
                .build();

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(10L);
            when(repository.existsByCompetencyCodeAndGroupPoid("C1", 10L)).thenReturn(true);

            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }
    }

    @Test
    void create_success_savesEntity_setsFlags_andLogs() {
        CompetencyMasterRequestDto req = CompetencyMasterRequestDto.builder()
                .competencyCode("C1")
                .competencyDescription("Desc")
                .competencyNarration("Nar")
                .seqNo(7)
                .build();

        CompetencyMasterEntity saved = CompetencyMasterEntity.builder()
                .competencyPoid(99L)
                .groupPoid(10L)
                .competencyCode("C1")
                .competencyDescription("Desc")
                .competencyNarration("Nar")
                .seqNo(7)
                .active("Y")
                .deleted("N")
                .build();

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(10L);
            uc.when(UserContext::getDocumentId).thenReturn("DOC1");

            when(repository.existsByCompetencyCodeAndGroupPoid("C1", 10L)).thenReturn(false);
            when(repository.save(any(CompetencyMasterEntity.class))).thenReturn(saved);

            var resp = service.create(req);

            ArgumentCaptor<CompetencyMasterEntity> captor = ArgumentCaptor.forClass(CompetencyMasterEntity.class);
            verify(repository).save(captor.capture());
            CompetencyMasterEntity toSave = captor.getValue();
            assertThat(toSave.getGroupPoid()).isEqualTo(10L);
            assertThat(toSave.getCompetencyCode()).isEqualTo("C1");
            assertThat(toSave.getActive()).isEqualTo("Y");
            assertThat(toSave.getDeleted()).isEqualTo("N");

            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "DOC1", "99");

            assertThat(resp.getCompetencyPoid()).isEqualTo(99L);
            assertThat(resp.getGroupPoid()).isEqualTo(10L);
            assertThat(resp.getCompetencyCode()).isEqualTo("C1");
            assertThat(resp.getSeqNo()).isEqualTo(7);
        }
    }

    @Test
    void getById_whenMissing_throws() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_success_mapsToResponse() {
        CompetencyMasterEntity entity = CompetencyMasterEntity.builder()
                .competencyPoid(1L)
                .groupPoid(10L)
                .competencyCode("C1")
                .competencyDescription("D")
                .competencyNarration("N")
                .seqNo(3)
                .active("Y")
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        var resp = service.getById(1L);
        assertThat(resp.getCompetencyPoid()).isEqualTo(1L);
        assertThat(resp.getGroupPoid()).isEqualTo(10L);
        assertThat(resp.getCompetencyCode()).isEqualTo("C1");
        assertThat(resp.getActive()).isEqualTo("Y");
    }

    @Test
    void update_whenMissing_throws() {
        CompetencyMasterRequestDto req = CompetencyMasterRequestDto.builder()
                .competencyCode("C2")
                .build();

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(10L);
            when(repository.findByIdAndGroupPoidAndNotDeleted(5L, 10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(5L, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    void update_whenDuplicateCode_throws() {
        CompetencyMasterRequestDto req = CompetencyMasterRequestDto.builder()
                .competencyCode("C2")
                .build();

        CompetencyMasterEntity existing = CompetencyMasterEntity.builder()
                .competencyPoid(5L)
                .groupPoid(10L)
                .competencyCode("C1")
                .build();

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(10L);
            when(repository.findByIdAndGroupPoidAndNotDeleted(5L, 10L)).thenReturn(Optional.of(existing));
            when(repository.existsByCompetencyCodeAndGroupPoidAndIdNot("C2", 10L, 5L)).thenReturn(true);

            assertThatThrownBy(() -> service.update(5L, req))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }
    }

    @Test
    void update_success_savesAndLogsChanges() {
        CompetencyMasterRequestDto req = CompetencyMasterRequestDto.builder()
                .competencyCode("C2")
                .competencyDescription("D2")
                .competencyNarration("N2")
                .seqNo(2)
                .build();

        CompetencyMasterEntity existing = CompetencyMasterEntity.builder()
                .competencyPoid(5L)
                .groupPoid(10L)
                .competencyCode("C1")
                .competencyDescription("D1")
                .competencyNarration("N1")
                .seqNo(1)
                .active("Y")
                .build();

        CompetencyMasterEntity saved = CompetencyMasterEntity.builder()
                .competencyPoid(5L)
                .groupPoid(10L)
                .competencyCode("C2")
                .competencyDescription("D2")
                .competencyNarration("N2")
                .seqNo(2)
                .active("Y")
                .build();

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(10L);
            uc.when(UserContext::getDocumentId).thenReturn("DOC1");

            when(repository.findByIdAndGroupPoidAndNotDeleted(5L, 10L)).thenReturn(Optional.of(existing));
            when(repository.existsByCompetencyCodeAndGroupPoidAndIdNot("C2", 10L, 5L)).thenReturn(false);
            when(repository.save(any(CompetencyMasterEntity.class))).thenReturn(saved);

            var resp = service.update(5L, req);

            verify(loggingService).logChanges(any(CompetencyMasterEntity.class), any(CompetencyMasterEntity.class),
                    eq(CompetencyMasterEntity.class), eq("DOC1"), eq("5"), eq(LogDetailsEnum.MODIFIED), eq("COMPETENCY_POID"));

            assertThat(resp.getCompetencyCode()).isEqualTo("C2");
            assertThat(resp.getCompetencyDescription()).isEqualTo("D2");
            assertThat(resp.getSeqNo()).isEqualTo(2);
        }
    }

    @Test
    void delete_whenMissing_throws() {
        DeleteReasonDto reason = mock(DeleteReasonDto.class);
        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(10L);
            when(repository.findByIdAndGroupPoidAndNotDeleted(7L, 10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(7L, reason))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    void delete_success_callsDocumentDeleteService() {
        DeleteReasonDto reason = mock(DeleteReasonDto.class);
        CompetencyMasterEntity entity = CompetencyMasterEntity.builder()
                .competencyPoid(7L)
                .groupPoid(10L)
                .competencyCode("C1")
                .build();

        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getGroupPoid).thenReturn(10L);
            when(repository.findByIdAndGroupPoidAndNotDeleted(7L, 10L)).thenReturn(Optional.of(entity));

            service.delete(7L, reason);

            verify(documentDeleteService).deleteDocument(
                    eq(7L),
                    eq("HR_COMPETENCY_MASTER"),
                    eq("COMPETENCY_POID"),
                    same(reason),
                    isNull()
            );
        }
    }

    @Test
    void list_delegatesToSearch_andWrapsPage() {
        FilterRequestDto request = mock(FilterRequestDto.class);
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult raw = mock(RawSearchResult.class);
        when(documentSearchService.resolveOperator(request)).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(request)).thenReturn("N");
        when(documentSearchService.resolveFilters(request)).thenReturn(List.of());

        when(raw.records()).thenReturn(List.of(Map.of("COMPETENCY_POID", 1L)));
        when(raw.totalRecords()).thenReturn(1L);
        when(raw.displayFields()).thenReturn(Map.of("COMPETENCY_DESCRIPTION", "Competency Description"));

        when(documentSearchService.search(eq("DOC1"), anyList(), eq("AND"), eq(pageable), eq("N"),
                eq("COMPETENCY_DESCRIPTION"), eq("COMPETENCY_POID")))
                .thenReturn(raw);

        Map<String, Object> result = service.list("DOC1", request, pageable);

        assertThat(result).isNotNull();
        verify(documentSearchService).search(eq("DOC1"), anyList(), eq("AND"), eq(pageable), eq("N"),
                eq("COMPETENCY_DESCRIPTION"), eq("COMPETENCY_POID"));
    }
}
