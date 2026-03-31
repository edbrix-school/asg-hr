package com.asg.hr.employeetraining.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.employeetraining.dto.EmployeeTrainingRequest;
import com.asg.hr.employeetraining.dto.EmployeeTrainingResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface EmployeeTrainingService {

    /**
     * Lists employee training transactions using document-search filters and pagination.
     *
     * @param docId document master id used by document-search
     * @param filterRequest dynamic search filters (may be null/empty)
     * @param pageable pagination and sorting information
     * @return a map containing paginated items and metadata (format depends on {@code PaginationUtil})
     */
    Map<String, Object> listTrainings(String docId, FilterRequestDto filterRequest, Pageable pageable);

    /**
     * Fetches a single employee training transaction (header + detail rows) and enriches LOV fields.
     *
     * @param transactionPoid header transaction poid
     * @return response containing header + detail data with LOVs populated
     */
    EmployeeTrainingResponse getTrainingById(Long transactionPoid);

    /**
     * Creates a new employee training transaction including its detail rows.
     * <p>
     * Performs request normalization/validation, checks duplicates, persists header and detail rows, and writes logs.
     * </p>
     *
     * @param request create request payload
     * @return created response
     */
    EmployeeTrainingResponse createTraining(EmployeeTrainingRequest request);

    /**
     * Updates an existing employee training transaction including its detail rows.
     * <p>
     * Performs request normalization/validation, checks duplicates, updates header, reconciles detail rows based on
     * {@code actionType}/{@code detRowId}, persists changes, and writes logs.
     * </p>
     *
     * @param transactionPoid header transaction poid to update
     * @param request update payload
     * @return updated response
     */
    EmployeeTrainingResponse updateTraining(Long transactionPoid, EmployeeTrainingRequest request);

    /**
     * Soft deletes an employee training transaction.
     *
     * @param transactionPoid header transaction poid
     * @param deleteReasonDto optional delete reason payload
     */
    void deleteTraining(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    /**
     * Print Employee Induction Report.
     *
     * @param transactionPoid header transaction poid
     */
    byte[] print(Long transactionPoid) throws Exception;
}
