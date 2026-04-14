package com.asg.hr.personaldatasheet.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.personaldatasheet.dto.PersonalDataSheetRequestDto;
import com.asg.hr.personaldatasheet.dto.PersonalDataSheetResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface PersonalDataSheetService {

    PersonalDataSheetResponseDto create(PersonalDataSheetRequestDto request);

    PersonalDataSheetResponseDto getById(Long transactionPoid);

    PersonalDataSheetResponseDto update(Long transactionPoid, PersonalDataSheetRequestDto request);

    void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    Map<String, Object> list(String documentId, FilterRequestDto filters, Pageable pageable);

    Map<String, Object> getLoginUserEmployee();

    List<Map<String, Object>> loadUserPolicies(Long employeePoid);

    byte[] print(Long transactionPoid);
}