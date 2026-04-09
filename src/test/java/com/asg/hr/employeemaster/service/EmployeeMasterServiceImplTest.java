package com.asg.hr.employeemaster.service;

import com.asg.common.lib.client.ParameterServiceClient;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.hr.airsector.repository.HrAirsectorRepository;
import com.asg.hr.client.GlMasterServiceClient;
import com.asg.hr.common.repository.AdminCrMasterRepository;
import com.asg.hr.common.repository.GlobalFixedVariablesRepository;
import com.asg.hr.common.repository.GlobalShiftMasterRepository;
import com.asg.hr.departmentmaster.repository.HrDepartmentMasterRepository;
import com.asg.hr.designation.repository.HrDesignationMasterRepository;
import com.asg.hr.employeemaster.dto.*;
import com.asg.hr.employeemaster.entity.*;
import com.asg.hr.employeemaster.enums.ActionType;
import com.asg.hr.employeemaster.repository.*;
import com.asg.hr.employeemaster.util.EmployeeMasterMapper;
import com.asg.hr.exceptions.ValidationException;
import com.asg.hr.locationmaster.repository.GlobalLocationMasterRepository;
import com.asg.hr.nationality.repository.HrNationalityRepository;
import com.asg.hr.religion.repository.ReligionRepository;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.mock.web.MockMultipartFile;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeMasterServiceImplTest {

    private static final String DOC_ID = "800-001";
    private static final Long GROUP_POID = 101L;
    private static final Long COMPANY_POID = 202L;
    private static final String USER_ID = "u1";

    private MockedStatic<UserContext> userContextMock;

    @Mock private HrEmployeeMasterRepository masterRepository;
    @Mock private HrEmployeeDependentRepository dependentRepository;
    @Mock private HrEmpDepndtsLmraDtlsRepository lmraRepository;
    @Mock private HrEmployeeDocumentDtlRepository documentRepository;
    @Mock private HrEmployeeExperienceDtlRepository experienceRepository;
    @Mock private HrEmployeeLeaveHistoryRepository leaveHistoryRepository;
    @Mock private HrAirsectorRepository airsectorRepository;
    @Mock private HrDepartmentMasterRepository hrDepartmentMasterRepository;
    @Mock private HrNationalityRepository hrNationalityRepository;
    @Mock private HrDesignationMasterRepository designationRepository;
    @Mock private ReligionRepository religionRepository;
    @Mock private GlMasterServiceClient glMasterServiceClient;
    @Mock private GlobalFixedVariablesRepository globalFixedVariablesRepository;
    @Mock private GlobalShiftMasterRepository globalShiftMasterRepository;
    @Mock private GlobalLocationMasterRepository locationMasterRepository;
    @Mock private AdminCrMasterRepository crMasterRepository;
    @Mock private EmployeeMasterMapper employeeMasterMapper;
    @Mock private ParameterServiceClient parameterServiceClient;
    @Mock private DocumentSearchService documentSearchService;
    @Mock private DocumentDeleteService documentDeleteService;
    @Mock private LoggingService loggingService;
    @Mock private PrintService printService;
    @Mock private DataSource dataSource;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private EmployeeMasterServiceImpl service;

    @BeforeEach
    void setUp() {
        userContextMock = org.mockito.Mockito.mockStatic(UserContext.class);
        userContextMock.when(UserContext::getDocumentId).thenReturn(DOC_ID);
        userContextMock.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);
        userContextMock.when(UserContext::getCompanyPoid).thenReturn(COMPANY_POID);
        userContextMock.when(UserContext::getUserId).thenReturn(USER_ID);

        // Default stubs to keep tests focused on specific branches.
        lenient().when(hrNationalityRepository.existsByNationPoid(anyLong())).thenReturn(true);
        lenient().when(crMasterRepository.existsByCrPoid(anyLong())).thenReturn(true);
        lenient().when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
        lenient().when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("N"));
    }

    @AfterEach
    void tearDown() {
        if (userContextMock != null) userContextMock.close();
    }

    @Nested
    class ListEmployees {
        @Test
        void wrapsSearchResults() {
            FilterRequestDto filters = new FilterRequestDto("AND", "N", Collections.emptyList());
            Pageable pageable = PageRequest.of(0, 5);
            RawSearchResult raw = mock(RawSearchResult.class);

            when(documentSearchService.resolveOperator(filters)).thenReturn("AND");
            when(documentSearchService.resolveIsDeleted(filters)).thenReturn("N");
            when(documentSearchService.resolveFilters(filters)).thenReturn(Collections.emptyList());
            when(documentSearchService.search(eq("X"), anyList(), eq("AND"), eq(pageable), eq("N"), eq("DISPLAY_NAME"), eq("EMPLOYEE_POID")))
                    .thenReturn(raw);
            when(raw.records()).thenReturn(List.of(Map.of("EMPLOYEE_POID", 1L)));
            when(raw.totalRecords()).thenReturn(1L);
            when(raw.displayFields()).thenReturn(Map.of("EMPLOYEE_POID", "EMPLOYEE_POID"));

            Map<String, Object> result = service.listEmployees("X", filters, pageable);

            assertThat(result).isNotNull();
            assertThat(result).containsKeys("content", "totalElements", "totalPages");
        }
    }

    @Nested
    class GetEmployeeById {
        @Test
        void returnsMappedDto() {
            HrEmployeeMaster entity = new HrEmployeeMaster();
            entity.setEmployeePoid(10L);
            EmployeeMasterResponseDto response = EmployeeMasterResponseDto.builder().employeePoid(10L).build();

            when(masterRepository.findByEmployeePoid(10L)).thenReturn(Optional.of(entity));
            when(employeeMasterMapper.toResponseDto(entity)).thenReturn(response);

            EmployeeMasterResponseDto out = service.getEmployeeById(10L);

            assertThat(out.getEmployeePoid()).isEqualTo(10L);
        }

        @Test
        void throwsWhenNotFound() {
            when(masterRepository.findByEmployeePoid(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getEmployeeById(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Employee");
        }
    }

    @Nested
    class CreateEmployee {

        @Test
        void createsEmployeeAndChildrenAndLogs() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            request.setDependentsDetails(List.of(dependentCreated("dep1", null)));
            request.setLmraDetails(List.of(lmraCreated(null)));
            request.setExperienceDetails(List.of(expCreated("ACME", null)));
            request.setDocumentDetails(List.of(docCreated("Passport", null)));

            // Header + parameter rules (skip manual emp code requirement)
            when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("N"));
            // expat rule: make "BAHRAIN_Nationality_Poid" equal to something else, so expat, and ticket fields are required
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));

            // master save
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(77L);
                // Skip PROC_GL_MASTER_CREATION on this test path
                e.setEmpGlPoid(555L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(77L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(77L);
                setEmpGlPoid(555L);
            }}));
            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);

            // child validations
            when(masterRepository.existsByEmployeePoid(77L)).thenReturn(true);
            when(hrNationalityRepository.existsByNationalityCode(anyString())).thenReturn(true);
            when(dependentRepository.existsByName(anyString())).thenReturn(false);
            when(experienceRepository.existsByEmployerIgnoreCase(anyString())).thenReturn(false);

            // child existing lists for nextDetRowId logic
            when(dependentRepository.findByEmployeePoid(77L)).thenReturn(Collections.emptyList());
            when(lmraRepository.findByEmployeePoid(77L)).thenReturn(Collections.emptyList());
            when(experienceRepository.findByEmployeePoid(77L)).thenReturn(Collections.emptyList());
            when(documentRepository.findByEmployeePoid(77L)).thenReturn(Collections.emptyList());

            // child saves
            when(dependentRepository.save(any(HrEmployeeDependentsDtl.class))).thenAnswer(inv -> inv.getArgument(0));
            when(lmraRepository.save(any(HrEmpDepndtsLmraDtls.class))).thenAnswer(inv -> inv.getArgument(0));
            when(experienceRepository.save(any(HrEmployeeExperienceDtl.class))).thenAnswer(inv -> inv.getArgument(0));
            when(documentRepository.save(any(HrEmployeeDocumentDtl.class))).thenAnswer(inv -> inv.getArgument(0));

            EmployeeMasterResponseDto mapped = EmployeeMasterResponseDto.builder().employeePoid(77L).build();
            when(employeeMasterMapper.toResponseDto(any(HrEmployeeMaster.class))).thenReturn(mapped);

            EmployeeMasterResponseDto out = service.createEmployee(request);

            assertThat(out.getEmployeePoid()).isEqualTo(77L);
            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.CREATED), eq(DOC_ID), eq("77"));
            verify(employeeMasterMapper).applyHeaderFields(any(HrEmployeeMaster.class), eq(request));
        }

        @Test
        void throwsWhenDirectSupervisorIsSelfOnUpdateValidationPath() {
            EmployeeMasterRequestDto request = baseValidRequest("REMOTE", 1L);
            request.setHod(12L);
            when(masterRepository.findByEmployeePoid(12L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(12L);
                setEmpGlPoid(1L);
            }}));

            assertThatThrownBy(() -> service.updateEmployee(12L, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("direct supervisor");
        }
    }

    @Nested
    class UpdateEmployee {
        @Test
        void updatesAndLogsChanges() {
            HrEmployeeMaster existing = new HrEmployeeMaster();
            existing.setEmployeePoid(88L);
            existing.setEmpGlPoid(999L);
            existing.setEmployeeName("Old");
            when(masterRepository.findByEmployeePoid(88L)).thenReturn(Optional.of(existing));

            EmployeeMasterRequestDto request = baseValidRequest("REMOTE", 1L);
            when(masterRepository.save(existing)).thenReturn(existing);

            // validate: supervisor exists when set
            request.setHod(5L);
            when(masterRepository.existsByEmployeePoid(5L)).thenReturn(true);

            // disable manual emp code logic (update ignores anyway, but keep consistent)
            when(parameterServiceClient.findParameterValueByName(anyString())).thenReturn(Optional.of("1"));

            // empty child tables, but still validates with null lists
            request.setDependentsDetails(null);
            request.setLmraDetails(null);
            request.setExperienceDetails(null);
            request.setDocumentDetails(null);

            EmployeeMasterResponseDto mapped = EmployeeMasterResponseDto.builder().employeePoid(88L).build();
            when(employeeMasterMapper.toResponseDto(existing)).thenReturn(mapped);

            EmployeeMasterResponseDto out = service.updateEmployee(88L, request);

            assertThat(out.getEmployeePoid()).isEqualTo(88L);
            verify(loggingService).logChanges(any(HrEmployeeMaster.class), eq(existing), eq(HrEmployeeMaster.class), eq(DOC_ID), eq("88"),
                    eq(LogDetailsEnum.MODIFIED), eq("EMPLOYEE_POID"));
        }

        @Test
        void throwsWhenEmployeeNotFound() {
            when(masterRepository.findByEmployeePoid(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateEmployee(1L, baseValidRequest("REMOTE", 1L)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class DeleteEmployee {
        @Test
        void deletesUsingDocumentDeleteService() {
            when(masterRepository.findByEmployeePoid(1L)).thenReturn(Optional.of(new HrEmployeeMaster()));
            DeleteReasonDto reason = new DeleteReasonDto();

            service.deleteEmployee(1L, reason);

            verify(documentDeleteService).deleteDocument(eq(1L), eq("HR_EMPLOYEE_MASTER"), eq("EMPLOYEE_POID"), eq(reason), isNull());
        }

        @Test
        void throwsWhenEmployeeNotFound() {
            when(masterRepository.findByEmployeePoid(1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.deleteEmployee(1L, new DeleteReasonDto()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class UpdateEmployeePhoto {
        @Test
        void updatesPhoto() {
            HrEmployeeMaster entity = new HrEmployeeMaster();
            entity.setEmployeePoid(3L);
            when(masterRepository.findByEmployeePoid(3L)).thenReturn(Optional.of(entity));
            when(masterRepository.save(entity)).thenReturn(entity);

            byte[] photo = new byte[]{1, 2, 3};
            EmployeePhotoUpdateRequestDto req = EmployeePhotoUpdateRequestDto.builder().photo(photo).build();
            EmployeePhotoUpdateResponseDto out = service.updateEmployeePhoto(3L, req);

            assertThat(out.getEmployeePoid()).isEqualTo(3L);
            assertThat(out.getPhoto()).containsExactly(photo);
        }

        @Test
        void throwsWhenEmployeeNotFound() {
            when(masterRepository.findByEmployeePoid(3L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateEmployeePhoto(3L, EmployeePhotoUpdateRequestDto.builder().photo(new byte[]{9}).build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class EmployeeCounts {
        @Test
        void delegatesToRepository() {
            EmployeeCountDto dto = new EmployeeCountDto(1L, 1L, 0L);
            when(masterRepository.getEmployeeCounts()).thenReturn(dto);
            assertThat(service.getEmployeeCounts()).isSameAs(dto);
        }
    }

    @Nested
    class EmployeeDashboardList {
        @Test
        void validatesDateRange() {
            EmployeeDashboardListRequestDto req = new EmployeeDashboardListRequestDto();
            req.setJoinDateFrom(LocalDate.of(2026, 1, 10));
            req.setJoinDateTo(LocalDate.of(2026, 1, 1));

            assertThatThrownBy(() -> service.listEmployeeDashboardDetails(req, PageRequest.of(0, 10)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("joinDateFrom");
        }

        @Test
        void appliesDefaultPageableWhenNull() {
            when(masterRepository.searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            Map<String, Object> out = service.listEmployeeDashboardDetails(null, null);

            assertThat(out).containsKeys("content", "totalElements", "totalPages");
            verify(masterRepository).searchEmployeeDashboardDetails(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), argThat(p ->
                    p.getPageNumber() == 0 && p.getPageSize() == 10 && p.getSort().isSorted()
            ));
        }

        @Test
        void remapsSortFieldsAndFallsBackForUnknown() {
            Sort sort = Sort.by(
                    Sort.Order.asc("EMPLOYEE_NAME"),
                    Sort.Order.desc("unknown_field"),
                    Sort.Order.asc("employeeName2") // lower-cased first char -> allowed through
            );
            Pageable pageable = PageRequest.of(1, 20, sort);

            Page<EmployeeDashboardDetailsDto> page = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(masterRepository.searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(page);

            EmployeeDashboardListRequestDto req = new EmployeeDashboardListRequestDto();
            req.setStatus("  Y ");
            req.setFilter("  abc ");

            service.listEmployeeDashboardDetails(req, pageable);

            verify(masterRepository).searchEmployeeDashboardDetails(
                    isNull(), isNull(), isNull(), isNull(), isNull(),
                    eq("Y"), eq("abc"),
                    argThat(p -> {
                        List<String> props = new ArrayList<>();
                        p.getSort().forEach(o -> props.add(o.getProperty()));
                        // EMPLOYEE_NAME -> employeeName; unknown_field stays (starts with lowercase); employeeName2 remains
                        return props.equals(List.of("employeeName", "unknown_field", "employeeName2"));
                    })
            );
        }

        @Test
        void appliesDefaultSortWhenUnsorted() {
            Pageable pageable = PageRequest.of(0, 10, Sort.unsorted());
            when(masterRepository.searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            service.listEmployeeDashboardDetails(new EmployeeDashboardListRequestDto(), pageable);

            verify(masterRepository).searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(),
                    argThat(p -> p.getSort().isSorted()));
        }

        @Test
        void fallsBackToEmployeePoidWhenSortPropertyUnknownUppercase() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("UNKNOWN_FIELD")));
            when(masterRepository.searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            service.listEmployeeDashboardDetails(new EmployeeDashboardListRequestDto(), pageable);

            verify(masterRepository).searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(),
                    argThat(p -> {
                        List<String> props = new ArrayList<>();
                        p.getSort().forEach(o -> props.add(o.getProperty()));
                        return props.equals(List.of("employeePoid"));
                    }));
        }

        @Test
        void joinsDateRangeBranchesDoNotThrowWhenOneOrBothBoundsNull() {
            when(masterRepository.searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            EmployeeDashboardListRequestDto fromOnly = new EmployeeDashboardListRequestDto();
            fromOnly.setJoinDateFrom(LocalDate.of(2026, 1, 1));
            service.listEmployeeDashboardDetails(fromOnly, PageRequest.of(0, 5));

            EmployeeDashboardListRequestDto toOnly = new EmployeeDashboardListRequestDto();
            toOnly.setJoinDateTo(LocalDate.of(2026, 1, 31));
            service.listEmployeeDashboardDetails(toOnly, PageRequest.of(0, 5));

            EmployeeDashboardListRequestDto validRange = new EmployeeDashboardListRequestDto();
            validRange.setJoinDateFrom(LocalDate.of(2026, 1, 1));
            validRange.setJoinDateTo(LocalDate.of(2026, 1, 31));
            service.listEmployeeDashboardDetails(validRange, PageRequest.of(0, 5));

            verify(masterRepository, times(3)).searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class));
        }

        @Test
        void remapsUnknownSortPropertyStartingWithNonLetterToEmployeePoid() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("1unknown")));
            when(masterRepository.searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            service.listEmployeeDashboardDetails(new EmployeeDashboardListRequestDto(), pageable);

            verify(masterRepository).searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(),
                    argThat(p -> {
                        List<String> props = new ArrayList<>();
                        p.getSort().forEach(o -> props.add(o.getProperty()));
                        return props.equals(List.of("employeePoid"));
                    }));
        }

        @Test
        void remapsMixedMappedAndUnknownSortOrders() {
            Sort sort = Sort.by(Sort.Order.desc("EMPLOYEE_POID"), Sort.Order.asc("1mixed"));
            Pageable pageable = PageRequest.of(2, 15, sort);
            when(masterRepository.searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            service.listEmployeeDashboardDetails(new EmployeeDashboardListRequestDto(), pageable);

            verify(masterRepository).searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(),
                    argThat(p -> {
                        List<String> props = new ArrayList<>();
                        p.getSort().forEach(o -> props.add(o.getProperty()));
                        return props.equals(List.of("employeePoid", "employeePoid")) && p.getPageNumber() == 2 && p.getPageSize() == 15;
                    }));
        }

        @Test
        void preservesUnknownLowerCamelSortProperty() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("fooBar"), Sort.Order.desc("zooBar")));
            when(masterRepository.searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            service.listEmployeeDashboardDetails(new EmployeeDashboardListRequestDto(), pageable);

            verify(masterRepository).searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(),
                    argThat(p -> {
                        List<String> props = new ArrayList<>();
                        List<Boolean> descs = new ArrayList<>();
                        p.getSort().forEach(o -> {
                            props.add(o.getProperty());
                            descs.add(o.isDescending());
                        });
                        return props.equals(List.of("fooBar", "zooBar")) && descs.equals(List.of(false, true));
                    }));
        }

        @Test
        void mapsEmptySortPropertyWhenPageableUsesCustomSort() {
            Pageable pageable = mock(Pageable.class);
            Sort sort = mock(Sort.class);
            Sort.Order order = mock(Sort.Order.class);
            when(pageable.getPageNumber()).thenReturn(0);
            when(pageable.getPageSize()).thenReturn(12);
            when(pageable.getSort()).thenReturn(sort);
            when(sort.isUnsorted()).thenReturn(false);
            when(order.getProperty()).thenReturn("");
            when(order.getDirection()).thenReturn(Sort.Direction.DESC);
            when(sort.stream()).thenReturn(Stream.of(order));
            when(masterRepository.searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            service.listEmployeeDashboardDetails(new EmployeeDashboardListRequestDto(), pageable);

            verify(masterRepository).searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(),
                    argThat(p -> {
                        Sort s = p.getSort();
                        return s.getOrderFor("employeePoid") != null && s.getOrderFor("employeePoid").getDirection() == Sort.Direction.DESC;
                    }));
        }

        @Test
        void mapsUnknownSingleUppercaseSortPropertyToEmployeePoid() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("A")));
            when(masterRepository.searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            service.listEmployeeDashboardDetails(new EmployeeDashboardListRequestDto(), pageable);

            verify(masterRepository).searchEmployeeDashboardDetails(any(), any(), any(), any(), any(), any(), any(),
                    argThat(p -> {
                        List<String> props = new ArrayList<>();
                        p.getSort().forEach(o -> props.add(o.getProperty()));
                        return props.equals(List.of("employeePoid"));
                    }));
        }
    }

    @Nested
    class Print {
        @Test
        void returnsPdfBytes() throws Exception {
            JasperReport report = mock(JasperReport.class);
            when(printService.buildBaseParams(5L, DOC_ID)).thenReturn(Map.of("x", 1));
            when(printService.load("EmployeeDetailsReportWithSalary.jrxml")).thenReturn(report);
            when(printService.fillReportToPdf(report, Map.of("x", 1), dataSource)).thenReturn(new byte[]{1, 2});

            assertThat(service.print(5L)).containsExactly(1, 2);
        }
    }

    @Nested
    class UploadExcel {
        @Test
        void throwsWhenFileEmpty() {
            MockMultipartFile file = new MockMultipartFile("file", "x.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);
            assertThatThrownBy(() -> service.uploadExcel(file)).isInstanceOf(RuntimeException.class).hasMessageContaining("empty");
        }

        @Test
        void throwsWhenWorkbookCannotBeParsed() {
            MockMultipartFile file = new MockMultipartFile("file", "x.xlsx", "application/octet-stream", new byte[]{1, 2, 3});

            try (MockedConstruction<SimpleJdbcCall> ignored = mockExcelConfigProcSuccess("TEMP_T", 2, 1, 4)) {
                assertThatThrownBy(() -> service.uploadExcel(file))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Error processing Excel file");
            }
        }

        @Test
        void importsRowsAndInsertsToTempTable() throws Exception {
            byte[] excelBytes = createWorkbookBytes();
            MockMultipartFile file = new MockMultipartFile("file", "x.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

            when(jdbcTemplate.update(startsWith("DELETE FROM TEMP_T"))).thenReturn(1);
            when(jdbcTemplate.update(startsWith("INSERT INTO TEMP_T"))).thenReturn(1);

            try (MockedConstruction<SimpleJdbcCall> ignored = mockExcelConfigProcSuccess("TEMP_T", 2, 1, 4)) {
                String msg = service.uploadExcel(file);
                assertThat(msg).contains("Successfully");
                verify(jdbcTemplate, atLeastOnce()).update(startsWith("INSERT INTO TEMP_T"));
            }
        }

        @Test
        void saveImportedDataEscapesSingleQuotes() throws Exception {
            byte[] excelBytes = createWorkbookBytesWithQuotedString();
            MockMultipartFile file = new MockMultipartFile("file", "x.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

            when(jdbcTemplate.update(startsWith("DELETE FROM TEMP_T"))).thenReturn(1);
            when(jdbcTemplate.update(startsWith("INSERT INTO TEMP_T"))).thenReturn(1);

            try (MockedConstruction<SimpleJdbcCall> ignored = mockExcelConfigProcSuccess("TEMP_T", 2, 1, 4)) {
                service.uploadExcel(file);
                verify(jdbcTemplate).update(contains("O''Reilly"));
            }
        }

        @Test
        void importsRowsMappingFormulaErrorAndBlankCellsThroughDefaultSwitchArm() throws Exception {
            byte[] excelBytes = createWorkbookBytesWithFormulaErrorAndBlank();
            MockMultipartFile file = new MockMultipartFile("file", "x.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

            when(jdbcTemplate.update(startsWith("DELETE FROM TEMP_T"))).thenReturn(1);
            when(jdbcTemplate.update(startsWith("INSERT INTO TEMP_T"))).thenReturn(1);

            try (MockedConstruction<SimpleJdbcCall> ignored = mockExcelConfigProcSuccess("TEMP_T", 2, 1, 7)) {
                String msg = service.uploadExcel(file);
                assertThat(msg).contains("Successfully");
                verify(jdbcTemplate, atLeastOnce()).update(startsWith("INSERT INTO TEMP_T"));
            }
        }

        @Test
        void throwsWhenExcelConfigStatusNotSuccess() {
            MockMultipartFile file = new MockMultipartFile("file", "x.xlsx", "application/octet-stream", new byte[]{1});
            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(Map.of("P_STATUS", "FAILED", "OUTDATA", Collections.emptyList()));
            })) {
                assertThatThrownBy(() -> service.uploadExcel(file))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Failed to get Excel config");
            }
        }

        @Test
        void throwsWhenExcelConfigOutdataEmpty() {
            MockMultipartFile file = new MockMultipartFile("file", "x.xlsx", "application/octet-stream", new byte[]{1});
            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(Map.of("P_STATUS", "SUCCESS", "OUTDATA", Collections.emptyList()));
            })) {
                assertThatThrownBy(() -> service.uploadExcel(file))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("No Excel configuration found");
            }
        }

        @Test
        void throwsWhenExcelConfigOutdataNull() {
            MockMultipartFile file = new MockMultipartFile("file", "x.xlsx", "application/octet-stream", new byte[]{1});
            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(new HashMap<>(Map.of("P_STATUS", "SUCCESS")));
            })) {
                assertThatThrownBy(() -> service.uploadExcel(file))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("No Excel configuration found");
            }
        }
    }

    @Nested
    class StoredProcCalls {

        @Test
        void getEmployeeLeaveDatesReturnsResponse() {
            when(masterRepository.findByEmployeePoid(1L)).thenReturn(Optional.of(new HrEmployeeMaster()));
            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(Map.of("P_RESULT", "01/01/2026", "P_RESULT1", "31/12/2026"));
            })) {
                EmployeeLeaveDatesResponseDto out = service.getEmployeeLeaveDates(1L);
                assertThat(out.getStartDate()).isEqualTo("01/01/2026");
                assertThat(out.getPeriodEndDate()).isEqualTo("31/12/2026");
            }
        }

        @Test
        void getEmployeeLeaveDatesWrapsDataAccessException() {
            when(masterRepository.findByEmployeePoid(1L)).thenReturn(Optional.of(new HrEmployeeMaster()));
            DataAccessException dae = new org.springframework.dao.RecoverableDataAccessException("x");
            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenThrow(dae);
            })) {
                assertThatThrownBy(() -> service.getEmployeeLeaveDates(1L))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("PROC_HR_EMP_LEAVE_DATES failed");
            }
        }

        @Test
        void getEmployeeLeaveDatesThrowsWhenEmployeeMissing() {
            when(masterRepository.findByEmployeePoid(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getEmployeeLeaveDates(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void updateLeaveRejoinReturnsStatusAndThrowsOnErrorStatus() {
            when(masterRepository.findByEmployeePoid(2L)).thenReturn(Optional.of(new HrEmployeeMaster()));
            LeaveRejoinUpdateRequestDto req = LeaveRejoinUpdateRequestDto.builder()
                    .rejoinDate(LocalDate.of(2026, 1, 2))
                    .rejoinLrqRef("R")
                    .build();

            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(Map.of("P_RESULT", "OK"));
            })) {
                assertThat(service.updateLeaveRejoin(2L, req)).isEqualTo("OK");
            }

            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(new HashMap<>());
            })) {
                assertThat(service.updateLeaveRejoin(2L, req)).isNull();
            }

            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(Map.of("P_RESULT", "ERROR:bad"));
            })) {
                assertThatThrownBy(() -> service.updateLeaveRejoin(2L, req))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("ERROR");
            }
        }

        @Test
        void updateLeaveRejoinWrapsDataAccessException() {
            when(masterRepository.findByEmployeePoid(2L)).thenReturn(Optional.of(new HrEmployeeMaster()));
            LeaveRejoinUpdateRequestDto req = LeaveRejoinUpdateRequestDto.builder()
                    .rejoinDate(LocalDate.of(2026, 1, 2))
                    .rejoinLrqRef("R")
                    .build();
            DataAccessException dae = new org.springframework.dao.RecoverableDataAccessException("x");
            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenThrow(dae);
            })) {
                assertThatThrownBy(() -> service.updateLeaveRejoin(2L, req))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("PROC_HR_EMP_REJOINDT_UPDATE_V2 failed");
            }
        }

        @Test
        void updateLeaveRejoinThrowsWhenEmployeeMissing() {
            when(masterRepository.findByEmployeePoid(999L)).thenReturn(Optional.empty());
            LeaveRejoinUpdateRequestDto req = LeaveRejoinUpdateRequestDto.builder()
                    .rejoinDate(LocalDate.of(2026, 1, 2))
                    .rejoinLrqRef("R")
                    .build();
            assertThatThrownBy(() -> service.updateLeaveRejoin(999L, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void removeLeaveRejoinSupportsNullDateAndThrowsOnErrorStatus() {
            when(masterRepository.findByEmployeePoid(2L)).thenReturn(Optional.of(new HrEmployeeMaster()));
            LeaveRejoinRemoveRequestDto req = LeaveRejoinRemoveRequestDto.builder()
                    .rejoinDate(null)
                    .rejoinLrqRef("R")
                    .build();

            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(Map.of("P_RESULT", "OK"));
            })) {
                assertThat(service.removeLeaveRejoin(2L, req)).isEqualTo("OK");
            }

            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(new HashMap<>());
            })) {
                assertThat(service.removeLeaveRejoin(2L, req)).isNull();
            }

            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(Map.of("P_RESULT", "ERROR:bad"));
            })) {
                assertThatThrownBy(() -> service.removeLeaveRejoin(2L, req))
                        .isInstanceOf(ValidationException.class);
            }
        }

        @Test
        void removeLeaveRejoinWrapsDataAccessException() {
            when(masterRepository.findByEmployeePoid(2L)).thenReturn(Optional.of(new HrEmployeeMaster()));
            LeaveRejoinRemoveRequestDto req = LeaveRejoinRemoveRequestDto.builder()
                    .rejoinDate(LocalDate.of(2026, 1, 2))
                    .rejoinLrqRef("R")
                    .build();
            DataAccessException dae = new org.springframework.dao.RecoverableDataAccessException("x");
            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenThrow(dae);
            })) {
                assertThatThrownBy(() -> service.removeLeaveRejoin(2L, req))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("PROC_HR_EMP_REJOINDT_REMOVE failed");
            }
        }

        @Test
        void removeLeaveRejoinThrowsWhenEmployeeMissing() {
            when(masterRepository.findByEmployeePoid(999L)).thenReturn(Optional.empty());
            LeaveRejoinRemoveRequestDto req = LeaveRejoinRemoveRequestDto.builder()
                    .rejoinLrqRef("R")
                    .rejoinDate(LocalDate.of(2026, 1, 2))
                    .build();
            assertThatThrownBy(() -> service.removeLeaveRejoin(999L, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void uploadLmraDataReturnsMappedRowsAndThrowsOnErrorStatus() throws Exception {
            // status OK
            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(new HashMap<>());
            })) {
                when(jdbcTemplate.query(eq("SELECT * FROM HR_EMP_DEPNDTS_LMRA_DTLS ORDER BY DET_ROW_ID DESC"), any(org.springframework.jdbc.core.RowMapper.class)))
                        .thenReturn(Collections.emptyList());
                LmraUploadResponse noStatus = service.uploadLmraData();
                assertThat(noStatus.getStatus()).isNull();
            }

            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(Map.of("P_RESULT", "SUCCESS"));
            })) {
                when(jdbcTemplate.query(eq("SELECT * FROM HR_EMP_DEPNDTS_LMRA_DTLS ORDER BY DET_ROW_ID DESC"), any(org.springframework.jdbc.core.RowMapper.class)))
                        .thenAnswer(inv -> {
                            @SuppressWarnings("unchecked")
                            org.springframework.jdbc.core.RowMapper<EmployeeDepndtsLmraDtlsResponseDto> rm =
                                    (org.springframework.jdbc.core.RowMapper<EmployeeDepndtsLmraDtlsResponseDto>) inv.getArgument(1);

                            ResultSet rs1 = mock(ResultSet.class);
                            when(rs1.getLong("EMPLOYEE_POID")).thenReturn(1L);
                            when(rs1.getLong("DET_ROW_ID")).thenReturn(9L);
                            when(rs1.getString(anyString())).thenAnswer(a -> a.getArgument(0) + "_V");
                            when(rs1.getObject(eq("PERMIT_MONTHS"))).thenReturn(null);
                            when(rs1.getObject(eq("WP_EXPIRY_DATE"), eq(LocalDate.class))).thenReturn(LocalDate.of(2026, 1, 1));
                            when(rs1.getObject(eq("PP_EXPIRY_DATE"), eq(LocalDate.class))).thenReturn(LocalDate.of(2026, 2, 1));

                            ResultSet rs2 = mock(ResultSet.class);
                            when(rs2.getLong("EMPLOYEE_POID")).thenReturn(2L);
                            when(rs2.getLong("DET_ROW_ID")).thenReturn(10L);
                            when(rs2.getString(anyString())).thenAnswer(a -> a.getArgument(0) + "_V2");
                            when(rs2.getObject(eq("PERMIT_MONTHS"))).thenReturn(5);
                            when(rs2.getInt("PERMIT_MONTHS")).thenReturn(5);
                            when(rs2.getObject(eq("WP_EXPIRY_DATE"), eq(LocalDate.class))).thenReturn(null);
                            when(rs2.getObject(eq("PP_EXPIRY_DATE"), eq(LocalDate.class))).thenReturn(null);

                            return List.of(rm.mapRow(rs1, 0), rm.mapRow(rs2, 1));
                        });

                LmraUploadResponse out = service.uploadLmraData();
                assertThat(out.getStatus()).isEqualTo("SUCCESS");
                assertThat(out.getLmraDetails()).hasSize(2);
                assertThat(out.getLmraDetails().getFirst().getPermitMonths()).isNull();
                assertThat(out.getLmraDetails().getLast().getPermitMonths()).isEqualTo(5);
            }

            // status ERROR -> ValidationException
            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(Map.of("P_RESULT", "ERROR: bad"));
            })) {
                assertThatThrownBy(() -> service.uploadLmraData())
                        .isInstanceOf(ValidationException.class);
            }
        }
    }

    @Nested
    class SyntheticLambdaCoverage {
        @Test
        void applyDependentsThrowsNotFoundDuringApplyPhase() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            request.setDependentsDetails(List.of(EmployeeDependentsDtlRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .detRowId(1L)
                    .name("X")
                    .nationality("BH")
                    .build()));

            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(500L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(500L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(500L);
                setEmpGlPoid(1L);
            }}));
            when(masterRepository.existsByEmployeePoid(500L)).thenReturn(true);
            lenient().when(hrNationalityRepository.existsByNationalityCode(anyString())).thenReturn(true);

            HrEmployeeDependentsDtl existing = new HrEmployeeDependentsDtl();
            existing.setEmployeePoid(500L);
            existing.setDetRowId(1L);
            existing.setName("OLD");
            when(dependentRepository.findById(any(HrEmployeeDependentsDtlId.class))).thenReturn(Optional.of(existing), Optional.empty());
            when(dependentRepository.findByEmployeePoid(500L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Dependents");
        }

        @Test
        void applyLmraThrowsNotFoundDuringApplyPhase() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            request.setLmraDetails(List.of(EmployeeDepndtsLmraDtlsRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .detRowId(1L)
                    .expatName("X")
                    .build()));

            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(501L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(501L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(501L);
                setEmpGlPoid(1L);
            }}));
            when(masterRepository.existsByEmployeePoid(501L)).thenReturn(true);

            when(lmraRepository.findById(any(HrEmpDepndtsLmraDtlsId.class))).thenReturn(Optional.empty());
            when(lmraRepository.findByEmployeePoid(501L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("LMRA Details");
        }

        @Test
        void applyExperienceThrowsNotFoundDuringApplyPhase() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            request.setExperienceDetails(List.of(EmployeeExperienceDtlRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .detRowId(1L)
                    .employer("X")
                    .build()));

            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(502L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(502L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(502L);
                setEmpGlPoid(1L);
            }}));
            when(masterRepository.existsByEmployeePoid(502L)).thenReturn(true);
            when(experienceRepository.existsByEmployerIgnoreCaseAndEmployeePoidNot(anyString(), anyLong())).thenReturn(false);

            when(experienceRepository.findById(any(HrEmployeeExperienceDtlId.class))).thenReturn(Optional.empty());
            when(experienceRepository.findByEmployeePoid(502L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Experience");
        }

        @Test
        void applyDocumentThrowsNotFoundDuringApplyPhase() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            request.setDocumentDetails(List.of(EmployeeDocumentDtlRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .detRowId(1L)
                    .docName("X")
                    .build()));

            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(503L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(503L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(503L);
                setEmpGlPoid(1L);
            }}));

            when(documentRepository.findById(any(HrEmployeeDocumentDtlId.class))).thenReturn(Optional.empty());
            when(documentRepository.findByEmployeePoid(503L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Document");
        }

        @Test
        void applyDocumentDeleteThrowsNotFound() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            request.setDocumentDetails(List.of(EmployeeDocumentDtlRequestDto.builder()
                    .actionType(ActionType.isDeleted)
                    .detRowId(1L)
                    .build()));

            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(504L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(504L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(504L);
                setEmpGlPoid(1L);
            }}));

            when(documentRepository.findById(any(HrEmployeeDocumentDtlId.class))).thenReturn(Optional.empty());
            when(documentRepository.findByEmployeePoid(504L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Document");
        }

        @Test
        void applyExperienceDeleteThrowsNotFound() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            request.setExperienceDetails(List.of(EmployeeExperienceDtlRequestDto.builder()
                    .actionType(ActionType.isDeleted)
                    .detRowId(1L)
                    .build()));

            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(505L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(505L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(505L);
                setEmpGlPoid(1L);
            }}));
            when(masterRepository.existsByEmployeePoid(505L)).thenReturn(true);

            when(experienceRepository.findById(any(HrEmployeeExperienceDtlId.class))).thenReturn(Optional.empty());
            when(experienceRepository.findByEmployeePoid(505L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Experience");
        }

        @Test
        void applyLmraDeleteThrowsNotFound() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            request.setLmraDetails(List.of(EmployeeDepndtsLmraDtlsRequestDto.builder()
                    .actionType(ActionType.isDeleted)
                    .detRowId(1L)
                    .build()));

            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(506L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(506L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(506L);
                setEmpGlPoid(1L);
            }}));
            when(masterRepository.existsByEmployeePoid(506L)).thenReturn(true);

            when(lmraRepository.findById(any(HrEmpDepndtsLmraDtlsId.class))).thenReturn(Optional.empty());
            when(lmraRepository.findByEmployeePoid(506L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("LMRA Details");
        }

        @Test
        void applyDependentsDeleteThrowsNotFound() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            request.setDependentsDetails(List.of(EmployeeDependentsDtlRequestDto.builder()
                    .actionType(ActionType.isDeleted)
                    .detRowId(1L)
                    .nationality("BH")
                    .build()));

            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(507L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(507L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(507L);
                setEmpGlPoid(1L);
            }}));
            when(masterRepository.existsByEmployeePoid(507L)).thenReturn(true);
            lenient().when(hrNationalityRepository.existsByNationalityCode(anyString())).thenReturn(true);

            when(dependentRepository.findById(any(HrEmployeeDependentsDtlId.class))).thenReturn(Optional.empty());
            when(dependentRepository.findByEmployeePoid(507L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Dependents");
        }
    }

    @Nested
    class PrivateBranchesViaReflection {
        @Test
        void applyLeaveHistoryCoversCreatedUpdatedDeletedAndValidation() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyLeaveHistoryDetails", Long.class, List.class);
            m.setAccessible(true);

            when(masterRepository.existsByEmployeePoid(1L)).thenReturn(true);
            when(leaveHistoryRepository.findByEmployeePoid(1L)).thenReturn(Collections.emptyList());

            // created: uses UserContext fallback for company/group when null
            EmployeeLeaveHistoryRequestDto created = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isCreated)
                    .leaveHistPoid(null)
                    .detRowId(null)
                    .companyPoid(null)
                    .groupPoid(null)
                    .employeeName("E")
                    .build();

            // updated requires both ids; not found -> ResourceNotFound
            EmployeeLeaveHistoryRequestDto updatedMissingIds = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .leaveHistPoid(null)
                    .detRowId(null)
                    .build();

            EmployeeLeaveHistoryRequestDto deletedMissingIds = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isDeleted)
                    .leaveHistPoid(null)
                    .detRowId(null)
                    .build();

            when(leaveHistoryRepository.save(any(HrEmployeeLeaveHistory.class))).thenAnswer(inv -> inv.getArgument(0));

            // created + noChange skip + null dto skip
            EmployeeLeaveHistoryRequestDto noChange = EmployeeLeaveHistoryRequestDto.builder().actionType(ActionType.noChange).build();
            @SuppressWarnings("unchecked")
            List<EmployeeLeaveHistoryRequestDto> list = Arrays.asList(created, null, noChange);
            m.invoke(service, 1L, list);
            verify(leaveHistoryRepository).save(any(HrEmployeeLeaveHistory.class));

            assertThatThrownBy(() -> m.invoke(service, 1L, List.of(updatedMissingIds)))
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .rootCause()
                    .hasMessageContaining("leaveHistPoid and detRowId");

            assertThatThrownBy(() -> m.invoke(service, 1L, List.of(deletedMissingIds)))
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .rootCause()
                    .hasMessageContaining("leaveHistPoid and detRowId");

            // updated branch with id and company/group retention
            EmployeeLeaveHistoryRequestDto updated = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .leaveHistPoid(10L)
                    .detRowId(20L)
                    .companyPoid(null)
                    .groupPoid(null)
                    .employeeName("E2")
                    .build();
            HrEmployeeLeaveHistory existing = new HrEmployeeLeaveHistory();
            existing.setLeaveHistPoid(10L);
            existing.setDetRowId(20L);
            existing.setCompanyPoid(777L);
            existing.setGroupPoid(888L);
            when(leaveHistoryRepository.findById(any(HrEmployeeLeaveHistoryId.class))).thenReturn(Optional.of(existing));
            m.invoke(service, 1L, List.of(updated));
            verify(leaveHistoryRepository, atLeast(2)).save(any(HrEmployeeLeaveHistory.class));

            // deleted branch
            EmployeeLeaveHistoryRequestDto deleted = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isDeleted)
                    .leaveHistPoid(10L)
                    .detRowId(20L)
                    .build();
            m.invoke(service, 1L, List.of(deleted));
            verify(leaveHistoryRepository).deleteById(any(HrEmployeeLeaveHistoryId.class));

            // employee missing -> ResourceNotFound
            when(masterRepository.existsByEmployeePoid(2L)).thenReturn(false);
            assertThatThrownBy(() -> m.invoke(service, 2L, List.of(created)))
                    .hasRootCauseInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void applyChildTablesSkipNullAndNoChangeDtos() throws Exception {
            long employeePoid = 123L;

            Method depM = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyDependents", Long.class, List.class);
            depM.setAccessible(true);
            Method lmraM = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyLmraDetails", Long.class, List.class);
            lmraM.setAccessible(true);
            Method expM = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyExperienceDetails", Long.class, List.class);
            expM.setAccessible(true);
            Method docM = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyDocumentDetails", Long.class, List.class);
            docM.setAccessible(true);

            when(dependentRepository.findByEmployeePoid(employeePoid)).thenReturn(Collections.emptyList());
            when(lmraRepository.findByEmployeePoid(employeePoid)).thenReturn(Collections.emptyList());
            when(experienceRepository.findByEmployeePoid(employeePoid)).thenReturn(Collections.emptyList());
            when(documentRepository.findByEmployeePoid(employeePoid)).thenReturn(Collections.emptyList());

            depM.invoke(service, employeePoid, Arrays.asList(null, EmployeeDependentsDtlRequestDto.builder().actionType(ActionType.noChange).build()));
            lmraM.invoke(service, employeePoid, Arrays.asList(null, EmployeeDepndtsLmraDtlsRequestDto.builder().actionType(ActionType.noChange).build()));
            expM.invoke(service, employeePoid, Arrays.asList(null, EmployeeExperienceDtlRequestDto.builder().actionType(ActionType.noChange).build()));
            docM.invoke(service, employeePoid, Arrays.asList(null, EmployeeDocumentDtlRequestDto.builder().actionType(ActionType.noChange).build()));

            verify(dependentRepository, never()).save(any());
            verify(lmraRepository, never()).save(any());
            verify(experienceRepository, never()).save(any());
            verify(documentRepository, never()).save(any());
        }

        @Test
        void applyLeaveHistoryUpdateThrowsNotFound() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyLeaveHistoryDetails", Long.class, List.class);
            m.setAccessible(true);

            when(masterRepository.existsByEmployeePoid(1L)).thenReturn(true);
            when(leaveHistoryRepository.findByEmployeePoid(1L)).thenReturn(Collections.emptyList());
            when(leaveHistoryRepository.findById(any(HrEmployeeLeaveHistoryId.class))).thenReturn(Optional.empty());

            EmployeeLeaveHistoryRequestDto updated = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .leaveHistPoid(10L)
                    .detRowId(20L)
                    .build();

            assertThatThrownBy(() -> m.invoke(service, 1L, List.of(updated)))
                    .hasRootCauseInstanceOf(ResourceNotFoundException.class)
                    .rootCause()
                    .hasMessageContaining("Leave History");
        }

        @Test
        void applyLeaveHistoryEarlyReturnForNullOrEmptyList() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyLeaveHistoryDetails", Long.class, List.class);
            m.setAccessible(true);
            m.invoke(service, 1L, null);
            m.invoke(service, 1L, Collections.emptyList());
            verify(leaveHistoryRepository, never()).save(any());
        }

        @Test
        void applyLeaveHistorySkipsRowWhenActionTypeNull() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyLeaveHistoryDetails", Long.class, List.class);
            m.setAccessible(true);
            when(masterRepository.existsByEmployeePoid(1L)).thenReturn(true);
            when(leaveHistoryRepository.findByEmployeePoid(1L)).thenReturn(Collections.emptyList());
            EmployeeLeaveHistoryRequestDto dto = EmployeeLeaveHistoryRequestDto.builder().actionType(null).build();
            m.invoke(service, 1L, List.of(dto));
            verify(leaveHistoryRepository, never()).save(any());
        }

        @Test
        void applyLeaveHistorySkipsEmployeeLookupWhenEmployeePoidNull() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyLeaveHistoryDetails", Long.class, List.class);
            m.setAccessible(true);
            when(leaveHistoryRepository.findByEmployeePoid(null)).thenReturn(Collections.emptyList());
            EmployeeLeaveHistoryRequestDto created = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isCreated)
                    .leaveHistPoid(501L)
                    .detRowId(601L)
                    .companyPoid(11L)
                    .groupPoid(22L)
                    .employeeName("E")
                    .build();
            when(leaveHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            m.invoke(service, null, List.of(created));
            verify(masterRepository, never()).existsByEmployeePoid(any());
            verify(leaveHistoryRepository).save(argThat(ent ->
                    ent.getLeaveHistPoid() == 501L && ent.getDetRowId() == 601L
                            && Objects.equals(ent.getCompanyPoid(), 11L) && Objects.equals(ent.getGroupPoid(), 22L)));
        }

        @Test
        void applyLeaveHistoryUpdateThrowsWhenOnlyOneCompositeIdPresent() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyLeaveHistoryDetails", Long.class, List.class);
            m.setAccessible(true);
            when(masterRepository.existsByEmployeePoid(1L)).thenReturn(true);
            when(leaveHistoryRepository.findByEmployeePoid(1L)).thenReturn(Collections.emptyList());

            EmployeeLeaveHistoryRequestDto missDet = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .leaveHistPoid(10L)
                    .detRowId(null)
                    .build();
            assertThatThrownBy(() -> m.invoke(service, 1L, List.of(missDet)))
                    .hasRootCauseInstanceOf(IllegalArgumentException.class);

            EmployeeLeaveHistoryRequestDto missHist = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .leaveHistPoid(null)
                    .detRowId(20L)
                    .build();
            assertThatThrownBy(() -> m.invoke(service, 1L, List.of(missHist)))
                    .hasRootCauseInstanceOf(IllegalArgumentException.class);

            EmployeeLeaveHistoryRequestDto delMissHist = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isDeleted)
                    .leaveHistPoid(null)
                    .detRowId(20L)
                    .build();
            assertThatThrownBy(() -> m.invoke(service, 1L, List.of(delMissHist)))
                    .hasRootCauseInstanceOf(IllegalArgumentException.class);

            EmployeeLeaveHistoryRequestDto delMissDet = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isDeleted)
                    .leaveHistPoid(10L)
                    .detRowId(null)
                    .build();
            assertThatThrownBy(() -> m.invoke(service, 1L, List.of(delMissDet)))
                    .hasRootCauseInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void applyLeaveHistoryUpdateKeepsExistingCompanyWhenDtoOmitsThem() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyLeaveHistoryDetails", Long.class, List.class);
            m.setAccessible(true);
            when(masterRepository.existsByEmployeePoid(7L)).thenReturn(true);
            when(leaveHistoryRepository.findByEmployeePoid(7L)).thenReturn(Collections.emptyList());

            HrEmployeeLeaveHistory existing = new HrEmployeeLeaveHistory();
            existing.setLeaveHistPoid(30L);
            existing.setDetRowId(40L);
            existing.setCompanyPoid(910L);
            existing.setGroupPoid(920L);
            when(leaveHistoryRepository.findById(any(HrEmployeeLeaveHistoryId.class))).thenReturn(Optional.of(existing));

            EmployeeLeaveHistoryRequestDto updated = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .leaveHistPoid(30L)
                    .detRowId(40L)
                    .companyPoid(null)
                    .groupPoid(null)
                    .employeeName("keepCg")
                    .build();

            when(leaveHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            m.invoke(service, 7L, List.of(updated));
            verify(leaveHistoryRepository).save(argThat(ent ->
                    Objects.equals(ent.getCompanyPoid(), 910L) && Objects.equals(ent.getGroupPoid(), 920L)));
        }

        @Test
        void applyLeaveHistoryUpdateUsesExplicitCompanyAndGroupFromDto() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyLeaveHistoryDetails", Long.class, List.class);
            m.setAccessible(true);
            when(masterRepository.existsByEmployeePoid(3L)).thenReturn(true);
            when(leaveHistoryRepository.findByEmployeePoid(3L)).thenReturn(Collections.emptyList());

            HrEmployeeLeaveHistory existing = new HrEmployeeLeaveHistory();
            existing.setLeaveHistPoid(10L);
            existing.setDetRowId(20L);
            existing.setCompanyPoid(700L);
            existing.setGroupPoid(800L);
            when(leaveHistoryRepository.findById(any(HrEmployeeLeaveHistoryId.class))).thenReturn(Optional.of(existing));

            EmployeeLeaveHistoryRequestDto updated = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .leaveHistPoid(10L)
                    .detRowId(20L)
                    .companyPoid(701L)
                    .groupPoid(801L)
                    .employeeName("E3")
                    .build();

            when(leaveHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            m.invoke(service, 3L, List.of(updated));
            verify(leaveHistoryRepository).save(argThat(ent ->
                    Objects.equals(ent.getCompanyPoid(), 701L) && Objects.equals(ent.getGroupPoid(), 801L)));
        }

        @Test
        void applyDependentsEmptyListAndNullActionTypeNoOps() throws Exception {
            Method depM = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyDependents", Long.class, List.class);
            depM.setAccessible(true);
            when(dependentRepository.findByEmployeePoid(9L)).thenReturn(Collections.emptyList());
            depM.invoke(service, 9L, Collections.emptyList());
            depM.invoke(service, 9L, null);
            depM.invoke(service, 9L, List.of(EmployeeDependentsDtlRequestDto.builder().actionType(null).name("x").build()));
            verify(dependentRepository, never()).save(any());
        }

        @Test
        void applyChildTablesDeleteOnlyRowHitsFinalBranch() throws Exception {
            long emp = 11L;
            Method depM = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyDependents", Long.class, List.class);
            depM.setAccessible(true);
            when(dependentRepository.findByEmployeePoid(emp)).thenReturn(Collections.emptyList());
            HrEmployeeDependentsDtl dep = new HrEmployeeDependentsDtl();
            dep.setEmployeePoid(emp);
            dep.setDetRowId(1L);
            when(dependentRepository.findById(any(HrEmployeeDependentsDtlId.class))).thenReturn(Optional.of(dep));
            depM.invoke(service, emp, List.of(EmployeeDependentsDtlRequestDto.builder().actionType(ActionType.isDeleted).detRowId(1L).build()));
            verify(dependentRepository).deleteById(any(HrEmployeeDependentsDtlId.class));

            Method lmraM = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyLmraDetails", Long.class, List.class);
            lmraM.setAccessible(true);
            when(lmraRepository.findByEmployeePoid(emp)).thenReturn(Collections.emptyList());
            HrEmpDepndtsLmraDtls lm = new HrEmpDepndtsLmraDtls();
            lm.setEmployeePoid(emp);
            lm.setDetRowId(2L);
            when(lmraRepository.findById(any(HrEmpDepndtsLmraDtlsId.class))).thenReturn(Optional.of(lm));
            lmraM.invoke(service, emp, List.of(EmployeeDepndtsLmraDtlsRequestDto.builder().actionType(ActionType.isDeleted).detRowId(2L).build()));
            verify(lmraRepository).deleteById(any(HrEmpDepndtsLmraDtlsId.class));

            Method expM = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyExperienceDetails", Long.class, List.class);
            expM.setAccessible(true);
            when(experienceRepository.findByEmployeePoid(emp)).thenReturn(Collections.emptyList());
            HrEmployeeExperienceDtl ex = new HrEmployeeExperienceDtl();
            ex.setEmployeePoid(emp);
            ex.setDetRowId(3L);
            when(experienceRepository.findById(any(HrEmployeeExperienceDtlId.class))).thenReturn(Optional.of(ex));
            expM.invoke(service, emp, List.of(EmployeeExperienceDtlRequestDto.builder().actionType(ActionType.isDeleted).detRowId(3L).build()));
            verify(experienceRepository).deleteById(any(HrEmployeeExperienceDtlId.class));

            Method docM = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyDocumentDetails", Long.class, List.class);
            docM.setAccessible(true);
            when(documentRepository.findByEmployeePoid(emp)).thenReturn(Collections.emptyList());
            HrEmployeeDocumentDtl doc = new HrEmployeeDocumentDtl();
            doc.setEmployeePoid(emp);
            doc.setDetRowId(4L);
            when(documentRepository.findById(any(HrEmployeeDocumentDtlId.class))).thenReturn(Optional.of(doc));
            docM.invoke(service, emp, List.of(EmployeeDocumentDtlRequestDto.builder().actionType(ActionType.isDeleted).detRowId(4L).build()));
            verify(documentRepository).deleteById(any(HrEmployeeDocumentDtlId.class));
        }

        @Test
        void applyChildDetailTablesReturnImmediatelyForNullList() throws Exception {
            Method lmraM = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyLmraDetails", Long.class, List.class);
            lmraM.setAccessible(true);
            Method expM = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyExperienceDetails", Long.class, List.class);
            expM.setAccessible(true);
            Method docM = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyDocumentDetails", Long.class, List.class);
            docM.setAccessible(true);
            lmraM.invoke(service, 1L, null);
            expM.invoke(service, 1L, null);
            docM.invoke(service, 1L, null);
            lmraM.invoke(service, 1L, Collections.emptyList());
            expM.invoke(service, 1L, Collections.emptyList());
            docM.invoke(service, 1L, Collections.emptyList());
            List<EmployeeDepndtsLmraDtlsRequestDto> lmraWithNull = new ArrayList<>();
            lmraWithNull.add(null);
            lmraM.invoke(service, 1L, lmraWithNull);
            List<EmployeeExperienceDtlRequestDto> expWithNull = new ArrayList<>();
            expWithNull.add(null);
            expM.invoke(service, 1L, expWithNull);
            List<EmployeeDocumentDtlRequestDto> docWithNull = new ArrayList<>();
            docWithNull.add(null);
            docM.invoke(service, 1L, docWithNull);
            lmraM.invoke(service, 1L, List.of(EmployeeDepndtsLmraDtlsRequestDto.builder().actionType(null).build()));
            expM.invoke(service, 1L, List.of(EmployeeExperienceDtlRequestDto.builder().actionType(null).build()));
            docM.invoke(service, 1L, List.of(EmployeeDocumentDtlRequestDto.builder().actionType(null).build()));
            verify(lmraRepository, never()).save(any());
            verify(experienceRepository, never()).save(any());
            verify(documentRepository, never()).save(any());
        }

        @Test
        void applyLeaveHistoryDeleteOnly() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyLeaveHistoryDetails", Long.class, List.class);
            m.setAccessible(true);
            when(masterRepository.existsByEmployeePoid(6L)).thenReturn(true);
            when(leaveHistoryRepository.findByEmployeePoid(6L)).thenReturn(Collections.emptyList());
            EmployeeLeaveHistoryRequestDto del = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isDeleted)
                    .leaveHistPoid(88L)
                    .detRowId(99L)
                    .build();
            m.invoke(service, 6L, List.of(del));
            verify(leaveHistoryRepository).deleteById(argThat(id ->
                    id.getLeaveHistPoid() == 88L && id.getDetRowId() == 99L));
        }

        @Test
        void applyLeaveHistoryCreateCoversIdAssignmentCombinations() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("applyLeaveHistoryDetails", Long.class, List.class);
            m.setAccessible(true);
            when(masterRepository.existsByEmployeePoid(4L)).thenReturn(true);
            HrEmployeeLeaveHistory prior = new HrEmployeeLeaveHistory();
            prior.setLeaveHistPoid(50L);
            prior.setDetRowId(60L);
            when(leaveHistoryRepository.findByEmployeePoid(4L)).thenReturn(List.of(prior));
            when(leaveHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EmployeeLeaveHistoryRequestDto a = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isCreated).leaveHistPoid(100L).detRowId(null).companyPoid(1L).groupPoid(2L).employeeName("a").build();
            EmployeeLeaveHistoryRequestDto b = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isCreated).leaveHistPoid(null).detRowId(200L).companyPoid(1L).groupPoid(2L).employeeName("b").build();
            EmployeeLeaveHistoryRequestDto c = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isCreated).leaveHistPoid(300L).detRowId(400L).companyPoid(1L).groupPoid(2L).employeeName("c").build();
            EmployeeLeaveHistoryRequestDto d = EmployeeLeaveHistoryRequestDto.builder()
                    .actionType(ActionType.isCreated).leaveHistPoid(5000L).detRowId(1L).companyPoid(1L).groupPoid(2L).employeeName("d").build();

            m.invoke(service, 4L, new ArrayList<>(List.of(a, b, c, d)));
            verify(leaveHistoryRepository, times(4)).save(any());
        }
    }

    @Nested
    class PrivateChildValidationExtraBranches {
        @Test
        void validateMasterSkipsSelfHodWhenCurrentEmployeePoidNullOnUpdatePath() throws Exception {
            Method val = EmployeeMasterServiceImpl.class.getDeclaredMethod("validateEmployeeMasterRequest",
                    EmployeeMasterRequestDto.class, boolean.class, Long.class);
            val.setAccessible(true);
            EmployeeMasterRequestDto r = baseValidRequest("REMOTE", 1L);
            r.setHod(10L);
            when(masterRepository.existsByEmployeePoid(10L)).thenReturn(true);
            val.invoke(service, r, true, null);
        }

        @Test
        void validateMasterRequestSkipsNameUniquenessWhenFirstNameNullOnCreate() throws Exception {
            Method val = EmployeeMasterServiceImpl.class.getDeclaredMethod("validateEmployeeMasterRequest",
                    EmployeeMasterRequestDto.class, boolean.class, Long.class);
            val.setAccessible(true);
            EmployeeMasterRequestDto r = new EmployeeMasterRequestDto();
            r.setServiceType("REMOTE");
            r.setFirstName(null);
            val.invoke(service, r, false, null);
            verify(masterRepository, never()).existsByEmployeeName(any());
        }

        @Test
        void validateDependentsSkipsEmployeeCheckWhenEmployeePoidNull() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("validateDependents", Long.class, List.class);
            m.setAccessible(true);
            EmployeeDependentsDtlRequestDto dto = EmployeeDependentsDtlRequestDto.builder()
                    .actionType(ActionType.isCreated)
                    .name("N1")
                    .nationality(null)
                    .build();
            m.invoke(service, null, List.of(dto));
            verify(masterRepository, never()).existsByEmployeePoid(any());
        }

        @Test
        void validateDependentsCreatedAllowsNullName() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("validateDependents", Long.class, List.class);
            m.setAccessible(true);
            when(masterRepository.existsByEmployeePoid(1L)).thenReturn(true);
            EmployeeDependentsDtlRequestDto dto = EmployeeDependentsDtlRequestDto.builder()
                    .actionType(ActionType.isCreated)
                    .name(null)
                    .nationality(null)
                    .build();
            m.invoke(service, 1L, List.of(dto));
            verify(dependentRepository, never()).existsByName(any());
        }

        @Test
        void validateDependentsUpdatedSkipsDuplicateWhenExistingNameBlank() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("validateDependents", Long.class, List.class);
            m.setAccessible(true);
            when(masterRepository.existsByEmployeePoid(1L)).thenReturn(true);
            HrEmployeeDependentsDtl existing = new HrEmployeeDependentsDtl();
            existing.setName(" ");
            when(dependentRepository.findById(any(HrEmployeeDependentsDtlId.class))).thenReturn(Optional.of(existing));
            EmployeeDependentsDtlRequestDto dto = EmployeeDependentsDtlRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .detRowId(1L)
                    .name("NEW")
                    .build();
            m.invoke(service, 1L, List.of(dto));
            verify(dependentRepository, never()).existsByName(anyString());
        }

        @Test
        void validateDependentsUpdatedSkipsDuplicateWhenNameUnchanged() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("validateDependents", Long.class, List.class);
            m.setAccessible(true);
            when(masterRepository.existsByEmployeePoid(1L)).thenReturn(true);
            HrEmployeeDependentsDtl existing = new HrEmployeeDependentsDtl();
            existing.setName("SAME");
            when(dependentRepository.findById(any(HrEmployeeDependentsDtlId.class))).thenReturn(Optional.of(existing));
            EmployeeDependentsDtlRequestDto dto = EmployeeDependentsDtlRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .detRowId(1L)
                    .name("SAME")
                    .build();
            m.invoke(service, 1L, List.of(dto));
            verify(dependentRepository, never()).existsByName(anyString());
        }

        @Test
        void validateLmraSkipsEmployeeCheckWhenEmployeePoidNull() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("validateLmraDetails", Long.class, List.class);
            m.setAccessible(true);
            m.invoke(service, null, List.of(lmraCreated(null)));
            verify(masterRepository, never()).existsByEmployeePoid(any());
        }

        @Test
        void validateLmraThrowsWhenDeleteMissingDetRowId() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("validateLmraDetails", Long.class, List.class);
            m.setAccessible(true);
            when(masterRepository.existsByEmployeePoid(1L)).thenReturn(true);
            EmployeeDepndtsLmraDtlsRequestDto dto = EmployeeDepndtsLmraDtlsRequestDto.builder()
                    .actionType(ActionType.isDeleted)
                    .detRowId(null)
                    .build();
            assertThatThrownBy(() -> m.invoke(service, 1L, List.of(dto)))
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .rootCause()
                    .hasMessageContaining("lmra");
        }

        @Test
        void validateExperienceSkipsEmployeeCheckWhenEmployeePoidNull() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("validateExperienceDetails", Long.class, List.class);
            m.setAccessible(true);
            m.invoke(service, null, List.of(expCreated("ACME", null)));
            verify(masterRepository, never()).existsByEmployeePoid(any());
        }

        @Test
        void validateExperienceSkipsEmployerChecksWhenBlank() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("validateExperienceDetails", Long.class, List.class);
            m.setAccessible(true);
            when(masterRepository.existsByEmployeePoid(1L)).thenReturn(true);
            EmployeeExperienceDtlRequestDto created = EmployeeExperienceDtlRequestDto.builder()
                    .actionType(ActionType.isCreated)
                    .employer("  ")
                    .detRowId(1L)
                    .build();
            m.invoke(service, 1L, List.of(created));
            verify(experienceRepository, never()).existsByEmployerIgnoreCase(anyString());
        }

        @Test
        void validateExperienceUpdateSkipsEmployerDuplicateWhenEmployerBlank() throws Exception {
            Method m = EmployeeMasterServiceImpl.class.getDeclaredMethod("validateExperienceDetails", Long.class, List.class);
            m.setAccessible(true);
            when(masterRepository.existsByEmployeePoid(1L)).thenReturn(true);
            EmployeeExperienceDtlRequestDto updated = EmployeeExperienceDtlRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .detRowId(2L)
                    .employer(null)
                    .build();
            m.invoke(service, 1L, List.of(updated));
            verify(experienceRepository, never()).existsByEmployerIgnoreCaseAndEmployeePoidNot(anyString(), anyLong());
        }
    }

    @Nested
    class ValidationBranches {
        @Test
        void validateMasterRequestCoversManyBranchesViaCreateUpdate() {
            // 1) PERMANENT requires CPR + CR_POID
            EmployeeMasterRequestDto permMissing = baseValidRequest("PERMANENT", 2L);
            permMissing.setCprNo(" ");
            permMissing.setCrPoid(null);
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("N"));
            assertThatThrownBy(() -> service.createEmployee(permMissing))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("CPR Number");

            // 2) PERMANENT expat requires ticket fields (but none provided)
            EmployeeMasterRequestDto permExpatMissingTickets = baseValidRequest("PERMANENT", 2L);
            permExpatMissingTickets.setAirSectorPoid(null);
            permExpatMissingTickets.setTicketPeriod(null);
            permExpatMissingTickets.setNoOfTickets(null);
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            assertThatThrownBy(() -> service.createEmployee(permExpatMissingTickets))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("AirSector");

            // 3) Non-permanent or Bahraini: ticket fields NOT allowed if any present
            EmployeeMasterRequestDto remoteWithTickets = baseValidRequest("REMOTE", 1L);
            remoteWithTickets.setAirSectorPoid(1L);
            remoteWithTickets.setTicketPeriod("P");
            remoteWithTickets.setNoOfTickets("1");
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            assertThatThrownBy(() -> service.createEmployee(remoteWithTickets))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("NOT required");

            // 4) discontinued=Y requires date
            EmployeeMasterRequestDto discNoDate = baseValidRequest("REMOTE", 1L);
            discNoDate.setDiscontinued("Y");
            discNoDate.setDiscontinuedDate(null);
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            assertThatThrownBy(() -> service.createEmployee(discNoDate))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Discontinued date");

            // 5) manual employee code on create when param=Y
            EmployeeMasterRequestDto missingEmpCode = baseValidRequest("REMOTE", 1L);
            missingEmpCode.setEmployeeCode(" ");
            when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("Y"));
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            assertThatThrownBy(() -> service.createEmployee(missingEmpCode))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Employee Code Is Required");
        }

        @Test
        void validateMasterRequestCoversNotFoundAndAlreadyExistsBranches() {
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("N"));

            // Not found checks (PERMANENT expat -> ticket fields allowed, and AirSector existence is validated)
            EmployeeMasterRequestDto perm = baseValidRequest("PERMANENT", 999L);
            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(false);
            assertThatThrownBy(() -> service.createEmployee(perm))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Air Sector");

            // Already exists: mobile on create
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setMobile("123");
            when(masterRepository.existsByMobile("123")).thenReturn(true);
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("Mobile");

            // Already exists: name on create
            when(masterRepository.existsByMobile("123")).thenReturn(false);
            req.setFirstName("AA");
            when(masterRepository.existsByEmployeeName("AA")).thenReturn(true);
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("Name");

            // Already exists: code / cpr / iban on create (trim branch)
            when(masterRepository.existsByEmployeeName("AA")).thenReturn(false);
            req.setEmployeeCode(" E1 ");
            when(masterRepository.existsByEmployeeCode("E1")).thenReturn(true);
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("Employee Code");

            when(masterRepository.existsByEmployeeCode("E1")).thenReturn(false);
            req.setCprNo(" C1 ");
            when(masterRepository.existsByCprNo("C1")).thenReturn(true);
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("CPR No");

            when(masterRepository.existsByCprNo("C1")).thenReturn(false);
            req.setIban(" IB1 ");
            when(masterRepository.existsByIban("IB1")).thenReturn(true);
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("IBAN");

            // GL clients are invoked when POIDs provided
            when(masterRepository.existsByIban("IB1")).thenReturn(false);
            req.setEmpGlPoid(9L);
            req.setPettyCashGlPoid(10L);
            // stop deeper execution by forcing a "save" to throw (we only want to see glMasterServiceClient called)
            when(masterRepository.save(any())).thenThrow(new RuntimeException("stop"));
            assertThatThrownBy(() -> service.createEmployee(req)).isInstanceOf(RuntimeException.class);
            verify(glMasterServiceClient).findById(9L);
            verify(glMasterServiceClient).findById(10L);
        }
    }

    @Nested
    class DependentAndChildValidationBranches {
        @Test
        void validateDependentsCoversUpdateDeleteArgChecksAndDuplicateNameLogic() {
            EmployeeMasterRequestDto request = baseValidRequest("REMOTE", 1L);
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("N"));

            // allow header save and skip GL
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(50L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(50L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(50L);
                setEmpGlPoid(1L);
            }}));
            when(masterRepository.existsByEmployeePoid(50L)).thenReturn(true);
            when(hrNationalityRepository.existsByNationalityCode(anyString())).thenReturn(true);

            // created duplicate -> ResourceAlreadyExists
            request.setDependentsDetails(List.of(dependentCreated("DUP", null)));
            when(dependentRepository.existsByName("DUP")).thenReturn(true);
            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceAlreadyExistsException.class);

            // update missing detRowId -> IllegalArgumentException
            request.setDependentsDetails(List.of(EmployeeDependentsDtlRequestDto.builder().actionType(ActionType.isUpdated).name("x").build()));
            when(dependentRepository.existsByName(anyString())).thenReturn(false);
            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("detRowId");

            // delete missing detRowId -> IllegalArgumentException
            request.setDependentsDetails(List.of(EmployeeDependentsDtlRequestDto.builder().actionType(ActionType.isDeleted).build()));
            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(IllegalArgumentException.class);

            // update branch: existing name differs and new duplicates
            request.setDependentsDetails(List.of(EmployeeDependentsDtlRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .detRowId(1L)
                    .name("NEW")
                    .build()));
            HrEmployeeDependentsDtl existing = new HrEmployeeDependentsDtl();
            existing.setEmployeePoid(50L);
            existing.setDetRowId(1L);
            existing.setName("OLD");
            when(dependentRepository.findById(any(HrEmployeeDependentsDtlId.class))).thenReturn(Optional.of(existing));
            when(dependentRepository.existsByName("NEW")).thenReturn(true);
            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceAlreadyExistsException.class);

            // update branch: existing row missing -> ResourceNotFound
            when(dependentRepository.findById(any(HrEmployeeDependentsDtlId.class))).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Dependents");
        }

        @Test
        void validateLmraAndExperienceCoversArgChecksAndDuplicateEmployer() {
            EmployeeMasterRequestDto request = baseValidRequest("REMOTE", 1L);
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("N"));

            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(60L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(60L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(60L);
                setEmpGlPoid(1L);
            }}));
            when(masterRepository.existsByEmployeePoid(60L)).thenReturn(true);

            // lmra updated missing detRowId
            request.setLmraDetails(List.of(EmployeeDepndtsLmraDtlsRequestDto.builder().actionType(ActionType.isUpdated).build()));
            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lmra");

            // experience created duplicate employer
            request.setLmraDetails(null);
            request.setExperienceDetails(List.of(expCreated("ACME", null)));
            when(experienceRepository.existsByEmployerIgnoreCase("ACME")).thenReturn(true);
            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("Employer");

            // experience updated missing detRowId
            request.setExperienceDetails(List.of(EmployeeExperienceDtlRequestDto.builder().actionType(ActionType.isUpdated).employer("E").build()));
            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("experience");

            // experience deleted missing detRowId
            request.setExperienceDetails(List.of(EmployeeExperienceDtlRequestDto.builder().actionType(ActionType.isDeleted).build()));
            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class AdditionalNotFoundValidationLines {
        @Test
        void validateDependentsThrowsWhenEmployeeDoesNotExist() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            request.setDependentsDetails(List.of(dependentCreated("dep", null)));

            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(600L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(600L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(600L);
                setEmpGlPoid(1L);
            }}));

            // employee existence check in validateDependents
            when(masterRepository.existsByEmployeePoid(600L)).thenReturn(false);

            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Employee Poid");
        }

        @Test
        void validateDependentsThrowsWhenNationalityCodeInvalid() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            request.setDependentsDetails(List.of(EmployeeDependentsDtlRequestDto.builder()
                    .actionType(ActionType.isCreated)
                    .name("dep")
                    .nationality("XX")
                    .build()));

            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(601L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(601L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(601L);
                setEmpGlPoid(1L);
            }}));
            when(masterRepository.existsByEmployeePoid(601L)).thenReturn(true);
            when(hrNationalityRepository.existsByNationalityCode("XX")).thenReturn(false);

            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Nationality Code");
        }

        @Test
        void validateLmraThrowsWhenEmployeeDoesNotExist() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            request.setLmraDetails(List.of(lmraCreated(null)));

            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(602L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(602L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(602L);
                setEmpGlPoid(1L);
            }}));
            when(masterRepository.existsByEmployeePoid(602L)).thenReturn(false);

            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Employee Poid");
        }

        @Test
        void validateExperienceThrowsWhenEmployeeDoesNotExist() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            request.setExperienceDetails(List.of(expCreated("ACME", null)));

            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(603L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(603L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(603L);
                setEmpGlPoid(1L);
            }}));
            when(masterRepository.existsByEmployeePoid(603L)).thenReturn(false);

            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Employee Poid");
        }

        @Test
        void validateMasterRequestThrowsWhenReferencesMissing() {
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setDepartmentPoid(10L);
            req.setReligionPoid(11L);
            req.setDesignationPoid(12L);
            req.setShiftPoid(13L);
            req.setDiscontinued("DISC");
            req.setLocationPoid(14L);
            req.setCrPoid(15L);

            when(hrDepartmentMasterRepository.existsByDeptPoid(10L)).thenReturn(false);
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Department");

            when(hrDepartmentMasterRepository.existsByDeptPoid(10L)).thenReturn(true);
            when(religionRepository.existsByReligionPoid(11L)).thenReturn(false);
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Religion");

            when(religionRepository.existsByReligionPoid(11L)).thenReturn(true);
            when(designationRepository.existsByDesigPoid(12L)).thenReturn(false);
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Designation");

            when(designationRepository.existsByDesigPoid(12L)).thenReturn(true);
            when(globalShiftMasterRepository.existsByShiftPoid(13L)).thenReturn(false);
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Shift");

            when(globalShiftMasterRepository.existsByShiftPoid(13L)).thenReturn(true);
            when(globalFixedVariablesRepository.existsByVariableName("DISC")).thenReturn(false);
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Fixed Variable");

            when(globalFixedVariablesRepository.existsByVariableName("DISC")).thenReturn(true);
            when(locationMasterRepository.existsByLocationPoid(14L)).thenReturn(false);
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Location");

            when(locationMasterRepository.existsByLocationPoid(14L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(15L)).thenReturn(false);
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Admin Cr Master");
        }

        @Test
        void validateExperienceUpdateThrowsWhenEmployerDuplicateForOtherEmployee() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            request.setExperienceDetails(List.of(EmployeeExperienceDtlRequestDto.builder()
                    .actionType(ActionType.isUpdated)
                    .detRowId(1L)
                    .employer("ACME")
                    .build()));

            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(604L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(604L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(604L);
                setEmpGlPoid(1L);
            }}));
            when(masterRepository.existsByEmployeePoid(604L)).thenReturn(true);
            when(experienceRepository.existsByEmployerIgnoreCaseAndEmployeePoidNot("ACME", 604L)).thenReturn(true);

            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("Employer");
        }

        @Test
        void validateLmraDeleteThrowsWhenDetRowIdMissing() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            request.setLmraDetails(List.of(EmployeeDepndtsLmraDtlsRequestDto.builder()
                    .actionType(ActionType.isDeleted)
                    .detRowId(null)
                    .build()));

            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);
            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(605L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(605L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(605L);
                setEmpGlPoid(1L);
            }}));
            when(masterRepository.existsByEmployeePoid(605L)).thenReturn(true);

            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lmra isDeleted");
        }

        @Test
        void validateUpdateUniquenessChecksThrowWhenDuplicate() {
            HrEmployeeMaster existing = new HrEmployeeMaster();
            existing.setEmployeePoid(700L);
            existing.setEmpGlPoid(1L);
            when(masterRepository.findByEmployeePoid(700L)).thenReturn(Optional.of(existing));

            EmployeeMasterRequestDto request = baseValidRequest("REMOTE", 1L);
            request.setMobile("123");
            when(masterRepository.existsByMobileAndEmployeePoidNot("123", 700L)).thenReturn(true);
            assertThatThrownBy(() -> service.updateEmployee(700L, request))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("Mobile");

            when(masterRepository.existsByMobileAndEmployeePoidNot("123", 700L)).thenReturn(false);
            request.setFirstName("AA");
            when(masterRepository.existsByEmployeeNameAndEmployeePoidNot("AA", 700L)).thenReturn(true);
            assertThatThrownBy(() -> service.updateEmployee(700L, request))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("Name");

            when(masterRepository.existsByEmployeeNameAndEmployeePoidNot("AA", 700L)).thenReturn(false);
            request.setEmployeeCode(" E1 ");
            when(masterRepository.existsByEmployeeCodeAndEmployeePoidNot("E1", 700L)).thenReturn(true);
            assertThatThrownBy(() -> service.updateEmployee(700L, request))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("Employee Code");

            when(masterRepository.existsByEmployeeCodeAndEmployeePoidNot("E1", 700L)).thenReturn(false);
            request.setCprNo(" C1 ");
            when(masterRepository.existsByCprNoAndEmployeePoidNot("C1", 700L)).thenReturn(true);
            assertThatThrownBy(() -> service.updateEmployee(700L, request))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("CPR No");

            when(masterRepository.existsByCprNoAndEmployeePoidNot("C1", 700L)).thenReturn(false);
            request.setIban(" IB1 ");
            when(masterRepository.existsByIbanAndEmployeePoidNot("IB1", 700L)).thenReturn(true);
            assertThatThrownBy(() -> service.updateEmployee(700L, request))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("IBAN");
        }
    }

    @Nested
    class CreateEmployeeGlIfMissingBranches {
        @Test
        void skipsWhenEmpGlPoidAlreadyPresent() {
            HrEmployeeMaster entity = new HrEmployeeMaster();
            entity.setEmployeePoid(99L);
            entity.setEmpGlPoid(1L);
            when(masterRepository.findByEmployeePoid(99L)).thenReturn(Optional.of(entity));
            // invoke via update (which calls it after save)
            when(masterRepository.save(entity)).thenReturn(entity);
            when(employeeMasterMapper.toResponseDto(entity)).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(99L).build());

            service.updateEmployee(99L, baseValidRequest("REMOTE", 1L));
        }

        @Test
        void throwsWhenProcReturnsErrorAndWrapsDataAccessException() {
            HrEmployeeMaster entity = new HrEmployeeMaster();
            entity.setEmployeePoid(100L);
            entity.setEmpGlPoid(null);
            entity.setEmployeeCode("C");
            entity.setEmployeeName("N1");
            entity.setEmployeeName2("N2"); // cover name2 join branch
            when(masterRepository.findByEmployeePoid(100L)).thenReturn(Optional.of(entity));
            when(masterRepository.save(entity)).thenReturn(entity);

            // 1) status contains ERROR => ValidationException
            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(Map.of("P_RESULT", "ERROR:bad"));
            })) {
                assertThatThrownBy(() -> service.updateEmployee(100L, baseValidRequest("REMOTE", 1L)))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("ERROR");
            }

            // 2) DataAccessException => wrapped ValidationException
            DataAccessException dae = new org.springframework.dao.RecoverableDataAccessException("boom");
            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenThrow(dae);
            })) {
                assertThatThrownBy(() -> service.updateEmployee(100L, baseValidRequest("REMOTE", 1L)))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("PROC_GL_MASTER_CREATION failed");
            }
        }

        @Test
        void procSuccessPathDoesNotThrow() {
            HrEmployeeMaster entity = new HrEmployeeMaster();
            entity.setEmployeePoid(101L);
            entity.setEmpGlPoid(null);
            entity.setEmployeeCode("C");
            entity.setEmployeeName("N1");
            when(masterRepository.findByEmployeePoid(101L)).thenReturn(Optional.of(entity));
            when(masterRepository.save(entity)).thenReturn(entity);
            when(employeeMasterMapper.toResponseDto(entity)).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(101L).build());

            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(Map.of("P_RESULT", "SUCCESS"));
            })) {
                service.updateEmployee(101L, baseValidRequest("REMOTE", 1L));
            }

            try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
                when(mock.withProcedureName(anyString())).thenReturn(mock);
                when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                when(mock.execute(any(SqlParameterSource.class))).thenReturn(new HashMap<>());
            })) {
                service.updateEmployee(101L, baseValidRequest("REMOTE", 1L));
            }
        }

        @Test
        void createEmployeeGlIfMissingThrowsWhenEmployeeMissing() {
            HrEmployeeMaster entity = new HrEmployeeMaster();
            entity.setEmployeePoid(202L);
            entity.setEmpGlPoid(null);
            entity.setEmployeeCode("C");
            entity.setEmployeeName("N1");

            // updateEmployee reads once; createEmployeeGlIfMissing reads again
            when(masterRepository.findByEmployeePoid(202L)).thenReturn(Optional.of(entity), Optional.empty());
            when(masterRepository.save(entity)).thenReturn(entity);

            assertThatThrownBy(() -> service.updateEmployee(202L, baseValidRequest("REMOTE", 1L)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class MoreValidationBranches {
        @Test
        void throwsWhenHodDoesNotExist() {
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setHod(55L);
            when(masterRepository.existsByEmployeePoid(55L)).thenReturn(false);
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Direct Supervisor");
        }

        @Test
        void throwsWhenUpdateSelectsSelfAsHod() {
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setHod(10L);
            when(masterRepository.findByEmployeePoid(10L)).thenReturn(Optional.of(new HrEmployeeMaster()));
            assertThatThrownBy(() -> service.updateEmployee(10L, req))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("direct supervisor");
        }

        @Test
        void createDoesNotApplySelfHodRuleWhenCurrentEmployeeUnknown() {
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setHod(10L);
            when(masterRepository.existsByEmployeePoid(10L)).thenReturn(true);
            when(masterRepository.save(any())).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(803L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(803L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(803L);
                setEmpGlPoid(1L);
            }}));
            when(employeeMasterMapper.toResponseDto(any())).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(803L).build());
            service.createEmployee(req);
        }

        @Test
        void updateWithNullHodSkipsSelfSupervisorRule() {
            HrEmployeeMaster existing = new HrEmployeeMaster();
            existing.setEmployeePoid(12L);
            existing.setEmpGlPoid(1L);
            when(masterRepository.findByEmployeePoid(12L)).thenReturn(Optional.of(existing));
            when(masterRepository.save(existing)).thenReturn(existing);
            when(employeeMasterMapper.toResponseDto(existing)).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(12L).build());
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setHod(null);
            service.updateEmployee(12L, req);
        }

        @Test
        void updateAcceptsDifferentHodThanSelf() {
            HrEmployeeMaster existing = new HrEmployeeMaster();
            existing.setEmployeePoid(10L);
            existing.setEmpGlPoid(1L);
            when(masterRepository.findByEmployeePoid(10L)).thenReturn(Optional.of(existing));
            when(masterRepository.save(existing)).thenReturn(existing);
            when(employeeMasterMapper.toResponseDto(existing)).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(10L).build());
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setHod(99L);
            when(masterRepository.existsByEmployeePoid(99L)).thenReturn(true);
            service.updateEmployee(10L, req);
            verify(employeeMasterMapper).applyHeaderFields(existing, req);
        }

        @Test
        void skipsManualEmployeeCodeRequirementOnUpdate() {
            HrEmployeeMaster existing = new HrEmployeeMaster();
            existing.setEmployeePoid(500L);
            existing.setEmpGlPoid(1L);
            when(masterRepository.findByEmployeePoid(500L)).thenReturn(Optional.of(existing));
            when(masterRepository.save(existing)).thenReturn(existing);
            when(employeeMasterMapper.toResponseDto(existing)).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(500L).build());
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setEmployeeCode(null);
            service.updateEmployee(500L, req);
        }

        @Test
        void permanentExpatRequiresAllTicketFieldsWhenOnlyPartiallyPresent() {
            EmployeeMasterRequestDto req = baseValidRequest("PERMANENT", 2L);
            req.setTicketPeriod(null);
            req.setNoOfTickets(null);
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("AirSector");
        }

        @Test
        void remoteBahrainiRejectsAnyTicketField() {
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setAirSectorPoid(1L);
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("NOT required");
        }

        @Test
        void remoteBahrainiRejectsPartialTicketFields() {
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setTicketPeriod("P");
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("NOT required");
        }

        @Test
        void remoteBahrainiRejectsWhenOnlyNoOfTicketsSet() {
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setNoOfTickets("9");
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("NOT required");
        }

        @Test
        void permanentExpatPartialTicketsWithBlankPeriodFailsAllTicketsCheck() {
            EmployeeMasterRequestDto req = baseValidRequest("PERMANENT", 2L);
            req.setTicketPeriod("   ");
            req.setNoOfTickets("1");
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("AirSector");
        }

        @Test
        void permanentExpatMissingNoOfTicketsFailsAllTicketsCheck() {
            EmployeeMasterRequestDto req = baseValidRequest("PERMANENT", 2L);
            req.setTicketPeriod("1Y");
            req.setNoOfTickets(null);
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("AirSector");
        }

        @Test
        void discontinuedYWithDateDoesNotThrowDateValidation() {
            EmployeeMasterRequestDto req = baseValidRequest("FLEXI", 2L);
            req.setDiscontinued("Y");
            req.setDiscontinuedDate(LocalDate.of(2026, 6, 1));
            when(globalFixedVariablesRepository.existsByVariableName("Y")).thenReturn(true);
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("N"));
            when(masterRepository.save(any())).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(880L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(880L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(880L);
                setEmpGlPoid(1L);
            }}));
            when(employeeMasterMapper.toResponseDto(any())).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(880L).build());
            service.createEmployee(req);
        }

        @Test
        void discontinuedValueWithoutYDoesNotRequireDate() {
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setDiscontinued("banana");
            req.setDiscontinuedDate(null);
            when(globalFixedVariablesRepository.existsByVariableName("banana")).thenReturn(true);
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("N"));
            when(masterRepository.save(any())).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(804L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(804L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(804L);
                setEmpGlPoid(1L);
            }}));
            when(employeeMasterMapper.toResponseDto(any())).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(804L).build());
            service.createEmployee(req);
        }

        @Test
        void discontinuedNoDoesNotRequireDiscontinuedDate() {
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setDiscontinued("N");
            req.setDiscontinuedDate(null);
            when(globalFixedVariablesRepository.existsByVariableName("N")).thenReturn(true);
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("N"));
            when(masterRepository.save(any())).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(802L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(802L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(802L);
                setEmpGlPoid(1L);
            }}));
            when(employeeMasterMapper.toResponseDto(any())).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(802L).build());
            service.createEmployee(req);
        }

        @Test
        void permanentRequiresCrPoidWhenCprPresent() {
            EmployeeMasterRequestDto req = baseValidRequest("PERMANENT", 2L);
            req.setCprNo("12345");
            req.setCrPoid(null);
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Emp Reg Co");
        }

        @Test
        void permanentBahrainiMustNotSendTicketFields() {
            EmployeeMasterRequestDto req = baseValidRequest("PERMANENT", 1L);
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("NOT required");
        }

        @Test
        void discontinuedFlagContainingYRequiresDate() {
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setDiscontinued("YES");
            req.setDiscontinuedDate(null);
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Discontinued date");
        }

        @Test
        void manualEmployeeCodeDetectedWithTrimmedParameterValue() {
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setEmployeeCode(null);
            when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("  y  "));
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Employee Code Is Required");
        }

        @Test
        void manualEmployeeCodeYAllowsCreateWhenCodeProvided() {
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setEmployeeCode("ECODE");
            when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("Y"));
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            when(masterRepository.save(any())).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(805L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(805L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(805L);
                setEmpGlPoid(1L);
            }}));
            when(employeeMasterMapper.toResponseDto(any())).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(805L).build());
            service.createEmployee(req);
        }

        @Test
        void skipsNationalityExistsCheckWhenNationalityPoidNull() {
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", null);
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("N"));
            when(masterRepository.save(any())).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(801L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(801L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(801L);
                setEmpGlPoid(1L);
            }}));
            when(employeeMasterMapper.toResponseDto(any())).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(801L).build());
            service.createEmployee(req);
            verify(hrNationalityRepository, never()).existsByNationPoid(any());
        }

        @Test
        void throwsWhenNationalityPoidNotFound() {
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 5L);
            when(hrNationalityRepository.existsByNationPoid(5L)).thenReturn(false);
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("N"));
            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Nationality");
        }

        @Test
        void updateThrowsWhenMobileBelongsToAnotherEmployee() {
            HrEmployeeMaster existing = new HrEmployeeMaster();
            existing.setEmployeePoid(44L);
            when(masterRepository.findByEmployeePoid(44L)).thenReturn(Optional.of(existing));
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setMobile("999");
            when(masterRepository.existsByMobileAndEmployeePoidNot("999", 44L)).thenReturn(true);
            assertThatThrownBy(() -> service.updateEmployee(44L, req))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("Mobile");
        }

        @Test
        void updateThrowsWhenFirstNameBelongsToAnotherEmployee() {
            HrEmployeeMaster existing = new HrEmployeeMaster();
            existing.setEmployeePoid(45L);
            when(masterRepository.findByEmployeePoid(45L)).thenReturn(Optional.of(existing));
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setFirstName("Taken");
            when(masterRepository.existsByEmployeeNameAndEmployeePoidNot("Taken", 45L)).thenReturn(true);
            assertThatThrownBy(() -> service.updateEmployee(45L, req))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("Name");
        }

        @Test
        void createAndUpdateAllowMobileWhenNoConflict() {
            EmployeeMasterRequestDto createReq = baseValidRequest("REMOTE", 1L);
            createReq.setMobile("555");
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("N"));
            when(masterRepository.existsByMobile("555")).thenReturn(false);
            when(masterRepository.save(any())).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(806L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(806L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(806L);
                setEmpGlPoid(1L);
            }}));
            when(employeeMasterMapper.toResponseDto(any())).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(806L).build());
            service.createEmployee(createReq);

            HrEmployeeMaster existing = new HrEmployeeMaster();
            existing.setEmployeePoid(807L);
            existing.setEmpGlPoid(1L);
            when(masterRepository.findByEmployeePoid(807L)).thenReturn(Optional.of(existing));
            when(masterRepository.save(existing)).thenReturn(existing);
            when(employeeMasterMapper.toResponseDto(existing)).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(807L).build());
            EmployeeMasterRequestDto updateReq = baseValidRequest("REMOTE", 1L);
            updateReq.setMobile("556");
            when(masterRepository.existsByMobileAndEmployeePoidNot("556", 807L)).thenReturn(false);
            service.updateEmployee(807L, updateReq);
        }

        @Test
        void updateSkipsNameUniquenessWhenFirstNameNull() {
            HrEmployeeMaster existing = new HrEmployeeMaster();
            existing.setEmployeePoid(49L);
            existing.setEmpGlPoid(1L);
            when(masterRepository.findByEmployeePoid(49L)).thenReturn(Optional.of(existing));
            when(masterRepository.save(existing)).thenReturn(existing);
            when(employeeMasterMapper.toResponseDto(existing)).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(49L).build());
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setFirstName(null);
            service.updateEmployee(49L, req);
            verify(masterRepository, never()).existsByEmployeeNameAndEmployeePoidNot(any(), any());
        }

        @Test
        void updateSkipsCprAndIbanUniquenessWhenNoConflict() {
            HrEmployeeMaster existing = new HrEmployeeMaster();
            existing.setEmployeePoid(48L);
            existing.setEmpGlPoid(1L);
            when(masterRepository.findByEmployeePoid(48L)).thenReturn(Optional.of(existing));
            when(masterRepository.save(existing)).thenReturn(existing);
            when(employeeMasterMapper.toResponseDto(existing)).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(48L).build());
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setCprNo("CPR2");
            req.setIban("IB2");
            when(masterRepository.existsByCprNoAndEmployeePoidNot("CPR2", 48L)).thenReturn(false);
            when(masterRepository.existsByIbanAndEmployeePoidNot("IB2", 48L)).thenReturn(false);
            service.updateEmployee(48L, req);
        }

        @Test
        void updateSkipsUniquenessAndCrLookupWhenOptionalFieldsBlankOrCrNull() {
            HrEmployeeMaster existing = new HrEmployeeMaster();
            existing.setEmployeePoid(47L);
            existing.setEmpGlPoid(1L);
            when(masterRepository.findByEmployeePoid(47L)).thenReturn(Optional.of(existing));
            when(masterRepository.save(existing)).thenReturn(existing);
            when(employeeMasterMapper.toResponseDto(existing)).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(47L).build());
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);
            req.setMobile(null);
            req.setEmployeeCode(" ");
            req.setCprNo(" ");
            req.setIban(" ");
            req.setCrPoid(null);
            service.updateEmployee(47L, req);
            verify(masterRepository, never()).existsByMobileAndEmployeePoidNot(any(), any());
            verify(masterRepository, never()).existsByEmployeeCodeAndEmployeePoidNot(any(), any());
            verify(masterRepository, never()).existsByCprNoAndEmployeePoidNot(any(), any());
            verify(masterRepository, never()).existsByIbanAndEmployeePoidNot(any(), any());
            verify(crMasterRepository, never()).existsByCrPoid(any());
        }

        @Test
        void updateThrowsWhenEmployeeCodeCprOrIbanBelongsToAnotherEmployee() {
            HrEmployeeMaster existing = new HrEmployeeMaster();
            existing.setEmployeePoid(46L);
            when(masterRepository.findByEmployeePoid(46L)).thenReturn(Optional.of(existing));
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);

            req.setEmployeeCode("E2");
            when(masterRepository.existsByEmployeeCodeAndEmployeePoidNot("E2", 46L)).thenReturn(true);
            assertThatThrownBy(() -> service.updateEmployee(46L, req))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("Employee Code");
            when(masterRepository.existsByEmployeeCodeAndEmployeePoidNot("E2", 46L)).thenReturn(false);

            req.setCprNo("C2");
            when(masterRepository.existsByCprNoAndEmployeePoidNot("C2", 46L)).thenReturn(true);
            assertThatThrownBy(() -> service.updateEmployee(46L, req))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("CPR No");
            when(masterRepository.existsByCprNoAndEmployeePoidNot("C2", 46L)).thenReturn(false);

            req.setIban("I2");
            when(masterRepository.existsByIbanAndEmployeePoidNot("I2", 46L)).thenReturn(true);
            assertThatThrownBy(() -> service.updateEmployee(46L, req))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("IBAN");
        }
    }

    @Nested
    class ApplyChildTablesBranches {
        @Test
        void applyCreatedUpdatedDeletedForAllChildTablesThroughCreate() {
            EmployeeMasterRequestDto request = baseValidRequest("PERMANENT", 999L);
            when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenReturn(Optional.of("N"));
            when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("1"));
            when(airsectorRepository.existsByAirsecPoid(1L)).thenReturn(true);
            when(crMasterRepository.existsByCrPoid(1L)).thenReturn(true);

            when(masterRepository.save(any(HrEmployeeMaster.class))).thenAnswer(inv -> {
                HrEmployeeMaster e = inv.getArgument(0);
                e.setEmployeePoid(70L);
                e.setEmpGlPoid(1L);
                return e;
            });
            when(masterRepository.findByEmployeePoid(70L)).thenReturn(Optional.of(new HrEmployeeMaster() {{
                setEmployeePoid(70L);
                setEmpGlPoid(1L);
            }}));
            when(masterRepository.existsByEmployeePoid(70L)).thenReturn(true);
            when(hrNationalityRepository.existsByNationalityCode(anyString())).thenReturn(true);

            // existing lists to make nextDetRowId non-trivial
            when(dependentRepository.findByEmployeePoid(70L)).thenReturn(List.of(new HrEmployeeDependentsDtl() {{
                setEmployeePoid(70L);
                setDetRowId(5L);
            }}));
            when(lmraRepository.findByEmployeePoid(70L)).thenReturn(List.of(new HrEmpDepndtsLmraDtls() {{
                setEmployeePoid(70L);
                setDetRowId(6L);
            }}));
            when(experienceRepository.findByEmployeePoid(70L)).thenReturn(List.of(new HrEmployeeExperienceDtl() {{
                setEmployeePoid(70L);
                setDetRowId(7L);
            }}));
            when(documentRepository.findByEmployeePoid(70L)).thenReturn(List.of(new HrEmployeeDocumentDtl() {{
                setEmployeePoid(70L);
                setDetRowId(8L);
            }}));

            // mixed actions; also include null dto / noChange to cover continues
            request.setDependentsDetails(Arrays.asList(
                    EmployeeDependentsDtlRequestDto.builder().actionType(ActionType.noChange).build(),
                    dependentCreated("A", 100L),
                    EmployeeDependentsDtlRequestDto.builder().actionType(ActionType.isUpdated).detRowId(5L).name("B").build(),
                    EmployeeDependentsDtlRequestDto.builder().actionType(ActionType.isDeleted).detRowId(5L).build()
            ));
            request.setLmraDetails(Arrays.asList(
                    EmployeeDepndtsLmraDtlsRequestDto.builder().actionType(ActionType.noChange).build(),
                    lmraCreated(100L),
                    EmployeeDepndtsLmraDtlsRequestDto.builder().actionType(ActionType.isUpdated).detRowId(6L).expatName("X").build(),
                    EmployeeDepndtsLmraDtlsRequestDto.builder().actionType(ActionType.isDeleted).detRowId(6L).build()
            ));
            request.setExperienceDetails(Arrays.asList(
                    EmployeeExperienceDtlRequestDto.builder().actionType(ActionType.noChange).build(),
                    expCreated("E1", 100L),
                    EmployeeExperienceDtlRequestDto.builder().actionType(ActionType.isUpdated).detRowId(7L).employer("E2").build(),
                    EmployeeExperienceDtlRequestDto.builder().actionType(ActionType.isDeleted).detRowId(7L).build()
            ));
            request.setDocumentDetails(Arrays.asList(
                    EmployeeDocumentDtlRequestDto.builder().actionType(ActionType.noChange).build(),
                    docCreated("D1", 100L),
                    EmployeeDocumentDtlRequestDto.builder().actionType(ActionType.isUpdated).detRowId(8L).docName("D2").build(),
                    EmployeeDocumentDtlRequestDto.builder().actionType(ActionType.isDeleted).detRowId(8L).build()
            ));

            // validate dependents "created" duplicate false
            when(dependentRepository.existsByName(anyString())).thenReturn(false);
            // validate experience duplicates false
            when(experienceRepository.existsByEmployerIgnoreCase(anyString())).thenReturn(false);
            when(experienceRepository.existsByEmployerIgnoreCaseAndEmployeePoidNot(anyString(), anyLong())).thenReturn(false);

            // findById for updates/deletes
            HrEmployeeDependentsDtl dep = new HrEmployeeDependentsDtl(); dep.setEmployeePoid(70L); dep.setDetRowId(5L); dep.setName("OLD");
            when(dependentRepository.findById(any(HrEmployeeDependentsDtlId.class))).thenReturn(Optional.of(dep));
            HrEmpDepndtsLmraDtls lm = new HrEmpDepndtsLmraDtls(); lm.setEmployeePoid(70L); lm.setDetRowId(6L);
            when(lmraRepository.findById(any(HrEmpDepndtsLmraDtlsId.class))).thenReturn(Optional.of(lm));
            HrEmployeeExperienceDtl ex = new HrEmployeeExperienceDtl(); ex.setEmployeePoid(70L); ex.setDetRowId(7L);
            when(experienceRepository.findById(any(HrEmployeeExperienceDtlId.class))).thenReturn(Optional.of(ex));
            HrEmployeeDocumentDtl doc = new HrEmployeeDocumentDtl(); doc.setEmployeePoid(70L); doc.setDetRowId(8L);
            when(documentRepository.findById(any(HrEmployeeDocumentDtlId.class))).thenReturn(Optional.of(doc));

            when(dependentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(lmraRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(experienceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            when(employeeMasterMapper.toResponseDto(any())).thenReturn(EmployeeMasterResponseDto.builder().employeePoid(70L).build());

            service.createEmployee(request);

            verify(dependentRepository, atLeastOnce()).save(any());
            verify(dependentRepository).deleteById(any(HrEmployeeDependentsDtlId.class));
            verify(lmraRepository).deleteById(any(HrEmpDepndtsLmraDtlsId.class));
            verify(experienceRepository).deleteById(any(HrEmployeeExperienceDtlId.class));
            verify(documentRepository).deleteById(any(HrEmployeeDocumentDtlId.class));
            verify(loggingService, atLeastOnce()).createLogSummaryEntry(anyString(), anyString(), anyString());
            verify(loggingService, atLeastOnce()).createLog(any(), any(), any(), anyString(), anyString(), anyString());
            verify(loggingService, atLeastOnce()).logDelete(any(), anyString(), anyString());
        }
    }

    @Nested
    class GlobalParameterValueBranches {
        @Test
        void getGlobalParameterValueTrimsAndDefaultsOnBlankOrException() {
            // exercise via create: BAHRAIN_Nationality_Poid
            lenient().when(parameterServiceClient.findParameterValueByName("BAHRAIN_Nationality_Poid")).thenReturn(Optional.of("  "));
            lenient().when(parameterServiceClient.findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE")).thenThrow(new RuntimeException("boom"));
            EmployeeMasterRequestDto req = baseValidRequest("REMOTE", 1L);

            // stop before save to keep test focused
            when(masterRepository.save(any())).thenThrow(new RuntimeException("stop"));
            assertThatThrownBy(() -> service.createEmployee(req)).isInstanceOf(RuntimeException.class);

            verify(parameterServiceClient, atLeastOnce()).findParameterValueByName("BAHRAIN_Nationality_Poid");
            verify(parameterServiceClient, atLeastOnce()).findParameterValueByName("HR_MANUAL_EMPLOYEE_CODE");
        }
    }

    private static EmployeeMasterRequestDto baseValidRequest(String serviceType, Long nationalityPoid) {
        // Keep it minimal but consistent with validations.
        EmployeeMasterRequestDto dto = new EmployeeMasterRequestDto();
        dto.setServiceType(serviceType);
        dto.setNationalityPoid(nationalityPoid);
        dto.setFirstName("FN");
        dto.setMobile(null);
        dto.setEmployeeCode("EC");
        dto.setCprNo("CPR");
        dto.setCrPoid(1L);
        // Ticket fields: only valid/required for PERMANENT expat employees.
        if ("PERMANENT".equalsIgnoreCase(serviceType)) {
            dto.setAirSectorPoid(1L);
            dto.setTicketPeriod("1Y");
            dto.setNoOfTickets("1");
        } else {
            dto.setAirSectorPoid(null);
            dto.setTicketPeriod(null);
            dto.setNoOfTickets(null);
        }
        return dto;
    }

    private static EmployeeDependentsDtlRequestDto dependentCreated(String name, Long detRowId) {
        return EmployeeDependentsDtlRequestDto.builder()
                .actionType(ActionType.isCreated)
                .detRowId(detRowId)
                .name(name)
                .nationality("BH")
                .build();
    }

    private static EmployeeDepndtsLmraDtlsRequestDto lmraCreated(Long detRowId) {
        return EmployeeDepndtsLmraDtlsRequestDto.builder()
                .actionType(ActionType.isCreated)
                .detRowId(detRowId)
                .expatName("X")
                .build();
    }

    private static EmployeeExperienceDtlRequestDto expCreated(String employer, Long detRowId) {
        return EmployeeExperienceDtlRequestDto.builder()
                .actionType(ActionType.isCreated)
                .detRowId(detRowId)
                .employer(employer)
                .build();
    }

    private static EmployeeDocumentDtlRequestDto docCreated(String docName, Long detRowId) {
        return EmployeeDocumentDtlRequestDto.builder()
                .actionType(ActionType.isCreated)
                .detRowId(detRowId)
                .docName(docName)
                .build();
    }

    private static MockedConstruction<SimpleJdbcCall> mockExcelConfigProcSuccess(String tempTable, int startRow, int startCol, int endCol) {
        return mockConstruction(SimpleJdbcCall.class, (mock, ctx) -> {
            when(mock.withProcedureName(anyString())).thenReturn(mock);
            when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
            Map<String, Object> row = new HashMap<>();
            row.put("START_ROW_NUMBER", startRow);
            row.put("START_COL_NUMBER", startCol);
            row.put("END_COL_NUMBER", endCol);
            row.put("TEMP_TABLE_NAME", tempTable);
            List<Map<String, Object>> outdata = new ArrayList<>();
            outdata.add(row);
            when(mock.execute(any(SqlParameterSource.class))).thenReturn(Map.of("P_STATUS", "SUCCESS", "OUTDATA", outdata));
        });
    }

    private static byte[] createWorkbookBytes() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = wb.createSheet();
            CreationHelper helper = wb.getCreationHelper();

            // header row (row 1)
            Row header = sheet.createRow(0);
            header.createCell(0, CellType.STRING).setCellValue("H1");
            header.createCell(1, CellType.STRING).setCellValue("H2");
            header.createCell(2, CellType.STRING).setCellValue("H3");
            header.createCell(3, CellType.STRING).setCellValue("H4");

            // data row (row 2) -> startRowNumber=2 will import from here
            Row row = sheet.createRow(1);

            // col1 numeric date formatted
            Cell c1 = row.createCell(0, CellType.NUMERIC);
            c1.setCellValue(java.util.Date.from(LocalDate.of(2026, 1, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
            var dateStyle = wb.createCellStyle();
            short df = helper.createDataFormat().getFormat("dd/MM/yyyy");
            dateStyle.setDataFormat(df);
            c1.setCellStyle(dateStyle);

            // col2 numeric non-date
            row.createCell(1, CellType.NUMERIC).setCellValue(42.5);
            // col3 string
            row.createCell(2, CellType.STRING).setCellValue("ABC");
            // col4 boolean -> default branch in switch => null
            row.createCell(3, CellType.BOOLEAN).setCellValue(true);

            wb.write(out);
            return out.toByteArray();
        }
    }

    private static byte[] createWorkbookBytesWithQuotedString() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0, CellType.STRING).setCellValue("H1");
            header.createCell(1, CellType.STRING).setCellValue("H2");
            header.createCell(2, CellType.STRING).setCellValue("H3");
            header.createCell(3, CellType.STRING).setCellValue("H4");

            Row row = sheet.createRow(1);
            row.createCell(0, CellType.STRING).setCellValue("O'Reilly");
            row.createCell(1, CellType.STRING).setCellValue("X");
            row.createCell(2, CellType.STRING).setCellValue("Y");
            row.createCell(3, CellType.STRING).setCellValue("Z");

            wb.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Covers {@link EmployeeMasterServiceImpl#uploadExcel} cell-type {@code switch} paths through the
     * {@code default} arm (BOOLEAN/BLANK/FORMULA/ERROR) for tools that track those probes separately.
     */
    private static byte[] createWorkbookBytesWithFormulaErrorAndBlank() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = wb.createSheet();
            CreationHelper helper = wb.getCreationHelper();

            Row header = sheet.createRow(0);
            for (int i = 0; i < 7; i++) {
                header.createCell(i, CellType.STRING).setCellValue("H" + (i + 1));
            }

            Row row = sheet.createRow(1);

            Cell c0 = row.createCell(0, CellType.NUMERIC);
            c0.setCellValue(java.util.Date.from(LocalDate.of(2026, 6, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
            var dateStyle = wb.createCellStyle();
            short df = helper.createDataFormat().getFormat("dd/MM/yyyy");
            dateStyle.setDataFormat(df);
            c0.setCellStyle(dateStyle);

            row.createCell(1, CellType.NUMERIC).setCellValue(99.0);
            row.createCell(2, CellType.STRING).setCellValue("text");
            row.createCell(3, CellType.BOOLEAN).setCellValue(true);
            row.createCell(4, CellType.BLANK);
            Cell formulaCell = row.createCell(5, CellType.FORMULA);
            formulaCell.setCellFormula("2+2");
            Cell errCell = row.createCell(6, CellType.ERROR);
            errCell.setCellErrorValue(FormulaError.NA.getCode());

            wb.write(out);
            return out.toByteArray();
        }
    }
}

