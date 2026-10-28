package com.zodiakk.projectservice.security.principal;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CurrentUser {

    private final UUID userId;

    private final String role;

    private final String email;
}
