package com.novabank.card.config;

import com.novabank.shared.correlation.Correlation;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var correlationId = request.getHeader(Correlation.HEADER_NAME);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(Correlation.MDC_KEY, correlationId);
        response.setHeader(Correlation.HEADER_NAME, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(Correlation.MDC_KEY);
        }
    }
}
