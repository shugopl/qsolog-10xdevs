package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.api.dto.UserResponse;
import com.pl.shugo.gsolog.application.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Admin REST controller.
 * Handles admin-only operations like user management.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Get all users (ADMIN only).
     * Returns list of all registered users without password hashes.
     *
     * @return Flux of UserResponse DTOs
     */
    @GetMapping("/users")
    public Flux<UserResponse> getAllUsers() {
        return adminService.getAllUsers()
                .map(UserResponse::from);
    }
}
