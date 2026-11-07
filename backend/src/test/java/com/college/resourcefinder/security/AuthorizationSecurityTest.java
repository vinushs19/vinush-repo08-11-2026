package com.college.resourcefinder.security;

import com.college.resourcefinder.dto.request.LoginRequest;
import com.college.resourcefinder.dto.request.UserStatusUpdateRequest;
import com.college.resourcefinder.entity.Role;
import com.college.resourcefinder.entity.User;
import com.college.resourcefinder.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthorizationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User studentUser;
    private User facultyUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        studentUser = userRepository.findByEmail("student.auth@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Student Auth",
                        "student.auth@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.STUDENT.name(),
                        null
                )));

        facultyUser = userRepository.findByEmail("faculty.auth@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Faculty Auth",
                        "faculty.auth@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.FACULTY.name(),
                        null
                )));

        adminUser = userRepository.findByEmail("admin@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "System Admin",
                        "admin@college.edu",
                        passwordEncoder.encode("Admin@123"),
                        Role.ADMIN.name(),
                        null
                )));
    }

    @Test
    @DisplayName("Unauthenticated request to admin API should return 401 Unauthorized")
    void shouldDenyUnauthenticatedToAdminApi() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Student user cannot access administrator API (403 Forbidden)")
    @WithMockUser(username = "student.auth@college.edu", roles = {"STUDENT"})
    void shouldDenyStudentAccessToAdminApi() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("Faculty user cannot access administrator API (403 Forbidden)")
    @WithMockUser(username = "faculty.auth@college.edu", roles = {"FACULTY"})
    void shouldDenyFacultyAccessToAdminApi() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("Admin user can successfully list all users (200 OK)")
    @WithMockUser(username = "admin@college.edu", roles = {"ADMIN"})
    void shouldAllowAdminAccessToAdminApi() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("Admin can deactivate an active user account")
    @WithMockUser(username = "admin@college.edu", roles = {"ADMIN"})
    void shouldAllowAdminToDeactivateUser() throws Exception {
        UserStatusUpdateRequest update = new UserStatusUpdateRequest(false, null);

        mockMvc.perform(patch("/api/v1/admin/users/" + studentUser.getUserId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.isActive", is(false)));

        User reloaded = userRepository.findById(studentUser.getUserId()).orElseThrow();
        assertFalse(reloaded.getIsActive());
    }

    @Test
    @DisplayName("Deactivated user cannot authenticate (rejected with safe error)")
    void shouldRejectLoginForDeactivatedUser() throws Exception {
        // Deactivate student user directly
        studentUser.setIsActive(false);
        userRepository.save(studentUser);

        LoginRequest loginRequest = new LoginRequest("student.auth@college.edu", "Password@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("deactivated")));
    }

    @Test
    @DisplayName("Admin cannot deactivate the primary administrator account")
    @WithMockUser(username = "admin@college.edu", roles = {"ADMIN"})
    void shouldPreventDeactivationOfPrimaryAdmin() throws Exception {
        UserStatusUpdateRequest update = new UserStatusUpdateRequest(false, null);

        mockMvc.perform(patch("/api/v1/admin/users/" + adminUser.getUserId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("primary administrator account cannot be deactivated")));
    }

    @Test
    @DisplayName("CSRF protection: Mutating protected requests without CSRF token must be rejected with 403 Forbidden")
    @WithMockUser(username = "admin@college.edu", roles = {"ADMIN"})
    void shouldRejectMutatingRequestWithoutCsrfToken() throws Exception {
        UserStatusUpdateRequest update = new UserStatusUpdateRequest(false, null);

        // Intentionally omit .with(csrf()) to verify CSRF filter rejection
        mockMvc.perform(patch("/api/v1/admin/users/" + studentUser.getUserId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }
}
