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
import com.asg.hr.lunchdeductionmonthly.entity.key.HrMonthlyLunchDtlKey;
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
    private HrMonthlyLunchHdr finalizedHdr;
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

        finalizedHdr = HrMonthlyLunchHdr.builder()
                .transactionPoid(TRANSACTION_POID)
                .payrollMonth(PAYROLL_MONTH)
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

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------
    @Nested
    class Create {

        @Test
        void success_returnsResponse() {
            when(hdrRepository.existsByPayrollMonthAndDeletedAndCompanyPoid(PAYROLL_MONTH, "N", COMPANY_POID))
                    .thenReturn(false);
            when(mapper.toEntity(createRequest)).thenReturn(activeHdr);
            when(hdrRepository.saveAndFlush(activeHdr)).thenReturn(activeHdr);
            doNothing().when(entityManager).refresh(activeHdr);
            when(mapper.toResponse(activeHdr)).thenReturn(mappedResponse);

            HrLunchDeductionResponse result = service.create(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getDocRef()).isEqualTo("LDM-001");
            assertThat(result.getTransactionPoid()).isEqualTo(TRANSACTION_POID);
            verify(hdrRepository).saveAndFlush(activeHdr);
            verify(entityManager).refresh(activeHdr);
            verify(loggingService).createLogSummaryEntry(eq(DOC_ID), eq("1"), anyString());
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

    // -------------------------------------------------------------------------
    // update()
    // -------------------------------------------------------------------------
    @Nested
    class Update {

        private HrLunchDeductionUpdateRequest updateRequest;

        @BeforeEach
        void setUp() {
            updateRequest = HrLunchDeductionUpdateRequest.builder()
                    .description("Updated desc")
                    .remarks("Updated remarks")
                    .build();
        }

        @Test
        void success_returnsUpdatedResponse() {
            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(hdrRepository.saveAndFlush(activeHdr)).thenReturn(activeHdr);
            when(mapper.toResponse(activeHdr)).thenReturn(mappedResponse);

            HrLunchDeductionResponse result = service.update(TRANSACTION_POID, updateRequest);

            assertThat(result).isNotNull();
            verify(mapper).updateEntity(activeHdr, updateRequest);
            verify(hdrRepository).saveAndFlush(activeHdr);
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.MODIFIED, DOC_ID, "1");
        }

        @Test
        void notFound_throwsResourceNotFoundException() {
            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(TRANSACTION_POID, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("transactionPoid");
        }
    }

    // -------------------------------------------------------------------------
    // getById()
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // loadAndProcess()
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // updateDetail()
    // -------------------------------------------------------------------------
    @Nested
    class UpdateDetail {

        private HrMonthlyLunchDtl existingDtl;
        private HrMonthlyLunchDtlKey dtlKey;

        @BeforeEach
        void setUp() {
            dtlKey = new HrMonthlyLunchDtlKey(1L, TRANSACTION_POID);
            existingDtl = buildDtl(1L, 22L, 2L, new BigDecimal("0.500"));
        }

        @Test
        void updateLeaveDays_recalculatesTotalDaysAndAmount() {
            // monthDays=22, new leaveDays=5 -> totalDays=17, amount=17*0.500=8.500
            HrLunchDeductionDtlRequest request = HrLunchDeductionDtlRequest.builder()
                    .detRowId(1L).leaveDays(5L).build();

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(dtlRepository.findById(dtlKey)).thenReturn(Optional.of(existingDtl));

            service.updateDetail(TRANSACTION_POID, request);

            assertThat(existingDtl.getOffDays()).isEqualTo(5L);
            assertThat(existingDtl.getTotalDays()).isEqualTo(17L);
            assertThat(existingDtl.getLunchDeductionAmt()).isEqualByComparingTo(new BigDecimal("8.500"));
            verify(dtlRepository).save(existingDtl);
            verify(loggingService).createLogSummaryEntry(eq(DOC_ID), eq("1"), anyString());
        }

        @Test
        void updateLeaveDays_whenMonthDaysNull_skipsRecalculation() {
            existingDtl.setMonthDays(null);
            HrLunchDeductionDtlRequest request = HrLunchDeductionDtlRequest.builder()
                    .detRowId(1L).leaveDays(3L).build();

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(dtlRepository.findById(dtlKey)).thenReturn(Optional.of(existingDtl));

            service.updateDetail(TRANSACTION_POID, request);

            assertThat(existingDtl.getOffDays()).isEqualTo(3L);
            assertThat(existingDtl.getTotalDays()).isEqualTo(20L);
            verify(dtlRepository).save(existingDtl);
            verify(loggingService).createLogSummaryEntry(eq(DOC_ID), eq("1"), anyString());
        }

        @Test
        void updateLeaveDays_whenCostPerDayNull_skipsAmountRecalculation() {
            existingDtl.setCostPerDay(null);
            existingDtl.setLunchDeductionAmt(null);
            HrLunchDeductionDtlRequest request = HrLunchDeductionDtlRequest.builder()
                    .detRowId(1L).leaveDays(2L).build();

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(dtlRepository.findById(dtlKey)).thenReturn(Optional.of(existingDtl));

            service.updateDetail(TRANSACTION_POID, request);

            assertThat(existingDtl.getTotalDays()).isEqualTo(20L);
            assertThat(existingDtl.getLunchDeductionAmt()).isNull();
            verify(dtlRepository).save(existingDtl);
            verify(loggingService).createLogSummaryEntry(eq(DOC_ID), eq("1"), anyString());
        }

        @Test
        void updateDeductionType_onlyUpdatesType() {
            HrLunchDeductionDtlRequest request = HrLunchDeductionDtlRequest.builder()
                    .detRowId(1L).deductionType("FREE").build();

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(dtlRepository.findById(dtlKey)).thenReturn(Optional.of(existingDtl));

            service.updateDetail(TRANSACTION_POID, request);

            assertThat(existingDtl.getDeductionType()).isEqualTo("FREE");
            verify(dtlRepository).save(existingDtl);
            verify(loggingService).createLogSummaryEntry(eq(DOC_ID), eq("1"), anyString());
        }

        @Test
        void updateAmount_overridesAmount() {
            HrLunchDeductionDtlRequest request = HrLunchDeductionDtlRequest.builder()
                    .detRowId(1L).amount(new BigDecimal("9.999")).build();

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(dtlRepository.findById(dtlKey)).thenReturn(Optional.of(existingDtl));

            service.updateDetail(TRANSACTION_POID, request);

            assertThat(existingDtl.getLunchDeductionAmt()).isEqualByComparingTo(new BigDecimal("9.999"));
            verify(dtlRepository).save(existingDtl);
            verify(loggingService).createLogSummaryEntry(eq(DOC_ID), eq("1"), anyString());
        }

        @Test
        void updateRemarks_onlyUpdatesRemarks() {
            HrLunchDeductionDtlRequest request = HrLunchDeductionDtlRequest.builder()
                    .detRowId(1L).remarks("Special case").build();

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(dtlRepository.findById(dtlKey)).thenReturn(Optional.of(existingDtl));

            service.updateDetail(TRANSACTION_POID, request);

            assertThat(existingDtl.getRemarks()).isEqualTo("Special case");
            verify(dtlRepository).save(existingDtl);
            verify(loggingService).createLogSummaryEntry(eq(DOC_ID), eq("1"), anyString());
        }

        @Test
        void nullLeaveDays_doesNotModifyExistingValues() {
            BigDecimal originalAmount = existingDtl.getLunchDeductionAmt();
            Long originalTotalDays = existingDtl.getTotalDays();

            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(dtlRepository.findById(dtlKey)).thenReturn(Optional.of(existingDtl));

            service.updateDetail(TRANSACTION_POID,
                    HrLunchDeductionDtlRequest.builder().detRowId(1L).leaveDays(null).build());

            assertThat(existingDtl.getTotalDays()).isEqualTo(originalTotalDays);
            assertThat(existingDtl.getLunchDeductionAmt()).isEqualByComparingTo(originalAmount);
            verify(loggingService).createLogSummaryEntry(eq(DOC_ID), eq("1"), anyString());
        }

        @Test
        void hdrNotFound_throwsResourceNotFoundException() {
            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateDetail(TRANSACTION_POID,
                    HrLunchDeductionDtlRequest.builder().detRowId(1L).build()))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(dtlRepository);
        }

        @Test
        void dtlNotFound_throwsResourceNotFoundException() {
            when(hdrRepository.findById(TRANSACTION_POID)).thenReturn(Optional.of(activeHdr));
            when(dtlRepository.findById(dtlKey)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateDetail(TRANSACTION_POID,
                    HrLunchDeductionDtlRequest.builder().detRowId(1L).build()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("detRowId");
        }
    }

    // -------------------------------------------------------------------------
    // list()
    // -------------------------------------------------------------------------
    @Nested
    class ListRecords {

        @Test
        void success_returnsPaginatedResult() {
            FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", List.of());
            Pageable pageable = PageRequest.of(0, 10);
            RawSearchResult rawResult = new RawSearchResult(
                    List.of(Map.of("DOC_REF", "LDM-001")),
                    Map.of("DOC_REF", "Doc Reference"),
                    1L
            );

            when(documentSearchService.resolveOperator(filterRequest)).thenReturn("AND");
            when(documentSearchService.resolveIsDeleted(filterRequest)).thenReturn("N");
            when(documentSearchService.resolveFilters(filterRequest)).thenReturn(List.of());
            when(documentSearchService.search(eq(DOC_ID), any(), eq("AND"), eq(pageable),
                    eq("N"), eq("DESCRIPTION"), eq("TRANSACTION_POID")))
                    .thenReturn(rawResult);

            Map<String, Object> result = service.list(filterRequest, pageable);

            assertThat(result).isNotNull();
            verify(documentSearchService).search(eq(DOC_ID), any(), eq("AND"), eq(pageable),
                    eq("N"), eq("DESCRIPTION"), eq("TRANSACTION_POID"));
        }

        @Test
        void nullFilterRequest_stillDelegatesToDocumentSearch() {
            Pageable pageable = PageRequest.of(0, 10);
            RawSearchResult rawResult = new RawSearchResult(List.of(), Map.of(), 0L);

            when(documentSearchService.resolveOperator(null)).thenReturn("AND");
            when(documentSearchService.resolveIsDeleted(null)).thenReturn("N");
            when(documentSearchService.resolveFilters(null)).thenReturn(List.of());
            when(documentSearchService.search(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(rawResult);

            Map<String, Object> result = service.list(null, pageable);

            assertThat(result).isNotNull();
        }
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------
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
