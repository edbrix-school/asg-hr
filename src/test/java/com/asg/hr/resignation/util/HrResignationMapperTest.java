package com.asg.hr.resignation.util;

import com.asg.hr.resignation.dto.HrResignationRequest;
import com.asg.hr.resignation.dto.HrResignationResponse;
import com.asg.hr.resignation.entity.HrResignationEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class HrResignationMapperTest {

    private final HrResignationMapper mapper = new HrResignationMapper();

    @Test
    void toResponse_MapsAllExpectedFields() {
        HrResignationEntity entity = new HrResignationEntity();
        entity.setTransactionPoid(10L);
        entity.setDocRef("DOC-1");
        entity.setTransactionDate(LocalDate.now());
        entity.setEmployeePoid(100L);
        entity.setDepartmentPoid(200L);
        entity.setDesignationPoid(300L);
        entity.setDirectSupervisorPoid(400L);
        entity.setLastDateOfWork(LocalDate.now().plusDays(10));
        entity.setResignationDetails("Leaving");
        entity.setJoinDate(LocalDate.now().minusYears(1));
        entity.setRpExpiryDate(LocalDate.now().plusDays(20));
        entity.setResignationType("VOLUNTARY");
        entity.setHodRemarks("ok");
        entity.setRemarks("note");
        entity.setDeleted("N");
        entity.setCreatedBy("u1");
        entity.setCreatedDate(LocalDateTime.now().minusDays(1));
        entity.setLastModifiedBy("u2");
        entity.setLastModifiedDate(LocalDateTime.now());

        HrResignationResponse response = mapper.toResponse(entity);
        assertEquals(10L, response.getTransactionPoid());
        assertEquals("DOC-1", response.getDocRef());
        assertEquals("u1", response.getCreatedBy());
        assertEquals("u2", response.getModifiedBy());
        assertEquals("Leaving", response.getResignationDetails());
    }

    @Test
    void toEntity_AndUpdateEntity_TrimValues() {
        HrResignationRequest request = new HrResignationRequest();
        request.setEmployeePoid(10L);
        request.setLastDateOfWork(LocalDate.now().plusDays(2));
        request.setResignationDetails("  details  ");
        request.setResignationType("  voluntary ");
        request.setHodRemarks("  hod ");
        request.setRemarks("   ");

        HrResignationEntity entity = mapper.toEntity(request);
        assertEquals("details", entity.getResignationDetails());
        assertEquals("voluntary", entity.getResignationType());
        assertEquals("hod", entity.getHodRemarks());
        assertNull(entity.getRemarks());
        assertEquals("N", entity.getDeleted());

        HrResignationRequest update = new HrResignationRequest();
        update.setEmployeePoid(11L);
        update.setLastDateOfWork(LocalDate.now().plusDays(5));
        update.setResignationDetails("  new ");
        update.setResignationType(" INVOLUNTARY ");
        update.setHodRemarks("  h2 ");
        update.setRemarks("  r2 ");

        mapper.updateEntity(entity, update);
        assertEquals(11L, entity.getEmployeePoid());
        assertEquals("new", entity.getResignationDetails());
        assertEquals("INVOLUNTARY", entity.getResignationType());
        assertEquals("h2", entity.getHodRemarks());
        assertEquals("r2", entity.getRemarks());
    }

    @Test
    void nullSafety_ReturnsNullOrNoThrow() {
        assertNull(mapper.toResponse(null));
        assertNull(mapper.toEntity(null));
        mapper.updateEntity(null, new HrResignationRequest());
        mapper.updateEntity(new HrResignationEntity(), null);
    }
}
