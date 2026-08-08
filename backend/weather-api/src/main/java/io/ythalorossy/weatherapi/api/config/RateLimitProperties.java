package io.ythalorossy.weatherapi.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound to the {@code weather.rate-limit.*} tree in {@code application.yml}.
 *
 * <p>Two-bandwidth limit:
 * <ul>
 *   <li><b>burst</b> — capacity and refill rate for short-term spikes.
 *       Defaults to 5 capacity, 5 tokens per second refill.</li>
 *   <li><b>sustained</b> — capacity and refill rate for long-term cap.
 *       Defaults to 60 capacity, 60 tokens per minute refill.</li>
 * </ul>
 * Each request consumes one token from <em>both</em> bandwidths; the more
 * restrictive decides.
 *
 * <p>The Redis connection is <em>not</em> configured here \u2014 it reuses the
 * Spring Boot {@code spring.data.redis.*} properties so the rate-limiter
 * shares the same Redis instance as the caches and the testcontainers
 * Testcontainers Redis in tests.
 */
@ConfigurationProperties("weather.rate-limit")
public class RateLimitProperties {

    /** Master switch. Set to false in tests so existing multi-request tests still pass. */
    private boolean enabled = true;
    private Bandwidth burst = new Bandwidth(5, 1);
    private Bandwidth sustained = new Bandwidth(60, 60);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Bandwidth getBurst() { return burst; }
    public void setBurst(Bandwidth burst) { this.burst = burst; }

    public Bandwidth getSustained() { return sustained; }
    public void setSustained(Bandwidth sustained) { this.sustained = sustained; }

    public static class Bandwidth {
        /** Maximum tokens (request capacity). */
        private int capacity;
        /** Refill period in seconds. {@code refill-period-seconds=1} + {@code capacity=5}
         *  means "5 tokens per second, greedy". */
        private int refillPeriodSeconds;

        public Bandwidth() {}

        public Bandwidth(int capacity, int refillPeriodSeconds) {
            this.capacity = capacity;
            this.refillPeriodSeconds = refillPeriodSeconds;
        }

        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }

        public int getRefillPeriodSeconds() { return refillPeriodSeconds; }
        public void setRefillPeriodSeconds(int refillPeriodSeconds) {
            this.refillPeriodSeconds = refillPeriodSeconds;
        }
    }
}