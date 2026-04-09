package com.asg.hr.attendencerequest.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "HR_ATTENDANCE_SPECIAL_REQ")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID")
    private Long attendancePoid;

    @Column(name = "EMPLOYEE_POID")
    private Long employeePoid;

    @Column(name = "ATTENDANCE_DATE")
    private LocalDate attendanceDate;

    @Column(name = "EXCEPTION_TYPE")
    private String exceptionType;

    @Column(name = "EXCEPTION_REASON")
    private String reason;

    @Column(name = "HOD_REMARKS")
    private String hodRemarks;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "DELETED")
    private String deleted;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

}