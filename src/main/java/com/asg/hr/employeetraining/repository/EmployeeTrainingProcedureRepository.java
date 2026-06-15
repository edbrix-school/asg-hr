package com.asg.hr.employeetraining.repository;

import com.asg.common.lib.dto.LovGetListDto;

import java.util.List;

public interface EmployeeTrainingProcedureRepository {

    List<LovGetListDto> getEmployeeLovByIds(List<Long> employeeIds);

    LovGetListDto getTrainingTypeByCode(String trainingTypeCode);

    List<LovGetListDto> getTrainingStatusByCodes(List<String> trainingStatusCodes);
}
