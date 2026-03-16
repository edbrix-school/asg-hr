package com.asg.hr.holidaymaster.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.holidaymaster.dto.HolidayBatchCreateRequest;
import com.asg.hr.holidaymaster.dto.HolidayMasterRequest;
import com.asg.hr.holidaymaster.dto.HolidayMasterResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface HolidayMasterService {

    Map<String, Object> listHolidays(
            String docId,
            FilterRequestDto filters,
            Pageable pageable);

    HolidayMasterResponse getById(Long holidayPoid);

    HolidayMasterResponse create(HolidayMasterRequest request);

    HolidayMasterResponse update(Long holidayPoid, HolidayMasterRequest request);

    void toggleActiveStatus(Long holidayPoid);

    void delete(Long holidayPoid);

    /**
     * Batch create holidays using legacy PROC_HR_CREATE_HOLIDAYS.
     *
     * @return status message coming from stored procedure (SUCCESS / ERROR...)
     */
    String batchCreateHolidays(HolidayBatchCreateRequest request);
}

