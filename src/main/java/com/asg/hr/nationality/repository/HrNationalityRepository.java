package com.asg.hr.nationality.repository;

import com.asg.hr.nationality.entity.HrNationalityMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HrNationalityRepository extends JpaRepository<HrNationalityMaster, Long> {

    @Query("SELECT COUNT(n) > 0 FROM HrNationalityMaster n WHERE n.nationalityDescription = :nationalityDescription AND n.nationPoid != :excludeNationPoid")
    boolean existsByNationalityDescriptionExcluding(
            @Param("nationalityDescription") String nationalityDescription,
            @Param("excludeNationPoid") Long excludeNationPoid);

    boolean existsByNationalityCode(String nationalityCode);

    boolean existsByNationalityDescription(String nationalityDescription);
}
