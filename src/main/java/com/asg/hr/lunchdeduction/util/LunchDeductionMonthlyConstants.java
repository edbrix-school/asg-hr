package com.asg.hr.lunchdeduction.util;

public final class LunchDeductionMonthlyConstants {

    public static final String DOCUMENT_TABLE = "HR_MONTHLY_LUNCH_HDR";
    public static final String DETAIL_TABLE = "HR_MONTHLY_LUNCH_DTL";
    public static final String PRIMARY_KEY = "TRANSACTION_POID";
    public static final String DOC_LABEL = "LUNCH_DESCRIPTION";
    public static final String MODULE_NAME = "Lunch Deduction Monthly";
    public static final String MODULE_KEY = "transactionPoid";
    public static final String IMPORT_PROCEDURE = "PROC_HR_LUNCH_SUMM_IMPORT";

    private LunchDeductionMonthlyConstants() {
    }
}