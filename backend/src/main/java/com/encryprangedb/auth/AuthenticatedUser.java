package com.encryprangedb.auth;

public record AuthenticatedUser(String username, UserRole role) {
}
