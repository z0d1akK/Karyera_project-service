package com.zodiakk.projectservice.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityConstants {

    public static final String ROLE_PREFIX = "ROLE_";

    public static final String EMPLOYEE_AUTHORITY = ROLE_PREFIX + "EMPLOYEE";

    public static final String MANAGER_AUTHORITY = ROLE_PREFIX + "MANAGER";

    public static final String HR_AUTHORITY = ROLE_PREFIX + "HR";

    public static final String ADMIN_AUTHORITY = ROLE_PREFIX + "ADMIN";

    public static final String INTERNAL_SERVICE_AUTHORITY = ROLE_PREFIX + "INTERNAL_SERVICE";
}
