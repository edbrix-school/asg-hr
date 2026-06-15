package com.asg.hr.employeetraining.repository;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@Slf4j
@RequiredArgsConstructor
public class EmployeeTrainingProcedureRepositoryImpl implements EmployeeTrainingProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<LovGetListDto> getEmployeeLovByIds(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return List.of();
        }

        String placeholders = employeeIds.stream().map(id -> "?").collect(Collectors.joining(","));
        final String sql = """
                SELECT EMPLOYEE_POID AS POID,
                       EMPLOYEE_CODE AS CODE,
                       DISPLAY_NAME || '(' || EMPLOYEE_NAME || ' ' || EMPLOYEE_NAME2 || ')' AS DESCRIPTION
                  FROM HR_EMPLOYEE_MASTER
                 WHERE EMPLOYEE_POID IN (%s)
                   AND NVL(ACTIVE, 'Y') = 'Y'
                 ORDER BY SEQNO
                """.formatted(placeholders);

        return jdbcTemplate.query(sql, lovMapper(), employeeIds.toArray());
    }

    @Override
    public LovGetListDto getTrainingTypeByCode(String trainingTypeCode) {
        if (trainingTypeCode == null || trainingTypeCode.isBlank()) {
            return null;
        }
        final String sql = """
                SELECT POID, CODE, DESCRIPTION
                  FROM (
                        SELECT 1 AS POID, 'INTERNAL ' AS CODE, 'Internal ' AS DESCRIPTION FROM DUAL
                        UNION
                        SELECT 2 AS POID, 'EXTERNAL' AS CODE, 'External' AS DESCRIPTION FROM DUAL
                       )
                 WHERE UPPER(TRIM(CODE)) = UPPER(TRIM(?))
                """;
        List<LovGetListDto> matches = jdbcTemplate.query(sql, lovMapper(), trainingTypeCode);
        return matches.isEmpty() ? null : matches.getFirst();
    }

    @Override
    public List<LovGetListDto> getTrainingStatusByCodes(List<String> trainingStatusCodes) {
        if (trainingStatusCodes == null || trainingStatusCodes.isEmpty()) {
            return List.of();
        }

        List<String> normalized = trainingStatusCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .distinct()
                .toList();

        if (normalized.isEmpty()) {
            return List.of();
        }

        String placeholders = normalized.stream().map(v -> "?").collect(Collectors.joining(","));
        final String sql = """
                SELECT POID, CODE, DESCRIPTION
                  FROM (
                        SELECT 1 AS POID, 'PLANNED ' AS CODE, 'PLANNED' AS DESCRIPTION FROM DUAL
                        UNION
                        SELECT 2 AS POID, 'IN_PROGRESS ' AS CODE, 'IN_PROGRESS' AS DESCRIPTION FROM DUAL
                        UNION
                        SELECT 3 AS POID, 'COMPLETED' AS CODE, 'COMPLETED' AS DESCRIPTION FROM DUAL
                       )
                 WHERE UPPER(TRIM(CODE)) IN (%s)
                """.formatted(placeholders);

        return jdbcTemplate.query(sql, lovMapper(), normalized.toArray());
    }

    private RowMapper<LovGetListDto> lovMapper() {
        return (rs, rowNum) -> {
            Long poid = rs.getLong("POID");
            String code = rs.getString("CODE");
            String description = rs.getString("DESCRIPTION");
            Integer seqNo = 0;
            try {
                seqNo = rs.getInt("SEQNO");
            } catch (Exception ignored) {
                // Some LOV queries do not provide SEQNO
            }
            return new LovGetListDto(
                    poid,
                    code,
                    description,
                    poid,
                    description,
                    seqNo,
                    null
            );
        };
    }
}
