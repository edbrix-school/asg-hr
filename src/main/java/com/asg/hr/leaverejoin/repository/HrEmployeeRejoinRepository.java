package com.asg.hr.leaverejoin.repository;

import com.asg.hr.leaverejoin.entity.HrEmployeeRejoinHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HrEmployeeRejoinRepository extends JpaRepository<HrEmployeeRejoinHdr, Long> {

    Optional<HrEmployeeRejoinHdr> findByTransactionPoid(Long transactionPoid);

    Optional<HrEmployeeRejoinHdr> findByTransactionPoidAndDeletedNot(Long transactionPoid, String deleted);

    boolean existsByEmployeePoidAndLeaveRequestPoidAndDeletedNot(Long employeePoid, Long leaveRequestPoid, String deleted);

    boolean existsByEmployeePoidAndLeaveRequestPoidAndDeletedNotAndTransactionPoidNot(
            Long employeePoid,
            Long leaveRequestPoid,
            String deleted,
            Long transactionPoid
    );
}
