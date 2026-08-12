package com.asg.hr.common.security;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Limits an HR list to the logged-in user's own employee records.
 * <p>
 * A user holding the employee selection right (Edit on {@value #EMPLOYEE_SELECTION_DOC_ID}) may
 * pick any employee and so sees every row; everyone else sees only rows for the employee their
 * login is linked to. This is the rule the legacy HR beans applied through customWhereClause, and
 * the screens it covers are leave request, resignation, personal data sheet, attendance request,
 * employee training, leave rejoin and performance review.
 * <p>
 * The restriction is opt-in: a list applies it by calling {@link #restrict}. It is deliberately not
 * keyed on document id, because the id arrives in a request header and a missing or unexpected
 * value would then silently drop the restriction.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeRowRestriction {

    /** Document carrying the employee selection right. */
    public static final String EMPLOYEE_SELECTION_DOC_ID = "000-219";

    private static final String EMPLOYEE_POID_FIELD = "EMPLOYEE_POID";

    /** Matches no employee, so a user with no linked employee record sees nothing. */
    private static final String NO_EMPLOYEE = "-1";

    private final UserRightsReader userRightsReader;
    private final EntityManager entityManager;

    @Value("${hr.user-rights.cache-ttl-seconds:300}")
    private long cacheTtlSeconds;

    private final TtlCache<Long, Optional<Long>> employeePoidCache = new TtlCache<>(1000);

    /**
     * The filters and operator a restricted list should search with.
     *
     * @param filters  the caller's filters, plus the employee restriction when one applies
     * @param operator forced to AND once restricted, see {@link #restrict}
     */
    public record ScopedSearch(List<FilterDto> filters, String operator) {}

    /**
     * Narrows a search to the logged-in user's own employee records unless they hold the employee
     * selection right.
     * <p>
     * The operator is forced to AND when the restriction applies. DocumentSearchService groups all
     * field filters into a single OR group when the operator is OR, which would let any filter the
     * user sends widen the search back out to every employee. Users who see all rows keep whichever
     * operator they asked for.
     */
    public ScopedSearch restrict(List<FilterDto> filters, String operator) {
        List<FilterDto> scoped = new ArrayList<>(filters != null ? filters : List.of());

        if (canSeeAllEmployees()) {
            return new ScopedSearch(scoped, operator);
        }

        Long employeePoid = loginUserEmployeePoid();
        if (employeePoid == null) {
            log.warn("User {} has no linked employee and no employee selection right; listing no rows",
                    UserContext.getUserId());
        }

        scoped.add(new FilterDto(EMPLOYEE_POID_FIELD,
                employeePoid != null ? String.valueOf(employeePoid) : NO_EMPLOYEE));

        return new ScopedSearch(scoped, "AND");
    }

    /** True when the logged-in user may list every employee's rows. */
    public boolean canSeeAllEmployees() {
        return userRightsReader.isGranted(EMPLOYEE_SELECTION_DOC_ID, UserRolesRightsEnum.EDIT);
    }

    /** Employee the logged-in user is linked to, or null when there is none. */
    public Long loginUserEmployeePoid() {
        Long userPoid = UserContext.getUserPoid();
        if (userPoid == null) {
            return null;
        }
        return employeePoidCache
                .get(userPoid, cacheTtlSeconds, () -> Optional.ofNullable(queryEmployeePoid(userPoid)))
                .orElse(null);
    }

    private Long queryEmployeePoid(Long userPoid) {
        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_GET_LOGIN_USER_EMP_ID");

            query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, Long.class, ParameterMode.OUT);
            query.setParameter(1, userPoid);
            query.execute();

            Object result = query.getOutputParameterValue(2);
            return result != null ? ((Number) result).longValue() : null;

        } catch (Exception e) {
            // Treated as "no linked employee", which shows no rows rather than everyone's
            log.error("Could not resolve the employee linked to user poid {}", userPoid, e);
            return null;
        }
    }
}
