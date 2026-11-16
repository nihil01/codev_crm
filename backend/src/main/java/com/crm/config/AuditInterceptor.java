package com.crm.config;

import com.crm.entity.AuditEvent;
import com.crm.repository.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {
    private final AuditEventRepository events;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || path.startsWith("/api/auth/") ||
            !(method.equals("POST") || method.equals("PUT") ||
              method.equals("PATCH") || method.equals("DELETE"))) return;
        if (response.getStatus() < 200 || response.getStatus() >= 300 || ex != null) return;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return;
        AuditEvent event = new AuditEvent();
        event.setActorEmail(auth.getName());
        event.setHttpMethod(method);
        event.setPath(path.length() > 512 ? path.substring(0, 512) : path);
        event.setStatusCode(response.getStatus());
        event.setAction("HTTP_REQUEST");
        event.setEntityType("HTTP");
        events.save(event);
    }
}
