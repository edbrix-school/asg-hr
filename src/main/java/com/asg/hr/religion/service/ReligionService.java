package com.asg.hr.religion.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.religion.dto.ReligionDtoRequest;
import com.asg.hr.religion.dto.ReligionDtoResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ReligionService {

    ReligionDtoResponse getReligionById(Long religionPoid);

    Long createReligion(ReligionDtoRequest religionDto);

    Long updateReligion(ReligionDtoRequest religionDto,Long religionPoid);

    void deleteReligion(Long religionPoid, DeleteReasonDto deleteReasonDto);

    Map<String, Object> listReligion(FilterRequestDto filterRequest, Pageable pageable);


}
