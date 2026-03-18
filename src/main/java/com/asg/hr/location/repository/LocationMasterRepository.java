package com.asg.hr.location.repository;

import com.asg.hr.location.entity.LocationMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationMasterRepository extends JpaRepository<LocationMasterEntity, Long> {

    @Query("SELECT e FROM LocationMasterEntity e WHERE e.locationPoid = :id AND (e.deleted IS NULL OR e.deleted = '' OR e.deleted = 'N')")
    Optional<LocationMasterEntity> findByIdAndNotDeleted(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM LocationMasterEntity e " +
            "WHERE e.locationCode = :code AND (e.deleted IS NULL OR e.deleted = '' OR e.deleted = 'N')")
    boolean existsByLocationCode(@Param("code") String code);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM LocationMasterEntity e " +
            "WHERE e.locationCode = :code AND e.locationPoid != :id AND (e.deleted IS NULL OR e.deleted = '' OR e.deleted = 'N')")
    boolean existsByLocationCodeAndIdNot(@Param("code") String code, @Param("id") Long id);
}