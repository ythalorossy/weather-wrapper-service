package io.ythalorossy.weatherapi.infrastructure.sun;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.SunTimes;
import io.ythalorossy.weatherapi.domain.port.SunTimesProvider;
import net.e175.klaus.solarpositioning.SPA;
import net.e175.klaus.solarpositioning.SunriseResult;
import org.springframework.stereotype.Component;

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
    public Optional<SunTimes> getSunTimes(Location location, LocalDate date, ZoneId zone) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(zone, "zone");

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            ZonedDateTime noon = date.atTime(12, 0).atZone(zone);

            SunriseResult result = SPA.calculateSunriseTransitSet(
                    noon, location.latitude(), location.longitude(), SUNRISE_SUNSET_ZENITH);

            sample.stop(meterRegistry.timer(TIMER_NAME, "outcome", "success"));

            // Library bug: at borderline polar latitudes (~64°N at solstice),
            // solarpositioning returns RegularDay with sunrise > sunset. Treat
            // those as all-day / all-night and surface empty.
            if (result instanceof SunriseResult.RegularDay r && r.sunrise().isBefore(r.sunset())) {
                return Optional.of(new SunTimes(
                        date, r.sunrise().toInstant(), r.sunset().toInstant(), zone.getId()));
            }
            return Optional.empty();
        } catch (Exception e) {
            sample.stop(meterRegistry.timer(TIMER_NAME, "outcome", "failure"));
            return Optional.empty();
        }
    }
}