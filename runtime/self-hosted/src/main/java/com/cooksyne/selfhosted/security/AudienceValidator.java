package com.cooksyne.selfhosted.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> allowedAudiences;
    private final OAuth2Error error = new OAuth2Error("invalid_token", "The required audience is missing", null);

    public AudienceValidator(List<String> allowedAudiences) {
        this.allowedAudiences = allowedAudiences;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Object audObj = token.getClaims().get("aud");
        if (audObj instanceof String) {
            if (allowedAudiences.contains(audObj)) {
                return OAuth2TokenValidatorResult.success();
            }
        } else if (audObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> tokenAud = (List<String>) audObj;
            for (String a : tokenAud) {
                if (allowedAudiences.contains(a)) {
                    return OAuth2TokenValidatorResult.success();
                }
            }
        }
        return OAuth2TokenValidatorResult.failure(error);
    }
}
