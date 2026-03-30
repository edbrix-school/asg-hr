package com.asg.hr.airsector.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.airsector.dto.HrAirsectorRequestDto;
import com.asg.hr.airsector.dto.HrAirsectorResponseDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface HrAirsectorService {

    HrAirsectorResponseDto create(HrAirsectorRequestDto request);

    HrAirsectorResponseDto update(Long airsecPoid,
                                  HrAirsectorRequestDto request);

    HrAirsectorResponseDto findById(Long airsecPoid);

    void deleteAirsectorMaster(Long airsecPoid, DeleteReasonDto deleteReasonDto);

    Map<String, Object> listOfRecordsAndGenericSearch(String docId,
                                                      FilterRequestDto request,
                                                      Pageable pageable,
                                                      LocalDate startDate,
                                                      LocalDate endDate);
}
