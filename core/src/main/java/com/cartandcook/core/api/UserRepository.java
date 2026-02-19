package com.cartandcook.core.api;

import com.cartandcook.core.domain.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
}
