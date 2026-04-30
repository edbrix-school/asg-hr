package com.asg.hr.leaverejoin.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.hr.departmentmaster.repository.HrDepartmentMasterRepository;
import com.asg.hr.designation.repository.DesignationRepository;
import com.asg.hr.employeemaster.entity.HrEmployeeLeaveHistory;
import com.asg.hr.employeemaster.entity.HrEmployeeMaster;
import com.asg.hr.employeemaster.repository.HrEmployeeLeaveHistoryRepository;
import com.asg.hr.employeemaster.repository.HrEmployeeMasterRepository;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinEmployeeDetailsResponse;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinLeaveDetailsResponse;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinRequest;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinResponse;
import com.asg.hr.leaverejoin.entity.HrEmployeeRejoinHdr;
import com.asg.hr.leaverejoin.repository.EmployeeLeaveRejoinProcRepository;
import com.asg.hr.leaverejoin.repository.HrEmployeeRejoinRepository;
import com.asg.hr.leaverejoin.service.impl.EmployeeLeaveRejoinServiceImpl;
import com.asg.hr.leaverejoin.util.EmployeeLeaveRejoinMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeLeaveRejoinServiceImplTest {

    @Mock
    private HrEmployeeRejoinRepository repository;
    @Mock
    private EmployeeLeaveRejoinProcRepository procRepository;
    @Mock
    private HrEmployeeMasterRepository employeeRepository;
    @Mock
    private HrEmployeeLeaveHistoryRepository leaveHistoryRepository;
    @Mock
    private HrDepartmentMasterRepository departmentRepository;
    @Mock
    private DesignationRepository designationRepository;
    @Mock
    private DocumentSearchService documentSearchService;
    @Mock
    private DocumentDeleteService documentDeleteService;
    @Mock
    private LoggingService loggingService;
    @Mock
    private LovDataService lovDataService;
    @Mock
    private PrintService printService;
    @Mock
    private DataSource dataSource;
    @Mock
    private EntityManager entityManager;
    @Spy
    private EmployeeLeaveRejoinMapper mapper = new EmployeeLeaveRejoinMapper();

    @InjectMocks
    private EmployeeLeaveRejoinServiceImpl service;

    private EmployeeLeaveRejoinRequest request;
    private HrEmployeeRejoinHdr entity;
    private HrEmployeeMaster employee;

    @BeforeEach
    void setUp() {
        request = EmployeeLeaveRejoinRequest.builder()
                .employeePoid(10L)
                .leaveRequestPoid(20L)
                .dateOfRejoining(LocalDate.of(2026, 2, 1))
                .remarks("Back from leave")
                .passportReceived("YES")
                .receivedBy("HR User")
                .remarksByHr("Checked")
                .transactionDate(LocalDate.of(2026, 2, 1))
                .build();

        entity = HrEmployeeRejoinHdr.builder()
                .transactionPoid(1L)
                .employeePoid(10L)
                .leaveRequestPoid(20L)
                .dateOfRejoining(LocalDate.of(2026, 2, 1))
                .remarks("Back from leave")
                .passportReceived("YES")
                .receivedBy("HR User")
                .remarksByHr("Checked")
                .transactionDate(LocalDate.of(2026, 2, 1))
                .companyPoid(1L)
                .deleted("N")
                .build();

        employee = HrEmployeeMaster.builder()
                .employeePoid(10L)
                .departmentPoid(100L)
                .designationPoid(200L)
                .loginUserPoid(500L)
                .active("Y")
                .build();
    }

    @Test
    void create_SuccessForPrivilegedUser() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("800-114");
            mockedUserContext.when(UserContext::getUserRole).thenReturn("HR MANAGER");

            when(employeeRepository.findByEmployeePoid(10L)).thenReturn(Optional.of(employee));
            when(procRepository.getEmployeeDetails(10L)).thenReturn(EmployeeLeaveRejoinEmployeeDetailsResponse.builder()
                    .employeePoid(10L)
                    .departmentName("Operations")
                    .designationName("Technician")
                    .status("SUCCESS")
                    .build());
            when(procRepository.getLeaveDetails(10L, 20L)).thenReturn(EmployeeLeaveRejoinLeaveDetailsResponse.builder()
                    .employeePoid(10L)
                    .leaveRequestPoid(20L)
                    .dateProceededOnLeave(LocalDate.of(2026, 1, 1))
                    .status("SUCCESS")
                    .build());
            when(repository.save(any(HrEmployeeRejoinHdr.class))).thenReturn(entity);
            when(lovDataService.getDetailsByPoidAndLovNameFast(anyLong(), any())).thenReturn(new LovGetListDto());
            when(lovDataService.getLovItemByCodeFast(any(), any())).thenReturn(new LovGetListDto());

            EmployeeLeaveRejoinResponse response = service.create(request);

            assertNotNull(response);
            assertEquals(1L, response.getTransactionPoid());
            assertEquals("Operations", response.getDepartmentName());
            assertEquals(LocalDate.of(2026, 1, 1), response.getDateProceededOnLeave());
            assertEquals("YES", response.getPassportReceived());
            verify(loggingService).createLogSummaryEntry("800-114", "1", "CREATED null");
        }
    }

    @Test
    void create_RejectsRejoinDateBeforeLeaveStart() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("800-114");
            mockedUserContext.when(UserContext::getUserRole).thenReturn("HR");

            when(employeeRepository.findByEmployeePoid(10L)).thenReturn(Optional.of(employee));
            when(procRepository.getEmployeeDetails(10L)).thenReturn(EmployeeLeaveRejoinEmployeeDetailsResponse.builder()
                    .employeePoid(10L)
                    .departmentName("Operations")
                    .designationName("Technician")
                    .status("SUCCESS")
                    .build());
            when(procRepository.getLeaveDetails(10L, 20L)).thenReturn(EmployeeLeaveRejoinLeaveDetailsResponse.builder()
                    .employeePoid(10L)
                    .leaveRequestPoid(20L)
                    .dateProceededOnLeave(LocalDate.of(2026, 3, 1))
                    .status("SUCCESS")
                    .build());

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(request));
            assertEquals("Rejoin date cannot be before leave start date.", ex.getMessage());
        }
    }

    @Test
    void create_RejectsOtherEmployeeForNonPrivilegedUser() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("800-114");
            mockedUserContext.when(UserContext::getUserRole).thenReturn("EMPLOYEE");
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(500L);

            when(employeeRepository.findByLoginUserPoid(500L)).thenReturn(Optional.of(HrEmployeeMaster.builder()
                    .employeePoid(99L)
                    .build()));

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(request));
            assertTrue(ex.getMessage().contains("not allowed"));
        }
    }

    @Test
    void create_RejectsDuplicateLeaveRequest() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("800-114");
            mockedUserContext.when(UserContext::getUserRole).thenReturn("HR");

            when(employeeRepository.findByEmployeePoid(10L)).thenReturn(Optional.of(employee));
            when(procRepository.getEmployeeDetails(10L)).thenReturn(EmployeeLeaveRejoinEmployeeDetailsResponse.builder()
                    .employeePoid(10L)
                    .departmentName("Operations")
                    .designationName("Technician")
                    .status("SUCCESS")
                    .build());
            when(procRepository.getLeaveDetails(10L, 20L)).thenReturn(EmployeeLeaveRejoinLeaveDetailsResponse.builder()
                    .employeePoid(10L)
                    .leaveRequestPoid(20L)
                    .dateProceededOnLeave(LocalDate.of(2026, 1, 1))
                    .status("SUCCESS")
                    .build());
            when(repository.existsByEmployeePoidAndLeaveRequestPoidAndDeletedNot(10L, 20L, "Y")).thenReturn(true);

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(request));
            assertEquals("Selected leave request already has an employee rejoining record.", ex.getMessage());
        }
    }

    @Test
    void create_RejectsLeaveRequestOwnedByAnotherEmployee() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("800-114");
            mockedUserContext.when(UserContext::getUserRole).thenReturn("HR");

            when(employeeRepository.findByEmployeePoid(10L)).thenReturn(Optional.of(employee));
            when(procRepository.getEmployeeDetails(10L)).thenReturn(EmployeeLeaveRejoinEmployeeDetailsResponse.builder()
                    .employeePoid(10L)
                    .departmentName("Operations")
                    .designationName("Technician")
                    .status("SUCCESS")
                    .build());
            when(procRepository.getLeaveDetails(10L, 20L)).thenReturn(EmployeeLeaveRejoinLeaveDetailsResponse.builder()
                    .employeePoid(99L)
                    .leaveRequestPoid(20L)
                    .dateProceededOnLeave(LocalDate.of(2026, 1, 1))
                    .status("SUCCESS")
                    .build());

            ValidationException ex = assertThrows(ValidationException.class, () -> service.create(request));
            assertEquals("Employee on Leave Request and Rejoining Form should be same.", ex.getMessage());
        }
    }

    @Test
    void update_SuccessLogsChanges() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("800-114");
            mockedUserContext.when(UserContext::getUserRole).thenReturn("ADMIN");

            when(repository.findByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(entity));
            when(employeeRepository.findByEmployeePoid(10L)).thenReturn(Optional.of(employee));
            when(procRepository.getEmployeeDetails(10L)).thenReturn(EmployeeLeaveRejoinEmployeeDetailsResponse.builder()
                    .employeePoid(10L)
                    .departmentName("Operations")
                    .designationName("Technician")
                    .status("SUCCESS")
                    .build());
            when(procRepository.getLeaveDetails(10L, 20L)).thenReturn(EmployeeLeaveRejoinLeaveDetailsResponse.builder()
                    .employeePoid(10L)
                    .leaveRequestPoid(20L)
                    .dateProceededOnLeave(LocalDate.of(2026, 1, 1))
                    .status("SUCCESS")
                    .build());
            when(repository.save(any(HrEmployeeRejoinHdr.class))).thenReturn(entity);
            when(lovDataService.getDetailsByPoidAndLovNameFast(anyLong(), any())).thenReturn(new LovGetListDto());
            when(lovDataService.getLovItemByCodeFast(any(), any())).thenReturn(new LovGetListDto());

            EmployeeLeaveRejoinResponse response = service.update(1L, request);

            assertEquals(1L, response.getTransactionPoid());
            verify(loggingService).logChanges(any(), any(), eq(HrEmployeeRejoinHdr.class), eq("800-114"), eq("1"),
                    eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
        }
    }

    @Test
    void getById_ReturnsHrFieldsAndLovDetailsForNormalEmployee() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserRole).thenReturn("EMPLOYEE");
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(500L);

            when(repository.findByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(entity));
            when(employeeRepository.findByLoginUserPoid(500L)).thenReturn(Optional.of(employee));
            when(employeeRepository.findByEmployeePoid(10L)).thenReturn(Optional.of(employee));
            when(procRepository.getEmployeeDetails(10L)).thenReturn(EmployeeLeaveRejoinEmployeeDetailsResponse.builder()
                    .employeePoid(10L)
                    .departmentName("Operations")
                    .designationName("Technician")
                    .status("SUCCESS")
                    .build());
            when(procRepository.getLeaveDetails(10L, 20L)).thenReturn(EmployeeLeaveRejoinLeaveDetailsResponse.builder()
                    .employeePoid(10L)
                    .leaveRequestPoid(20L)
                    .dateProceededOnLeave(LocalDate.of(2026, 1, 1))
                    .status("SUCCESS")
                    .build());
            when(lovDataService.getDetailsByPoidAndLovNameFast(anyLong(), any())).thenReturn(new LovGetListDto());
            when(lovDataService.getLovItemByCodeFast(any(), any())).thenReturn(new LovGetListDto());

            EmployeeLeaveRejoinResponse response = service.getById(1L);

            assertEquals("YES", response.getPassportReceived());
            assertEquals("HR User", response.getReceivedBy());
            assertEquals("Checked", response.getRemarksByHr());
            assertEquals("Operations", response.getDepartmentName());
            assertNotNull(response.getPassportReceivedDet());
        }
    }

    @Test
    void list_AppendsEmployeeFilterForNormalEmployee() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("800-114");
            mockedUserContext.when(UserContext::getUserRole).thenReturn("EMPLOYEE");
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(500L);

            FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of(new FilterDto("DOC_REF", "LR")));
            Pageable pageable = PageRequest.of(0, 10);
            when(employeeRepository.findByLoginUserPoid(500L)).thenReturn(Optional.of(employee));
            when(documentSearchService.resolveDateFilters(eq(filters), eq("TRANSACTION_DATE"), any(), any()))
                    .thenReturn(new ArrayList<>(List.of(new FilterDto("DOC_REF", "LR"))));
            when(documentSearchService.resolveOperator(filters)).thenReturn("AND");
            when(documentSearchService.resolveIsDeleted(filters)).thenReturn("N");
            when(documentSearchService.search(eq("800-114"), any(), eq("AND"), eq(pageable), eq("N"), any(), any()))
                    .thenReturn(new RawSearchResult(List.of(Map.of("DOC_REF", "LR")), Map.of("DOC_REF", "Document Ref"), 1L));

            service.list(filters, null, null, pageable);

            ArgumentCaptor<List<FilterDto>> captor = ArgumentCaptor.forClass(List.class);
            verify(documentSearchService).search(eq("800-114"), captor.capture(), eq("AND"), eq(pageable), eq("N"), any(), any());
            assertTrue(captor.getValue().stream().anyMatch(filter ->
                    "EMPLOYEE_POID".equals(filter.searchField()) && "10".equals(filter.searchValue())));
        }
    }

    @Test
    void getLeaveDetails_UsesLeaveHistoryFallback() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserRole).thenReturn("ADMIN");

            when(employeeRepository.findByEmployeePoid(10L)).thenReturn(Optional.of(employee));
            when(lovDataService.getDetailsByPoidAndLovNameFast(anyLong(), any())).thenReturn(new LovGetListDto());
            when(procRepository.getLeaveDetails(10L, 20L)).thenReturn(EmployeeLeaveRejoinLeaveDetailsResponse.builder()
                    .employeePoid(10L)
                    .leaveRequestPoid(20L)
                    .status("ERROR : not found")
                    .build());
            when(leaveHistoryRepository.findTopByEmployeePoidAndSourceDocPoidAndDeletedNotOrderByLeaveHistPoidDescDetRowIdDesc(
                    10L, 20L, "Y")).thenReturn(Optional.of(HrEmployeeLeaveHistory.builder()
                    .employeePoid(10L)
                    .sourceDocPoid(20L)
                    .leaveStartDate(LocalDate.of(2026, 1, 1))
                    .deleted("N")
                    .build()));

            EmployeeLeaveRejoinLeaveDetailsResponse response = service.getLeaveDetails(10L, 20L);

            assertEquals(LocalDate.of(2026, 1, 1), response.getDateProceededOnLeave());
            assertNotNull(response.getEmployeeDet());
            assertNotNull(response.getLeaveRequestDet());
        }
    }

    @Test
    void delete_Success() {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserRole).thenReturn("ADMIN");

            when(repository.findByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(entity));

            DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
            deleteReasonDto.setDeleteReason("cleanup");

            assertDoesNotThrow(() -> service.delete(1L, deleteReasonDto));

            verify(documentDeleteService).deleteDocument(1L, "HR_EMP_REJOIN_HDR", "TRANSACTION_POID", deleteReasonDto, null);
        }
    }

    @Test
    void print_Success() throws Exception {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserRole).thenReturn("ADMIN");
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("800-114");

            when(repository.findByTransactionPoidAndDeletedNot(1L, "Y")).thenReturn(Optional.of(entity));
            when(printService.buildBaseParams(1L, "800-114")).thenReturn(Map.of("DOC_KEY_POID", "1"));
            when(printService.load("HR/EmployeeRejoiningReport.jrxml")).thenReturn(org.mockito.Mockito.mock(net.sf.jasperreports.engine.JasperReport.class));
            when(printService.fillReportToPdf(any(), any(), eq(dataSource))).thenReturn(new byte[]{1, 2, 3});

            byte[] pdf = service.print(1L);

            assertNotNull(pdf);
            assertEquals(3, pdf.length);
        }
    }
}
