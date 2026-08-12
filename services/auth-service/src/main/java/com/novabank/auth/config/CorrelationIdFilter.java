package com.novabank.auth.config;

import com.novabank.shared.correlation.Correlation;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var correlationId = request.getHeader(Correlation.HEADER_NAME);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        response.setHeader(Correlation.HEADER_NAME, correlationId);
        MDC.put(Correlation.MDC_KEY, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(Correlation.MDC_KEY);
        }
    }
}
