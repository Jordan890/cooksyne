package com.cooksyne.core.domain;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ExternalIdentity {

    private final Long userId;
    private final String issuer; // OIDC issuer
    private final String subject; // sub from JWT
}
