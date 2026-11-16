package com.crm.config;

import com.crm.entity.User;
import com.crm.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class ActiveUserInterceptor implements HandlerInterceptor {
    private final UserRepository users;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!request.getRequestURI().startsWith("/api/") || request.getRequestURI().startsWith("/api/auth/")) return true;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return true;
        User user = users.findByEmail(auth.getName()).orElse(null);
        if (user == null || !user.isActive()) {
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "İstifadəçi deaktivdir");
            return false;
        }
        return true;
    }
}
