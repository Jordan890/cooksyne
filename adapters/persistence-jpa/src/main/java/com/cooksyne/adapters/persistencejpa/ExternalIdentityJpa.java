package com.cooksyne.adapters.persistencejpa;

import com.cooksyne.core.api.ExternalIdentityRepository;
import com.cooksyne.core.domain.ExternalIdentity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ExternalIdentityJpa implements ExternalIdentityRepository {

    private final SpringDataExternalIdentityRepository repository;

    public ExternalIdentityJpa(SpringDataExternalIdentityRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ExternalIdentity> findByIssuerAndSubject(String issuer, String subject) {
        return repository.findByIssuerAndSubject(issuer, subject)
                .map(e -> new ExternalIdentity(e.getUserId(), e.getIssuer(), e.getSubject()));
    }

    @Override
    public ExternalIdentity save(ExternalIdentity identity) {
        ExternalIdentityEntity entity = new ExternalIdentityEntity();
        entity.setUserId(identity.getUserId());
        entity.setIssuer(identity.getIssuer());
        entity.setSubject(identity.getSubject());
        ExternalIdentityEntity saved = repository.save(entity);
        return new ExternalIdentity(saved.getUserId(), saved.getIssuer(), saved.getSubject());
    }
}
