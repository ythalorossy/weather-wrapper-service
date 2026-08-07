package io.ythalorossy.weatherapi.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemperatureTest {

    @Test
    void fahrenheitFactory() {
        Temperature t = Temperature.fahrenheit(85);
        assertThat(t.value()).isEqualTo(85);
        assertThat(t.unit()).isEqualTo(Temperature.Unit.FAHRENHEIT);
    }

    @Test
    void celsiusFactory() {
        Temperature t = Temperature.celsius(30);
        assertThat(t.value()).isEqualTo(30);
        assertThat(t.unit()).isEqualTo(Temperature.Unit.CELSIUS);
    }

    @Test
    void formattedFahrenheit() {
        assertThat(Temperature.fahrenheit(85).formatted()).isEqualTo("85°F");
    }

    @Test
    void formattedCelsius() {
        assertThat(Temperature.celsius(30).formatted()).isEqualTo("30°C");
    }

    @Test
    void rejectsNullUnit() {
        assertThatThrownBy(() -> new Temperature(85, null))
                .isInstanceOf(NullPointerException.class);
    }
}