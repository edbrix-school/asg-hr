package com.asg.hr.leaverequest.dto;

import lombok.Data;

import java.util.List;

@Data
public class EarlierPendingLeaveCheckDto {

    /** True when an earlier leave request of the same employee is still pending. */
    private boolean earlierPendingLeaveExists;

    /** False when saving would be rejected, so the UI can block Save up front. */
    private boolean canSave;

    /** Warning to show when {@code earlierPendingLeaveExists} is true, otherwise null. */
    private String message;

    /** The pending earlier leave requests, oldest leave period first. */
    private List<PendingLeaveRequestDto> pendingLeaveRequests;
}
