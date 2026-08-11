package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.WeatherAlert;

import java.util.List;

public record AlertsPayload(List<WeatherAlert> alerts) {}

