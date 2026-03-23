package com.asg.hr.holidaymaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.hr.holidaymaster.dto.HolidayBatchCreateRequest;
import com.asg.hr.holidaymaster.dto.HolidayMasterRequest;
import com.asg.hr.holidaymaster.dto.HolidayMasterResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface HolidayMasterService {
    /**
     * Lists holidays using the document search infrastructure, applying the provided filters and pagination.
     *
     * @param docId document id to scope the search.
     * @param filters search/filter criteria (operator and isDeleted flags are resolved by the underlying service).
     * @param pageable pagination information.
     * @return a map containing the paginated result set and pagination metadata (structure depends on {@code PaginationUtil}).
     */
    Map<String, Object> listHolidays(
            String docId,
            FilterRequestDto filters,
            Pageable pageable);

    /**
     * Fetches a single non-deleted holiday by its identifier.
     *
     * @param holidayPoid unique identifier of the holiday record.
     * @return the populated holiday response.
     * @throws ResourceNotFoundException if the holiday is not found or is marked as deleted.
     */
    HolidayMasterResponse getById(Long holidayPoid);

    /**
     * Creates a new holiday record.
     *
     * @param request input data for the holiday to create.
     * @return the created holiday response.
     * @throws ValidationException if a holiday already exists for the requested date (or input validation fails).
     */
    HolidayMasterResponse create(HolidayMasterRequest request);

    /**
     * Updates an existing holiday record.
     *
     * @param holidayPoid unique identifier of the holiday to update.
     * @param request input data for the update.
     * @return the updated holiday response.
     * @throws ResourceNotFoundException if the holiday is not found or is marked as deleted.
     * @throws ValidationException if the updated date conflicts with an existing holiday.
     */
    HolidayMasterResponse update(Long holidayPoid, HolidayMasterRequest request);

    /**
     * Soft deletes a holiday record and creates an appropriate deletion record in the document delete system.
     *
     * @param holidayPoid unique identifier of the holiday to delete.
     * @param deleteReasonDto reason payload used for audit/logging.
     * @throws ResourceNotFoundException if the holiday is not found or is marked as deleted.
     */
    void delete(Long holidayPoid, DeleteReasonDto deleteReasonDto);

    /**
     * Batch create holidays using legacy PROC_HR_CREATE_HOLIDAYS.
     *
     * @param request batch request containing start date, number of days, and reason text.
     * @return status message coming from the stored procedure (e.g. SUCCESS / ERROR...).
     * @throws ValidationException if the stored procedure execution fails.
     */
    String batchCreateHolidays(HolidayBatchCreateRequest request);
}

