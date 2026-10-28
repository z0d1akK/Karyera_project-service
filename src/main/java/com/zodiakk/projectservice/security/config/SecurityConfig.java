package com.zodiakk.projectservice.security.config;

import com.zodiakk.projectservice.common.constants.Headers;
import com.zodiakk.projectservice.common.constants.SecurityConstants;
import com.zodiakk.projectservice.security.filter.InternalApiKeyFilter;
import com.zodiakk.projectservice.security.handler.CustomAccessDeniedHandler;
import com.zodiakk.projectservice.security.handler.CustomAuthenticationEntryPoint;
import com.zodiakk.projectservice.security.principal.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import java.util.List;
import java.util.UUID;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Bean
    @Order(1)
    public SecurityFilterChain internalSecurityFilterChain(HttpSecurity http) throws Exception {

        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();

        InternalApiKeyFilter internalApiKeyFilter =
                new InternalApiKeyFilter(internalApiKey, entryPoint);

        return http
                .securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().hasAuthority(SecurityConstants.INTERNAL_SERVICE_AUTHORITY))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(new CustomAccessDeniedHandler()))
                .addFilterBefore(
                        internalApiKeyFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();

        RequestHeaderAuthenticationFilter gatewayAuthenticationFilter =
                gatewayAuthenticationFilter();

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/api-docs",
                                "/api-docs/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(new CustomAccessDeniedHandler()))
                .addFilterBefore(
                        gatewayAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    private RequestHeaderAuthenticationFilter gatewayAuthenticationFilter() {
        RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter() {
            @Override
            protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
                String userId = request.getHeader(Headers.USER_ID);
                String role = request.getHeader(Headers.USER_ROLE);
                String email = request.getHeader(Headers.USER_EMAIL);

                if (userId == null || role == null || email == null) {
                    return null;
                }

                try {
                    return CurrentUser.builder()
                            .userId(UUID.fromString(userId))
                            .role(toAuthority(role))
                            .email(email)
                            .build();
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }

            @Override
            protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
                return null;
            }
        };

        filter.setPrincipalRequestHeader(Headers.USER_ID);
        filter.setExceptionIfHeaderMissing(false);
        filter.setAuthenticationManager(authentication -> {
            CurrentUser currentUser = (CurrentUser) authentication.getPrincipal();

            return new UsernamePasswordAuthenticationToken(
                    currentUser,
                    null,
                    List.of(new SimpleGrantedAuthority(currentUser.getRole()))
            );
        });

        return filter;
    }

    private String toAuthority(String role) {
        return role.startsWith(SecurityConstants.ROLE_PREFIX)
                ? role
                : SecurityConstants.ROLE_PREFIX + role;
    }
}
