package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.SunTimes;
import io.ythalorossy.weatherapi.domain.model.WeatherOffice;

import java.util.Objects;
import java.util.Optional;

public record LocationMetadataResult(WeatherOffice office, Optional<SunTimes> sunTimes) {

    public LocationMetadataResult {
        Objects.requireNonNull(office, "office");
        Objects.requireNonNull(sunTimes, "sunTimes");
    }
}
