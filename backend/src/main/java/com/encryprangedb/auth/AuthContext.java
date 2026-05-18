package com.encryprangedb.auth;

public final class AuthContext {
    private static final ThreadLocal<AuthenticatedUser> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthenticatedUser user) {
        CURRENT.set(user);
    }

    public static AuthenticatedUser current() {
        return CURRENT.get();
    }

    public static String usernameOrSystem() {
        AuthenticatedUser user = current();
        return user == null ? "system" : user.username();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
