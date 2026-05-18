package com.encryprangedb.controller;

import com.encryprangedb.auth.AuthContext;
import com.encryprangedb.auth.AuthService;
import com.encryprangedb.model.LoginRequest;
import com.encryprangedb.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuditLogService auditLogService;

    public AuthController(AuthService authService, AuditLogService auditLogService) {
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/login")
    public AuthService.LoginResult login(@Valid @RequestBody LoginRequest request) {
        long start = System.currentTimeMillis();
        try {
            AuthService.LoginResult result = authService.login(request.username(), request.password());
            auditLogService.log("LOGIN", null, null, null, null, null, null,
                    System.currentTimeMillis() - start, "SUCCESS", "user=" + result.username() + ", role=" + result.role());
            return result;
        } catch (RuntimeException ex) {
            auditLogService.log("LOGIN", null, null, null, null, null, null,
                    System.currentTimeMillis() - start, "FAILED", "user=" + request.username());
            throw ex;
        }
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        authService.logout(request.getHeader("X-Auth-Token"));
        return Map.of("success", true);
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        var user = AuthContext.current();
        return Map.of("username", user.username(), "role", user.role().name());
    }
}
