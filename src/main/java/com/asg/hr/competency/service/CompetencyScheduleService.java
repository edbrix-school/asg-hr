package com.asg.hr.competency.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.competency.dto.CompetencyScheduleListDto;
import com.asg.hr.competency.dto.CompetencyScheduleRequestDto;
import com.asg.hr.competency.dto.CompetencyScheduleResponseDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CompetencyScheduleService {
    
    Long createSchedule(CompetencyScheduleRequestDto requestDto);
    
    Long updateSchedule(Long schedulePoid, CompetencyScheduleRequestDto requestDto);
    
    CompetencyScheduleResponseDto getScheduleById(Long schedulePoid);
    
    List<CompetencyScheduleListDto> getAllSchedules();
    
    Map<String, Object> listSchedules(FilterRequestDto filterRequest, Pageable pageable);
    
    void deleteSchedule(Long schedulePoid, DeleteReasonDto deleteReasonDto);
    
    void createBatchEvaluation(Long schedulePoid, LocalDate evaluationDate, Boolean recreate);
}
