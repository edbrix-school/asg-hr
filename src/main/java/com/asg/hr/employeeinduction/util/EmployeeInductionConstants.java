package com.asg.hr.employeeinduction.util;

public class EmployeeInductionConstants {

    public static final String STATUS_COMPLETED = "Y";
    public static final String STATUS_PENDING = "N";
    public static final String DELETED_YES = "Y";
    public static final String DELETED_NO = "N";
    
    // Email notification constants
    public static final String EMAIL_SUBJECT = "Alert: Employee Induction Update";
    public static final String EMAIL_BODY_TEMPLATE = "The induction activity %s assigned to %s is overdue. " +
            "The activity was scheduled from %s to %s and is pending completion.";
    
    // LOV constants
    public static final String LOV_EMPLOYEE_NAME = "EMPLOYEE_NAME";
    public static final String LOV_INDUCTION_CATEGORY = "INDUCTION_CATEGORY";
    public static final String LOV_YES_NO = "YES_NO";
    
    private EmployeeInductionConstants() {
        // Private constructor to prevent instantiation
    }
}