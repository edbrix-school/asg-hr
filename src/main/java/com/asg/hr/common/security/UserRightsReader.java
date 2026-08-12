package com.asg.hr.common.security;

import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads the rights granted to the logged-in user.
 * <p>
 * PROC_GLOB_USR_RIGHTS_APPSTART returns one row per (DOC_ID, RIGHTS) granted to the user, where
 * RIGHTS is a six character flag string in the order View/Create/Edit/Delete/Print/Email — the same
 * order as {@link UserRolesRightsEnum}, so a right maps to a character by its ordinal. A user with
 * several roles gets a row per role for the same document; those merge so that a right granted by
 * any one role wins. This mirrors the legacy ASGCommonClass.IsGrantedRights session lookup.
 * <p>
 * Rights are cached per user for {@code hr.user-rights.cache-ttl-seconds} (default 5 minutes), so a
 * role change can take that long to apply. Call {@link #evict(String)} after updating a user's
 * roles to apply it at once, or set the ttl to 0 to read through every time.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserRightsReader {

    /** Rights string when nothing is granted, or the row is malformed. */
    private static final String NO_RIGHTS = "000000";

    private final JdbcTemplate jdbcTemplate;

    @Value("${hr.user-rights.cache-ttl-seconds:300}")
    private long cacheTtlSeconds;

    private final TtlCache<String, Map<String, String>> cache = new TtlCache<>(1000);

    /** True when the logged-in user holds {@code right} on {@code docId}. */
    public boolean isGranted(String docId, UserRolesRightsEnum right) {
        return isGranted(UserContext.getUserId(), docId, right);
    }

    /** True when {@code userId} holds {@code right} on {@code docId}; false for any unknown input. */
    public boolean isGranted(String userId, String docId, UserRolesRightsEnum right) {
        if (userId == null || docId == null || right == null) {
            return false;
        }
        String rights = loadRights(userId).getOrDefault(docId, NO_RIGHTS);
        return rights.charAt(right.ordinal()) == '1';
    }

    /** All rights of {@code userId} as {docId -> "VCEDPE" flag string}, served from cache when fresh. */
    public Map<String, String> loadRights(String userId) {
        if (userId == null || userId.isBlank()) {
            return Map.of();
        }
        return cache.get(userId, cacheTtlSeconds, () -> queryRights(userId));
    }

    /** Drops one user's cached rights so a role change applies before the ttl expires. */
    public void evict(String userId) {
        if (userId != null) {
            cache.evict(userId);
        }
    }

    /** Drops every cached entry; for a screen that reassigns roles in bulk. */
    public void clearCache() {
        cache.clear();
    }

    private Map<String, String> queryRights(String userId) {
        String sql = "{CALL PROC_GLOB_USR_RIGHTS_APPSTART(?,?)}";

        try {
            Map<String, String> rights = jdbcTemplate.execute(sql, (CallableStatement stmt) -> {
                stmt.setString(1, userId);
                stmt.registerOutParameter(2, Types.REF_CURSOR);
                stmt.execute();

                Map<String, String> loaded = new HashMap<>();
                try (ResultSet rs = (ResultSet) stmt.getObject(2)) {
                    while (rs.next()) {
                        String docId = rs.getString("DOC_ID");
                        if (docId == null) continue;
                        loaded.merge(docId, normalize(rs.getString("RIGHTS")), UserRightsReader::mergeRights);
                    }
                }
                return loaded;
            });
            return rights != null ? rights : Map.of();

        } catch (Exception e) {
            // Grant nothing when rights cannot be read, so a failure narrows access instead of widening it
            log.error("Could not read rights for user {}; treating as no rights granted", userId, e);
            return Map.of();
        }
    }

    /** A malformed or missing rights string grants nothing, as in the legacy client. */
    private static String normalize(String rights) {
        return (rights == null || rights.length() != NO_RIGHTS.length()) ? NO_RIGHTS : rights;
    }

    /** Merge rows for the same document across roles: a right granted by any role is granted. */
    private static String mergeRights(String first, String second) {
        StringBuilder merged = new StringBuilder(NO_RIGHTS.length());
        for (int i = 0; i < NO_RIGHTS.length(); i++) {
            merged.append(first.charAt(i) == '1' || second.charAt(i) == '1' ? '1' : '0');
        }
        return merged.toString();
    }
}
