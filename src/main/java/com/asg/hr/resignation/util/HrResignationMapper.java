package com.asg.hr.resignation.util;

import com.asg.hr.resignation.dto.HrResignationRequest;
import com.asg.hr.resignation.dto.HrResignationResponse;
import com.asg.hr.resignation.entity.HrResignationEntity;
import org.springframework.stereotype.Component;

@Component
public class HrResignationMapper {

    public HrResignationResponse toResponse(HrResignationEntity entity) {
        if (entity == null) {
            return null;
        }

        HrResignationResponse response = new HrResignationResponse();
        response.setTransactionPoid(entity.getTransactionPoid());
        response.setDocRef(entity.getDocRef());
        response.setTransactionDate(entity.getTransactionDate());

        response.setEmployeePoid(entity.getEmployeePoid());
        response.setDepartmentPoid(entity.getDepartmentPoid());
        response.setDesignationPoid(entity.getDesignationPoid());
        response.setDirectSupervisorPoid(entity.getDirectSupervisorPoid());

        response.setLastDateOfWork(entity.getLastDateOfWork());
        response.setResignationDetails(entity.getResignationDetails());

        response.setJoinDate(entity.getJoinDate());
        response.setRpExpiryDate(entity.getRpExpiryDate());

        response.setResignationType(entity.getResignationType());
        response.setHodRemarks(entity.getHodRemarks());
        response.setRemarks(entity.getRemarks());

        response.setDeleted(entity.getDeleted());

        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedDate(entity.getCreatedDate());
        response.setModifiedBy(entity.getLastModifiedBy());
        response.setModifiedDate(entity.getLastModifiedDate());

        return response;
    }

    public HrResignationEntity toEntity(HrResignationRequest request) {
        if (request == null) {
            return null;
        }

        HrResignationEntity entity = new HrResignationEntity();
        entity.setEmployeePoid(request.getEmployeePoid());
        entity.setLastDateOfWork(request.getLastDateOfWork());
        entity.setResignationDetails(trimToNull(request.getResignationDetails()));

        // service handles defaulting/validation of resignationType
        entity.setResignationType(trimToNull(request.getResignationType()));

        entity.setHodRemarks(trimToNull(request.getHodRemarks()));
        entity.setRemarks(trimToNull(request.getRemarks()));

        entity.setDeleted("N");
        return entity;
    }

    public void updateEntity(HrResignationEntity entity, HrResignationRequest request) {
        if (entity == null || request == null) {
            return;
        }

        entity.setEmployeePoid(request.getEmployeePoid());
        entity.setLastDateOfWork(request.getLastDateOfWork());
        entity.setResignationDetails(trimToNull(request.getResignationDetails()));
        entity.setResignationType(trimToNull(request.getResignationType()));
        entity.setHodRemarks(trimToNull(request.getHodRemarks()));
        entity.setRemarks(trimToNull(request.getRemarks()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

