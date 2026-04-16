package com.asg.hr.employeetraining.util;

import com.asg.hr.employeetraining.dto.EmployeeTrainingDetailRequest;
import com.asg.hr.employeetraining.dto.EmployeeTrainingDetailResponse;
import com.asg.hr.employeetraining.dto.EmployeeTrainingRequest;
import com.asg.hr.employeetraining.dto.EmployeeTrainingResponse;
import com.asg.hr.employeetraining.entity.EmployeeTrainingDetailEntity;
import com.asg.hr.employeetraining.entity.EmployeeTrainingDetailId;
import com.asg.hr.employeetraining.entity.EmployeeTrainingHeaderEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class EmployeeTrainingMapper {

    public EmployeeTrainingHeaderEntity toHeaderEntity(EmployeeTrainingRequest request, Long companyPoid, Long groupPoid) {
        return EmployeeTrainingHeaderEntity.builder()
                .courseName(trim(request.getCourseName()))
                .periodFrom(request.getPeriodFrom())
                .periodTo(request.getPeriodTo())
                .durationDays(request.getDurationDays())
                .trainingType(trim(request.getTrainingType()))
                .institution(trim(request.getInstitution()))
                .trainingCost(request.getTrainingCost())
                .trainingLocation(trim(request.getTrainingLocation()))
                .remarks(trim(request.getRemarks()))
                .transactionDate(request.getTransactionDate())
                .employeePoid(trim(request.getEmployeePoid()))
                .companyPoid(companyPoid)
                .groupPoid(groupPoid)
                .deleted(EmployeeTrainingConstants.DELETED_NO)
                .build();
    }

    public void updateHeaderEntity(EmployeeTrainingHeaderEntity entity, EmployeeTrainingRequest request, Long companyPoid, Long groupPoid) {
        entity.setCourseName(trim(request.getCourseName()));
        entity.setPeriodFrom(request.getPeriodFrom());
        entity.setPeriodTo(request.getPeriodTo());
        entity.setDurationDays(request.getDurationDays());
        entity.setTrainingType(trim(request.getTrainingType()));
        entity.setInstitution(trim(request.getInstitution()));
        entity.setTrainingCost(request.getTrainingCost());
        entity.setTrainingLocation(trim(request.getTrainingLocation()));
        entity.setRemarks(trim(request.getRemarks()));
        entity.setTransactionDate(request.getTransactionDate());
        entity.setEmployeePoid(trim(request.getEmployeePoid()));
        entity.setCompanyPoid(companyPoid);
        entity.setGroupPoid(groupPoid);
    }

    public List<EmployeeTrainingDetailEntity> toDetailEntities(Long transactionPoid, List<EmployeeTrainingDetailRequest> requests) {
        List<EmployeeTrainingDetailEntity> list = new ArrayList<>();

        if (requests == null || requests.isEmpty()) {
            return list;
        }

        int rowId = 1;
        for (EmployeeTrainingDetailRequest request : requests) {
            list.add(EmployeeTrainingDetailEntity.builder()
                    .id(new EmployeeTrainingDetailId(transactionPoid, rowId))
                    .empPoid(request.getEmpPoid())
                    .trainingStatus(trim(request.getTrainingStatus()))
                    .completedOn(request.getCompletedOn())
                    .otherRemarks(trim(request.getOtherRemarks()))
                    .build());
            rowId++;
        }

        list.sort(Comparator.comparing(d -> d.getId().getDetRowId()));
        return list;
    }

    public EmployeeTrainingResponse toResponse(EmployeeTrainingHeaderEntity header, List<EmployeeTrainingDetailEntity> details) {
        EmployeeTrainingResponse response = new EmployeeTrainingResponse();
        response.setTransactionPoid(header.getTransactionPoid());
        response.setDocRef(header.getDocRef());
        response.setCourseName(header.getCourseName());
        response.setPeriodFrom(header.getPeriodFrom());
        response.setPeriodTo(header.getPeriodTo());
        response.setDurationDays(header.getDurationDays());
        response.setTrainingType(header.getTrainingType());
        response.setInstitution(header.getInstitution());
        response.setTrainingCost(header.getTrainingCost());
        response.setTrainingLocation(header.getTrainingLocation());
        response.setRemarks(header.getRemarks());
        response.setTransactionDate(header.getTransactionDate());
        response.setCompanyPoid(header.getCompanyPoid());
        response.setGroupPoid(header.getGroupPoid());
        response.setEmployeePoid(header.getEmployeePoid());
        response.setDeleted(header.getDeleted());
        response.setActive(EmployeeTrainingConstants.DELETED_YES.equalsIgnoreCase(header.getDeleted()) ? "N" : "Y");
        response.setCreatedBy(header.getCreatedBy());
        response.setCreatedDate(header.getCreatedDate());
        response.setLastModifiedBy(header.getLastModifiedBy());
        response.setLastModifiedDate(header.getLastModifiedDate());

        if (details != null) {
            List<EmployeeTrainingDetailResponse> detailResponses = details.stream()
                    .sorted(Comparator.comparing(d -> d.getId().getDetRowId()))
                    .map(this::toDetailResponse)
                    .toList();
            response.setDetails(detailResponses);
        }

        return response;
    }

    private EmployeeTrainingDetailResponse toDetailResponse(EmployeeTrainingDetailEntity detail) {
        EmployeeTrainingDetailResponse response = new EmployeeTrainingDetailResponse();
        response.setDetRowId(detail.getId().getDetRowId());
        response.setEmpPoid(detail.getEmpPoid());
        response.setTrainingStatus(detail.getTrainingStatus());
        response.setCompletedOn(detail.getCompletedOn());
        response.setOtherRemarks(detail.getOtherRemarks());
        response.setCreatedBy(detail.getCreatedBy());
        response.setCreatedDate(detail.getCreatedDate());
        response.setLastModifiedBy(detail.getLastModifiedBy());
        response.setLastModifiedDate(detail.getLastModifiedDate());
        return response;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
