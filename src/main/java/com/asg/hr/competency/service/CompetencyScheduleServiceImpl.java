package com.asg.hr.competency.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.competency.dto.CompetencyScheduleListDto;
import com.asg.hr.competency.dto.CompetencyScheduleRequestDto;
import com.asg.hr.competency.dto.CompetencyScheduleResponseDto;
import com.asg.hr.competency.entity.HrCompetencySchedule;
import com.asg.hr.competency.repository.CompetencyScheduleProcRepository;
import com.asg.hr.competency.repository.HrCompetencyScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetencyScheduleServiceImpl implements CompetencyScheduleService {
    
    private final HrCompetencyScheduleRepository scheduleRepository;
    private final CompetencyScheduleProcRepository procRepository;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;
    private final DocumentSearchService documentSearchService;
    
    @Override
    @Transactional
    public Long createSchedule(CompetencyScheduleRequestDto requestDto) {
        validatePeriod(requestDto, null);
        
        HrCompetencySchedule schedule = HrCompetencySchedule.builder()
                .groupPoid(UserContext.getGroupPoid())
                .scheduleDescription(requestDto.getScheduleDescription())
                .periodFrom(requestDto.getPeriodFrom())
                .periodTo(requestDto.getPeriodTo())
                .seqNo(requestDto.getSeqNo())
                .active(requestDto.getActive())
                .evaluationDate(requestDto.getEvaluationDate())
                .deleted("N")
                .build();
        
        HrCompetencySchedule saved = scheduleRepository.save(schedule);
        log.info("Created competency schedule with ID: {}", saved.getSchedulePoid());
        
        // Log the creation
        String key = saved.getSchedulePoid().toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), key);
        
        return saved.getSchedulePoid();
    }
    
    @Override
    @Transactional
    public Long updateSchedule(Long schedulePoid, CompetencyScheduleRequestDto requestDto) {
        HrCompetencySchedule schedule = scheduleRepository.findBySchedulePoidAndDeleted(schedulePoid, "N")
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "schedulePoid", schedulePoid));
        
        validatePeriod(requestDto, schedulePoid);
        
        // Create a copy of the existing entity for logging
        HrCompetencySchedule oldEntity = new HrCompetencySchedule();
        BeanUtils.copyProperties(schedule, oldEntity);
        
        schedule.setScheduleDescription(requestDto.getScheduleDescription());
        schedule.setPeriodFrom(requestDto.getPeriodFrom());
        schedule.setPeriodTo(requestDto.getPeriodTo());
        schedule.setSeqNo(requestDto.getSeqNo());
        schedule.setActive(requestDto.getActive());
        schedule.setEvaluationDate(requestDto.getEvaluationDate());
        
        HrCompetencySchedule updated = scheduleRepository.save(schedule);
        log.info("Updated competency schedule with ID: {}", updated.getSchedulePoid());
        
        // Log the update with changes
        String key = updated.getSchedulePoid().toString();
        loggingService.logChanges(oldEntity, updated, HrCompetencySchedule.class, 
                UserContext.getDocumentId(), key, LogDetailsEnum.MODIFIED, "COMP_SCHEDULE_POID");
        
        return updated.getSchedulePoid();
    }
    
    @Override
    public CompetencyScheduleResponseDto getScheduleById(Long schedulePoid) {
        HrCompetencySchedule schedule = scheduleRepository.findBySchedulePoidAndDeleted(schedulePoid, "N")
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "schedulePoid", schedulePoid));
        
        return mapToResponseDto(schedule);
    }
    
    @Override
    public List<CompetencyScheduleListDto> getAllSchedules() {
        List<HrCompetencySchedule> schedules = scheduleRepository
                .findByGroupPoidAndDeletedOrderByScheduleDescription(UserContext.getGroupPoid(), "N");
        
        return schedules.stream()
                .map(this::mapToListDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Object> listSchedules(FilterRequestDto filterRequest, Pageable pageable) {
        String operator = documentSearchService.resolveOperator(filterRequest);
        String isDeleted = documentSearchService.resolveIsDeleted(filterRequest);
        List<FilterDto> filterList = documentSearchService.resolveFilters(filterRequest);
        
        RawSearchResult raw = documentSearchService.search(UserContext.getDocumentId(), filterList, operator, pageable, isDeleted,
                "SCHEDULE_DESCRIPTION", "COMP_SCHEDULE_POID");
        
        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }
    
    @Override
    @Transactional
    public void deleteSchedule(Long schedulePoid, DeleteReasonDto deleteReasonDto) {
        HrCompetencySchedule schedule = scheduleRepository.findBySchedulePoidAndDeleted(schedulePoid, "N")
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "schedulePoid", schedulePoid));
        
        // Use DocumentDeleteService for deletion (handles logging internally)
        documentDeleteService.deleteDocument(
                schedulePoid,
                "HR_COMPETENCY_SCHEDULE",
                "COMP_SCHEDULE_POID",
                deleteReasonDto,
                null
        );
        
        log.info("Soft deleted competency schedule with ID: {}", schedulePoid);
    }
    
    @Override
    @Transactional
    public void createBatchEvaluation(Long schedulePoid, LocalDate evaluationDate, Boolean recreate) {
        HrCompetencySchedule schedule = scheduleRepository.findBySchedulePoidAndDeleted(schedulePoid, "N")
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "schedulePoid", schedulePoid));
        
        LocalDate evalDate = evaluationDate != null ? evaluationDate : schedule.getEvaluationDate();
        procRepository.createBatchEvaluation(schedulePoid, schedule.getGroupPoid(), recreate, evalDate);
        log.info("Batch evaluation created for schedule ID: {}", schedulePoid);
    }
    
    private void validatePeriod(CompetencyScheduleRequestDto requestDto, Long schedulePoid) {
        if (requestDto.getPeriodFrom().isAfter(requestDto.getPeriodTo())) {
            throw new ValidationException("Period From date must be before Period To date");
        }
        
        Long poid = schedulePoid != null ? schedulePoid : 0L;
        boolean overlaps = scheduleRepository.existsOverlappingPeriod(
                UserContext.getGroupPoid(),
                requestDto.getPeriodFrom(),
                requestDto.getPeriodTo(),
                poid
        );
        
        if (overlaps) {
            throw new ValidationException("Period overlaps with an existing schedule");
        }
    }
    
    private CompetencyScheduleResponseDto mapToResponseDto(HrCompetencySchedule schedule) {
        return CompetencyScheduleResponseDto.builder()
                .schedulePoid(schedule.getSchedulePoid())
                .scheduleDescription(schedule.getScheduleDescription())
                .periodFrom(schedule.getPeriodFrom())
                .periodTo(schedule.getPeriodTo())
                .seqNo(schedule.getSeqNo())
                .active(schedule.getActive())
                .evaluationDate(schedule.getEvaluationDate())
                .build();
    }
    
    private CompetencyScheduleListDto mapToListDto(HrCompetencySchedule schedule) {
        return CompetencyScheduleListDto.builder()
                .schedulePoid(schedule.getSchedulePoid())
                .scheduleDescription(schedule.getScheduleDescription())
                .periodFrom(schedule.getPeriodFrom())
                .periodTo(schedule.getPeriodTo())
                .active(schedule.getActive())
                .build();
    }
}
