package com.cooksyne.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class User {

    private final Long id;
    private final String email;
    private final String displayName;

    public static User hydrate(Long id, String email, String displayName) {
        return new User(id, email, displayName);
    }
}
