package com.asg.hr.attendancemonthly.service.impl;

import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.AsgException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.attendancemonthly.dto.*;
import com.asg.hr.attendancemonthly.entity.HrAttendanceMonthlyDtl;
import com.asg.hr.attendancemonthly.entity.HrAttendanceMonthlyHdr;
import com.asg.hr.attendancemonthly.entity.key.HrAttendanceMonthlyDtlKey;
import com.asg.hr.attendancemonthly.mapper.HrAttendanceMonthlyMapper;
import com.asg.hr.attendancemonthly.repository.HrAttendanceMonthlyDtlRepository;
import com.asg.hr.attendancemonthly.repository.HrAttendanceMonthlyHdrRepository;
import com.asg.hr.attendancemonthly.repository.HrAttendanceMonthlyProcRepository;
import com.asg.hr.attendancemonthly.service.HrAttendanceMonthlyService;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.hr.exceptions.ResourceAlreadyExistsException;
import com.asg.hr.exceptions.ResourceNotFoundException;
import com.asg.hr.exceptions.ValidationException;
import com.asg.common.lib.service.GlobalParameterService;
import com.asg.common.lib.service.LovDataService;
import org.springframework.beans.BeanUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleTypes;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class HrAttendanceMonthlyServiceImpl implements HrAttendanceMonthlyService {

    private final HrAttendanceMonthlyHdrRepository hdrRepository;
    private final HrAttendanceMonthlyDtlRepository dtlRepository;
    private final HrAttendanceMonthlyProcRepository procRepository;
    private final HrAttendanceMonthlyMapper mapper;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    private final GlobalParameterService globalParameterService;
    private final JdbcTemplate jdbcTemplate;
    private final LovDataService lovDataService;

    private static final String LOV_EMPLOYEE_NAME = "EMPLOYEE_NAME";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public HrAttendanceMonthlyResponse saveAttendance(HrAttendanceMonthlyRequest request) {

        validatePeriod(request);
        HrAttendanceMonthlyHdr hdr = mapper.toEntity(request);
        hdr.setCompanyPoid(String.valueOf(UserContext.getCompanyPoid()));
        HrAttendanceMonthlyHdr savedHdr = hdrRepository.save(hdr);
        Long transactionPoid = savedHdr.getTransactionPoid();

        entityManager.flush();
        entityManager.refresh(savedHdr);

        loggingService.createLogSummaryEntry(
                UserContext.getDocumentId(),
                transactionPoid.toString(),
                String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), savedHdr.getDocRef())
        );
        return mapper.toResponse(savedHdr);
    }

    @Override
    @Transactional
    public HrAttendanceMonthlyResponse updateAttendance(Long transactionPoid, HrAttendanceMonthlyUpdateRequest request) {
        HrAttendanceMonthlyHdr hdr = getHrAttendanceMonthlyHdr(transactionPoid);
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        if ("Y".equalsIgnoreCase(hdr.getLoadedPayroll())) {
            throw new ValidationException("Cannot update record because payroll has already been processed for this period.");
        }

        HrAttendanceMonthlyHdr oldCopy = snapshot(hdr);
        mapper.updateEntity(hdr, request);
        final HrAttendanceMonthlyHdr savedHdr = hdrRepository.saveAndFlush(hdr);

        List<LogRequestDto<HrAttendanceMonthlyDtl>> detailLogRequests = new ArrayList<>();
        if (request.getDetails() != null) {
            entityManager.flush();
            entityManager.clear();
            for (HrAttendanceMonthlyDtlUpdateRequest dtlReq : request.getDetails()) {
                if ("ISDELETED".equalsIgnoreCase(dtlReq.getActionType())) {
                    dtlRepository.deleteById(new HrAttendanceMonthlyDtlKey(dtlReq.getDetRowId(), transactionPoid));
                } else if ("ISCREATED".equalsIgnoreCase(dtlReq.getActionType())) {
                    HrAttendanceMonthlyDtl dtl = mapper.toDtlEntity(dtlReq);
                    dtl.setTransactionPoid(transactionPoid);
                    long nextDetRowId = dtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid) + 1;
                    dtl.setDetRowId(nextDetRowId);
                    dtlRepository.save(dtl);
                } else if ("ISUPDATED".equalsIgnoreCase(dtlReq.getActionType())) {
                    dtlRepository.findById(new HrAttendanceMonthlyDtlKey(dtlReq.getDetRowId(), transactionPoid))
                            .ifPresent(dtl -> {
                                HrAttendanceMonthlyDtl oldDtl = snapshotDtl(dtl);
                                mapper.updateDtlEntity(dtl, dtlReq);
                                dtlRepository.saveAndFlush(dtl);
                                String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, dtlReq.getDetRowId());
                                detailLogRequests.add(new LogRequestDto<>(oldDtl, dtl, HrAttendanceMonthlyDtl.class, docId, docKeyPoid, logDetail));
                            });
                }
            }
        }

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, docId, docKeyPoid);
        loggingService.logDetails(oldCopy, savedHdr, HrAttendanceMonthlyHdr.class, docId, docKeyPoid, "TRANSACTION_POID");
        if (!detailLogRequests.isEmpty()) {
            loggingService.createLogBatch(detailLogRequests);
        }

        List<HrAttendanceMonthlyDtl> updatedDetails = getHrAttendanceMonthlyDtls(transactionPoid);
        HrAttendanceMonthlyResponse response = mapper.toResponse(savedHdr);
        List<HrAttendanceMonthlyDtlResponse> dtlResponses = mapper.toDtlResponseList(updatedDetails);
        enrichEmployeeNames(dtlResponses);
        response.setDetails(dtlResponses);
        return response;
    }

    @Override
    @Transactional
    public HrAttendanceMonthlyLoadAttendanceDto loadAndProcessAttendance(Long transactionPoid, String lateDeductionCheck) {
        try {
            HrAttendanceMonthlyHdr hdr = getHrAttendanceMonthlyHdr(transactionPoid);

            if ("Y".equalsIgnoreCase(hdr.getLoadedPayroll())) {
                throw new ValidationException("Cannot process attendance because payroll has already been processed for this period.");
            }

            String string =   procRepository.loadAttendance(
                      transactionPoid,
                      UserContext.getUserPoid(),
                      hdr.getAttendanceFrom(),
                      hdr.getAttendanceTo(),
                      hdr.getEmployeePoid(),
                      lateDeductionCheck != null ? lateDeductionCheck : "Y"
              );

            List<HrAttendanceMonthlyDtl> attendanceMonthlyDtls = getHrAttendanceMonthlyDtls(transactionPoid);
            List<HrAttendanceMonthlyDtlResponse> dtlResponses = mapper.toDtlResponseList(attendanceMonthlyDtls);
            List<Long> empPoids = dtlResponses.stream().map(HrAttendanceMonthlyDtlResponse::getEmployeePoid).filter(Objects::nonNull).distinct().toList();
            Map<Long, LovGetListDto> empMap = lovDataService.getDetailsByPoidsAndLovName(empPoids, LOV_EMPLOYEE_NAME);
            dtlResponses.forEach(dtl -> dtl.setEmpDtl(empMap.get(dtl.getEmployeePoid())));
            return HrAttendanceMonthlyLoadAttendanceDto.builder()
                    .attendanceDetails(dtlResponses)
                    .build();
        } catch (ValidationException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public void unloadAttendance(Long transactionPoid) {
        HrAttendanceMonthlyHdr hdr = getHrAttendanceMonthlyHdr(transactionPoid);

        if ("Y".equalsIgnoreCase(hdr.getLoadedPayroll())) {
            throw new ValidationException("Cannot unload attendance because payroll has already been processed for this period.");
        }

        procRepository.unloadAttendance(transactionPoid);
        
        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, UserContext.getDocumentId(), transactionPoid.toString());
    }

    private void validatePeriod(HrAttendanceMonthlyRequest request) {
        if (request.getAttendanceFrom().isAfter(request.getAttendanceTo())) {
            throw new ValidationException("Attendance From date cannot be after Attendance To date.");
        }

        if (hdrRepository.existsOverlappingPeriod(request.getAttendanceFrom(), request.getAttendanceTo())) {
            throw new ResourceAlreadyExistsException("Attendance record", "overlapping date range");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public HrAttendanceMonthlyResponse getAttendanceSummary(Long transactionPoid) {
        HrAttendanceMonthlyHdr hdr = getHrAttendanceMonthlyHdr(transactionPoid);
        List<HrAttendanceMonthlyDtl> details = getHrAttendanceMonthlyDtls(transactionPoid);
        HrAttendanceMonthlyResponse response = mapper.toResponse(hdr);
        List<HrAttendanceMonthlyDtlResponse> dtlResponses = mapper.toDtlResponseList(details);
        enrichEmployeeNames(dtlResponses);
        response.setDetails(dtlResponses);
        return response;
    }

    private List<HrAttendanceMonthlyDtl> getHrAttendanceMonthlyDtls(Long transactionPoid) {
        return dtlRepository.findByTransactionPoid(transactionPoid);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAllAttendanceWithFilters(String documentId, FilterRequestDto filterRequestDto, Pageable pageable) {
        String operator = documentSearchService.resolveOperator(filterRequestDto);
        String isDeleted = documentSearchService.resolveIsDeleted(filterRequestDto);
        List<FilterDto> filters = documentSearchService.resolveFilters(filterRequestDto);

        RawSearchResult raw = documentSearchService.search(documentId, filters, operator, pageable, isDeleted, "ATTENDANCE_DESCRIPTION", "TRANSACTION_POID");
        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    public void deleteAttendance(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        HrAttendanceMonthlyHdr hdr = getHrAttendanceMonthlyHdr(transactionPoid);

        if ("Y".equalsIgnoreCase(hdr.getLoadedPayroll())) {
            throw new ValidationException("Cannot delete record because payroll has already been processed for this period.");
        }

        documentDeleteService.deleteDocument(
                transactionPoid,
                "HR_ATTENDANCE_MONTHLY_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                hdr.getTransactionDate()
        );


    }

    private void enrichEmployeeNames(List<HrAttendanceMonthlyDtlResponse> dtlResponses) {
        if (dtlResponses == null || dtlResponses.isEmpty()) return;
        List<Long> poids = dtlResponses.stream()
                .map(HrAttendanceMonthlyDtlResponse::getEmployeePoid)
                .filter(p -> p != null)
                .distinct()
                .toList();
        if (poids.isEmpty()) return;
        Map<Long, LovGetListDto> empMap = lovDataService.getDetailsByPoidsAndLovName(poids, LOV_EMPLOYEE_NAME);
        dtlResponses.forEach(dtl -> {
            if (dtl.getEmployeePoid() != null) {
                LovGetListDto lov = empMap.get(dtl.getEmployeePoid());
                if (lov != null) dtl.setEmployeeName(lov.getDescription());
            }
        });
    }

    private HrAttendanceMonthlyHdr snapshot(HrAttendanceMonthlyHdr hdr) {
        HrAttendanceMonthlyHdr copy = new HrAttendanceMonthlyHdr();
        BeanUtils.copyProperties(hdr, copy);
        return copy;
    }

    private HrAttendanceMonthlyDtl snapshotDtl(HrAttendanceMonthlyDtl dtl) {
        HrAttendanceMonthlyDtl copy = new HrAttendanceMonthlyDtl();
        BeanUtils.copyProperties(dtl, copy);
        return copy;
    }

    private HrAttendanceMonthlyHdr getHrAttendanceMonthlyHdr(Long transactionPoid) {
        return hdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance Record", "transactionPoid", transactionPoid));
    }

    @Override
    public HrAttendanceMonthlyDateParams calculateDateParams(LocalDate fromDate) {
        String crossingMonths = globalParameterService.getParameterValue(
                "Attendance_CrossingMonths", "Company", String.valueOf(UserContext.getCompanyPoid()), "N");

        if ("N".equalsIgnoreCase(crossingMonths)) {
            LocalDate toDate = fromDate.with(TemporalAdjusters.lastDayOfMonth());
            String description = toDate.format(DateTimeFormatter.ofPattern("MMM-yyyy"));
            
            return HrAttendanceMonthlyDateParams.builder()
                    .attendanceTo(toDate)
                    .description(description)
                    .build();
        }

        return HrAttendanceMonthlyDateParams.builder().build();
    }

    @Override
    @Transactional
    public void removeDeductionDaysRecords(Long transactionPoid) {
        HrAttendanceMonthlyHdr hdr = getHrAttendanceMonthlyHdr(transactionPoid);

        /*if ("Y".equalsIgnoreCase(hdr.getLoadedPayroll())) {
            throw new ValidationException("Cannot remove deduction days because payroll has already been processed for this period.");
        }*/

        List<HrAttendanceMonthlyDtl> details = getHrAttendanceMonthlyDtls(transactionPoid);

        details.forEach(dtl -> dtl.setDeductDays(0L));
        dtlRepository.saveAll(details);

        loggingService.createLogSummaryEntry(
                UserContext.getDocumentId(),
                transactionPoid.toString(),
                "Deduct days removed for all.."
        );
    }

    @Override
    @Transactional
    public String uploadOtExcel(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("File is empty");
        }

        String docId = "800-100_1";
        List<ExcelConfig> configs = getExcelConfigs(docId);
        
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            for (ExcelConfig config : configs) {
                jdbcTemplate.update("DELETE FROM " + config.tempTableName);
                
                Sheet sheet = (config.sheetName != null && !config.sheetName.isEmpty()) 
                        ? workbook.getSheet(config.sheetName) 
                        : workbook.getSheetAt(0);
                
                if (sheet == null) {
                    throw new ValidationException("Sheet '" + (config.sheetName != null ? config.sheetName : "0") + "' not found");
                }

                List<List<Object>> rowsCollection = new ArrayList<>();
                for (Row row : sheet) {
                    List<Object> colCollection = new ArrayList<>();
                    for (int cn = config.startColNumber - 1; cn <= config.endColNumber - 1; cn++) {
                        Cell cell = row.getCell(cn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        switch (cell.getCellType()) {
                            case NUMERIC -> colCollection.add(cell.getNumericCellValue());
                            case STRING -> colCollection.add(cell.getStringCellValue());
                            default -> colCollection.add(null);
                        }
                    }
                    rowsCollection.add(colCollection);
                }
                saveImportedData(config.startRowNumber, rowsCollection, config.tempTableName);
            }
        } catch (Exception e) {
            if (e instanceof ValidationException) throw (ValidationException) e;
            throw new AsgException("Error processing Excel file: " + e.getMessage(), e);
        }

        return "Successfully imported Excel data to temp table";
    }

    private List<ExcelConfig> getExcelConfigs(String docId) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("PROC_GLOB_EXCEL_IMPORT_SHEETS")
                .declareParameters(
                        new SqlParameter("P_COMPANY_POID", Types.NUMERIC),
                        new SqlParameter("P_DOC_ID", Types.VARCHAR),
                        new SqlOutParameter("OUTDATA", OracleTypes.CURSOR),
                        new SqlOutParameter("P_STATUS", Types.VARCHAR)
                );

        Map<String, Object> result = jdbcCall.execute(
                new MapSqlParameterSource()
                        .addValue("P_COMPANY_POID", UserContext.getCompanyPoid())
                        .addValue("P_DOC_ID", docId)
        );

        String status = (String) result.get("P_STATUS");
        if (!"SUCCESS".equals(status)) {
            throw new AsgException("Failed to get Excel config: " + status);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> configRows = (List<Map<String, Object>>) result.get("OUTDATA");
        if (configRows == null || configRows.isEmpty()) {
            throw new AsgException("No Excel configuration found for DOC_ID: " + docId);
        }

        List<ExcelConfig> configs = new ArrayList<>();
        for (Map<String, Object> row : configRows) {
            ExcelConfig config = new ExcelConfig();
            config.sheetName = (String) row.get("EXCEL_SHEET_NAME");
            config.startRowNumber = row.get("START_ROW_NUMBER") != null ? ((Number) row.get("START_ROW_NUMBER")).intValue() : 1;
            config.startColNumber = row.get("START_COL_NUMBER") != null ? ((Number) row.get("START_COL_NUMBER")).intValue() : 1;
            config.endColNumber = row.get("END_COL_NUMBER") != null ? ((Number) row.get("END_COL_NUMBER")).intValue() : 1;
            config.tempTableName = (String) row.get("TEMP_TABLE_NAME");
            configs.add(config);
        }
        return configs;
    }

    private void saveImportedData(int startRowNumber, List<List<Object>> rowsCollection, String tempTableName) {
        int rowNum = 0;
        for (List<Object> cols : rowsCollection) {
            rowNum++;
            if (startRowNumber <= rowNum) {
                StringBuilder insertQuery = new StringBuilder("INSERT INTO " + tempTableName + " VALUES (");
                for (Object col : cols) {
                    if (col == null) {
                        insertQuery.append("NULL,");
                    } else {
                        insertQuery.append("'").append(col.toString().replace("'", "''")).append("',");
                    }
                }
                insertQuery.setLength(insertQuery.length() - 1);
                insertQuery.append(")");
                jdbcTemplate.update(insertQuery.toString());
            }
        }
    }

    private static class ExcelConfig {
        String sheetName;
        int startRowNumber;
        int startColNumber;
        int endColNumber;
        String tempTableName;
    }
}
