package com.asg.hr.employeemaster.repository;

import com.asg.hr.employeemaster.dto.EmployeeCountDto;
import com.asg.hr.employeemaster.dto.EmployeeDashboardDetailsDto;
import com.asg.hr.employeemaster.entity.HrEmployeeMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface HrEmployeeMasterRepository extends JpaRepository<HrEmployeeMaster, Long> {
    Optional<HrEmployeeMaster> findByEmployeePoid(@Param("employeePoid") Long employeePoid);

    boolean existsByEmployeePoid(@Param("employeePoid") Long employeePoid);

    boolean existsByMobile(@Param("mobile") String mobile);

    boolean existsByMobileAndEmployeePoidNot(@Param("mobile") String mobile, @Param("employeePoid") Long employeePoid);

    boolean existsByEmployeeName(@Param("employeeName") String employeeName);

    boolean existsByEmployeeNameAndEmployeePoidNot(@Param("employeeName") String employeeName, @Param("employeePoid") Long employeePoid);

    @Query("""
                SELECT new com.asg.hr.employeemaster.dto.EmployeeCountDto(
                    COUNT(e),
                    COALESCE(SUM(CASE WHEN UPPER(TRIM(e.active)) = 'Y' THEN 1 ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN UPPER(TRIM(e.active)) <> 'Y' THEN 1 ELSE 0 END), 0)
                )
                FROM HrEmployeeMaster e
                WHERE COALESCE(e.deleted, 'N') = 'N'
            """)
    EmployeeCountDto getEmployeeCounts();

    @Query("""
                SELECT new com.asg.hr.employeemaster.dto.EmployeeDashboardDetailsDto(
                    e.employeePoid,
                    e.employeeName,
                    e.employeeName2,
                    e.designationPoid,
                    e.locationPoid,
                    e.departmentPoid,
                    e.joinDate,
                    e.mobile,
                    e.photo,
                    e.active
                )
                FROM HrEmployeeMaster e
                LEFT JOIN HrDepartmentMaster d ON d.deptPoid = e.departmentPoid
                LEFT JOIN HrDesignationMaster des ON des.desigPoid = e.designationPoid
                LEFT JOIN GlobalLocationMaster loc ON loc.locationPoid = e.locationPoid
                WHERE COALESCE(e.deleted, 'N') = 'N'
                  AND (:designationPoid IS NULL OR e.designationPoid = :designationPoid)
                  AND (:locationPoid IS NULL OR e.locationPoid = :locationPoid)
                  AND (:departmentPoid IS NULL OR e.departmentPoid = :departmentPoid)
                  AND (:joinDateFrom IS NULL OR e.joinDate >= :joinDateFrom)
                  AND (:joinDateTo IS NULL OR e.joinDate <= :joinDateTo)
                  AND (:status IS NULL OR UPPER(TRIM(COALESCE(e.active, ''))) = UPPER(TRIM(:status)))
                  AND (:filter IS NULL OR (
                        LOWER(e.employeeName) LIKE LOWER(CONCAT('%', :filter, '%'))
                        OR (e.employeeName2 IS NOT NULL AND LOWER(e.employeeName2) LIKE LOWER(CONCAT('%', :filter, '%')))
                        OR (e.mobile IS NOT NULL AND LOWER(e.mobile) LIKE LOWER(CONCAT('%', :filter, '%')))
                        OR (d.deptPoid IS NOT NULL AND LOWER(d.deptName) LIKE LOWER(CONCAT('%', :filter, '%')))
                        OR (des.desigPoid IS NOT NULL AND LOWER(des.designationName) LIKE LOWER(CONCAT('%', :filter, '%')))
                        OR (loc.locationPoid IS NOT NULL AND (
                              LOWER(loc.locationName) LIKE LOWER(CONCAT('%', :filter, '%'))
                              OR (loc.locationName2 IS NOT NULL AND LOWER(loc.locationName2) LIKE LOWER(CONCAT('%', :filter, '%')))
                        ))
                        OR (e.joinDate IS NOT NULL AND (
                              LOWER(FUNCTION('TO_CHAR', e.joinDate, 'DD-Mon-YYYY')) LIKE LOWER(CONCAT('%', :filter, '%'))
                              OR LOWER(FUNCTION('TO_CHAR', e.joinDate, 'YYYY-MM-DD')) LIKE LOWER(CONCAT('%', :filter, '%'))
                              OR LOWER(FUNCTION('TO_CHAR', e.joinDate, 'DD/MM/YYYY')) LIKE LOWER(CONCAT('%', :filter, '%'))
                        ))
                        OR (e.active IS NOT NULL AND LOWER(e.active) LIKE LOWER(CONCAT('%', :filter, '%')))
                        OR LOWER(CASE
                              WHEN UPPER(TRIM(COALESCE(e.active, ''))) = 'Y' THEN 'active'
                              WHEN UPPER(TRIM(COALESCE(e.active, ''))) = 'N' THEN 'inactive'
                              ELSE COALESCE(e.active, '')
                        END) LIKE LOWER(CONCAT('%', :filter, '%'))
                  ))
            """)
    Page<EmployeeDashboardDetailsDto> searchEmployeeDashboardDetails(@Param("designationPoid") Long designationPoid,
                                                                     @Param("locationPoid") Long locationPoid,
                                                                     @Param("departmentPoid") Long departmentPoid,
                                                                     @Param("joinDateFrom") LocalDate joinDateFrom,
                                                                     @Param("joinDateTo") LocalDate joinDateTo,
                                                                     @Param("status") String status,
                                                                     @Param("filter") String filter,
                                                                     Pageable pageable);
}