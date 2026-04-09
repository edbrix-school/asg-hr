package com.asg.hr.lunchdeduction.util;

import com.asg.hr.lunchdeduction.dto.LunchDeductionDetailRequestDto;
import com.asg.hr.lunchdeduction.dto.LunchDeductionDetailResponseDto;
import com.asg.hr.lunchdeduction.dto.LunchDeductionMonthlyRequestDto;
import com.asg.hr.lunchdeduction.dto.LunchDeductionMonthlyResponseDto;
import com.asg.hr.lunchdeduction.entity.LunchDeductionMonthlyDetail;
import com.asg.hr.lunchdeduction.entity.LunchDeductionMonthlyDetailId;
import com.asg.hr.lunchdeduction.entity.LunchDeductionMonthlyHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LunchDeductionMonthlyMapperTest {

    private LunchDeductionMonthlyMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LunchDeductionMonthlyMapper();
    }

    @Test
    void toEntity_ShouldMapHeaderFields() {
        LunchDeductionMonthlyRequestDto requestDto = LunchDeductionMonthlyRequestDto.builder()
                .transactionDate(LocalDate.of(2030, 1, 1))
                .payrollMonth(LocalDate.of(2030, 1, 1))
                .lunchDescription("January Lunch")
                .remarks("Header remarks")
                .build();

        LunchDeductionMonthlyHeader header = mapper.toEntity(requestDto, 10L, 20L);

        assertEquals(10L, header.getGroupPoid());
        assertEquals(20L, header.getCompanyPoid());
        assertEquals(LocalDate.of(2030, 1, 1), header.getTransactionDate());
        assertEquals("N", header.getDeleted());
    }

    @Test
    void recalculateDetail_ShouldComputeTotals() {
        LunchDeductionMonthlyDetail detail = LunchDeductionMonthlyDetail.builder()
                .id(LunchDeductionMonthlyDetailId.builder().transactionPoid(1L).detRowId(1L).build())
                .monthDays(22L)
                .offDays(2L)
                .costPerDay(BigDecimal.valueOf(5))
                .build();

        mapper.recalculateDetail(detail);

        assertEquals(20L, detail.getTotalDays());
        assertEquals(BigDecimal.valueOf(100), detail.getLunchDeductionAmt());
    }

    @Test
    void recalculateDetail_WhenMonthDaysMissing_ShouldClearDerivedFields() {
        LunchDeductionMonthlyDetail detail = LunchDeductionMonthlyDetail.builder()
                .id(LunchDeductionMonthlyDetailId.builder().transactionPoid(1L).detRowId(1L).build())
                .costPerDay(BigDecimal.TEN)
                .build();

        mapper.recalculateDetail(detail);

        assertNull(detail.getTotalDays());
        assertNull(detail.getLunchDeductionAmt());
    }

    @Test
    void toResponse_ShouldAggregateDetailTotals() {
        LunchDeductionMonthlyHeader header = LunchDeductionMonthlyHeader.builder()
                .transactionPoid(1L)
                .groupPoid(10L)
                .companyPoid(20L)
                .transactionDate(LocalDate.of(2030, 1, 1))
                .payrollMonth(LocalDate.of(2030, 1, 1))
                .lunchDescription("January Lunch")
                .details(List.of(
                        LunchDeductionMonthlyDetail.builder()
                                .id(LunchDeductionMonthlyDetailId.builder().transactionPoid(1L).detRowId(2L).build())
                                .lunchDays(3L)
                                .lunchDeductionAmt(BigDecimal.valueOf(15))
                                .build(),
                        LunchDeductionMonthlyDetail.builder()
                                .id(LunchDeductionMonthlyDetailId.builder().transactionPoid(1L).detRowId(1L).build())
                                .lunchDays(2L)
                                .lunchDeductionAmt(BigDecimal.valueOf(10))
                                .build()
                ))
                .build();

        LunchDeductionMonthlyResponseDto responseDto = mapper.toResponse(header);

        assertEquals(2L, responseDto.getDetailCount());
        assertEquals(5L, responseDto.getTotalLunchDays());
        assertEquals(BigDecimal.valueOf(25), responseDto.getTotalDeductionAmount());
        assertNotNull(responseDto.getDetails());
        LunchDeductionDetailResponseDto firstDetail = responseDto.getDetails().getFirst();
        assertEquals(1L, firstDetail.getDetRowId());
    }

    @Test
    void updateDetail_ShouldOverwriteMutableFields() {
        LunchDeductionMonthlyDetail detail = LunchDeductionMonthlyDetail.builder()
                .id(LunchDeductionMonthlyDetailId.builder().transactionPoid(1L).detRowId(1L).build())
                .monthDays(22L)
                .offDays(1L)
                .costPerDay(BigDecimal.valueOf(4))
                .remarks("old")
                .build();

        LunchDeductionDetailRequestDto requestDto = LunchDeductionDetailRequestDto.builder()
                .monthDays(20L)
                .offDays(2L)
                .costPerDay(BigDecimal.valueOf(5))
                .remarks("new")
                .build();

        mapper.updateDetail(detail, requestDto);

        assertEquals(18L, detail.getTotalDays());
        assertEquals(BigDecimal.valueOf(90), detail.getLunchDeductionAmt());
        assertEquals("new", detail.getRemarks());
    }
}