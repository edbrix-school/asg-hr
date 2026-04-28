package com.asg.hr.designation.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.model.CustomAuthDetails;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.designation.dto.DesignationRequest;
import com.asg.hr.designation.dto.DesignationResponse;
import com.asg.hr.designation.entity.HrDesignationMaster;
import com.asg.hr.designation.repository.DesignationRepository;
import com.asg.hr.exceptions.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DesignationServiceImplTest {

    private static final String DESIGNATION_POID = "DESIG_POID";

    private RepositoryStub repository;
    private FakeLoggingService loggingService;
    private FakeDocumentDeleteService documentDeleteService;
    private FakeDocumentSearchService documentSearchService;
    private DesignationServiceImpl service;

    @BeforeEach
    void setUp() {
        UserContext.clear();
        UserContext.setCurrentUser(userContext("DOC123", 1L));

        repository = new RepositoryStub();
        loggingService = new FakeLoggingService();
        documentDeleteService = new FakeDocumentDeleteService();
        documentSearchService = new FakeDocumentSearchService();
        service = new DesignationServiceImpl(
                repository.proxy(),
                loggingService,
                documentDeleteService,
                documentSearchService
        );
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void getDesignationById_Success() {
        HrDesignationMaster entity = createMockEntity();
        repository.seed(entity);

        DesignationResponse result = service.getDesignationById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getDesignationPoid());
        assertEquals("DEV001", result.getDesignationCode());
        assertEquals("Developer", result.getDesignationName());
        assertThat(repository.findByIdCount).isEqualTo(1);
    }

    @Test
    void getDesignationById_NotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.getDesignationById(1L));
        assertThat(repository.findByIdCount).isEqualTo(1);
    }

    @Test
    void createDesignation_Success() {
        DesignationRequest request = createMockRequest();
        repository.existsByCodeAndPoidNot = false;
        repository.existsByNameAndPoidNot = false;
        repository.nextSaveId = 1L;

        Long result = service.createDesignation(request);

        assertEquals(1L, result);
        assertThat(repository.saveCount).isEqualTo(1);
        assertThat(repository.lastSaved.getGroupPoid()).isEqualTo(1L);
        assertThat(repository.lastSaved.getDeleted()).isEqualTo("N");
        assertThat(loggingService.summaryEntries).hasSize(1);
        assertThat(loggingService.summaryEntries.get(0).details).isEqualTo(LogDetailsEnum.CREATED);
        assertThat(loggingService.summaryEntries.get(0).documentId).isEqualTo("DOC123");
        assertThat(loggingService.summaryEntries.get(0).id).isEqualTo("1");
    }

    @Test
    void createDesignation_DuplicateCode_ThrowsDuplicateKeyException() {
        DesignationRequest request = createMockRequest();
        repository.existsByCodeAndPoidNot = true;

        DuplicateKeyException exception = assertThrows(DuplicateKeyException.class,
                () -> service.createDesignation(request));

        assertEquals("Designation Code already exists: DEV001", exception.getMessage());
        assertThat(repository.saveCount).isZero();
    }

    @Test
    void createDesignation_DuplicateName_ThrowsDuplicateKeyException() {
        DesignationRequest request = createMockRequest();
        repository.existsByCodeAndPoidNot = false;
        repository.existsByNameAndPoidNot = true;

        DuplicateKeyException exception = assertThrows(DuplicateKeyException.class,
                () -> service.createDesignation(request));

        assertEquals("Designation Name already exists: Developer", exception.getMessage());
        assertThat(repository.saveCount).isZero();
    }

    @Test
    void createDesignation_NegativeSeqNo_ThrowsValidationException() {
        DesignationRequest request = createMockRequest();
        request.setSeqNo(-1L);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.createDesignation(request));

        assertEquals("Sequence number must be numeric and non-negative", exception.getMessage());
        assertThat(repository.saveCount).isZero();
    }

    @Test
    void updateDesignation_Success() {
        HrDesignationMaster entity = createMockEntity();
        repository.seed(entity);

        DesignationRequest request = createMockRequest();
        request.setDesignationCode("DEV002");
        request.setDesignationName("Senior Developer");
        repository.existsByCodeAndPoidNot = false;
        repository.existsByNameAndPoidNot = false;

        DesignationResponse result = service.updateDesignation(1L, request);

        assertNotNull(result);
        assertEquals("DEV002", result.getDesignationCode());
        assertEquals("Senior Developer", result.getDesignationName());
        assertThat(repository.saveCount).isEqualTo(1);
        assertThat(loggingService.lastLogChanges).isNotNull();
        assertThat(loggingService.lastLogChanges.documentId).isEqualTo("DOC123");
        assertThat(loggingService.lastLogChanges.id).isEqualTo("1");
        assertThat(loggingService.lastLogChanges.detailsEnum).isEqualTo(LogDetailsEnum.MODIFIED);
        assertThat(loggingService.lastLogChanges.fieldName).isEqualTo(DESIGNATION_POID);
    }

    @Test
    void updateDesignation_NotFound() {
        DesignationRequest request = createMockRequest();

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateDesignation(1L, request));
        assertThat(repository.saveCount).isZero();
    }

    @Test
    void deleteDesignation_Success() {
        HrDesignationMaster entity = createMockEntity();
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("No longer required");
        repository.seed(entity);

        assertDoesNotThrow(() -> service.deleteDesignation(1L, deleteReasonDto));
        assertThat(documentDeleteService.lastId).isEqualTo(1L);
        assertThat(documentDeleteService.lastDocumentName).isEqualTo("HR_DESIGNATION_MASTER");
        assertThat(documentDeleteService.lastFieldName).isEqualTo(DESIGNATION_POID);
        assertThat(documentDeleteService.lastReason).isSameAs(deleteReasonDto);
    }

    @Test
    void deleteDesignation_NotFound() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("No longer required");

        assertThrows(ResourceNotFoundException.class, () -> service.deleteDesignation(1L, deleteReasonDto));
        assertThat(documentDeleteService.lastId).isNull();
    }

    @Test
    void listDesignations_Success() {
        FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", new ArrayList<>());
        Pageable pageable = PageRequest.of(0, 10);
        documentSearchService.rawResult = new RawSearchResult(
                List.of(Map.of("DESIG_POID", 1L, "DESIGNATION_NAME", "Developer")),
                Map.of("DESIG_POID", "Designation ID", "DESIGNATION_NAME", "Designation Name"),
                1L
        );

        Map<String, Object> result = service.listDesignations(filterRequest, pageable);

        assertNotNull(result);
        assertThat(result).containsKey("content");
        assertThat(documentSearchService.lastDocId).isEqualTo("DOC123");
        assertThat(documentSearchService.lastOperator).isEqualTo("AND");
        assertThat(documentSearchService.lastIsDeleted).isEqualTo("N");
        assertThat(documentSearchService.lastPageable).isEqualTo(pageable);
        assertThat(documentSearchService.lastDateField).isEqualTo("DESIGNATION_NAME");
        assertThat(documentSearchService.lastPoidField).isEqualTo(DESIGNATION_POID);
    }

    private static HrDesignationMaster createMockEntity() {
        HrDesignationMaster entity = new HrDesignationMaster();
        entity.setDesignationPoid(1L);
        entity.setGroupPoid(1L);
        entity.setDesignationCode("DEV001");
        entity.setDesignationName("Developer");
        entity.setJobDescription("<p>Writes code</p>");
        entity.setSkillDescription("Java, Spring");
        entity.setReportingToPoid("MGR001");
        entity.setSeqNo(1L);
        entity.setActive("Y");
        entity.setDeleted("N");
        entity.setCreatedBy("admin");
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy("admin");
        entity.setLastModifiedDate(LocalDateTime.now());
        return entity;
    }

    private static DesignationRequest createMockRequest() {
        return DesignationRequest.builder()
                .designationCode("DEV001")
                .designationName("Developer")
                .jobDescription("<p>Writes code</p>")
                .skillDescription("Java, Spring")
                .reportingToPoid("MGR001")
                .seqNo(1L)
                .active("Y")
                .build();
    }

    private static CustomAuthDetails userContext(String documentId, Long groupPoid) {
        CustomAuthDetails details = new CustomAuthDetails();
        details.setDocumentId(documentId);
        details.setGroupPoid(groupPoid);
        return details;
    }

    private static final class RepositoryStub {
        private final Map<Long, HrDesignationMaster> store = new HashMap<>();
        private long nextSaveId = 1L;
        private int saveCount;
        private int findByIdCount;
        private boolean existsByCodeAndPoidNot;
        private boolean existsByNameAndPoidNot;
        private HrDesignationMaster lastSaved;

        DesignationRepository proxy() {
            InvocationHandler handler = this::invoke;
            return (DesignationRepository) Proxy.newProxyInstance(
                    DesignationRepository.class.getClassLoader(),
                    new Class<?>[]{DesignationRepository.class},
                    handler
            );
        }

        void seed(HrDesignationMaster entity) {
            store.put(entity.getDesignationPoid(), entity);
        }

        private Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "save" -> save(args);
                case "findById" -> findById(args);
                case "findByDesignationPoidAndDeleted" -> findByDesignationPoidAndDeleted(args);
                case "existsByDesignationCodeIgnoreCase" -> false;
                case "existsByDesignationNameIgnoreCase" -> false;
                case "existsByDesignationCodeIgnoreCaseAndDesignationPoidNot" -> existsByCodeAndPoidNot;
                case "existsByDesignationNameIgnoreCaseAndDesignationPoidNot" -> existsByNameAndPoidNot;
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
            HrDesignationMaster entity = (HrDesignationMaster) args[0];
            if (entity.getDesignationPoid() == null) {
                entity.setDesignationPoid(nextSaveId++);
            }
            store.put(entity.getDesignationPoid(), entity);
            lastSaved = entity;
            return entity;
        }

        private Object findById(Object[] args) {
            findByIdCount++;
            Long id = (Long) args[0];
            return Optional.ofNullable(store.get(id));
        }

        private Object findByDesignationPoidAndDeleted(Object[] args) {
            Long designationPoid = (Long) args[0];
            String deleted = (String) args[1];
            return store.values().stream()
                    .filter(entity -> designationPoid.equals(entity.getDesignationPoid()))
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
                    (HrDesignationMaster) oldObj,
                    (HrDesignationMaster) newObj,
                    clazz,
                    documentId,
                    id,
                    details,
                    fieldName
            );
        }
    }

    private static final class LogChangesCall {
        private final HrDesignationMaster oldEntity;
        private final HrDesignationMaster newEntity;
        private final Class<?> entityClass;
        private final String documentId;
        private final String id;
        private final LogDetailsEnum detailsEnum;
        private final String fieldName;

        private LogChangesCall(HrDesignationMaster oldEntity, HrDesignationMaster newEntity, Class<?> entityClass,
                               String documentId, String id, LogDetailsEnum detailsEnum, String fieldName) {
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

    private static final class FakeDocumentDeleteService extends DocumentDeleteService {
        private Long lastId;
        private String lastDocumentName;
        private String lastFieldName;
        private DeleteReasonDto lastReason;

        @Override
        public String deleteDocument(Long id, String documentName, String fieldName, DeleteReasonDto reason, java.time.LocalDate date) {
            lastId = id;
            lastDocumentName = documentName;
            lastFieldName = fieldName;
            lastReason = reason;
            return "SUCCESS";
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
}
