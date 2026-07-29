package com.mall.config;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 统一采集 HTTP 请求耗时和异常数量，避免每个业务 Controller 重复埋点。
 */
@Component
public class MetricsFilter extends OncePerRequestFilter {

    private final MeterRegistry registry;

    public MetricsFilter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } catch (Exception exception) {
            registry.counter("mall.http.errors",
                    "method", request.getMethod(),
                    "path", request.getRequestURI()).increment();
            throw exception;
        } finally {
            registry.timer("mall.http.requests",
                    "method", request.getMethod(),
                    "path", request.getRequestURI(),
                    "status", String.valueOf(response.getStatus()))
                    .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }
}
