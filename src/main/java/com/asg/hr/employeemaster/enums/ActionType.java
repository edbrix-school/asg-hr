package com.asg.hr.employeemaster.enums;

/**
 * Action types used by the frontend to apply changes on child detail tables.
 * <p>
 * Values are intentionally aligned with the common “child row action” pattern used in other modules.
 */
public enum ActionType {
    isCreated,
    isUpdated,
    noChange,
    isDeleted
}

