package com.asg.hr.leaverequest.repository;

import com.asg.hr.leaverequest.entity.HrLeaveRequestHdrEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HrLeaveRequestHdrRepository extends JpaRepository<HrLeaveRequestHdrEntity, Long> {

    @Query("""
            select h from HrLeaveRequestHdrEntity h
            where (h.deleted is null or upper(h.deleted) <> 'Y')
            order by h.transactionPoid desc
            """)
    List<HrLeaveRequestHdrEntity> findActiveList();

    @Query("""
            select count(h) from HrLeaveRequestHdrEntity h
            where h.employeePoid = :employeePoid
              and (:transactionPoid is null or h.transactionPoid <> :transactionPoid)
              and (h.status is null or upper(h.status) not in ('REJECTED', 'DELETED'))
              and (h.deleted is null or upper(h.deleted) <> 'Y')
              and h.leaveStartDate <= :planedRejoinDate
              and h.planedRejoinDate >= :leaveStartDate
            """)
    long countOverlappingLeaveRequests(
            @Param("employeePoid") Long employeePoid,
            @Param("transactionPoid") Long transactionPoid,
            @Param("leaveStartDate") LocalDate leaveStartDate,
            @Param("planedRejoinDate") LocalDate planedRejoinDate
    );

    @Query(value = """
            select count(*)
              from HR_EMPLOYEE_LEAVE_HISTORY h
             where h.EMPLOYEE_POID = :employeePoid
               and (:transactionPoid is null or h.SOURCE_DOC_POID <> :transactionPoid)
               and (:documentId is null or h.SOURCE_DOC_ID <> :documentId)
               and h.LEAVE_START_DATE <= :planedRejoinDate
               and h.REJOIN_DATE >= :leaveStartDate
            """, nativeQuery = true)
    long countOverlappingLeaveHistory(
            @Param("employeePoid") Long employeePoid,
            @Param("transactionPoid") Long transactionPoid,
            @Param("documentId") String documentId,
            @Param("leaveStartDate") LocalDate leaveStartDate,
            @Param("planedRejoinDate") LocalDate planedRejoinDate
    );
}
