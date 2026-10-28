package com.zodiakk.projectservice.security.filter;

import com.zodiakk.projectservice.common.constants.Headers;
import com.zodiakk.projectservice.common.constants.SecurityConstants;
import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.security.util.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PATH_PREFIX = "/internal/";

    private static final String INTERNAL_SERVICE_PRINCIPAL = "internal-service";

    private final String internalApiKey;

    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX)
                || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(Headers.INTERNAL_KEY);

        if (!SecurityUtils.keysMatch(internalApiKey, providedKey)) {
            SecurityContextHolder.clearContext();

            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException(ErrorMessages.INVALID_INTERNAL_API_KEY)
            );
            return;
        }

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        INTERNAL_SERVICE_PRINCIPAL,
                        null,
                        List.of(new SimpleGrantedAuthority(SecurityConstants.INTERNAL_SERVICE_AUTHORITY))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
