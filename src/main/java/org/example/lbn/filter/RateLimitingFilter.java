package org.example.lbn.filter;


import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    @Value("${request.limit}")
    private int requestLimit;
    private static final Duration REFILL_DURATION = Duration.ofSeconds(1);
    private final Map<String, Bucket> ipBucketMap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        Bucket bucket = ipBucketMap.computeIfAbsent(clientIp, this::createNewBucket);
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.SC_TOO_MANY_REQUESTS);
            response.getWriter().write("Rate limit exceeded. Try again later.");
        }
    }

    private Bucket createNewBucket(String clientIp) {
        Bandwidth limit = Bandwidth.classic(requestLimit, Refill.greedy(requestLimit, REFILL_DURATION));
        return Bucket4j.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0];
    }
}
