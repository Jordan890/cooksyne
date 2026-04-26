package com.cooksyne.adapters.persistencejpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataExternalIdentityRepository extends JpaRepository<ExternalIdentityEntity, Long> {
    Optional<ExternalIdentityEntity> findByIssuerAndSubject(String issuer, String subject);
}
