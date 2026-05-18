package com.encryprangedb.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || request.getRequestURI().startsWith("/api/auth/login")) {
            return true;
        }
        String token = request.getHeader("X-Auth-Token");
        AuthenticatedUser user = authService.authenticate(token)
                .orElseThrow(() -> new AuthException("请先登录"));
        AuthContext.set(user);
        authorize(request.getRequestURI(), request.getMethod(), user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private void authorize(String path, String method, AuthenticatedUser user) {
        if (user.role() == UserRole.ADMIN) {
            return;
        }
        if (user.role() == UserRole.AUDITOR) {
            boolean readAudit = "GET".equalsIgnoreCase(method)
                    && (path.equals("/api/admin/audit-logs") || path.equals("/api/auth/me"));
            if (!readAudit) {
                throw new AccessDeniedException("审计员仅允许查看审计日志");
            }
            return;
        }
        if (user.role() == UserRole.USER) {
            if (path.startsWith("/api/admin/")) {
                throw new AccessDeniedException("普通用户不能访问管理功能");
            }
            return;
        }
        throw new AccessDeniedException("角色无权访问");
    }
}
