package io.ythalorossy.weatherapi.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StationTest {

    @Test
    void constructsValidStation() {
        Station s = new Station("KDCA", "Washington National", 38.85, -77.04);
        assertThat(s.stationId()).isEqualTo("KDCA");
        assertThat(s.name()).isEqualTo("Washington National");
    }

    @Test
    void rejectsBlankStationId() {
        assertThatThrownBy(() -> new Station("  ", "Name", 0.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
