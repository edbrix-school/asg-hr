package com.asg.hr.holidaymaster.util;

import com.asg.hr.holidaymaster.dto.HolidayMasterRequest;
import com.asg.hr.holidaymaster.dto.HolidayMasterResponse;
import com.asg.hr.holidaymaster.entity.HolidayMasterEntity;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HolidayMasterMapper {

    public HolidayMasterResponse toResponse(HolidayMasterEntity entity) {
        if (entity == null) {
            return null;
        }

        HolidayMasterResponse response = new HolidayMasterResponse();
        response.setHolidayPoid(entity.getHolidayPoid());
        response.setHolidayDate(entity.getHolidayDate());
        response.setHolidayReason(entity.getHolidayReason());
        response.setStatus(entity.getStatus());
        response.setActive(entity.getActive());
        response.setDeleted(entity.getDeleted());
        response.setSeqNo(entity.getSeqno());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedDate(entity.getCreatedDate());
        response.setModifiedBy(entity.getLastModifiedBy());
        response.setModifiedDate(entity.getLastModifiedDate());
        return response;
    }

    public List<HolidayMasterResponse> toResponseList(List<HolidayMasterEntity> entities) {
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public HolidayMasterEntity toEntity(HolidayMasterRequest request, String userId) {
        if (request == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        HolidayMasterEntity entity = new HolidayMasterEntity();
        entity.setHolidayDate(request.getHolidayDate());
        entity.setHolidayReason(request.getHolidayReason() != null ? request.getHolidayReason().trim() : null);
        entity.setSeqno(request.getSeqNo() != null ? BigInteger.valueOf(request.getSeqNo()) : null);

        String activeValue = request.getActive() != null ? request.getActive() : "Y";
        entity.setActive(
                activeValue.equalsIgnoreCase("true")
                        || activeValue.equalsIgnoreCase("Y")
                        ? "Y"
                        : "N"
        );
        entity.setDeleted("N");
        entity.setStatus("O");

        entity.setCreatedBy(userId);
        entity.setCreatedDate(now);
        entity.setLastModifiedBy(userId);
        entity.setLastModifiedDate(now);

        return entity;
    }

    public void updateEntity(HolidayMasterEntity entity, HolidayMasterRequest request, String userId) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getHolidayDate() != null) {
            entity.setHolidayDate(request.getHolidayDate());
        }
        if (request.getHolidayReason() != null) {
            entity.setHolidayReason(request.getHolidayReason().trim());
        }
        if (request.getSeqNo() != null) {
            entity.setSeqno(BigInteger.valueOf(request.getSeqNo()));
        }
        if (request.getActive() != null) {
            String activeValue = request.getActive();
            entity.setActive(
                    activeValue.equalsIgnoreCase("true")
                            || activeValue.equalsIgnoreCase("Y")
                            ? "Y"
                            : "N"
            );
        }
        entity.setLastModifiedBy(userId);
        entity.setLastModifiedDate(LocalDateTime.now());
    }
}

