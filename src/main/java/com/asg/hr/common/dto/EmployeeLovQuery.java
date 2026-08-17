package com.asg.hr.common.dto;

/**
 * What the caller asked of the employee LOV on the current-employee endpoint.
 *
 * @param lovName    LOV to read the employee list from; no list is fetched when this is blank
 * @param filter     search text, as the LOV endpoints take it
 * @param pageNumber 0 based page, defaulted from {@code pagination.*} when null
 * @param pageSize   page size, defaulted from {@code pagination.*} when null
 * @param sortBy     one of code / description / label / value / seqno / poid
 * @param sortDir    asc or desc
 */
public record EmployeeLovQuery(
        String lovName,
        String filter,
        Integer pageNumber,
        Integer pageSize,
        String sortBy,
        String sortDir) {

    /** True when the caller named a LOV, so the list is worth fetching. */
    public boolean requested() {
        return lovName != null && !lovName.isBlank();
    }
}
