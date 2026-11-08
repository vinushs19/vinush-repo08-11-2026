package com.college.resourcefinder.config;

import com.college.resourcefinder.security.RestAccessDeniedHandler;
import com.college.resourcefinder.security.RestAuthenticationEntryPoint;
import com.college.resourcefinder.security.RestLogoutSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Spring Security Configuration for AI-Based College Resource Finder.
 * Enforces session-based authentication, role-based access control, CSRF protection,
 * and structured JSON responses for unauthorized/forbidden events.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final RestLogoutSuccessHandler logoutSuccessHandler;

    public SecurityConfig(RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          RestLogoutSuccessHandler logoutSuccessHandler) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
    }

    /**
     * Recommended password encoder: BCrypt with work factor 12.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Exposes AuthenticationManager bean for programmatic authentication in AuthService.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        // Opt into BREACH protection handling
        requestHandler.setCsrfRequestAttributeName(null);

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(tokenRepository)
                .csrfTokenRequestHandler(requestHandler)
                .ignoringRequestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "/api/v1/auth/logout",
                    "/api/v1/health",
                    "/actuator/**"
                )
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fixation -> fixation.changeSessionId())
            )
            .authorizeHttpRequests(auth -> auth
                // Static resources & frontend views
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/login.html",
                    "/master-data.html",
                    "/resources.html",
                    "/resource-detail.html",
                    "/search.html",
                    "/attendance.html",
                    "/admin.html",
                    "/chat.html",
                    "/css/**",
                    "/js/**",
                    "/favicon.ico"
                ).permitAll()

                // Health & diagnostics
                .requestMatchers("/api/v1/health", "/api/v1/ai/health", "/actuator/**").permitAll()

                // Public authentication endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()

                // Administrator-only management APIs
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // Academic master data modifications restricted to administrators
                .requestMatchers(HttpMethod.POST, "/api/v1/master/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/master/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/master/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/master/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/master/**").authenticated()

                // Academic resource repository permissions
                .requestMatchers(HttpMethod.POST, "/api/v1/resources/**").hasAnyRole("FACULTY", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/resources/**").hasAnyRole("FACULTY", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/resources/**").hasAnyRole("FACULTY", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/resources/**").hasAnyRole("FACULTY", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/resources/**").authenticated()

                // Attendance subsystem permissions (Confidential - Staff and Administrators only)
                .requestMatchers("/api/v1/attendance/**").hasAnyRole("FACULTY", "ADMIN")

                // Controlled file-saving workflow permissions (Staff and Administrators only)
                .requestMatchers("/api/v1/files/**").hasAnyRole("FACULTY", "ADMIN")

                // AI/NLP Subsystem permissions
                .requestMatchers("/api/v1/ai/parse-file-save").hasAnyRole("FACULTY", "ADMIN")
                .requestMatchers("/api/v1/ai/**").authenticated()

                // Authenticated user profile & logout
                .requestMatchers("/api/v1/auth/me", "/api/v1/auth/profile").authenticated()

                // Default rule: all other requests require authentication
                .anyRequest().authenticated()
            )
            .logout(logout -> logout
                .logoutUrl("/api/v1/auth/logout")
                .logoutSuccessHandler(logoutSuccessHandler)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}
