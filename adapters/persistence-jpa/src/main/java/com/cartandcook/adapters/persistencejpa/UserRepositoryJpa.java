package com.cartandcook.adapters.persistencejpa;

import com.cartandcook.core.api.UserRepository;
import com.cartandcook.core.domain.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryJpa implements UserRepository {

    private final SpringDataUserRepository repository;

    public UserRepositoryJpa(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId() != null ? user.getId() : null);
        entity.setEmail(user.getEmail());
        entity.setDisplayName(user.getDisplayName());
        UserEntity saved = repository.save(entity);
        return new User(saved.getId(), saved.getEmail(), saved.getDisplayName());
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id)
                .map(e -> new User(e.getId(), e.getEmail(), e.getDisplayName()));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(e -> new User(e.getId(), e.getEmail(), e.getDisplayName()));
    }
}
