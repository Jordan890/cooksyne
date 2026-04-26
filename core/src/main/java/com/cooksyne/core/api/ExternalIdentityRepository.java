package com.cooksyne.core.api;

import com.cooksyne.core.domain.ExternalIdentity;

import java.util.Optional;

public interface ExternalIdentityRepository {

    Optional<ExternalIdentity> findByIssuerAndSubject(String issuer, String subject);
    ExternalIdentity save(ExternalIdentity identity);
}
