package com.asg.hr.holidaymaster.util;

import com.asg.hr.holidaymaster.dto.HolidayMasterRequest;
import com.asg.hr.holidaymaster.dto.HolidayMasterResponse;
import com.asg.hr.holidaymaster.entity.HolidayMasterEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HolidayMasterMapperTest {

    private HolidayMasterMapper mapper;
    private HolidayMasterEntity entity;

    @BeforeEach
    void setUp() {
        mapper = new HolidayMasterMapper();
        entity = HolidayMasterEntity.builder()
                .holidayPoid(10L)
                .holidayDate(LocalDate.of(2030, 1, 1))
                .holidayReason("New Year")
                .status("O")
                .active("Y")
                .deleted("N")
                .seqno(BigInteger.TEN)
                .createdBy("creator")
                .createdDate(LocalDateTime.of(2026, 1, 1, 0, 0))
                .lastModifiedBy("modifier")
                .lastModifiedDate(LocalDateTime.of(2026, 1, 2, 0, 0))
                .build();
    }

    @Test
    void toResponse_ReturnsMappedResponse() {
        HolidayMasterResponse response = mapper.toResponse(entity);

        assertNotNull(response);
        assertEquals(entity.getHolidayPoid(), response.getHolidayPoid());
        assertEquals(entity.getHolidayDate(), response.getHolidayDate());
        assertEquals(entity.getHolidayReason(), response.getHolidayReason());
        assertEquals(entity.getStatus(), response.getStatus());
        assertEquals(entity.getActive(), response.getActive());
        assertEquals(entity.getDeleted(), response.getDeleted());
        assertEquals(entity.getSeqno(), response.getSeqNo());
        assertEquals(entity.getCreatedBy(), response.getCreatedBy());
        assertEquals(entity.getCreatedDate(), response.getCreatedDate());
        assertEquals(entity.getLastModifiedBy(), response.getModifiedBy());
        assertEquals(entity.getLastModifiedDate(), response.getModifiedDate());
    }

    @Test
    void toResponse_WhenEntityNull_ReturnsNull() {
        assertNull(mapper.toResponse(null));
    }

    @Test
    void toResponseList_ReturnsMappedList() {
        List<HolidayMasterResponse> responseList = mapper.toResponseList(List.of(entity));

        assertEquals(1, responseList.size());
        assertEquals(10L, responseList.getFirst().getHolidayPoid());
    }

    @Test
    void toEntity_WithNullRequest_ReturnsNull() {
        assertNull(mapper.toEntity(null, "tester"));
    }

    @Test
    void toEntity_WithBooleanLikeActive_NormalizesAndTrims() {
        HolidayMasterRequest request = new HolidayMasterRequest();
        request.setHolidayDate(LocalDate.of(2030, 12, 25));
        request.setHolidayReason("  Christmas  ");
        request.setSeqNo(7);
        request.setActive("true");

        HolidayMasterEntity mapped = mapper.toEntity(request, "tester");

        assertNotNull(mapped);
        assertEquals("Christmas", mapped.getHolidayReason());
        assertEquals(BigInteger.valueOf(7), mapped.getSeqno());
        assertEquals("Y", mapped.getActive());
        assertEquals("N", mapped.getDeleted());
        assertEquals("O", mapped.getStatus());
        assertEquals("tester", mapped.getCreatedBy());
        assertEquals("tester", mapped.getLastModifiedBy());
        assertNotNull(mapped.getCreatedDate());
        assertNotNull(mapped.getLastModifiedDate());
    }

    @Test
    void toEntity_WithNullActive_DefaultsToY() {
        HolidayMasterRequest request = new HolidayMasterRequest();
        request.setHolidayDate(LocalDate.of(2030, 5, 1));
        request.setHolidayReason("Labor Day");
        request.setActive(null);

        HolidayMasterEntity mapped = mapper.toEntity(request, "tester");

        assertEquals("Y", mapped.getActive());
    }

    @Test
    void updateEntity_UpdatesProvidedFieldsOnly() {
        HolidayMasterRequest updateRequest = new HolidayMasterRequest();
        updateRequest.setHolidayDate(LocalDate.of(2031, 1, 1));
        updateRequest.setHolidayReason("  Updated Reason ");
        updateRequest.setSeqNo(20);
        updateRequest.setActive("N");

        mapper.updateEntity(entity, updateRequest, "updater");

        assertEquals(LocalDate.of(2031, 1, 1), entity.getHolidayDate());
        assertEquals("Updated Reason", entity.getHolidayReason());
        assertEquals(BigInteger.valueOf(20), entity.getSeqno());
        assertEquals("N", entity.getActive());
        assertEquals("updater", entity.getLastModifiedBy());
        assertNotNull(entity.getLastModifiedDate());
    }

    @Test
    void updateEntity_WhenEntityOrRequestNull_DoesNothing() {
        mapper.updateEntity(null, new HolidayMasterRequest(), "user");
        mapper.updateEntity(entity, null, "user");
        assertEquals(10L, entity.getHolidayPoid());
    }
}
