package com.cartandcook.adapters.persistencejpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataUserRuntimeConfigRepository extends JpaRepository<UserRuntimeConfigEntity, Long> {
    Optional<UserRuntimeConfigEntity> findTopByOrderByIdAsc();
}
