package com.asg.hr.holidaymaster.service.impl;

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
import com.asg.hr.holidaymaster.dto.HolidayBatchCreateRequest;
import com.asg.hr.holidaymaster.dto.HolidayMasterRequest;
import com.asg.hr.holidaymaster.dto.HolidayMasterResponse;
import com.asg.hr.holidaymaster.entity.HolidayMasterEntity;
import com.asg.hr.holidaymaster.repository.HolidayMasterRepository;
import com.asg.hr.holidaymaster.service.HolidayMasterService;
import com.asg.hr.holidaymaster.util.HolidayMasterConstants;
import com.asg.hr.holidaymaster.util.HolidayMasterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayMasterServiceImpl implements HolidayMasterService {

    private final HolidayMasterRepository repository;
    private final HolidayMasterMapper mapper;
    private final DocumentSearchService documentService;
    private final LoggingService loggingService;
    private final JdbcTemplate jdbcTemplate;
    private final DocumentDeleteService documentDeleteService;

    @Override
    public Map<String, Object> listHolidays(String docId, FilterRequestDto filters, Pageable pageable) {
        log.info("Listing holidays with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> resolvedFilters = documentService.resolveFilters(filters);

        RawSearchResult raw = documentService.search(
                docId,
                resolvedFilters,
                operator,
                pageable,
                isDeleted,
                "HOLIDAY_REASON", // label
                HolidayMasterConstants.HOLIDAY_POID   // value
        );

        Page<Map<String, Object>> page =
                new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public HolidayMasterResponse getById(Long holidayPoid) {
        log.info("Getting holiday with id: {}", holidayPoid);

        HolidayMasterEntity entity =
                repository.findById(holidayPoid)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                HolidayMasterConstants.HOLIDAY_MASTER,
                                HolidayMasterConstants.HOLIDAYPOID,
                                holidayPoid
                        ));

        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public HolidayMasterResponse create(HolidayMasterRequest request) {
        log.info("Creating holiday for date: {}", request.getHolidayDate());

        if (repository.existsByHolidayDate(request.getHolidayDate())) {
            throw new ValidationException(
                    "Holiday already exists for date: " + request.getHolidayDate()
            );
        }

        HolidayMasterEntity entity = mapper.toEntity(request);
        HolidayMasterEntity saved = repository.save(entity);

        String key = saved.getHolidayPoid().toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), key);

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HolidayMasterResponse update(Long holidayPoid, HolidayMasterRequest request) {
        log.info("Updating holiday with id: {}", holidayPoid);

        HolidayMasterEntity entity =
                repository.findByHolidayPoidAndDeletedNot(holidayPoid, "Y")
                        .orElseThrow(() -> new ResourceNotFoundException(
                                HolidayMasterConstants.HOLIDAY_MASTER,
                                HolidayMasterConstants.HOLIDAYPOID,
                                holidayPoid
                        ));

        if (request.getHolidayDate() != null
                && !request.getHolidayDate().equals(entity.getHolidayDate())
                && repository.existsByHolidayDate(request.getHolidayDate())) {
            throw new ValidationException(
                    "Holiday already exists for date: " + request.getHolidayDate()
            );
        }

        HolidayMasterEntity oldEntity = new HolidayMasterEntity();
        BeanUtils.copyProperties(entity, oldEntity);

        mapper.updateEntity(entity, request);
        repository.save(entity);

        String key = entity.getHolidayPoid().toString();
        loggingService.logChanges(oldEntity, entity, HolidayMasterEntity.class, UserContext.getDocumentId(), key, LogDetailsEnum.MODIFIED, HolidayMasterConstants.HOLIDAY_POID);

        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void delete(Long holidayPoid, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting (soft) holiday with id: {}", holidayPoid);
        repository.findById(holidayPoid).orElseThrow(() -> new ResourceNotFoundException(
                                HolidayMasterConstants.HOLIDAY_MASTER,
                                HolidayMasterConstants.HOLIDAYPOID,
                                holidayPoid
                        ));

        documentDeleteService.deleteDocument(
                holidayPoid,
                "HR_HOLIDAY_MASTER",
                HolidayMasterConstants.HOLIDAY_POID,
                deleteReasonDto,
                LocalDate.now()
        );
    }

    @Override
    @Transactional
    public String batchCreateHolidays(HolidayBatchCreateRequest request) {
        log.info("Batch creating holidays starting from {}, days={}, reason={}",
                request.getStartDate(), request.getDays(), request.getReason());

        try {
            SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("PROC_HR_CREATE_HOLIDAYS")
                    .declareParameters(
                            new SqlParameter("P_DATE", Types.DATE),
                            new SqlParameter("P_REASON", Types.VARCHAR),
                            new SqlParameter("P_DAYS", Types.NUMERIC),
                            new SqlParameter("P_LOGIN_USER_POID", Types.NUMERIC),
                            new SqlOutParameter("P_STATUS", Types.VARCHAR)
                    );

            Map<String, Object> inParams = Map.of(
                    "P_DATE", java.sql.Date.valueOf(request.getStartDate()),
                    "P_REASON", request.getReason(),
                    "P_DAYS", request.getDays(),
                    "P_LOGIN_USER_POID", UserContext.getUserPoid()
            );

            Map<String, Object> out = call.execute(inParams);
            String status = (String) out.get("P_STATUS");

            log.info("PROC_HR_CREATE_HOLIDAYS completed with status: {}", status);

            return status;
        } catch (DataAccessException ex) {
            log.error("Error while executing PROC_HR_CREATE_HOLIDAYS", ex);
            throw new ValidationException("Error while creating holidays: " + ex.getMostSpecificCause().getMessage());
        }
    }

}

