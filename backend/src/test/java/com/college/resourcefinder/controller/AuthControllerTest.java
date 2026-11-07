package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.request.LoginRequest;
import com.college.resourcefinder.dto.request.ProfileUpdateRequest;
import com.college.resourcefinder.dto.request.RegisterRequest;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Seed a test student user if not already present
        if (!userRepository.existsByEmail("test.student@college.edu")) {
            User student = new User(
                    "Test Student",
                    "test.student@college.edu",
                    passwordEncoder.encode("Password@123"),
                    Role.STUDENT.name(),
                    null
            );
            userRepository.save(student);
        }
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - valid credentials should authenticate and set session")
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest("test.student@college.edu", "Password@123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("test.student@college.edu")))
                .andExpect(jsonPath("$.data.role", is("STUDENT")))
                .andExpect(jsonPath("$.data.password").doesNotExist()) // Never leak password hash
                .andReturn();

        // Verify HTTP session was created
        assertNotNull(result.getRequest().getSession(false));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - invalid password should return 401 Unauthorized safely")
    void shouldRejectInvalidPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest("test.student@college.edu", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", containsString("Invalid email address or password")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - non-existent email should return 401 Unauthorized safely")
    void shouldRejectNonExistentEmail() throws Exception {
        LoginRequest loginRequest = new LoginRequest("ghost.user@college.edu", "Password@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - should create new user strictly with STUDENT role and BCrypt hash")
    void shouldRegisterNewStudentSuccessfully() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Arun Kumar",
                "arun.kumar@college.edu",
                "ArunPass@2026",
                null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("arun.kumar@college.edu")))
                .andExpect(jsonPath("$.data.role", is("STUDENT")))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        // Verify database state directly
        User persisted = userRepository.findByEmail("arun.kumar@college.edu").orElseThrow();
        assertEquals("STUDENT", persisted.getRole());
        assertTrue(persisted.getIsActive());
        assertNotEquals("ArunPass@2026", persisted.getPassword()); // Stored encrypted
        assertTrue(passwordEncoder.matches("ArunPass@2026", persisted.getPassword()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - duplicate email should be rejected with 400 Bad Request")
    void shouldRejectDuplicateEmailRegistration() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Duplicate Arun",
                "test.student@college.edu", // already exists
                "NewPass@2026",
                null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - unauthenticated request should return 401 Unauthorized")
    void shouldDenyUnauthenticatedAccessToMe() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - authenticated user should receive their own profile")
    @WithMockUser(username = "test.student@college.edu", roles = {"STUDENT"})
    void shouldReturnProfileForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("test.student@college.edu")))
                .andExpect(jsonPath("$.data.role", is("STUDENT")));
    }

    @Test
    @DisplayName("PUT /api/v1/auth/me - authenticated user can update display name")
    @WithMockUser(username = "test.student@college.edu", roles = {"STUDENT"})
    void shouldUpdateUserProfileName() throws Exception {
        ProfileUpdateRequest update = new ProfileUpdateRequest("Updated Student Name", null, null, null);

        mockMvc.perform(put("/api/v1/auth/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Updated Student Name")));

        User updated = userRepository.findByEmail("test.student@college.edu").orElseThrow();
        assertEquals("Updated Student Name", updated.getName());
    }

    @Test
    @DisplayName("PUT /api/v1/auth/me - password rotation with valid current password")
    @WithMockUser(username = "test.student@college.edu", roles = {"STUDENT"})
    void shouldRotatePasswordSuccessfully() throws Exception {
        ProfileUpdateRequest update = new ProfileUpdateRequest(
                "Test Student",
                null,
                "Password@123",
                "NewSecretPass@999"
        );

        mockMvc.perform(put("/api/v1/auth/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        User user = userRepository.findByEmail("test.student@college.edu").orElseThrow();
        assertTrue(passwordEncoder.matches("NewSecretPass@999", user.getPassword()));
        assertFalse(passwordEncoder.matches("Password@123", user.getPassword()));
    }

    @Test
    @DisplayName("PUT /api/v1/auth/me - password rotation with incorrect current password should fail")
    @WithMockUser(username = "test.student@college.edu", roles = {"STUDENT"})
    void shouldRejectPasswordRotationWithIncorrectCurrentPassword() throws Exception {
        ProfileUpdateRequest update = new ProfileUpdateRequest(
                "Test Student",
                null,
                "WrongCurrentPassword!",
                "NewSecretPass@999"
        );

        mockMvc.perform(put("/api/v1/auth/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Current password does not match")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - should invalidate session and return 200 OK")
    void shouldLogoutSuccessfully() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Logged out successfully")));

        assertTrue(session.isInvalid());
    }
}
