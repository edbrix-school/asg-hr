package com.asg.hr.common.dto;

import lombok.Builder;
import lombok.Data;

/**
 * The employee the logged-in user may work as, and how far their access reaches.
 * <p>
 * A screen uses this to decide what to put in its employee field: a user who may select any employee
 * gets an open picker, everyone else gets their own employee prefilled and locked.
 */
@Data
@Builder
public class CurrentUserEmployeeDto {

    /** Logged-in user, from the request context. */
    private String userId;

    /** Logged-in user's poid, from the request context. */
    private Long userPoid;

    /** Document the access below was read for, i.e. the X-Document-Id of the request. */
    private String documentId;

    /** Employee linked to the login, or null when the login is not linked to one. */
    private Long employeePoid;

    /** Employee code of {@link #employeePoid}, or null when there is no linked employee. */
    private String employeeCode;

    /** Employee name of {@link #employeePoid}, or null when there is no linked employee. */
    private String employeeName;

    /** Whether the linked employee record is active ("Y"/"N"), or null when there is none. */
    private String active;

    /** True when the login is linked to an employee record. */
    private boolean linkedToEmployee;

    /**
     * True when the user holds Edit on {@link #documentId}, so the calling screen may let them pick
     * any employee rather than only the one they are linked to.
     */
    private boolean canSelectAnyEmployee;

    /**
     * The employee list to populate the screen's picker with, or null when the caller named no LOV.
     * A restricted user gets only their own employee here.
     */
    private EmployeeLovDto employeeLov;
}
