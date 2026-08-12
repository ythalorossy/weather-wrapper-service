package io.ythalorossy.weatherapi.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
