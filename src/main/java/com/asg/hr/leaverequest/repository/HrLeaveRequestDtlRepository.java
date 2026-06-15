package com.asg.hr.leaverequest.repository;

import com.asg.hr.leaverequest.entity.HrLeaveRequestDtl;
import com.asg.hr.leaverequest.entity.HrLeaveRequestDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrLeaveRequestDtlRepository extends JpaRepository<HrLeaveRequestDtl, HrLeaveRequestDtlId> {

    void deleteByIdTransactionPoid(Long transactionPoid);

    List<HrLeaveRequestDtl> findByIdTransactionPoid(Long transactionPoid);

    @Query("""
    SELECT MAX(d.id.detRowId)
    FROM HrLeaveRequestDtl d
    WHERE d.id.transactionPoid = :transactionPoid
""")
    Long findMaxDetRowIdByTransactionPoid(Long transactionPoid);
}
