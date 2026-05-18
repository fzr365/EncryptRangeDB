package com.encryprangedb.auth;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@DependsOn("adminSchemaInitializer")
public class AuthService {
    private final JdbcTemplate jdbcTemplate;
    private final Map<String, AuthenticatedUser> sessions = new ConcurrentHashMap<>();

    public AuthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureDefaultUsers() {
        createDefaultUser("admin", "admin123", UserRole.ADMIN);
        createDefaultUser("user", "user123", UserRole.USER);
        createDefaultUser("audit", "audit123", UserRole.AUDITOR);
    }

    public LoginResult login(String username, String password) {
        UserRow row = findUser(username).orElseThrow(() -> new AuthException("用户名或密码错误"));
        if (!row.enabled() || !PasswordUtil.matches(password, row.salt(), row.passwordHash())) {
            throw new AuthException("用户名或密码错误");
        }
        AuthenticatedUser user = new AuthenticatedUser(row.username(), row.role());
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, user);
        return new LoginResult(token, user.username(), user.role().name(), OffsetDateTime.now().plusHours(8));
    }

    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public Optional<AuthenticatedUser> authenticate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(token));
    }

    private Optional<UserRow> findUser(String username) {
        var rows = jdbcTemplate.query("""
                        SELECT username, password_hash, salt, role, enabled
                        FROM app_user
                        WHERE username = ?
                        """,
                (rs, rowNum) -> new UserRow(
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("salt"),
                        UserRole.valueOf(rs.getString("role")),
                        rs.getBoolean("enabled")),
                username);
        return rows.stream().findFirst();
    }

    private void createDefaultUser(String username, String password, UserRole role) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_user WHERE username = ?", Integer.class, username);
        if (count != null && count > 0) {
            return;
        }
        String salt = PasswordUtil.newSaltHex();
        jdbcTemplate.update("""
                        INSERT INTO app_user (username, password_hash, salt, role, enabled)
                        VALUES (?, ?, ?, ?, 1)
                        """,
                username, PasswordUtil.hash(password, salt), salt, role.name());
    }

    private record UserRow(String username, String passwordHash, String salt, UserRole role, boolean enabled) {
    }

    public record LoginResult(String token, String username, String role, OffsetDateTime expiresAt) {
    }
}
