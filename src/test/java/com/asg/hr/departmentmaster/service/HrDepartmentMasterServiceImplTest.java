package com.asg.hr.departmentmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.client.CostCenterServiceClient;
import com.asg.hr.departmentmaster.dto.HrDepartmentMasterRequest;
import com.asg.hr.departmentmaster.dto.HrDepartmentMasterResponse;
import com.asg.hr.departmentmaster.entity.HrDepartmentMaster;
import com.asg.hr.departmentmaster.repository.HrDepartmentMasterRepository;
import com.asg.hr.exceptions.ResourceAlreadyExistsException;
import com.asg.hr.exceptions.ResourceNotFoundException;
import com.asg.hr.exceptions.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrDepartmentMasterServiceImplTest {

    private static final Long DEPT_POID = 1L;
    private static final Long GROUP_POID = 100L;
    private static final Long COST_CENTRE_POID = 200L;
    private static final Long PARENT_DEPT_POID = 2L;
    private static final String USER_ID = "user1";
    private static final String DOCUMENT_ID = "doc1";

    @Mock
    private DocumentDeleteService documentDeleteService;
    @Mock
    private DocumentSearchService documentSearchService;
    @Mock
    private HrDepartmentMasterRepository repository;
    @Mock
    private LoggingService loggingService;

    @Mock
    private CostCenterServiceClient costCenterServiceClient;

    @InjectMocks
    private HrDepartmentMasterServiceImpl service;

    private MockedStatic<UserContext> userContextMock;

    @BeforeEach
    void setUp() {
        userContextMock = org.mockito.Mockito.mockStatic(UserContext.class);
        userContextMock.when(UserContext::getDocumentId).thenReturn(DOCUMENT_ID);
        userContextMock.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);
        userContextMock.when(UserContext::getUserId).thenReturn(USER_ID);
    }

    @AfterEach
    void tearDown() {
        if (userContextMock != null) {
            userContextMock.close();
        }
    }

    @Nested
    @DisplayName("getAllDepartmentsWithFilters")
    class GetAllDepartmentsWithFilters {

        @Test
        @DisplayName("returns wrapped page from document search")
        void returnsWrappedPage() {
            Pageable pageable = PageRequest.of(0, 10);
            FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", Collections.emptyList());
            RawSearchResult rawResult = org.mockito.Mockito.mock(RawSearchResult.class);
            when(documentSearchService.resolveOperator(any())).thenReturn("AND");
            when(documentSearchService.resolveIsDeleted(any())).thenReturn("N");
            when(documentSearchService.resolveFilters(any())).thenReturn(Collections.emptyList());
            when(documentSearchService.search(eq(DOCUMENT_ID), anyList(), eq("AND"), eq(pageable), eq("N"), eq("DEPT_NAME"), eq("DEPT_POID")))
                    .thenReturn(rawResult);
            when(rawResult.records()).thenReturn(Collections.emptyList());
            when(rawResult.totalRecords()).thenReturn(0L);
            when(rawResult.displayFields()).thenReturn(Map.of("DEPT_POID", "DEPT_POID", "DEPT_NAME", "DEPT_NAME"));

            Map<String, Object> result = service.getAllDepartmentsWithFilters(DOCUMENT_ID, filterRequest, pageable);

            assertThat(result).isNotNull();
            assertThat(result).containsKeys("content", "totalElements", "totalPages");
            verify(documentSearchService).search(eq(DOCUMENT_ID), anyList(), eq("AND"), eq(pageable), eq("N"), eq("DEPT_NAME"), eq("DEPT_POID"));
        }

        @Test
        @DisplayName("handles null filter request")
        void handlesNullFilterRequest() {
            Pageable pageable = PageRequest.of(0, 10);
            RawSearchResult rawResult = org.mockito.Mockito.mock(RawSearchResult.class);
            when(documentSearchService.resolveOperator(null)).thenReturn("AND");
            when(documentSearchService.resolveIsDeleted(null)).thenReturn("N");
            when(documentSearchService.resolveFilters(null)).thenReturn(Collections.emptyList());
            when(documentSearchService.search(eq(DOCUMENT_ID), anyList(), anyString(), eq(pageable), anyString(), eq("DEPT_NAME"), eq("DEPT_POID")))
                    .thenReturn(rawResult);
            when(rawResult.records()).thenReturn(Collections.emptyList());
            when(rawResult.totalRecords()).thenReturn(0L);
            when(rawResult.displayFields()).thenReturn(Map.of());

            Map<String, Object> result = service.getAllDepartmentsWithFilters(DOCUMENT_ID, null, pageable);

            assertThat(result).isNotNull();
            verify(documentSearchService).resolveOperator(null);
            verify(documentSearchService).resolveIsDeleted(null);
            verify(documentSearchService).resolveFilters(null);
        }
    }

    @Nested
    @DisplayName("getDepartmentById")
    class GetDepartmentById {

        @Test
        @DisplayName("returns response when department exists")
        void returnsResponseWhenExists() {
            HrDepartmentMaster entity = createEntity(DEPT_POID, "IT", "Information Technology");
            when(repository.findById(DEPT_POID)).thenReturn(Optional.of(entity));

            HrDepartmentMasterResponse response = service.getDepartmentById(DEPT_POID);

            assertThat(response).isNotNull();
            assertThat(response.getDeptPoid()).isEqualTo(DEPT_POID);
            assertThat(response.getDeptName()).isEqualTo("Information Technology");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsWhenNotFound() {
            when(repository.findById(DEPT_POID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDepartmentById(DEPT_POID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Department")
                    .hasMessageContaining("deptPoid");
        }

        @Test
        @DisplayName("maps all fields correctly including base entity fields")
        void mapsAllFieldsCorrectly() {
            HrDepartmentMaster entity = createEntity(DEPT_POID, "IT", "Information Technology");
            entity.setBaseGroup("BASE_GROUP");
            entity.setDeptCode("IT001");
            when(repository.findById(DEPT_POID)).thenReturn(Optional.of(entity));

            HrDepartmentMasterResponse response = service.getDepartmentById(DEPT_POID);

            assertThat(response.getDeptPoid()).isEqualTo(entity.getDeptPoid());
            assertThat(response.getGroupPoid()).isEqualTo(entity.getGroupPoid());
            assertThat(response.getBaseGroup()).isEqualTo(entity.getBaseGroup());
            assertThat(response.getDeptCode()).isEqualTo(entity.getDeptCode());
            assertThat(response.getDeptName()).isEqualTo(entity.getDeptName());
            assertThat(response.getSubdeptYN()).isEqualTo(entity.getSubdeptYN());
            assertThat(response.getActive()).isEqualTo(entity.getActive());
            assertThat(response.getSeqNo()).isEqualTo(entity.getSeqNo());
            assertThat(response.getParentDeptPoid()).isEqualTo(entity.getParentDeptPoid());
            assertThat(response.getCostCentrePoid()).isEqualTo(entity.getCostCentrePoid());
            assertThat(response.getDeleted()).isEqualTo(entity.getDeleted());
            assertThat(response.getCreatedBy()).isEqualTo(entity.getCreatedBy());
            assertThat(response.getCreatedDate()).isEqualTo(entity.getCreatedDate());
            assertThat(response.getLastModifiedBy()).isEqualTo(entity.getLastModifiedBy());
            assertThat(response.getLastModifiedDate()).isEqualTo(entity.getLastModifiedDate());
        }
    }

    @Nested
    @DisplayName("createDepartment")
    class CreateDepartment {

        @Test
        @DisplayName("creates department successfully when valid")
        void createsSuccessfully() {
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("Engineering")
                    .subdeptYN("N")
                    .active("Y")
                    .seqNo(1L)
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();
            when(costCenterServiceClient.findById(COST_CENTRE_POID)).thenReturn(null);
            when(repository.existsByDeptNameIgnoreCase("Engineering")).thenReturn(false);
            HrDepartmentMaster saved = createEntity(DEPT_POID, "ENG", "Engineering");
            saved.setDeptCode("ENG");
            when(repository.save(any(HrDepartmentMaster.class))).thenAnswer(inv -> {
                HrDepartmentMaster e = inv.getArgument(0);
                e.setDeptPoid(DEPT_POID);
                e.setDeptCode("ENG");
                return e;
            });

            HrDepartmentMasterResponse response = service.createDepartment(request, GROUP_POID, USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.getDeptName()).isEqualTo("Engineering");
            verify(repository).save(any(HrDepartmentMaster.class));
            verify(loggingService).createLogSummaryEntry(any(com.asg.common.lib.enums.LogDetailsEnum.class), eq(DOCUMENT_ID), anyString());
        }

        @Test
        @DisplayName("throws when subdept Y but parent dept missing")
        void throwsWhenSubdeptWithoutParent() {
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("Sub Dept")
                    .subdeptYN("Y")
                    .parentDeptPoid(null)
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();

            assertThatThrownBy(() -> service.createDepartment(request, GROUP_POID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Parent department");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("throws when subdept Y but parent dept does not exist")
        void throwsWhenSubdeptParentNotFound() {
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("Sub Dept")
                    .subdeptYN("Y")
                    .parentDeptPoid(PARENT_DEPT_POID)
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();
            when(repository.existsByDeptPoid(PARENT_DEPT_POID)).thenReturn(false);

            assertThatThrownBy(() -> service.createDepartment(request, GROUP_POID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Parent department");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("throws when department name already exists")
        void throwsWhenDeptNameExists() {
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("Duplicate Name")
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();
            when(repository.existsByDeptNameIgnoreCase("Duplicate Name")).thenReturn(true);

            assertThatThrownBy(() -> service.createDepartment(request, GROUP_POID, USER_ID))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("Department name");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("throws when cost centre does not exist")
        void throwsWhenCostCentreNotFound() {
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("Dept")
                    .costCentrePoid(999L)
                    .build();
            when(costCenterServiceClient.findById(999L))
                    .thenThrow(new ResourceNotFoundException("CostCenter", "costCenterPoid", 999L));

            assertThatThrownBy(() -> service.createDepartment(request, GROUP_POID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("CostCenter");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("sets defaults for subdeptYN and active when blank")
        void setsDefaultsWhenBlank() {
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("Dept")
                    .subdeptYN(null)
                    .active(null)
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();
            when(costCenterServiceClient.findById(COST_CENTRE_POID)).thenReturn(null);
            when(repository.existsByDeptNameIgnoreCase("Dept")).thenReturn(false);
            when(repository.save(any(HrDepartmentMaster.class))).thenAnswer(inv -> {
                HrDepartmentMaster e = inv.getArgument(0);
                e.setDeptPoid(DEPT_POID);
                e.setDeptCode("D");
                return e;
            });

            HrDepartmentMasterResponse response = service.createDepartment(request, GROUP_POID, USER_ID);

            assertThat(response).isNotNull();
            verify(repository).save(any(HrDepartmentMaster.class));
        }

        @Test
        @DisplayName("creates subdepartment successfully when parent exists")
        void createsSubdepartmentSuccessfully() {
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("Sub Dept")
                    .subdeptYN("Y")
                    .parentDeptPoid(PARENT_DEPT_POID)
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();
            when(repository.existsByDeptPoid(PARENT_DEPT_POID)).thenReturn(true);
            when(repository.existsByDeptNameIgnoreCase("Sub Dept")).thenReturn(false);
            when(costCenterServiceClient.findById(COST_CENTRE_POID)).thenReturn(null);
            when(repository.save(any(HrDepartmentMaster.class))).thenAnswer(inv -> {
                HrDepartmentMaster e = inv.getArgument(0);
                e.setDeptPoid(DEPT_POID);
                return e;
            });

            HrDepartmentMasterResponse response = service.createDepartment(request, GROUP_POID, USER_ID);

            assertThat(response).isNotNull();
            verify(repository).save(any(HrDepartmentMaster.class));
        }

        @Test
        @DisplayName("skips dept name check when blank")
        void skipsDeptNameCheckWhenBlank() {
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("")
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();
            when(costCenterServiceClient.findById(COST_CENTRE_POID)).thenReturn(null);
            when(repository.save(any(HrDepartmentMaster.class))).thenAnswer(inv -> {
                HrDepartmentMaster e = inv.getArgument(0);
                e.setDeptPoid(DEPT_POID);
                return e;
            });

            HrDepartmentMasterResponse response = service.createDepartment(request, GROUP_POID, USER_ID);

            assertThat(response).isNotNull();
            verify(repository, never()).existsByDeptNameIgnoreCase(anyString());
        }
    }

    @Nested
    @DisplayName("updateDepartment")
    class UpdateDepartment {

        @Test
        @DisplayName("updates department successfully when valid")
        void updatesSuccessfully() {
            HrDepartmentMaster entity = createEntity(DEPT_POID, "IT", "IT Dept");
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("IT Updated")
                    .subdeptYN("N")
                    .active("Y")
                    .seqNo(2L)
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();
            when(repository.findById(DEPT_POID)).thenReturn(Optional.of(entity));
            when(repository.existsByDeptNameIgnoreCaseAndDeptPoidNot("IT Updated", DEPT_POID)).thenReturn(false);
            when(costCenterServiceClient.findById(COST_CENTRE_POID)).thenReturn(null);
            when(repository.save(any(HrDepartmentMaster.class))).thenAnswer(inv -> inv.getArgument(0));

            HrDepartmentMasterResponse response = service.updateDepartment(DEPT_POID, request, GROUP_POID, USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.getDeptName()).isEqualTo("IT Updated");
            verify(loggingService).logChanges(any(), any(), eq(HrDepartmentMaster.class), eq(DOCUMENT_ID), anyString(), any(com.asg.common.lib.enums.LogDetailsEnum.class), eq("DEPT_POID"));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when department not found")
        void throwsWhenNotFound() {
            when(repository.findById(DEPT_POID)).thenReturn(Optional.empty());

            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("Dept")
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();

            assertThatThrownBy(() -> service.updateDepartment(DEPT_POID, request, GROUP_POID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("throws when subdept Y but parent dept missing")
        void throwsWhenSubdeptWithoutParent() {
            HrDepartmentMaster entity = createEntity(DEPT_POID, "IT", "IT");
            when(repository.findById(DEPT_POID)).thenReturn(Optional.of(entity));
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("Sub")
                    .subdeptYN("Y")
                    .parentDeptPoid(null)
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();

            assertThatThrownBy(() -> service.updateDepartment(DEPT_POID, request, GROUP_POID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Parent department");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("throws when department name duplicate by another dept")
        void throwsWhenDeptNameDuplicate() {
            HrDepartmentMaster entity = createEntity(DEPT_POID, "IT", "IT");
            when(repository.findById(DEPT_POID)).thenReturn(Optional.of(entity));
            when(repository.existsByDeptNameIgnoreCaseAndDeptPoidNot("Existing Name", DEPT_POID)).thenReturn(true);
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("Existing Name")
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();

            assertThatThrownBy(() -> service.updateDepartment(DEPT_POID, request, GROUP_POID, USER_ID))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("Department name");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("throws when cost centre does not exist")
        void throwsWhenCostCentreNotFound() {
            HrDepartmentMaster entity = createEntity(DEPT_POID, "IT", "IT");
            when(repository.findById(DEPT_POID)).thenReturn(Optional.of(entity));
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("Dept")
                    .costCentrePoid(999L)
                    .build();

            when(costCenterServiceClient.findById(999L))
                    .thenThrow(new ResourceNotFoundException("CostCenter", "costCenterPoid", 999L));

            assertThatThrownBy(() -> service.updateDepartment(DEPT_POID, request, GROUP_POID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("CostCenter");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("throws when parent department is self")
        void throwsWhenParentIsSelf() {
            HrDepartmentMaster entity = createEntity(DEPT_POID, "IT", "IT");
            when(repository.findById(DEPT_POID)).thenReturn(Optional.of(entity));
            when(costCenterServiceClient.findById(COST_CENTRE_POID)).thenReturn(null);
            when(repository.existsByDeptNameIgnoreCaseAndDeptPoidNot("IT", DEPT_POID)).thenReturn(false);
            when(repository.existsByDeptPoid(DEPT_POID)).thenReturn(true);
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("IT")
                    .subdeptYN("Y")
                    .parentDeptPoid(DEPT_POID)
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();

            assertThatThrownBy(() -> service.updateDepartment(DEPT_POID, request, GROUP_POID, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Should not select current department as parent");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("throws when subdept Y but parent dept does not exist")
        void throwsWhenSubdeptParentNotFound() {
            HrDepartmentMaster entity = createEntity(DEPT_POID, "IT", "IT");
            when(repository.findById(DEPT_POID)).thenReturn(Optional.of(entity));
            when(repository.existsByDeptPoid(PARENT_DEPT_POID)).thenReturn(false);
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("Sub")
                    .subdeptYN("Y")
                    .parentDeptPoid(PARENT_DEPT_POID)
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();

            assertThatThrownBy(() -> service.updateDepartment(DEPT_POID, request, GROUP_POID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Parent department");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("updates subdepartment successfully when parent exists")
        void updatesSubdepartmentSuccessfully() {
            HrDepartmentMaster entity = createEntity(DEPT_POID, "IT", "IT");
            when(repository.findById(DEPT_POID)).thenReturn(Optional.of(entity));
            when(repository.existsByDeptPoid(PARENT_DEPT_POID)).thenReturn(true);
            when(repository.existsByDeptNameIgnoreCaseAndDeptPoidNot("Sub Dept", DEPT_POID)).thenReturn(false);
            when(costCenterServiceClient.findById(COST_CENTRE_POID)).thenReturn(null);
            when(repository.save(any(HrDepartmentMaster.class))).thenAnswer(inv -> inv.getArgument(0));
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("Sub Dept")
                    .subdeptYN("Y")
                    .parentDeptPoid(PARENT_DEPT_POID)
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();

            HrDepartmentMasterResponse response = service.updateDepartment(DEPT_POID, request, GROUP_POID, USER_ID);

            assertThat(response).isNotNull();
            verify(repository).save(any(HrDepartmentMaster.class));
        }

        @Test
        @DisplayName("retains existing values when subdeptYN and active are blank")
        void retainsExistingValuesWhenBlank() {
            HrDepartmentMaster entity = createEntity(DEPT_POID, "IT", "IT");
            entity.setSubdeptYN("Y");
            entity.setActive("N");
            when(repository.findById(DEPT_POID)).thenReturn(Optional.of(entity));
            when(repository.existsByDeptNameIgnoreCaseAndDeptPoidNot("IT", DEPT_POID)).thenReturn(false);
            when(costCenterServiceClient.findById(COST_CENTRE_POID)).thenReturn(null);
            when(repository.save(any(HrDepartmentMaster.class))).thenAnswer(inv -> inv.getArgument(0));
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("IT")
                    .subdeptYN(null)
                    .active(null)
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();

            HrDepartmentMasterResponse response = service.updateDepartment(DEPT_POID, request, GROUP_POID, USER_ID);

            assertThat(response).isNotNull();
            verify(repository).save(any(HrDepartmentMaster.class));
        }

        @Test
        @DisplayName("skips dept name check when blank")
        void skipsDeptNameCheckWhenBlank() {
            HrDepartmentMaster entity = createEntity(DEPT_POID, "IT", "IT");
            when(repository.findById(DEPT_POID)).thenReturn(Optional.of(entity));
            when(costCenterServiceClient.findById(COST_CENTRE_POID)).thenReturn(null);
            when(repository.save(any(HrDepartmentMaster.class))).thenAnswer(inv -> inv.getArgument(0));
            HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                    .deptName("")
                    .costCentrePoid(COST_CENTRE_POID)
                    .build();

            HrDepartmentMasterResponse response = service.updateDepartment(DEPT_POID, request, GROUP_POID, USER_ID);

            assertThat(response).isNotNull();
            verify(repository, never()).existsByDeptNameIgnoreCaseAndDeptPoidNot(anyString(), anyLong());
        }
    }

    @Nested
    @DisplayName("deleteDepartment")
    class DeleteDepartment {

        @Test
        @DisplayName("calls document delete when department exists")
        void deletesSuccessfully() {
            HrDepartmentMaster entity = createEntity(DEPT_POID, "IT", "IT");
            when(repository.findById(DEPT_POID)).thenReturn(Optional.of(entity));
            DeleteReasonDto dto = new DeleteReasonDto();

            service.deleteDepartment(DEPT_POID, GROUP_POID, USER_ID, dto);

            verify(documentDeleteService).deleteDocument(
                    eq(DEPT_POID),
                    eq("HR_DEPARTMENT_MASTER"),
                    eq("DEPT_POID"),
                    eq(dto),
                    any(java.time.LocalDate.class)
            );
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when department not found")
        void throwsWhenNotFound() {
            when(repository.findById(DEPT_POID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteDepartment(DEPT_POID, GROUP_POID, USER_ID, new DeleteReasonDto()))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(documentDeleteService, never()).deleteDocument(anyLong(), anyString(), anyString(), any(), any());
        }

        @Test
        @DisplayName("calls document delete with null deleteReasonDto")
        void deletesSuccessfullyWithNullDto() {
            HrDepartmentMaster entity = createEntity(DEPT_POID, "IT", "IT");
            when(repository.findById(DEPT_POID)).thenReturn(Optional.of(entity));

            service.deleteDepartment(DEPT_POID, GROUP_POID, USER_ID, null);

            verify(documentDeleteService).deleteDocument(
                    eq(DEPT_POID),
                    eq("HR_DEPARTMENT_MASTER"),
                    eq("DEPT_POID"),
                    eq(null),
                    any(java.time.LocalDate.class)
            );
        }
    }

    private static HrDepartmentMaster createEntity(Long deptPoid, String code, String name) {
        HrDepartmentMaster e = new HrDepartmentMaster();
        e.setDeptPoid(deptPoid);
        e.setDeptCode(code);
        e.setDeptName(name);
        e.setGroupPoid(GROUP_POID);
        e.setActive("Y");
        e.setDeleted("N");
        e.setSubdeptYN("N");
        e.setSeqNo(1L);
        e.setCostCentrePoid(COST_CENTRE_POID);
        e.setCreatedBy(USER_ID);
        e.setCreatedDate(LocalDateTime.now());
        e.setLastModifiedBy(USER_ID);
        e.setLastModifiedDate(LocalDateTime.now());
        return e;
    }
}
