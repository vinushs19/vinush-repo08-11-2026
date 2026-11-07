package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.request.LoginRequest;
import com.college.resourcefinder.dto.request.ProfileUpdateRequest;
import com.college.resourcefinder.dto.request.RegisterRequest;
import com.college.resourcefinder.dto.response.ApiResponse;
import com.college.resourcefinder.dto.response.UserProfileResponse;
import com.college.resourcefinder.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication and User Profile API controller.
 * Base path: {@code /api/v1/auth}
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates user credentials and establishes an HTTP session.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserProfileResponse>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                                  HttpServletRequest request,
                                                                  HttpServletResponse response) {
        UserProfileResponse profile = authService.authenticate(loginRequest, request, response);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful.", profile));
    }

    /**
     * Registers a new student account. Role is strictly assigned as STUDENT.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserProfileResponse>> register(@Valid @RequestBody RegisterRequest registerRequest,
                                                                     HttpServletRequest request) {
        UserProfileResponse profile = authService.register(registerRequest, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account registered successfully.", profile));
    }

    /**
     * Retrieves profile details for the currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser() {
        UserProfileResponse profile = authService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * Updates display name, department association, or rotates password for the authenticated user.
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@Valid @RequestBody ProfileUpdateRequest updateRequest,
                                                                          HttpServletRequest request) {
        UserProfileResponse profile = authService.updateCurrentUserProfile(updateRequest, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully.", profile));
    }

    /**
     * Returns the active CSRF token for the session so frontend clients can initialize their headers.
     */
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCsrfToken(CsrfToken token) {
        if (token == null) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("message", "CSRF token not required for this request")));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "token", token.getToken(),
                "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName()
        )));
    }
}
