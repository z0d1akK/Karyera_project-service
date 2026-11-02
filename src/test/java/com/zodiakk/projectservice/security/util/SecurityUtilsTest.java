package com.zodiakk.projectservice.security.util;

import com.zodiakk.projectservice.common.constants.SecurityConstants;
import com.zodiakk.projectservice.security.principal.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityUtils")
class SecurityUtilsTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String EMAIL = "hr@company.com";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUser {

        @Test
        @DisplayName("returns the authenticated current user")
        void shouldReturnAuthenticatedUser() {
            authenticate(principal(SecurityConstants.HR_AUTHORITY));

            CurrentUser currentUser = SecurityUtils.getCurrentUser();

            assertThat(currentUser.getUserId()).isEqualTo(USER_ID);
            assertThat(currentUser.getRole()).isEqualTo(SecurityConstants.HR_AUTHORITY);
            assertThat(currentUser.getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("throws when there is no authentication")
        void shouldThrowWithoutAuthentication() {
            assertThatThrownBy(SecurityUtils::getCurrentUser)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Current user is not authenticated");
        }

        @Test
        @DisplayName("throws when the principal is not a CurrentUser (anonymous)")
        void shouldThrowForAnonymousPrincipal() {
            SecurityContextHolder.getContext().setAuthentication(
                    new AnonymousAuthenticationToken(
                            "anonymous-key",
                            "anonymousUser",
                            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
                    )
            );

            assertThatThrownBy(SecurityUtils::getCurrentUser)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Current user is not authenticated");
        }

        @Test
        @DisplayName("throws when the principal is a foreign authenticated user")
        void shouldThrowForForeignPrincipal() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "external-user",
                            null,
                            List.of(new SimpleGrantedAuthority(SecurityConstants.EMPLOYEE_AUTHORITY))
                    )
            );

            assertThatThrownBy(SecurityUtils::getCurrentUser)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Current user is not authenticated");
        }

        @Test
        @DisplayName("returns a CurrentUser principal even if the token is marked unauthenticated")
        void shouldReturnPrincipalWhenTokenNotMarkedAuthenticated() {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal(SecurityConstants.EMPLOYEE_AUTHORITY),
                            null,
                            List.of()
                    );
            authentication.setAuthenticated(false);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            assertThat(SecurityUtils.getCurrentUser().getUserId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("getCurrentUserId")
    class GetCurrentUserId {

        @Test
        @DisplayName("returns the id of the current user")
        void shouldReturnUserId() {
            authenticate(principal(SecurityConstants.HR_AUTHORITY));

            assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("throws when there is no current user")
        void shouldThrowWithoutAuthentication() {
            assertThatThrownBy(SecurityUtils::getCurrentUserId)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Current user is not authenticated");
        }
    }

    @Nested
    @DisplayName("isAdmin")
    class IsAdmin {

        @Test
        @DisplayName("returns true for an administrator")
        void shouldReturnTrueForAdmin() {
            authenticate(principal(SecurityConstants.ADMIN_AUTHORITY));

            assertThat(SecurityUtils.isAdmin()).isTrue();
        }

        @Test
        @DisplayName("returns false for a non-administrator")
        void shouldReturnFalseForNonAdmin() {
            authenticate(principal(SecurityConstants.HR_AUTHORITY));

            assertThat(SecurityUtils.isAdmin()).isFalse();
        }

        @Test
        @DisplayName("returns false when the current user has no role")
        void shouldReturnFalseWhenRoleIsNull() {
            authenticate(CurrentUser.builder()
                    .userId(USER_ID)
                    .email(EMAIL)
                    .build());

            assertThat(SecurityUtils.isAdmin()).isFalse();
        }

        @Test
        @DisplayName("throws when there is no current user")
        void shouldThrowWithoutAuthentication() {
            assertThatThrownBy(SecurityUtils::isAdmin)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Current user is not authenticated");
        }
    }

    @Nested
    @DisplayName("hasRole")
    class HasRole {

        @Test
        @DisplayName("returns true when the role matches")
        void shouldReturnTrueForMatchingRole() {
            authenticate(principal(SecurityConstants.MANAGER_AUTHORITY));

            assertThat(SecurityUtils.hasRole(SecurityConstants.MANAGER_AUTHORITY)).isTrue();
        }

        @Test
        @DisplayName("returns false when the role does not match")
        void shouldReturnFalseForDifferentRole() {
            authenticate(principal(SecurityConstants.MANAGER_AUTHORITY));

            assertThat(SecurityUtils.hasRole(SecurityConstants.ADMIN_AUTHORITY)).isFalse();
        }

        @Test
        @DisplayName("returns false when the current user has no role")
        void shouldReturnFalseWhenRoleIsNull() {
            authenticate(CurrentUser.builder()
                    .userId(USER_ID)
                    .email(EMAIL)
                    .build());

            assertThat(SecurityUtils.hasRole(SecurityConstants.EMPLOYEE_AUTHORITY)).isFalse();
        }

        @Test
        @DisplayName("throws when there is no current user")
        void shouldThrowWithoutAuthentication() {
            assertThatThrownBy(() -> SecurityUtils.hasRole(SecurityConstants.HR_AUTHORITY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Current user is not authenticated");
        }
    }

    @Nested
    @DisplayName("hasAnyRole")
    class HasAnyRole {

        @Test
        @DisplayName("returns true when the first role matches")
        void shouldReturnTrueForFirstMatch() {
            authenticate(principal(SecurityConstants.HR_AUTHORITY));

            assertThat(SecurityUtils.hasAnyRole(
                    SecurityConstants.HR_AUTHORITY,
                    SecurityConstants.ADMIN_AUTHORITY
            )).isTrue();
        }

        @Test
        @DisplayName("returns true when a later role matches")
        void shouldReturnTrueForLaterMatch() {
            authenticate(principal(SecurityConstants.ADMIN_AUTHORITY));

            assertThat(SecurityUtils.hasAnyRole(
                    SecurityConstants.HR_AUTHORITY,
                    SecurityConstants.MANAGER_AUTHORITY,
                    SecurityConstants.ADMIN_AUTHORITY
            )).isTrue();
        }

        @Test
        @DisplayName("returns false when no role matches")
        void shouldReturnFalseWhenNothingMatches() {
            authenticate(principal(SecurityConstants.EMPLOYEE_AUTHORITY));

            assertThat(SecurityUtils.hasAnyRole(
                    SecurityConstants.HR_AUTHORITY,
                    SecurityConstants.ADMIN_AUTHORITY
            )).isFalse();
        }

        @Test
        @DisplayName("returns false for an empty role list")
        void shouldReturnFalseForEmptyRoles() {
            authenticate(principal(SecurityConstants.EMPLOYEE_AUTHORITY));

            assertThat(SecurityUtils.hasAnyRole()).isFalse();
        }

        @Test
        @DisplayName("returns false when the current user has no role")
        void shouldReturnFalseWhenRoleIsNull() {
            authenticate(CurrentUser.builder()
                    .userId(USER_ID)
                    .email(EMAIL)
                    .build());

            assertThat(SecurityUtils.hasAnyRole(SecurityConstants.HR_AUTHORITY)).isFalse();
        }

        @Test
        @DisplayName("throws when there is no current user")
        void shouldThrowWithoutAuthentication() {
            assertThatThrownBy(() -> SecurityUtils.hasAnyRole(SecurityConstants.HR_AUTHORITY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Current user is not authenticated");
        }
    }

    @Nested
    @DisplayName("keysMatch")
    class KeysMatch {

        @Test
        @DisplayName("returns true for identical keys")
        void shouldReturnTrueForEqualKeys() {
            assertThat(SecurityUtils.keysMatch("internal-key", "internal-key")).isTrue();
        }

        @Test
        @DisplayName("returns false for different keys of the same length")
        void shouldReturnFalseForDifferentKeys() {
            assertThat(SecurityUtils.keysMatch("internal-key", "external-key")).isFalse();
        }

        @Test
        @DisplayName("returns false for different keys of different length")
        void shouldReturnFalseForDifferentLengths() {
            assertThat(SecurityUtils.keysMatch("internal-key", "internal-key-longer")).isFalse();
        }

        @Test
        @DisplayName("returns false when the expected key is null")
        void shouldReturnFalseForNullExpectedKey() {
            assertThat(SecurityUtils.keysMatch(null, "internal-key")).isFalse();
        }

        @Test
        @DisplayName("returns false when the expected key is blank")
        void shouldReturnFalseForBlankExpectedKey() {
            assertThat(SecurityUtils.keysMatch("   ", "   ")).isFalse();
            assertThat(SecurityUtils.keysMatch("", "")).isFalse();
        }

        @Test
        @DisplayName("returns false when the provided key is null")
        void shouldReturnFalseForNullProvidedKey() {
            assertThat(SecurityUtils.keysMatch("internal-key", null)).isFalse();
        }

        @Test
        @DisplayName("returns false when both keys are null")
        void shouldReturnFalseForBothNull() {
            assertThat(SecurityUtils.keysMatch(null, null)).isFalse();
        }

        @Test
        @DisplayName("returns false for a blank provided key")
        void shouldReturnFalseForBlankProvidedKey() {
            assertThat(SecurityUtils.keysMatch("internal-key", "  ")).isFalse();
        }
    }

    private void authenticate(CurrentUser currentUser) {
        List<SimpleGrantedAuthority> authorities = currentUser.getRole() == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority(currentUser.getRole()));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, authorities)
        );
    }

    private CurrentUser principal(String role) {
        return CurrentUser.builder()
                .userId(USER_ID)
                .role(role)
                .email(EMAIL)
                .build();
    }
}
