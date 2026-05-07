package com.asg.hr.employeeinduction.repository;

import com.asg.hr.employeeinduction.entity.HrEmployeeInductionDtl;
import com.asg.hr.employeeinduction.entity.HrEmployeeInductionDtlId;
import com.asg.hr.employeeinduction.entity.HrEmployeeInductionHdr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class EmployeeInductionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HrEmployeeInductionHdrRepository hdrRepository;

    @Autowired
    private HrEmployeeInductionDtlRepository dtlRepository;

    private HrEmployeeInductionHdr headerEntity;
    private HrEmployeeInductionDtl detailEntity;

    @BeforeEach
    void setUp() {
        // Create header entity
        headerEntity = HrEmployeeInductionHdr.builder()
                .docRef("IND-001")
                .employeePoid(1L)
                .remarks("Test induction")
                .companyPoid(1L)
                .transactionDate(LocalDate.now())
                .build();
        
        headerEntity.setCreatedBy("testuser");
        headerEntity.setCreatedDate(LocalDateTime.now());
        
        headerEntity = entityManager.persistAndFlush(headerEntity);

        // Create detail entity
        detailEntity = HrEmployeeInductionDtl.builder()
                .transactionPoid(headerEntity.getTransactionPoid())
                .detRowId(1L)
                .header(headerEntity)
                .inductionCatgPoid(1L)
                .sheduledDate(LocalDate.now().plusDays(1))
                .status("N")
                .remarks("Test detail")
                .build();
        
        detailEntity.setCreatedBy("testuser");
        detailEntity.setCreatedDate(LocalDateTime.now());
        
        detailEntity = entityManager.persistAndFlush(detailEntity);
    }

    @Test
    void findAllActive_ReturnsActiveRecords() {
        // When
        List<HrEmployeeInductionHdr> result = hdrRepository.findAllActive();

        // Then
        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(h -> "N".equals(h.getDeleted())));
    }

    @Test
    void findByPoidAndNotDeleted_ExistingRecord_ReturnsRecord() {
        // When
        Optional<HrEmployeeInductionHdr> result = hdrRepository.findByPoidAndNotDeleted(headerEntity.getTransactionPoid());

        // Then
        assertTrue(result.isPresent());
        assertEquals(headerEntity.getTransactionPoid(), result.get().getTransactionPoid());
        assertEquals("IND-001", result.get().getDocRef());
    }

    @Test
    void findByPoidAndNotDeleted_DeletedRecord_ReturnsEmpty() {
        // Given
        headerEntity.setDeleted("Y");
        entityManager.persistAndFlush(headerEntity);

        // When
        Optional<HrEmployeeInductionHdr> result = hdrRepository.findByPoidAndNotDeleted(headerEntity.getTransactionPoid());

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void findByEmployeePoidAndNotDeleted_ReturnsEmployeeRecords() {
        // When
        List<HrEmployeeInductionHdr> result = hdrRepository.findByEmployeePoidAndNotDeleted(1L);

        // Then
        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(h -> h.getEmployeePoid().equals(1L)));
        assertTrue(result.stream().allMatch(h -> "N".equals(h.getDeleted())));
    }

    @Test
    void findByDocIdAndNotDeleted_ExistingDocId_ReturnsRecord() {
        // When
        Optional<HrEmployeeInductionHdr> result = hdrRepository.findByDocIdAndNotDeleted("IND-001");

        // Then
        assertTrue(result.isPresent());
        assertEquals("IND-001", result.get().getDocRef());
    }

    @Test
    void findByHdrPoidAndNotDeleted_ReturnsDetailRecords() {
        // When
        List<HrEmployeeInductionDtl> result = dtlRepository.findByHdrPoidAndNotDeleted(headerEntity.getTransactionPoid());

        // Then
        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(d -> d.getTransactionPoid().equals(headerEntity.getTransactionPoid())));
    }

    @Test
    void findOverdueInductions_ReturnsOverdueRecords() {
        // Given - Create an overdue induction
        HrEmployeeInductionDtl overdueDetail = HrEmployeeInductionDtl.builder()
                .transactionPoid(headerEntity.getTransactionPoid())
                .detRowId(2L)
                .header(headerEntity)
                .inductionCatgPoid(2L)
                .sheduledDate(LocalDate.now().minusDays(1)) // Past date
                .status("N") // Not completed
                .remarks("Overdue detail")
                .build();
        
        overdueDetail.setCreatedBy("testuser");
        overdueDetail.setCreatedDate(LocalDateTime.now());
        entityManager.persistAndFlush(overdueDetail);

        // When
        List<HrEmployeeInductionDtl> result = dtlRepository.findOverdueInductions(LocalDate.now());

        // Then
        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(d -> d.getSheduledDate().isBefore(LocalDate.now())));
        assertTrue(result.stream().allMatch(d -> "N".equals(d.getStatus())));
    }

    @Test
    void save_HeaderEntity_Success() {
        // Given
        HrEmployeeInductionHdr newHeader = HrEmployeeInductionHdr.builder()
                .docRef("IND-002")
                .employeePoid(2L)
                .remarks("New test induction")
                .companyPoid(1L)
                .transactionDate(LocalDate.now())
                .build();
        
        newHeader.setCreatedBy("testuser");
        newHeader.setCreatedDate(LocalDateTime.now());

        // When
        HrEmployeeInductionHdr saved = hdrRepository.save(newHeader);

        // Then
        assertNotNull(saved.getTransactionPoid());
        assertEquals("IND-002", saved.getDocRef());
        assertEquals(2L, saved.getEmployeePoid());
    }

    @Test
    void save_DetailEntity_Success() {
        // Given
        HrEmployeeInductionDtl newDetail = HrEmployeeInductionDtl.builder()
                .transactionPoid(headerEntity.getTransactionPoid())
                .detRowId(3L)
                .header(headerEntity)
                .inductionCatgPoid(3L)
                .sheduledDate(LocalDate.now().plusDays(2))
                .status("N")
                .remarks("New detail")
                .build();
        
        newDetail.setCreatedBy("testuser");
        newDetail.setCreatedDate(LocalDateTime.now());

        // When
        HrEmployeeInductionDtl saved = dtlRepository.save(newDetail);

        // Then
        assertNotNull(saved.getTransactionPoid());
        assertEquals(3L, saved.getInductionCatgPoid());
        assertEquals(3L, saved.getDetRowId());
    }

    @Test
    void delete_SoftDelete_Success() {
        // Given
        Long poidToDelete = headerEntity.getTransactionPoid();

        // When
        headerEntity.setDeleted("Y");
        hdrRepository.save(headerEntity);

        // Then
        Optional<HrEmployeeInductionHdr> result = hdrRepository.findByPoidAndNotDeleted(poidToDelete);
        assertFalse(result.isPresent());
        
        // But the record still exists in database
        Optional<HrEmployeeInductionHdr> directResult = hdrRepository.findById(poidToDelete);
        assertTrue(directResult.isPresent());
        assertEquals("Y", directResult.get().getDeleted());
    }
}