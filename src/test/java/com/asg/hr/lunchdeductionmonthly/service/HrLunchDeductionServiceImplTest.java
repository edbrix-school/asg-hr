package com.asg.hr.lunchdeductionmonthly.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.exceptions.ResourceAlreadyExistsException;
import com.asg.hr.exceptions.ResourceNotFoundException;
import com.asg.hr.exceptions.ValidationException;
import com.asg.hr.lunchdeductionmonthly.dto.*;
import com.asg.hr.lunchdeductionmonthly.entity.HrMonthlyLunchDtl;
import com.asg.hr.lunchdeductionmonthly.entity.HrMonthlyLunchHdr;
import com.asg.hr.lunchdeductionmonthly.mapper.HrLunchDeductionMapper;
import com.asg.hr.lunchdeductionmonthly.repository.HrLunchDeductionProcRepository;
import com.asg.hr.lunchdeductionmonthly.repository.HrMonthlyLunchDtlRepository;
import com.asg.hr.lunchdeductionmonthly.repository.HrMonthlyLunchHdrRepository;
import com.asg.hr.lunchdeductionmonthly.service.impl.HrLunchDeductionServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrLunchDeductionServiceImplTest {

    @Mock private HrMonthlyLunchHdrRepository hdrRepository;
    @Mock private HrMonthlyLunchDtlRepository dtlRepository;
    @Mock private HrLunchDeductionProcRepository procRepository;
    @Mock private HrLunchDeductionMapper mapper;
    @Mock private DocumentSearchService documentSearchService;
    @Mock private DocumentDeleteService documentDeleteService;
    @Mock private LoggingService loggingService;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private HrLunchDeductionServiceImpl service;

    private MockedStatic<UserContext> userContextMock;

    private static final Long TRANSACTION_POID = 1L;
    private static final Long USER_POID = 99L;
    private static final Long COMPANY_POID = 1L;
    private static final String DOC_ID = "800-115";
    private static final LocalDate PAYROLL_MONTH = LocalDate.of(2025, 9, 1);

    private HrMonthlyLunchHdr activeHdr;
    private HrLunchDeductionRequest createRequest;
    private HrLunchDeductionResponse mappedResponse;

    @BeforeEach
    void setUp() {
        userContextMock = mockStatic(UserContext.class);
        userContextMock.when(UserContext::getCompanyPoid).thenReturn(1L);
        userContextMock.when(UserContext::getUserPoid).thenReturn(USER_POID);
        userContextMock.when(UserContext::getDocumentId).thenReturn(DOC_ID);

        org.springframework.test.util.ReflectionTestUtils.setField(service, "entityManager", entityManager);

        activeHdr = HrMonthlyLunchHdr.builder()
                .transactionPoid(TRANSACTION_POID)
                .docRef("LDM-001")
                .companyPoid(1L)
                .payrollMonth(PAYROLL_MONTH)
                .description("Sep 2025")
                .deleted("N")
                .build();

        createRequest = HrLunchDeductionRequest.builder()
                .payrollMonth(PAYROLL_MONTH)
                .description("Sep 2025")
                .remarks("Test")
                .build();

        mappedResponse = HrLunchDeductionResponse.builder()
                .transactionPoid(TRANSACTION_POID)
                .docRef("LDM-001")
                .payrollMonth(PAYROLL_MONTH)
                .build();
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    @Nested
    class Create {

        @Test
        void success_withDetails_returnsResponse() {
            HrLunchDeductionDtlRequest dtlReq = HrLunchDeductionDtlRequest.builder()
                    .deductionType("LUNCH").leaveDays(2L).amount(new BigDecimal("500.00")).build();
            createRequest.setDetails(List.of(dtlReq));

            HrMonthlyLunchDtl dtlEntity = buildDtl(1L, 22L, 2L, new BigDecimal("0.500"));
            HrLunchDeductionDtlResponse dtlResponse = HrLunchDeductionDtlResponse.builder()
                    .detRowId(1L).transactionPoid(TRANSACTION_POID).build();

            when(hdrRepository.existsByPayrollMonthAndDeletedAndCompanyPoid(PAYROLL_MONTH, "N", COMPANY_POID))
                    .thenReturn(false);
            when(mapper.toEntity(createRequest)).thenReturn(activeHdr);
            when(hdrRepository.saveAndFlush(activeHdr)).thenReturn(activeHdr);
            doNothing().when(entityManager).refresh(activeHdr);
            when(mapper.toDtlEntityList(TRANSACTION_POID, List.of(dtlReq))).thenReturn(List.of(dtlEntity));
            when(mapper.toResponse(activeHdr)).thenReturn(mappedResponse);
            when(mapper.toDtlResponseList(List.of(dtlEntity))).thenReturn(List.of(dtlResponse));

            HrLunchDeductionResponse result = service.create(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getDetails()).hasSize(1);
            verify(dtlRepository).saveAll(List.of(dtlEntity));
            verify(loggingService).createLogSummaryEntry(anyString(), anyString(), anyString());
        }

        @Test
        void success_noDetails_returnsResponse() {
            when(hdrRepository.existsByPayrollMonthAndDeletedAndCompanyPoid(PAYROLL_MONTH, "N", COMPANY_POID))
                    .thenReturn(false);
            when(mapper.toEntity(createRequest)).thenReturn(activeHdr);
            when(hdrRepository.saveAndFlush(activeHdr)).thenReturn(activeHdr);
            doNothing().when(entityManager).refresh(activeHdr);
            when(mapper.toDtlEntityList(TRANSACTION_POID, List.of())).thenReturn(List.of());
            when(mapper.toResponse(activeHdr)).thenReturn(mappedResponse);
            when(mapper.toDtlResponseList(List.of())).thenReturn(List.of());

            HrLunchDeductionResponse result = service.create(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getDetails()).isEmpty();
            verify(dtlRepository, never()).saveAll(any());
        }

        @Test
        void duplicatePayrollMonth_throwsResourceAlreadyExistsException() {
            when(hdrRepository.existsByPayrollMonthAndDeletedAndCompanyPoid(PAYROLL_MONTH, "N", COMPANY_POID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.create(createRequest))
                    .isInstanceOf(ResourceAlreadyExistsException.class);

            verifyNoInteractions(mapper, entityManager);
            verify(hdrRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    class Update {

        @Test
        void success_withDetailsCREATED_returnsResponse() {
            HrLunchDeductionDtlRequest dtlReq = HrLunchDeductionDtlRequest.builder()
                    .actionType("isCreated")
                    .deductionType("LUNCH")
                    .leaveDays(2L)
                    .amount(new BigDecimal("500.00"))
                    .build();
            HrLunchDeductionRequest updateRequest = HrLunchDeductionRequest.builder()
                    .description("Updated")
                    .details(List.of(dtlReq))
                    .build();

            HrMonthlyLunchDtl newDtl = buildDtl(1L, 22L, 2L, new BigDecimal("0.500"));
            HrLunchDeductionDtlResponse dtlResponse = HrLunchDeductionDtlResponse.builder()
                    .detRowId(1L).transactionPoid(TRANSACTION_POID).build();

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(hdrRepository.saveAndFlush(activeHdr)).thenReturn(activeHdr);
            when(dtlRepository.findByTransactionPoid(TRANSACTION_POID)).thenReturn(List.of());
            when(mapper.toDtlEntity(TRANSACTION_POID, dtlReq, 1L)).thenReturn(newDtl);
            when(dtlRepository.saveAll(any())).thenReturn(List.of(newDtl));
            when(mapper.toResponse(activeHdr)).thenReturn(mappedResponse);
            when(mapper.toDtlResponseList(any())).thenReturn(List.of(dtlResponse));

            HrLunchDeductionResponse result = service.update(TRANSACTION_POID, updateRequest);

            assertThat(result).isNotNull();
            verify(mapper).updateEntity(activeHdr, updateRequest);
            verify(hdrRepository).saveAndFlush(activeHdr);
            verify(dtlRepository).saveAll(any());
        }

        @Test
        void success_withDetailsUPDATED_modifiesExistingDetail() {
            HrMonthlyLunchDtl existingDtl = buildDtl(1L, 22L, 2L, new BigDecimal("0.500"));
            HrLunchDeductionDtlRequest dtlReq = HrLunchDeductionDtlRequest.builder()
                    .detRowId(1L)
                    .actionType("isUpdated")
                    .leaveDays(5L)
                    .amount(new BigDecimal("750.00"))
                    .build();
            HrLunchDeductionRequest updateRequest = HrLunchDeductionRequest.builder()
                    .description("Updated")
                    .details(List.of(dtlReq))
                    .build();

            HrLunchDeductionDtlResponse dtlResponse = HrLunchDeductionDtlResponse.builder()
                    .detRowId(1L).transactionPoid(TRANSACTION_POID).build();

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(hdrRepository.saveAndFlush(activeHdr)).thenReturn(activeHdr);
            when(dtlRepository.findByTransactionPoid(TRANSACTION_POID)).thenReturn(List.of(existingDtl));
            when(dtlRepository.saveAll(any())).thenReturn(List.of(existingDtl));
            when(mapper.toResponse(activeHdr)).thenReturn(mappedResponse);
            when(mapper.toDtlResponseList(any())).thenReturn(List.of(dtlResponse));

            HrLunchDeductionResponse result = service.update(TRANSACTION_POID, updateRequest);

            assertThat(result).isNotNull();
            assertThat(existingDtl.getLunchDeductionAmt()).isEqualByComparingTo(new BigDecimal("750.00"));
        }

        @Test
        void success_withDetailsDELETED_removesDetail() {
            HrMonthlyLunchDtl existingDtl = buildDtl(1L, 22L, 2L, new BigDecimal("0.500"));
            HrLunchDeductionDtlRequest dtlReq = HrLunchDeductionDtlRequest.builder()
                    .detRowId(1L)
                    .actionType("isDeleted")
                    .build();
            HrLunchDeductionRequest updateRequest = HrLunchDeductionRequest.builder()
                    .description("Updated")
                    .details(List.of(dtlReq))
                    .build();

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(hdrRepository.saveAndFlush(activeHdr)).thenReturn(activeHdr);
            when(dtlRepository.findByTransactionPoid(TRANSACTION_POID)).thenReturn(List.of(existingDtl));
            when(mapper.toResponse(activeHdr)).thenReturn(mappedResponse);
            when(mapper.toDtlResponseList(any())).thenReturn(List.of());

            HrLunchDeductionResponse result = service.update(TRANSACTION_POID, updateRequest);

            assertThat(result).isNotNull();
            verify(dtlRepository).deleteAll(List.of(existingDtl));
        }

        @Test
        void autoDetectAction_noDetRowIdMeansCreated() {
            HrLunchDeductionDtlRequest dtlReq = HrLunchDeductionDtlRequest.builder()
                    .deductionType("LUNCH")
                    .leaveDays(2L)
                    .build();
            HrLunchDeductionRequest updateRequest = HrLunchDeductionRequest.builder()
                    .details(List.of(dtlReq))
                    .build();

            HrMonthlyLunchDtl newDtl = buildDtl(1L, 22L, 2L, new BigDecimal("0.500"));

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(hdrRepository.saveAndFlush(activeHdr)).thenReturn(activeHdr);
            when(dtlRepository.findByTransactionPoid(TRANSACTION_POID)).thenReturn(List.of());
            when(mapper.toDtlEntity(TRANSACTION_POID, dtlReq, 1L)).thenReturn(newDtl);
            when(dtlRepository.saveAll(any())).thenReturn(List.of(newDtl));
            when(mapper.toResponse(activeHdr)).thenReturn(mappedResponse);
            when(mapper.toDtlResponseList(any())).thenReturn(List.of());

            HrLunchDeductionResponse result = service.update(TRANSACTION_POID, updateRequest);

            assertThat(result).isNotNull();
            verify(dtlRepository).saveAll(any());
        }

        @Test
        void autoDetectAction_withDetRowIdMeansUpdated() {
            HrMonthlyLunchDtl existingDtl = buildDtl(1L, 22L, 2L, new BigDecimal("0.500"));
            HrLunchDeductionDtlRequest dtlReq = HrLunchDeductionDtlRequest.builder()
                    .detRowId(1L)
                    .leaveDays(3L)
                    .build();
            HrLunchDeductionRequest updateRequest = HrLunchDeductionRequest.builder()
                    .details(List.of(dtlReq))
                    .build();

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(hdrRepository.saveAndFlush(activeHdr)).thenReturn(activeHdr);
            when(dtlRepository.findByTransactionPoid(TRANSACTION_POID)).thenReturn(List.of(existingDtl));
            when(dtlRepository.saveAll(any())).thenReturn(List.of(existingDtl));
            when(mapper.toResponse(activeHdr)).thenReturn(mappedResponse);
            when(mapper.toDtlResponseList(any())).thenReturn(List.of());

            HrLunchDeductionResponse result = service.update(TRANSACTION_POID, updateRequest);

            assertThat(result).isNotNull();
            assertThat(existingDtl.getOffDays()).isEqualTo(3L);
        }

        @Test
        void updateWithInvalidDetRowId_throwsException() {
            HrLunchDeductionDtlRequest dtlReq = HrLunchDeductionDtlRequest.builder()
                    .detRowId(99L)
                    .actionType("isUpdated")
                    .build();
            HrLunchDeductionRequest updateRequest = HrLunchDeductionRequest.builder()
                    .details(List.of(dtlReq))
                    .build();

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(dtlRepository.findByTransactionPoid(TRANSACTION_POID)).thenReturn(List.of());

            assertThatThrownBy(() -> service.update(TRANSACTION_POID, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("detRowId");
        }

        @Test
        void hdrNotFound_throwsResourceNotFoundException() {
            HrLunchDeductionRequest updateRequest = HrLunchDeductionRequest.builder()
                    .description("Updated")
                    .build();

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(TRANSACTION_POID, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("transactionPoid");
        }
    }

    @Nested
    class GetById {

        @Test
        void success_returnsResponseWithDetails() {
            HrMonthlyLunchDtl dtl = buildDtl(1L, 22L, 2L, new BigDecimal("0.500"));
            HrLunchDeductionDtlResponse dtlResponse = HrLunchDeductionDtlResponse.builder()
                    .detRowId(1L).transactionPoid(TRANSACTION_POID).build();

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(dtlRepository.findByTransactionPoid(TRANSACTION_POID)).thenReturn(List.of(dtl));
            when(mapper.toResponse(activeHdr)).thenReturn(mappedResponse);
            when(mapper.toDtlResponseList(List.of(dtl))).thenReturn(List.of(dtlResponse));

            HrLunchDeductionResponse result = service.getById(TRANSACTION_POID);

            assertThat(result).isNotNull();
            assertThat(result.getDetails()).hasSize(1);
        }

        @Test
        void notFound_throwsResourceNotFoundException() {
            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(TRANSACTION_POID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("transactionPoid");
        }

        @Test
        void emptyDetails_returnsEmptyList() {
            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(dtlRepository.findByTransactionPoid(TRANSACTION_POID)).thenReturn(List.of());
            when(mapper.toResponse(activeHdr)).thenReturn(mappedResponse);
            when(mapper.toDtlResponseList(List.of())).thenReturn(List.of());

            HrLunchDeductionResponse result = service.getById(TRANSACTION_POID);

            assertThat(result.getDetails()).isEmpty();
        }
    }

    @Nested
    class LoadAndProcess {

        @Test
        void success_callsProcAndReturnsDetails() {
            HrMonthlyLunchDtl dtl = buildDtl(1L, 22L, 2L, new BigDecimal("0.500"));
            HrLunchDeductionDtlResponse dtlResponse = HrLunchDeductionDtlResponse.builder().detRowId(1L).build();

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(procRepository.loadLunchDetails(TRANSACTION_POID, USER_POID, PAYROLL_MONTH)).thenReturn("SUCCESS");
            when(dtlRepository.findByTransactionPoid(TRANSACTION_POID)).thenReturn(List.of(dtl));
            when(mapper.toDtlResponseList(List.of(dtl))).thenReturn(List.of(dtlResponse));

            HrLunchDeductionLoadDto result = service.loadAndProcess(TRANSACTION_POID);

            assertThat(result).isNotNull();
            assertThat(result.getLunchDetails()).hasSize(1);
            verify(procRepository).loadLunchDetails(TRANSACTION_POID, USER_POID, PAYROLL_MONTH);
        }

        @Test
        void notFound_throwsResourceNotFoundException() {
            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadAndProcess(TRANSACTION_POID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(procRepository);
        }
    }

    @Nested
    class ListRecords {

        @Test
        void success_returnsPaginatedResult() {
            FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", List.of());
            LocalDate startDate = LocalDate.of(2025, 9, 1);
            LocalDate endDate = LocalDate.of(2025, 9, 30);
            Pageable pageable = PageRequest.of(0, 10);
            RawSearchResult rawResult = new RawSearchResult(
                    List.of(Map.of("DOC_REF", "LDM-001")),
                    Map.of("DOC_REF", "Doc Reference"),
                    1L
            );

            when(documentSearchService.resolveOperator(filterRequest)).thenReturn("AND");
            when(documentSearchService.resolveIsDeleted(filterRequest)).thenReturn("N");
            when(documentSearchService.resolveDateFilters(filterRequest, "PAYROLL_MONTH", startDate, endDate)).thenReturn(List.of());
            when(documentSearchService.search(eq(DOC_ID), any(), eq("AND"), eq(pageable),
                    eq("N"), eq("DESCRIPTION"), eq("TRANSACTION_POID")))
                    .thenReturn(rawResult);

            Map<String, Object> result = service.list(filterRequest, startDate, endDate, pageable);

            assertThat(result).isNotNull();
            verify(documentSearchService).search(eq(DOC_ID), any(), eq("AND"), eq(pageable),
                    eq("N"), eq("DESCRIPTION"), eq("TRANSACTION_POID"));
        }

        @Test
        void nullFilterRequest_stillDelegatesToDocumentSearch() {
            LocalDate startDate = LocalDate.of(2025, 9, 1);
            LocalDate endDate = LocalDate.of(2025, 9, 30);
            Pageable pageable = PageRequest.of(0, 10);
            RawSearchResult rawResult = new RawSearchResult(List.of(), Map.of(), 0L);

            when(documentSearchService.resolveOperator(null)).thenReturn("AND");
            when(documentSearchService.resolveIsDeleted(null)).thenReturn("N");
            when(documentSearchService.resolveDateFilters(null, "PAYROLL_MONTH", startDate, endDate)).thenReturn(List.of());
            when(documentSearchService.search(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(rawResult);

            Map<String, Object> result = service.list(null, startDate, endDate, pageable);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    class Delete {

        @Test
        void success_callsDeleteDocumentAndLogsEntry() {
            DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));

            service.delete(TRANSACTION_POID, deleteReasonDto);

            verify(documentDeleteService).deleteDocument(
                    eq(TRANSACTION_POID), eq("HR_MONTHLY_LUNCH_HDR"),
                    eq("TRANSACTION_POID"), eq(deleteReasonDto), isNull());
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.DELETED, DOC_ID, "1");
        }

        @Test
        void notFound_throwsResourceNotFoundException() {
            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(TRANSACTION_POID, new DeleteReasonDto()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("transactionPoid");

            verifyNoInteractions(documentDeleteService);
        }
    }

    private HrMonthlyLunchDtl buildDtl(Long detRowId, Long monthDays, Long offDays, BigDecimal costPerDay) {
        long totalDays = monthDays - offDays;
        return HrMonthlyLunchDtl.builder()
                .detRowId(detRowId)
                .transactionPoid(TRANSACTION_POID)
                .employeePoid(10L)
                .deductionType("DEDUCT")
                .lunchDays(20L)
                .monthDays(monthDays)
                .offDays(offDays)
                .totalDays(totalDays)
                .costPerDay(costPerDay)
                .lunchDeductionAmt(costPerDay.multiply(BigDecimal.valueOf(totalDays)))
                .build();
    }
}
