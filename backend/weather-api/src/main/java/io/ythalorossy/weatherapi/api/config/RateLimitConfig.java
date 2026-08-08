package io.ythalorossy.weatherapi.api.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.time.Duration;

/**
 * Wires Bucket4j distributed over Redis (Lettuce) and registers the
 * {@link io.ythalorossy.weatherapi.api.filter.RateLimitFilter} for the
 * {@code /api/*} URL pattern.
 *
 * <p>Two-bandwidth bucket:
 * <ul>
 *   <li><b>burst</b> — short spikes (default: capacity 5, refill 5/s)</li>
 *   <li><b>sustained</b> — long-term cap (default: capacity 60, refill 60/min)</li>
 * </ul>
 * Both must have at least one token for a request to be allowed.
 *
 * <p>The Lettuce {@link RedisClient} is created here (not shared with Spring
 * Boot's {@code StringRedisTemplate} connection factory) because Bucket4j's
 * {@link LettuceBasedProxyManager} needs a raw {@code RedisClient}. The cost
 * is one multiplexed connection — negligible.
 */
@Configuration
public class RateLimitConfig {

    @Bean(destroyMethod = "shutdown")
    public RedisClient rateLimitRedisClient(RedisConnectionDetails connectionDetails) {
        RedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();
        String host = standalone.getHost();
        int port = standalone.getPort();
        return RedisClient.create(RedisURI.create("redis://" + host + ":" + port));
    }

    @Bean
    public ProxyManager<byte[]> rateLimitProxyManager(RedisClient redisClient) {
        // Expire bucket keys 10 minutes after last write so we don't accumulate
        // an unbounded set of one-off IPs in Redis. Buckets that see traffic
        // stay alive; idle ones fall out.
        return LettuceBasedProxyManager.builderFor(redisClient)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                Duration.ofMinutes(10)))
                .build();
    }

    @Bean
    public BucketConfiguration bucketConfiguration(RateLimitProperties props) {
        RateLimitProperties.Bandwidth burst = props.getBurst();
        RateLimitProperties.Bandwidth sustained = props.getSustained();

        return BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(
                        burst.getCapacity(),
                        Refill.greedy(burst.getCapacity(),
                                Duration.ofSeconds(burst.getRefillPeriodSeconds()))))
                .addLimit(Bandwidth.classic(
                        sustained.getCapacity(),
                        Refill.greedy(sustained.getCapacity(),
                                Duration.ofSeconds(sustained.getRefillPeriodSeconds()))))
                .build();
    }

    /**
     * Filter scoped to {@code /api/*}. Runs early in the chain so we reject
     * before any work happens (no controller resolution, no Spring MVC).
     *
     * <p>Disabled when {@code weather.rate-limit.enabled=false}; useful for tests
     * that make many requests from the same MockMvc IP and would otherwise hit
     * the burst bucket.
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            name = "weather.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<Filter> rateLimitFilterRegistration(
            ProxyManager<byte[]> proxyManager,
            BucketConfiguration bucketConfiguration) {

        Filter filter = new io.ythalorossy.weatherapi.api.filter.RateLimitFilter(
                proxyManager, bucketConfiguration);

        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>(filter);
        reg.addUrlPatterns("/api/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        reg.setName("rateLimitFilter");
        return reg;
    }
}