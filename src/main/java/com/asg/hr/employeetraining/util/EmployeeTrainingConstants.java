package com.asg.hr.employeetraining.util;

public final class EmployeeTrainingConstants {

    private EmployeeTrainingConstants() {
    }

    public static final String TABLE_NAME = "HR_EMPLOYEE_TRAINING_HDR";
    public static final String KEY_FIELD = "TRANSACTION_POID";

    public static final String DELETED_NO = "N";
    public static final String DELETED_YES = "Y";

    public static final String COMPLETED_STATUS = "COMPLETED";

    public static final String ACTION_IS_CREATED = "isCreated";
    public static final String ACTION_IS_UPDATED = "isUpdated";
    public static final String ACTION_IS_DELETED = "isDeleted";
    public static final String ACTION_NO_CHANGE = "noChange";
}
