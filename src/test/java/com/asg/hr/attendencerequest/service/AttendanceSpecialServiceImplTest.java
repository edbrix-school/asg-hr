package com.asg.hr.attendencerequest.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.model.CustomAuthDetails;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.attendencerequest.dto.AttendanceRequestDto;
import com.asg.hr.attendencerequest.dto.AttendanceResponseDto;
import com.asg.hr.attendencerequest.entity.AttendanceEntity;
import com.asg.hr.attendencerequest.repository.AttendanceRepository;
import com.asg.hr.attendencerequest.util.AttendanceMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttendanceSpecialServiceImplTest {

    private static final String ATTENDANCE_DATE_FIELD = "ATTENDANCE_DATE";
    private static final String ATTENDANCE_POID_FIELD = "TRANSACTION_POID";
    private static final String HR_ATTENDANCE_SPECIAL_REQ = "HR_ATTENDANCE_SPECIAL_REQ";

    private RepositoryStub repository;
    private FakeDocumentSearchService documentSearchService;
    private FakeDocumentDeleteService documentDeleteService;
    private FakeLoggingService loggingService;
    private TestJdbcTemplate jdbcTemplate;
    private AttendanceSpecialServiceImpl service;

    @BeforeEach
    void setUp() {
        UserContext.clear();
        UserContext.setCurrentUser(userContext("DOC1", 100L));

        repository = new RepositoryStub();
        documentSearchService = new FakeDocumentSearchService();
        documentDeleteService = new FakeDocumentDeleteService();
        loggingService = new FakeLoggingService();
        jdbcTemplate = new TestJdbcTemplate();
        service = new AttendanceSpecialServiceImpl(
                repository.proxy(),
                documentSearchService,
                documentDeleteService,
                loggingService,
                jdbcTemplate,
                new AttendanceMapper()
        );
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void create_success_setsDefaults_andLogs() {
        AttendanceRequestDto dto = validDto();
        dto.setStatus(null);

        repository.nextSaveId = 99L;

        AttendanceResponseDto resp = service.create(dto);

        assertThat(repository.saveCount).isEqualTo(1);
        assertThat(repository.lastSaved.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(repository.lastSaved.getDeleted()).isEqualTo("N");
        assertThat(repository.lastSaved.getGroupPoid()).isEqualTo(100L);
        assertThat(resp.getAttendancePoid()).isEqualTo(99L);
        assertThat(resp.getEmployeePoid()).isEqualTo(11L);
        assertThat(loggingService.summaryEntries).hasSize(1);
        assertThat(loggingService.summaryEntries.get(0).details).isEqualTo(LogDetailsEnum.CREATED);
        assertThat(loggingService.summaryEntries.get(0).documentId).isEqualTo("DOC1");
        assertThat(loggingService.summaryEntries.get(0).id).isEqualTo("99");
    }

    @Test
    void create_whenSpReturnsError_throwsValidationException() {
        AttendanceRequestDto dto = validDto();
        jdbcTemplate.result = "ERROR: invalid";

        assertThrows(ValidationException.class, () -> service.create(dto));
        assertThat(repository.saveCount).isZero();
        assertThat(loggingService.summaryEntries).isEmpty();
    }

    @Test
    void list_success_delegatesToDocumentSearch_andWraps() {
        FilterDto filter = new FilterDto("EXCEPTION_TYPE", "E2");
        FilterRequestDto request = new FilterRequestDto("AND", "N", List.of(filter));
        Pageable pageable = PageRequest.of(0, 10);
        documentSearchService.rawResult = new RawSearchResult(
                List.of(Map.of(ATTENDANCE_POID_FIELD, 1L)),
                Map.of(ATTENDANCE_DATE_FIELD, "Attendance Date"),
                1L
        );

        Map<String, Object> result = service.list("DOC1", request, pageable);

        assertThat(result).isNotNull();
        assertThat(documentSearchService.lastDocId).isEqualTo("DOC1");
        assertThat(documentSearchService.lastOperator).isEqualTo("AND");
        assertThat(documentSearchService.lastIsDeleted).isEqualTo("N");
        assertThat(documentSearchService.lastPageable).isEqualTo(pageable);
        assertThat(documentSearchService.lastFilters).containsExactly(filter);
    }

    @Test
    void getById_whenMissing_throws() {
        assertThrows(ResourceNotFoundException.class, () -> service.getById(1L));
        assertThat(loggingService.summaryEntries).isEmpty();
    }

    @Test
    void getById_success_mapsAndDoesNotLogViewed() {
        AttendanceEntity entity = entity(1L, 2L, LocalDate.of(2024, 1, 1), "E1", "R1", "H1", "IN_PROGRESS");
        repository.seed(entity);

        AttendanceResponseDto resp = service.getById(1L);

        assertThat(resp.getAttendancePoid()).isEqualTo(1L);
        assertThat(resp.getEmployeePoid()).isEqualTo(2L);
        assertThat(loggingService.summaryEntries).isEmpty();
    }

    @Test
    void update_whenMissing_throws() {
        AttendanceRequestDto dto = validDto();

        assertThrows(ResourceNotFoundException.class, () -> service.update(1L, dto));
        assertThat(repository.saveCount).isZero();
    }

    @Test
    void update_success_preservesStatus_whenDtoStatusNull_andLogsChanges() {
        AttendanceRequestDto dto = validDto();
        dto.setStatus(null);

        AttendanceEntity existing = entity(1L, 10L, LocalDate.of(2024, 1, 1), "E1", "R1", "H1", "IN_PROGRESS");
        repository.seed(existing);

        AttendanceResponseDto resp = service.update(1L, dto);

        assertThat(repository.saveCount).isEqualTo(1);
        assertThat(repository.lastSaved.getEmployeePoid()).isEqualTo(11L);
        assertThat(repository.lastSaved.getExceptionType()).isEqualTo("E2");
        assertThat(repository.lastSaved.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(loggingService.lastLogChanges).isNotNull();
        assertThat(loggingService.lastLogChanges.documentId).isEqualTo("DOC1");
        assertThat(loggingService.lastLogChanges.id).isEqualTo("1");
        assertThat(loggingService.lastLogChanges.detailsEnum).isEqualTo(LogDetailsEnum.MODIFIED);
        assertThat(loggingService.lastLogChanges.fieldName).isEqualTo(ATTENDANCE_POID_FIELD);
        assertThat(loggingService.lastLogChanges.oldEntity.getEmployeePoid()).isEqualTo(10L);
        assertThat(loggingService.lastLogChanges.newEntity.getEmployeePoid()).isEqualTo(11L);
        assertThat(resp.getReason()).isEqualTo("R2");
    }

    @Test
    void update_whenSpReturnsError_throwsValidationException() {
        AttendanceRequestDto dto = validDto();
        jdbcTemplate.result = "ERROR: invalid";

        assertThrows(ValidationException.class, () -> service.update(1L, dto));
        assertThat(repository.saveCount).isZero();
        assertThat(repository.findByIdCount).isZero();
    }

    @Test
    void delete_whenMissing_throws() {
        DeleteReasonDto reason = new DeleteReasonDto();

        assertThrows(ResourceNotFoundException.class, () -> service.delete(7L, reason));
        assertThat(documentDeleteService.lastId).isNull();
    }

    @Test
    void delete_success_callsDocumentDeleteService() {
        DeleteReasonDto reason = new DeleteReasonDto();
        reason.setDeleteReason("no longer needed");
        repository.seed(entity(7L, 2L, LocalDate.of(2024, 1, 1), "E1", "R1", "H1", "IN_PROGRESS"));

        service.delete(7L, reason);

        assertThat(documentDeleteService.lastId).isEqualTo(7L);
        assertThat(documentDeleteService.lastDocumentName).isEqualTo(HR_ATTENDANCE_SPECIAL_REQ);
        assertThat(documentDeleteService.lastFieldName).isEqualTo(ATTENDANCE_POID_FIELD);
        assertThat(documentDeleteService.lastReason).isSameAs(reason);
        assertThat(documentDeleteService.lastDate).isNull();
    }

    private static AttendanceRequestDto validDto() {
        AttendanceRequestDto dto = new AttendanceRequestDto();
        dto.setEmployeePoid(11L);
        dto.setAttendanceDate(LocalDate.of(2024, 2, 2));
        dto.setExceptionType("E2");
        dto.setReason("R2");
        dto.setHodRemarks("H2");
        return dto;
    }

    private static AttendanceEntity entity(Long id, Long employeePoid, LocalDate date, String exceptionType, String reason,
                                            String hodRemarks, String status) {
        return AttendanceEntity.builder()
                .attendancePoid(id)
                .employeePoid(employeePoid)
                .attendanceDate(date)
                .exceptionType(exceptionType)
                .reason(reason)
                .hodRemarks(hodRemarks)
                .status(status)
                .groupPoid(100L)
                .deleted("N")
                .build();
    }

    private static CustomAuthDetails userContext(String documentId, Long groupPoid) {
        CustomAuthDetails details = new CustomAuthDetails();
        details.setDocumentId(documentId);
        details.setGroupPoid(groupPoid);
        return details;
    }

    private static final class RepositoryStub {
        private final Map<Long, AttendanceEntity> store = new HashMap<>();
        private long nextSaveId = 1L;
        private int saveCount;
        private int findByIdCount;
        private AttendanceEntity lastSaved;

        AttendanceRepository proxy() {
            InvocationHandler handler = this::invoke;
            return (AttendanceRepository) Proxy.newProxyInstance(
                    AttendanceRepository.class.getClassLoader(),
                    new Class<?>[]{AttendanceRepository.class},
                    handler
            );
        }

        void seed(AttendanceEntity entity) {
            store.put(entity.getAttendancePoid(), entity);
        }

        private Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "save" -> save(args);
                case "findById" -> findById(args);
                case "findByAttendancePoidAndGroupPoidAndDeleted" -> findByAttendancePoidAndGroupPoidAndDeleted(args);
                case "count" -> (long) store.size();
                case "existsById" -> args != null && args.length > 0 && store.containsKey(args[0]);
                case "deleteById" -> {
                    store.remove(args[0]);
                    yield null;
                }
                case "findAll" -> new ArrayList<>(store.values());
                case "toString" -> "RepositoryStub";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultReturn(method.getReturnType());
            };
        }

        private Object save(Object[] args) {
            saveCount++;
            AttendanceEntity entity = (AttendanceEntity) args[0];
            if (entity.getAttendancePoid() == null) {
                entity.setAttendancePoid(nextSaveId++);
            }
            store.put(entity.getAttendancePoid(), entity);
            lastSaved = entity;
            return entity;
        }

        private Object findById(Object[] args) {
            findByIdCount++;
            Long id = (Long) args[0];
            return Optional.ofNullable(store.get(id));
        }

        private Object findByAttendancePoidAndGroupPoidAndDeleted(Object[] args) {
            Long attendancePoid = (Long) args[0];
            Long groupPoid = (Long) args[1];
            String deleted = (String) args[2];
            return store.values().stream()
                    .filter(entity -> attendancePoid.equals(entity.getAttendancePoid()))
                    .filter(entity -> groupPoid.equals(entity.getGroupPoid()))
                    .filter(entity -> deleted.equals(entity.getDeleted()))
                    .findFirst();
        }

        private Object defaultReturn(Class<?> returnType) {
            if (returnType == void.class) {
                return null;
            }
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == byte.class) {
                return (byte) 0;
            }
            if (returnType == short.class) {
                return (short) 0;
            }
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == long.class) {
                return 0L;
            }
            if (returnType == float.class) {
                return 0f;
            }
            if (returnType == double.class) {
                return 0d;
            }
            if (returnType == char.class) {
                return '\0';
            }
            return null;
        }
    }

    private static final class FakeDocumentSearchService extends DocumentSearchService {
        private String lastDocId;
        private List<FilterDto> lastFilters;
        private String lastOperator;
        private Pageable lastPageable;
        private String lastIsDeleted;
        private String lastDateField;
        private String lastPoidField;
        private RawSearchResult rawResult = new RawSearchResult(List.of(), Map.of(), 0L);

        @Override
        public String resolveOperator(FilterRequestDto request) {
            return request.operator();
        }

        @Override
        public String resolveIsDeleted(FilterRequestDto request) {
            return request.isDeleted();
        }

        @Override
        public List<FilterDto> resolveFilters(FilterRequestDto request) {
            return request.filters();
        }

        @Override
        public RawSearchResult search(String docId, List<FilterDto> filters, String operator, Pageable pageable,
                                      String isDeleted, String dateField, String poidField) {
            lastDocId = docId;
            lastFilters = filters;
            lastOperator = operator;
            lastPageable = pageable;
            lastIsDeleted = isDeleted;
            lastDateField = dateField;
            lastPoidField = poidField;
            return rawResult;
        }
    }

    private static final class FakeDocumentDeleteService extends DocumentDeleteService {
        private Long lastId;
        private String lastDocumentName;
        private String lastFieldName;
        private DeleteReasonDto lastReason;
        private LocalDate lastDate;

        @Override
        public String deleteDocument(Long id, String documentName, String fieldName, DeleteReasonDto reason, LocalDate date) {
            lastId = id;
            lastDocumentName = documentName;
            lastFieldName = fieldName;
            lastReason = reason;
            lastDate = date;
            return "SUCCESS";
        }
    }

    private static final class FakeLoggingService extends LoggingService {
        private final List<SummaryEntry> summaryEntries = new ArrayList<>();
        private LogChangesCall lastLogChanges;

        @Override
        public void createLogSummaryEntry(LogDetailsEnum details, String documentId, String id) {
            summaryEntries.add(new SummaryEntry(details, documentId, id));
        }

        @Override
        public <T> void logChanges(T oldObj, T newObj, Class<T> clazz, String documentId, String id,
                                   LogDetailsEnum details, String fieldName) {
            lastLogChanges = new LogChangesCall(
                    (AttendanceEntity) oldObj,
                    (AttendanceEntity) newObj,
                    clazz,
                    documentId,
                    id,
                    details,
                    fieldName
            );
        }
    }

    private static final class LogChangesCall {
        private final AttendanceEntity oldEntity;
        private final AttendanceEntity newEntity;
        private final Class<?> entityClass;
        private final String documentId;
        private final String id;
        private final LogDetailsEnum detailsEnum;
        private final String fieldName;

        private LogChangesCall(AttendanceEntity oldEntity, AttendanceEntity newEntity, Class<?> entityClass, String documentId,
                               String id, LogDetailsEnum detailsEnum, String fieldName) {
            this.oldEntity = oldEntity;
            this.newEntity = newEntity;
            this.entityClass = entityClass;
            this.documentId = documentId;
            this.id = id;
            this.detailsEnum = detailsEnum;
            this.fieldName = fieldName;
        }
    }

    private record SummaryEntry(LogDetailsEnum details, String documentId, String id) {
    }

    private static final class TestJdbcTemplate extends JdbcTemplate {
        private String result = "OK";

        @Override
        public <T> T execute(ConnectionCallback<T> action) {
            @SuppressWarnings("unchecked")
            T value = (T) result;
            return value;
        }
    }
}
