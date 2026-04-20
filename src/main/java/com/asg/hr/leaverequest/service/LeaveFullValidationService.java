package com.asg.hr.leaverequest.service;

import com.asg.hr.exceptions.ValidationException;
import com.asg.hr.leaverequest.dto.LeaveCreateRequestDto;
import org.springframework.stereotype.Service;

@Service
public class LeaveFullValidationService {

    public void validateBeforeSave(LeaveCreateRequestDto req) {

        if (req.getCompanyPoid() == null) {
            throw new ValidationException("Company not selected");
        }

        if (req.getEmployeePoid() == null) {
            throw new ValidationException("Employee not selected");
        }

        if (isBlank(req.getLeaveType())) {
            throw new ValidationException("Leave type required");
        }

        if (contains(req.getLeaveType(), "ANNUAL") && isBlank(req.getAnnualLeaveType())) {
            throw new ValidationException("Annual Leave type is empty");
        }

        if (contains(req.getLeaveType(), "EMERGENCY") && isBlank(req.getEmergencyLeaveType())) {
            throw new ValidationException("Emergency Leave type is empty");
        }

        if (contains(req.getLeaveType(), "SPECIAL_LEAVE") && isBlank(req.getSplLeaveTypes())) {
            throw new ValidationException("Special Leave type is empty");
        }

        if (req.getLeaveStartDate() == null || req.getPlanedRejoinDate() == null) {
            throw new ValidationException("Leave dates required");
        }

        if (req.getPlanedRejoinDate().isBefore(req.getLeaveStartDate())) {
            throw new ValidationException("Planned rejoin date should be after leave start date");
        }

        //  FIX 3: LeaveDays NULL validation
        if (req.getLeaveDays() == null) {
            throw new ValidationException("Leave days is not calculated, please check the dates entered");
        }

        if (req.getLeaveDays().signum() < 0) {
            throw new ValidationException("Leave days is not calculated, please check the dates entered");
        }

        if (req.getBalanceTillRejoin() != null && req.getBalanceTillRejoin().signum() < 0) {
            throw new ValidationException("Check Final Leave Balance can not be negative");
        }

        //  FIX 1: Correct condition
        if ("Y".equals(req.getTicketRequired())) {

            if (isBlank(req.getTicketFromLocn()) ||
                    isBlank(req.getTicketToLocn()) ||
                    req.getTicketTravelDate() == null ||
                    req.getTicketReturnDate() == null ||
                    req.getTicketCount() == null) {

                throw new ValidationException("All the ticket details are required if ticket required is selected");
            }

            if (req.getTicketCount().signum() < 0) {
                throw new ValidationException("Ticket count cannot be negative");
            }

            if (req.getTicketReturnDate().isBefore(req.getTicketTravelDate())) {
                throw new ValidationException("Travel Return should be after travel date");
            }
        } else {
            //  FIX 2: Cleanup ticket fields
            req.setTicketFromLocn(null);
            req.setTicketToLocn(null);
            req.setTicketTravelDate(null);
            req.setTicketReturnDate(null);
            req.setTicketCount(null);
        }
    }

    private boolean contains(String value, String token) {
        return value != null && value.toUpperCase().contains(token);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}