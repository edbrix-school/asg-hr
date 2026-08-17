package com.asg.hr.common.service;

import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.PaginationProperties;
import com.asg.hr.common.dto.CurrentUserEmployeeDto;
import com.asg.hr.common.dto.EmployeeLovDto;
import com.asg.hr.common.dto.EmployeeLovQuery;
import com.asg.hr.common.security.EmployeeRowRestriction;
import com.asg.hr.common.security.UserRightsReader;
import com.asg.hr.employeemaster.entity.HrEmployeeMaster;
import com.asg.hr.employeemaster.repository.HrEmployeeMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves the employee the logged-in user may work as, and the list they may pick from.
 * <p>
 * The employee comes from {@link EmployeeRowRestriction}, the same lookup the restricted lists use,
 * so a screen that prefills from here shows an employee whose rows the user can actually see.
 * <p>
 * Access is read with {@link UserRightsReader} (PROC_GLOB_USR_RIGHTS_APPSTART), the procedure the
 * whole stack checks permissions with, against the calling screen's own document — the
 * {@code X-Document-Id} of the request — rather than a fixed document: a user holding Edit on the
 * screen they are on may pick any employee. Note this is a different question from the one the
 * restricted lists ask, which is Edit on the employee selection document
 * {@value EmployeeRowRestriction#EMPLOYEE_SELECTION_DOC_ID}; a user granted Edit on a screen but not
 * on that document gets an open picker here while the list still shows only their own rows.
 * <p>
 * A request without a document id grants nothing, so a missing header locks the field rather than
 * opening it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentUserEmployeeService {

    private final EmployeeRowRestriction employeeRowRestriction;
    private final UserRightsReader userRightsReader;
    private final HrEmployeeMasterRepository employeeMasterRepository;
    private final LovDataService lovDataService;
    private final PaginationProperties paginationProperties;

    /** The logged-in user's employee and access, with null ids when no employee is linked. */
    @Transactional(readOnly = true)
    public CurrentUserEmployeeDto getCurrentUserEmployee(EmployeeLovQuery query) {
        Long employeePoid = employeeRowRestriction.loginUserEmployeePoid();
        String documentId = UserContext.getDocumentId();
        boolean canSelectAnyEmployee = userRightsReader.isGranted(documentId, UserRolesRightsEnum.EDIT);

        Optional<HrEmployeeMaster> employee = employeePoid != null
                ? employeeMasterRepository.findByEmployeePoid(employeePoid)
                : Optional.empty();

        CurrentUserEmployeeDto.CurrentUserEmployeeDtoBuilder response = CurrentUserEmployeeDto.builder()
                .userId(UserContext.getUserId())
                .userPoid(UserContext.getUserPoid())
                .documentId(documentId)
                .employeePoid(employeePoid)
                .linkedToEmployee(employeePoid != null)
                .canSelectAnyEmployee(canSelectAnyEmployee);

        employee.ifPresent(found -> response
                .employeeCode(found.getEmployeeCode())
                .employeeName(employeeName(found))
                .active(found.getActive()));

        if (query != null && query.requested()) {
            response.employeeLov(employeeLov(query, employeePoid, canSelectAnyEmployee, employee));
        }

        return response.build();
    }

    /**
     * The rows the user may pick from.
     * <p>
     * Only a user who may select any employee is worth reading the whole LOV for; a restricted user
     * gets a single lookup of their own employee, and no rows at all when their login is linked to no
     * employee — the same "sees nothing" the restricted lists fall back to.
     */
    private EmployeeLovDto employeeLov(EmployeeLovQuery query, Long employeePoid, boolean canSelectAnyEmployee,
                                       Optional<HrEmployeeMaster> employee) {
        EmployeeLovDto.EmployeeLovDtoBuilder lov = EmployeeLovDto.builder()
                .lovName(query.lovName())
                .restrictedToOwnEmployee(!canSelectAnyEmployee);

        if (canSelectAnyEmployee) {
            Map<String, Object> lovList = readLov(query);
            List<LovGetListDto> rows = lovRows(lovList);
            return lov.data(rows).totalRecords(lovTotal(lovList, rows)).build();
        }

        if (employeePoid == null) {
            log.warn("User {} has no linked employee and may not select another; returning an empty {} list",
                    UserContext.getUserId(), query.lovName());
            return lov.data(List.of()).totalRecords(0).build();
        }

        LovGetListDto ownEmployee = ownEmployeeRow(employeePoid, query.lovName(), employee);

        return lov.data(List.of(ownEmployee)).totalRecords(1).build();
    }

    /**
     * The user's own employee as a LOV row. The LOV returns a poid only stub when the employee is not
     * in it — filtered out by the LOV's own conditions, for instance — so the labels are filled from
     * the employee record to keep the row renderable.
     */
    private LovGetListDto ownEmployeeRow(Long employeePoid, String lovName, Optional<HrEmployeeMaster> employee) {
        LovGetListDto lovRow = lovDataService.getDetailsByPoidAndLovNameFast(employeePoid, lovName);
        LovGetListDto row = lovRow != null ? lovRow : new LovGetListDto();

        if (row.getPoid() == null) {
            row.setPoid(employeePoid);
        }
        if (row.getValue() == null) {
            row.setValue(employeePoid);
        }
        if (row.getCode() == null) {
            employee.ifPresent(found -> row.setCode(found.getEmployeeCode()));
        }
        if (row.getDescription() == null) {
            employee.ifPresent(found -> row.setDescription(employeeName(found)));
        }
        if (row.getLabel() == null) {
            row.setLabel(row.getDescription());
        }

        return row;
    }

    @SuppressWarnings("unchecked")
    private List<LovGetListDto> lovRows(Map<String, Object> lovList) {
        Object data = lovList.get("data");
        return data != null ? (List<LovGetListDto>) data : List.of();
    }

    /** The count before paging, falling back to the rows returned when the LOV reports none. */
    private int lovTotal(Map<String, Object> lovList, List<LovGetListDto> rows) {
        Object total = lovList.get("totalRecords");
        return total instanceof Number number ? number.intValue() : rows.size();
    }

    private Map<String, Object> readLov(EmployeeLovQuery query) {
        Map<String, Object> result = lovDataService.getLovList(
                query.filter(),
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                query.lovName(),
                query.pageNumber() != null ? query.pageNumber() : paginationProperties.getPageNumber(),
                query.pageSize() != null ? query.pageSize() : paginationProperties.getPageSize(),
                query.sortBy(),
                query.sortDir());

        return result != null ? result : Map.of();
    }

    /** Falls back to the secondary name so a record kept only in the alternate language still reads. */
    private String employeeName(HrEmployeeMaster employee) {
        String name = employee.getEmployeeName();
        return (name != null && !name.isBlank()) ? name : employee.getEmployeeName2();
    }
}
