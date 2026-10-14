package com.vsl.social.service;

import com.vsl.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.vsl.social.service.TestSupport.ADMIN;
import static com.vsl.social.service.TestSupport.AUTHOR;
import static com.vsl.social.service.TestSupport.OTHER;
import static com.vsl.social.service.TestSupport.assertAppError;
import static com.vsl.social.service.TestSupport.loginAs;
import static com.vsl.social.service.TestSupport.logout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AccessControlTest {

    @AfterEach
    void tearDown() {
        logout();
    }

    @Test
    void isAdmin_falseWhenNotLoggedIn() {
        assertThat(AccessControl.isAdmin()).isFalse();
    }

    @Test
    void isAdmin_trueOnlyForAdminRole() {
        loginAs(OTHER, "USER");
        assertThat(AccessControl.isAdmin()).isFalse();

        loginAs(ADMIN, "ADMIN");
        assertThat(AccessControl.isAdmin()).isTrue();
    }

    @Test
    void requireOwnerOrAdmin_allowsAnyListedOwner() {
        loginAs(OTHER, "USER");
        assertThatCode(() -> AccessControl.requireOwnerOrAdmin(OTHER, AUTHOR, OTHER)).doesNotThrowAnyException();
    }

    @Test
    void requireOwnerOrAdmin_allowsAdmin() {
        loginAs(ADMIN, "ADMIN");
        assertThatCode(() -> AccessControl.requireOwnerOrAdmin(ADMIN, AUTHOR)).doesNotThrowAnyException();
    }

    @Test
    void requireOwnerOrAdmin_rejectsStranger() {
        loginAs(OTHER, "USER");
        assertAppError(() -> AccessControl.requireOwnerOrAdmin(OTHER, AUTHOR), ErrorCode.FORBIDDEN);
    }

    @Test
    void requireOwner_rejectsEvenAdmin() {
        loginAs(ADMIN, "ADMIN");
        assertAppError(() -> AccessControl.requireOwner(ADMIN, AUTHOR), ErrorCode.FORBIDDEN);
    }

    @Test
    void requireOwner_rejectsGuest() {
        assertAppError(() -> AccessControl.requireOwner(null, AUTHOR), ErrorCode.FORBIDDEN);
    }
}
