package com.asg.hr.leaverejoin.util;

public final class EmployeeLeaveRejoinConstants {

    private EmployeeLeaveRejoinConstants() {
    }

    public static final String TABLE_NAME = "HR_EMP_REJOIN_HDR";
    public static final String KEY_FIELD = "TRANSACTION_POID";
    public static final String DOC_REF_FIELD = "DOC_REF";
    public static final String TRANSACTION_DATE_FIELD = "TRANSACTION_DATE";
    public static final String EMPLOYEE_POID_FIELD = "EMPLOYEE_POID";
    public static final String DELETED_NO = "N";
    public static final String DELETED_YES = "Y";
    public static final String ACTIVE_YES = "Y";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_ERROR = "ERROR";
    public static final String RESOURCE_NAME = "Leave Rejoin";
    public static final String EMPLOYEE_RESOURCE_NAME = "Employee";

    public static final String EMPLOYEE_LOV = "EMPLOYEE_NAME_ON_LEAVE";
    public static final String LEAVE_REQUEST_LOV = "EMP_REJOIN_LEAVE_REQUEST";
    public static final String PASSPORT_RECEIVED_LOV = "EMP_REJOIN_PASSPORT_RECEIVED";

    public static final String PROC_GET_LEAVE_DETAILS = "PROC_EMP_REJOIN_GET_LEAVE_DET";
    public static final String PROC_GET_EMPLOYEE_DETAILS = "PROC_EMP_REJOIN_GET_EMP_DET";
}
