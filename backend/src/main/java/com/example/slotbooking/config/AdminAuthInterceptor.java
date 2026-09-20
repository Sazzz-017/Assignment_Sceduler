package com.example.slotbooking.config;

import com.example.slotbooking.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Guards {@code /api/admin/**} by requiring the {@code X-Admin-Passcode} header to match
 * the configured passcode. This is a lightweight gate (single shared secret), not per-user auth.
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final String HEADER = "X-Admin-Passcode";

    private final String passcode;

    public AdminAuthInterceptor(@Value("${app.admin.passcode}") String passcode) {
        this.passcode = passcode;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String provided = request.getHeader(HEADER);
        if (provided == null || !provided.equals(passcode)) {
            throw new UnauthorizedException("Invalid or missing admin passcode.");
        }
        return true;
    }
}
