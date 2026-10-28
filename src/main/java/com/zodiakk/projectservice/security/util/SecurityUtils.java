package com.zodiakk.projectservice.security.util;

import com.zodiakk.projectservice.common.constants.SecurityConstants;
import com.zodiakk.projectservice.security.principal.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() { }

    public static CurrentUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new IllegalStateException("Current user is not authenticated");
        }

        return currentUser;
    }

    public static UUID getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public static boolean isAdmin() {
        return hasRole(SecurityConstants.ADMIN_AUTHORITY);
    }

    public static boolean hasRole(String authority) {
        return authority.equals(getCurrentUser().getRole());
    }

    public static boolean hasAnyRole(String... authorities) {
        String role = getCurrentUser().getRole();

        for (String authority : authorities) {
            if (authority.equals(role)) {
                return true;
            }
        }

        return false;
    }

    public static boolean keysMatch(String expectedKey, String providedKey) {
        if (expectedKey == null || expectedKey.isBlank() || providedKey == null) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
