package io.ythalorossy.weatherapi.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationTest {

    @Test
    void constructsValidLocation() {
        Location loc = new Location(38.8816, -77.0910, "Arlington, VA, US");
        assertThat(loc.latitude()).isEqualTo(38.8816);
        assertThat(loc.longitude()).isEqualTo(-77.0910);
        assertThat(loc.displayName()).isEqualTo("Arlington, VA, US");
    }

    @Test
    void rejectsLatitudeAbove90() {
        assertThatThrownBy(() -> new Location(91.0, 0.0, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitude");
    }

    @Test
    void rejectsLatitudeBelowNegative90() {
        assertThatThrownBy(() -> new Location(-91.0, 0.0, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsLongitudeAbove180() {
        assertThatThrownBy(() -> new Location(0.0, 181.0, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longitude");
    }

    @Test
    void rejectsLongitudeBelowNegative180() {
        assertThatThrownBy(() -> new Location(0.0, -181.0, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullDisplayName() {
        assertThatThrownBy(() -> new Location(0.0, 0.0, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsBlankDisplayName() {
        assertThatThrownBy(() -> new Location(0.0, 0.0, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("displayName");
    }

    @Test
    void cacheKeyUsesFourDecimalsAndRounds() {
        Location loc = new Location(38.88162, -77.09103, "Arlington");
        assertThat(loc.cacheKey("weather")).isEqualTo("weather:38.8816,-77.0910");
    }

    @Test
    void cacheKeyIsStableForTrivialVariations() {
        Location a = new Location(38.88160001, -77.09100001, "A");
        Location b = new Location(38.88160002, -77.09100002, "B");
        assertThat(a.cacheKey("weather")).isEqualTo(b.cacheKey("weather"));
    }

    @Test
    void geocodingCacheKeyTrimsAndLowercases() {
        assertThat(Location.geocodingCacheKey("Arlington, VA"))
                .isEqualTo("geo:arlington, va");
    }

    @Test
    void geocodingCacheKeyCollapsesInternalWhitespace() {
        assertThat(Location.geocodingCacheKey("Arlington,   VA"))
                .isEqualTo("geo:arlington, va");
        assertThat(Location.geocodingCacheKey("  Arlington ,  VA  "))
                .isEqualTo("geo:arlington , va");
    }

    @Test
    void geocodingCacheKeyRejectsNull() {
        assertThatThrownBy(() -> Location.geocodingCacheKey(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void geocodingCacheKeyCollapsesNewlinesAndTabs() {
        assertThat(Location.geocodingCacheKey("Arlington,\n\tVA"))
                .isEqualTo("geo:arlington, va");
    }
}