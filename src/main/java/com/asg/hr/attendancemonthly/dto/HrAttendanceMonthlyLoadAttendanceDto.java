package com.asg.hr.attendancemonthly.dto;

import com.asg.hr.attendancemonthly.entity.HrAttendanceMonthlyDtl;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrAttendanceMonthlyLoadAttendanceDto {
   private List<HrAttendanceMonthlyDtl> attendanceDetails;
}
