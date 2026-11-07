package com.college.resourcefinder.dto.response;

import com.college.resourcefinder.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Publicly viewable user profile information.
 * Password hash is strictly excluded.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    private Long userId;
    private String name;
    private String email;
    private String role;
    private Long departmentId;
    private String departmentName;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public UserProfileResponse() {}

    public static UserProfileResponse fromEntity(User user) {
        if (user == null) return null;
        UserProfileResponse res = new UserProfileResponse();
        res.userId = user.getUserId();
        res.name = user.getName();
        res.email = user.getEmail();
        res.role = user.getRole();
        res.isActive = user.getIsActive();
        res.createdAt = user.getCreatedAt();
        if (user.getDepartment() != null) {
            res.departmentId = user.getDepartment().getDepartmentId();
            res.departmentName = user.getDepartment().getDepartmentName();
        }
        return res;
    }

    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Long getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
