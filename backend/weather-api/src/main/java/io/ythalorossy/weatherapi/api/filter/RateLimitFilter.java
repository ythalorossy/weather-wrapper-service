package io.ythalorossy.weatherapi.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Servlet filter that consumes a token from a per-client-IP bucket on every
 * request, returning HTTP 429 (Too Many Requests) when either bandwidth
 * (burst or sustained) is empty.
 *
 * <p>The IP key is taken from {@code X-Forwarded-For} (first hop) if present,
 * otherwise from {@code request.getRemoteAddr()}. Set up for the standard
 * "X-Forwarded-For behind one reverse proxy" topology.
 *
 * <p>Failure mode: if Redis is unreachable, the filter logs a warning and
 * <em>fails open</em> (allows the request through). Rate limiting is best-
 * effort; a broken limiter shouldn't take down the API.
 */
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final ProxyManager<byte[]> proxyManager;
    private final BucketConfiguration bucketConfiguration;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(ProxyManager<byte[]> proxyManager,
                           BucketConfiguration bucketConfiguration) {
        this.proxyManager = proxyManager;
        this.bucketConfiguration = bucketConfiguration;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpReq)
                || !(response instanceof HttpServletResponse httpResp)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = clientIp(httpReq);
        ConsumptionProbe probe;
        try {
            // Bucket4j's Lettuce impl uses byte[] keys; convert the IP.
            byte[] key = ip.getBytes(StandardCharsets.UTF_8);
            BucketProxy bucket = proxyManager.builder().build(key, () -> bucketConfiguration);
            probe = bucket.tryConsumeAndReturnRemaining(1);
        } catch (Exception e) {
            // Fail open: if Redis is down, don't take down the API.
            log.warn("Rate-limit lookup failed for ip={}: {} — allowing request", ip, e.toString());
            chain.doFilter(request, response);
            return;
        }

        if (probe.isConsumed()) {
            httpResp.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1,
                TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
        writeTooManyRequests(httpResp, ip, retryAfterSeconds);
    }

    private static String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            // First IP in the comma-separated chain is the original client.
            int comma = fwd.indexOf(',');
            return (comma > 0 ? fwd.substring(0, comma) : fwd).trim();
        }
        return req.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse resp, String ip, long retryAfterSeconds)
            throws IOException {
        resp.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        resp.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        resp.setHeader("X-RateLimit-Remaining", "0");
        resp.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded. Try again in " + retryAfterSeconds + " seconds.");
        pd.setType(URI.create("https://weather-wrapper-service.ythalorossy.io/errors/rate-limit-exceeded"));
        pd.setTitle("Rate limit exceeded");
        pd.setProperty("retryAfterSeconds", retryAfterSeconds);

        objectMapper.writeValue(resp.getWriter(), pd);
        log.debug("Rate-limited ip={} retry-after={}s", ip, retryAfterSeconds);
    }
}