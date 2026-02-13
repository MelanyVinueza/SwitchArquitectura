package com.bancario.msdirectorio.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filtro de seguridad para validar que las peticiones vienen del APIM.
 */
@Slf4j
@Component
@Order(1)
public class ApimSecurityFilter implements Filter {

    @Value("${apim.origin.secret:}")
    private String expectedSecret;

    @Value("${apim.security.enabled:false}")
    private boolean securityEnabled;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!securityEnabled) {
            chain.doFilter(request, response);
            return;
        }

        String path = httpRequest.getRequestURI();
        if (path.contains("/actuator/health") || path.contains("/health")) {
            chain.doFilter(request, response);
            return;
        }

        String receivedSecret = httpRequest.getHeader("x-origin-secret");

        if (receivedSecret == null || !receivedSecret.equals(expectedSecret)) {
            log.warn("[DIRECTORIO] Petición rechazada - x-origin-secret inválido. URI: {}", path);
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Invalid origin\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
