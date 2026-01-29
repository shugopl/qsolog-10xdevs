package com.pl.shugo.gsolog.domain.entity;

import com.pl.shugo.gsolog.domain.enums.Role;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * User domain entity.
 * Represents a user account with authentication credentials and role.
 */
@Table("users")
public class User {

    @Id
    private UUID id;

    private String email;
    private String username;
    private String passwordHash;
    private Role role;
    private Instant createdAt;
    private Instant updatedAt;

    public User() {
    }

    public User(UUID id, String email, String username, String passwordHash, Role role, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Factory method for new user registration
    public static User create(String email, String username, String passwordHash, Role role) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.email = email;
        user.username = username;
        user.passwordHash = passwordHash;
        user.role = role != null ? role : Role.OPERATOR;
        user.createdAt = Instant.now();
        user.updatedAt = Instant.now();
        return user;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
