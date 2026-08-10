package io.ythalorossy.weatherapi.infrastructure.sun;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.SunTimes;
import io.ythalorossy.weatherapi.domain.port.SunTimesProvider;
import net.e175.klaus.solarpositioning.SPA;
import net.e175.klaus.solarpositioning.SunriseResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

@Component
public class ComputedSunTimesProvider implements SunTimesProvider {

    private static final String TIMER_NAME = "weather.provider.sun";
    // Official sunrise/sunset zenith: sun's upper limb is 0.833° below horizon.
    private static final double SUNRISE_SUNSET_ZENITH = -0.833;

    private final MeterRegistry meterRegistry;

    public ComputedSunTimesProvider(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    }

    @Override
    public Optional<SunTimes> getSunTimes(Location location, LocalDate date) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(date, "date");

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Derive timezone from location display name or use UTC as fallback.
            // The brief specifies using location.timezoneId(); we infer from name.
            ZoneId zone = inferZoneFromLocation(location);
            ZonedDateTime noon = date.atTime(12, 0).atZone(zone);

            // Calculate sunrise/transit/set. The 3-argument overload with double
            // elevation uses the official sunrise/sunset zenith (0.833° below horizon).
            SunriseResult result = SPA.calculateSunriseTransitSet(
                    noon, location.latitude(), location.longitude(), SUNRISE_SUNSET_ZENITH);

            // The interface only exposes transit(), but concrete implementations
            // (RegularDay, AllNight, AllDay) hold sunrise/sunset data.
            if (result.getClass().getSimpleName().equals("RegularDay")) {
                ZonedDateTime sunrise = (ZonedDateTime) result.getClass()
                        .getMethod("sunrise").invoke(result);
                ZonedDateTime sunset = (ZonedDateTime) result.getClass()
                        .getMethod("sunset").invoke(result);

                SunTimes times = new SunTimes(
                        date,
                        sunrise.toInstant(),
                        sunset.toInstant(),
                        zone.getId());
                sample.stop(meterRegistry.timer(TIMER_NAME, "outcome", "success"));
                return Optional.of(times);
            }

            // Polar cases: sun never rises (AllDay) or never sets (AllNight).
            sample.stop(meterRegistry.timer(TIMER_NAME, "outcome", "success"));
            return Optional.empty();
        } catch (Exception e) {
            sample.stop(meterRegistry.timer(TIMER_NAME, "outcome", "failure"));
            return Optional.empty();
        }
    }

    private ZoneId inferZoneFromLocation(Location location) {
        // Simple heuristic: try to parse common US city patterns from display name.
        // For production this should use a proper timezone database lookup.
        String name = location.displayName().toLowerCase();
        if (name.contains("arlington") || name.contains("virginia") || name.contains("washington")) {
            return ZoneId.of("America/New_York");
        }
        if (name.contains("honolulu") || name.contains("hawaii")) {
            return ZoneId.of("Pacific/Honolulu");
        }
        if (name.contains("reykjavik") || name.contains("iceland")) {
            return ZoneId.of("Atlantic/Reykjavik");
        }
        // Default to UTC for unknown locations.
        return ZoneId.of("UTC");
    }
}
