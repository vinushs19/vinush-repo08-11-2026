package com.college.resourcefinder.entity;

/**
 * Standardized user roles for the AI-Based College Resource Finder.
 * Matches database check constraints and Spring Security authority conventions.
 */
public enum Role {
    STUDENT,
    FACULTY,
    ADMIN;

    public static final String ROLE_PREFIX = "ROLE_";

    public String getAuthority() {
        return ROLE_PREFIX + this.name();
    }

    public static boolean isValid(String roleStr) {
        if (roleStr == null) return false;
        for (Role r : values()) {
            if (r.name().equalsIgnoreCase(roleStr.trim())) {
                return true;
            }
        }
        return false;
    }

    public static Role fromString(String roleStr) {
        if (roleStr == null) return STUDENT;
        String clean = roleStr.trim().toUpperCase();
        if (clean.startsWith(ROLE_PREFIX)) {
            clean = clean.substring(ROLE_PREFIX.length());
        }
        for (Role r : values()) {
            if (r.name().equals(clean)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + roleStr);
    }
}
