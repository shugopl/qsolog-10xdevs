package com.pl.shugo.gsolog.application.service;

import com.pl.shugo.gsolog.domain.entity.User;
import com.pl.shugo.gsolog.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Admin service for user management operations.
 * Only accessible by ADMIN role.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get all users in the system.
     * No filtering by userId - admins can see all users.
     *
     * @return Flux of all users
     */
    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }
}
