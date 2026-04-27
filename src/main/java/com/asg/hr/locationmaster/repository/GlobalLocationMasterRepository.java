package com.asg.hr.locationmaster.repository;

import com.asg.hr.locationmaster.entity.GlobalLocationMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlobalLocationMasterRepository extends JpaRepository<GlobalLocationMaster, Long> {

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM GlobalLocationMaster l " +
            "WHERE l.locationCode = :locationCode AND l.companyPoid = :companyPoid AND (l.deleted IS NULL OR l.deleted = '' OR l.deleted = 'N')")
    boolean existsByLocationCodeAndCompanyPoid(@Param("locationCode") String locationCode, @Param("companyPoid") Long companyPoid);
    
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM GlobalLocationMaster l " +
            "WHERE l.locationCode = :locationCode AND l.companyPoid = :companyPoid AND l.locationPoid != :locationPoid AND (l.deleted IS NULL OR l.deleted = '' OR l.deleted = 'N')")
    boolean existsByLocationCodeAndCompanyPoidAndLocationPoidNot(@Param("locationCode") String locationCode, @Param("companyPoid") Long companyPoid, @Param("locationPoid") Long locationPoid);
    
    @Query("SELECT l FROM GlobalLocationMaster l WHERE l.locationPoid = :locationPoid AND l.companyPoid = :companyPoid AND (l.deleted IS NULL OR l.deleted = '' OR l.deleted = 'N')")
    Optional<GlobalLocationMaster> findByIdAndCompanyPoidAndNotDeleted(@Param("locationPoid") Long locationPoid, @Param("companyPoid") Long companyPoid);

}