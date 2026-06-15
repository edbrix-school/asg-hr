package com.asg.hr.attendancemonthly.mapper;

import com.asg.hr.attendancemonthly.dto.*;
import com.asg.hr.attendancemonthly.entity.HrAttendanceMonthlyDtl;
import com.asg.hr.attendancemonthly.entity.HrAttendanceMonthlyHdr;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HrAttendanceMonthlyMapper {

    @Mapping(target = "details", ignore = true)
    HrAttendanceMonthlyResponse toResponse(HrAttendanceMonthlyHdr hdr);

    @Mapping(target = "employeeName", ignore = true)
    HrAttendanceMonthlyDtlResponse toDtlResponse(HrAttendanceMonthlyDtl dtl);

    List<HrAttendanceMonthlyDtlResponse> toDtlResponseList(List<HrAttendanceMonthlyDtl> dtlList);

    @Mapping(target = "transactionPoid", ignore = true)
    @Mapping(target = "docRef", ignore = true)
    @Mapping(target = "deleted", constant = "N")
    @Mapping(target = "loadedPayroll", constant = "N")
    @Mapping(target = "employeeWise", constant = "N")
    @Mapping(target = "employeePoid", ignore = true)
    HrAttendanceMonthlyHdr toEntity(HrAttendanceMonthlyRequest request);

    @Mapping(target = "transactionPoid", ignore = true)
    @Mapping(target = "detRowId", ignore = true)
    HrAttendanceMonthlyDtl toDtlEntity(HrAttendanceMonthlyDtlRequest dtlRequest);


    @Mapping(target = "transactionPoid", ignore = true)
    @Mapping(target = "docRef", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "loadedPayroll", ignore = true)
    @Mapping(target = "employeeWise", constant = "N")
    @Mapping(target = "employeePoid", ignore = true)
    void updateEntity(@MappingTarget HrAttendanceMonthlyHdr hdr, HrAttendanceMonthlyUpdateRequest request);
}
