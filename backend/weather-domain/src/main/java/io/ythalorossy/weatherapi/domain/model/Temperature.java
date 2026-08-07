package io.ythalorossy.weatherapi.domain.model;

import java.util.Objects;

/**
 * Temperature value object with unit. Immutable.
 */
public record Temperature(int value, Unit unit) {

    public enum Unit { FAHRENHEIT, CELSIUS }

    public Temperature {
        Objects.requireNonNull(unit, "unit must not be null");
    }

    public static Temperature fahrenheit(int value) {
        return new Temperature(value, Unit.FAHRENHEIT);
    }

    public static Temperature celsius(int value) {
        return new Temperature(value, Unit.CELSIUS);
    }

    public String formatted() {
        return value + "°" + (unit == Unit.FAHRENHEIT ? "F" : "C");
    }
}