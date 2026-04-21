package com.asg.hr.resignation.util;


public final class HrResignationConstants {

    private HrResignationConstants() {
    }

    // Main table + key column used by soft-delete document procedure
    public static final String TABLE_NAME = "HR_EMP_RESIGNATION_HDR";
    public static final String KEY_FIELD = "TRANSACTION_POID";

    public static final String PROC_GET_EMP_DTLS = "PROC_HR_RESIGN_GET_EMP_DTLS";
    public static final String PROC_BEFORE_SAVE_VAL = "PROC_HR_RESIGN_BEFOR_SAVE_VAL";
    public static final String STATUS_ERROR = "ERROR";
    public static final String PARAM_POIDS = "poids";

    public static final String RESIGNATION_TYPE_VOLUNTARY = "VOLUNTARY";
}

