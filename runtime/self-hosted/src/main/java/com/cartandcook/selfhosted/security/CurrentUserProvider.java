package com.cartandcook.selfhosted.security;

import com.cartandcook.core.api.ExternalIdentityRepository;
import com.cartandcook.core.api.UserRepository;
import com.cartandcook.core.domain.ExternalIdentity;
import com.cartandcook.core.domain.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    private final ExternalIdentityRepository identityRepo;
    private final UserRepository userRepo;

    public CurrentUserProvider(
            ExternalIdentityRepository identityRepo,
            UserRepository userRepo
    ) {
        this.identityRepo = identityRepo;
        this.userRepo = userRepo;
    }

    /**
     * Resolve the internal User from the JWT.
     * Auto-provisions a User + ExternalIdentity if first login.
     */
    public User getCurrentUser(Jwt jwt) {
        String issuer = jwt.getIssuer().toString();
        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        Boolean verified = jwt.getClaimAsBoolean("email_verified");


        // 1️⃣ existing external identity
        return identityRepo.findByIssuerAndSubject(issuer, sub)
                .map(identity -> userRepo.findById(identity.getUserId())
                        .orElseThrow(() -> new RuntimeException("User not found for identity: " + identity)))
                .orElseGet(() -> linkOrProvision(issuer, sub, email, name, verified));
    }

    private User linkOrProvision(String issuer, String sub, String email, String name, Boolean verified) {

        if(!Boolean.TRUE.equals(verified)){
            throw new RuntimeException("Email not verified for " + email);
        }
        // 2️⃣ try email match
        User user = userRepo.findByEmail(email)
                .orElseGet(() -> userRepo.save(new User(null, email, name)));

        // 3️⃣ link new identity
        identityRepo.save(new ExternalIdentity(user.getId(), issuer, sub));

        return user;
    }
}
