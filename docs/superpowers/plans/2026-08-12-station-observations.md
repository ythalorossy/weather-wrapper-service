# Station Observations & Raw METAR Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement station-level weather data (nearby reporting stations list, decoded metrics, raw METAR text, and observation trends) with backend domain ports, infrastructure adapters, use cases, REST endpoints, and frontend tabs/components.

**Architecture:** Clean architecture (domain → application → infrastructure → api) with multi-layer Redis cache-aside (1h stations cache, 10m observation cache).

**TechStack:** Spring Boot 3.3.5, Java 21, Redis, WireMock, Vite, React 19, TypeScript, TanStack Query 5, Tailwind CSS v4.

## Global Constraints

- Dependency direction is one-way only: `domain ← application ← infrastructure`, and `domain, application, infrastructure ← api`.
- Errors must use RFC 9457 `ProblemDetail` via `GlobalExceptionHandler`.
- Cache ports are generic (`Cache<V>`) with `RedisJsonCache<V>` adapter.

---

## Tasks

### Task 1: Domain Models & Ports

**Files:**
- Create: `backend/weather-domain/src/main/java/io/ythalorossy/weatherapi/domain/model/Station.java`
- Create: `backend/weather-domain/src/main/java/io/ythalorossy/weatherapi/domain/model/StationObservation.java`
- Create: `backend/weather-domain/src/main/java/io/ythalorossy/weatherapi/domain/port/StationsProvider.java`
- Create: `backend/weather-domain/src/main/java/io/ythalorossy/weatherapi/domain/port/StationObservationProvider.java`
- Test: `backend/weather-domain/src/test/java/io/ythalorossy/weatherapi/domain/model/StationTest.java`

**Interfaces:**
- Produces: `Station`, `StationObservation`, `StationsProvider`, `StationObservationProvider`.

- [ ] **Step 1: Write failing test for Station model**

```java
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
```

- [ ] **Step 2: Run test to verify failure**

Run: `mvn -pl backend/weather-domain test -Dtest=StationTest`
Expected: FAIL (class not found)

- [ ] **Step 3: Implement domain models and ports**

Create `Station.java`, `StationObservation.java`, `StationsProvider.java`, and `StationObservationProvider.java`.

- [ ] **Step 4: Run test to verify pass**

Run: `mvn -pl backend/weather-domain test -Dtest=StationTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/weather-domain
git commit -m "feat(domain): add Station models and providers ports"
```

---

### Task 2: Infrastructure Adapters & DTOs

**Files:**
- Create: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/dto/StationsResponse.java`
- Create: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/dto/StationObservationsResponse.java`
- Create: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsStationsProvider.java`
- Create: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsStationObservationProvider.java`
- Test: `backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsStationsProviderTest.java`

**Interfaces:**
- Consumes: `StationsProvider`, `StationObservationProvider`.
- Produces: `NwsStationsProvider`, `NwsStationObservationProvider`.

- [ ] **Step 1: Write test for NwsStationsProvider**

```java
package io.ythalorossy.weatherapi.infrastructure.weather;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.ythalorossy.weatherapi.domain.model.Station;
import io.ythalorossy.weatherapi.infrastructure.InfrastructureTestConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InfrastructureTestConfig.class)
@ContextConfiguration(classes = InfrastructureTestConfig.class)
class NwsStationsProviderTest {

    static WireMockServer wireMock = new WireMockServer(options().dynamicPort());

    @DynamicPropertySource
    static void overrideBaseUrl(DynamicPropertyRegistry registry) {
        wireMock.start();
        registry.add("weather.provider.base-url", wireMock::baseUrl);
    }

    @AfterAll
    static void teardown() {
        wireMock.stop();
    }

    @Autowired
    NwsStationsProvider provider;

    @Test
    void fetchesStationsList() {
        wireMock.stubFor(get(urlPathEqualTo("/gridpoints/LWX/11,22/stations"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "features": [
                                    {
                                      "properties": {
                                        "stationIdentifier": "KDCA",
                                        "name": "Washington National"
                                      },
                                      "geometry": {
                                        "coordinates": [-77.04, 38.85]
                                      }
                                    }
                                  ]
                                }
                                """)));

        List<Station> stations = provider.getStations("LWX", 11, 22);
        assertThat(stations).hasSize(1);
        assertThat(stations.get(0).stationId()).isEqualTo("KDCA");
        assertThat(stations.get(0).latitude()).isEqualTo(38.85);
        assertThat(stations.get(0).longitude()).isEqualTo(-77.04);
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `mvn -pl backend/weather-infrastructure test -Dtest=NwsStationsProviderTest`
Expected: FAIL

- [ ] **Step 3: Implement DTOs and Adapters**

Implement `StationsResponse`, `StationObservationsResponse`, `NwsStationsProvider`, and `NwsStationObservationProvider`.

- [ ] **Step 4: Run test to verify pass**

Run: `mvn -pl backend/weather-infrastructure test -Dtest=NwsStationsProviderTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/weather-infrastructure
git commit -m "feat(infra): add NwsStationsProvider and NwsStationObservationProvider adapters"
```

---

### Task 3: Application Use Cases

**Files:**
- Create: `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetStationsUseCase.java`
- Create: `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetStationObservationsUseCase.java`
- Test: `backend/weather-application/src/test/java/io/ythalorossy/weatherapi/application/usecase/GetStationsUseCaseTest.java`

**Interfaces:**
- Consumes: `StationsProvider`, `StationObservationProvider`, `Cache<List<Station>>`, `Cache<List<StationObservation>>`, `LocationResolver`.
- Produces: `GetStationsUseCase`, `GetStationObservationsUseCase`.

- [ ] **Step 1: Write test for GetStationsUseCase**

```package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Station;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.StationsProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class GetStationsUseCaseTest {

    @SuppressWarnings("unchecked")
    @Test
    void executesSuccessfully() {
        LocationResolver resolver = Mockito.mock(LocationResolver.class);
        StationsProvider provider = Mockito.mock(StationsProvider.class);
        Cache<List<Station>> cache = Mockito.mock(Cache.class);

        Location loc = new Location(38.88, -77.09, "Arlington, VA");
        when(resolver.resolve("Arlington, VA")).thenReturn(loc);
        when(cache.get(any())).thenReturn(Optional.empty());
        when(provider.getStations(any(), any(Integer.class), any(Integer.class)))
                .thenReturn(List.of(new Station("KDCA", "National", 38.85, -77.04)));

        GetStationsUseCase uc = new GetStationsUseCase(resolver, provider, cache);
        List<Station> stations = uc.execute("Arlington, VA");

        assertThat(stations).hasSize(1);
        assertThat(stations.get(0).stationId()).isEqualTo("KDCA");
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `mvn -pl backend/weather-application test -Dtest=GetStationsUseCaseTest`
Expected: FAIL

- [ ] **Step 3: Implement use cases**

Create `GetStationsUseCase.java` and `GetStationObservationsUseCase.java`.

- [ ] **Step 4: Run test to verify pass**

Run: `mvn -pl backend/weather-application test -Dtest=GetStationsUseCaseTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/weather-application
git commit -m "feat(application): add GetStationsUseCase and GetStationObservationsUseCase with cache-aside"
```

---

### Task 4: API Controllers & Wiring

**Files:**
- Create: `backend/weather-api/src/main/java/io/ythalorossy/weatherapi/api/controller/StationController.java`
- Modify: `backend/weather-api/src/main/java/io/ythalorossy/weatherapi/api/config/UseCaseConfig.java`
- Test: `backend/weather-api/src/test/java/io/ythalorossy/weatherapi/api/controller/StationControllerTest.java`

**Interfaces:**
- Consumes: `GetStationsUseCase`, `GetStationObservationsUseCase`.
- Produces: `GET /api/v1/weather/stations`, `GET /api/v1/weather/stations/{stationId}/observations`.

- [ ] **Step 1: Write test for StationController**

```java
package io.ythalorossy.weatherapi.api.controller;

import io.ythalorossy.weatherapi.application.usecase.GetStationObservationsUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetStationsUseCase;
import io.ythalorossy.weatherapi.domain.model.Station;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StationController.class)
class StationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    GetStationsUseCase getStationsUseCase;

    @MockitoBean
    GetStationObservationsUseCase getStationObservationsUseCase;

    @Test
    void getStationsEndpointReturnsList() throws Exception {
        when(getStationsUseCase.execute("Arlington, VA"))
                .thenReturn(List.of(new Station("KDCA", "National", 38.85, -77.04)));

        mvc.perform(get("/api/v1/weather/stations").param("city", "Arlington, VA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stationId").value("KDCA"));
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `mvn -pl backend/weather-api test -Dtest=StationControllerTest`
Expected: FAIL

- [ ] **Step 3: Implement StationController and wire in UseCaseConfig**

Create `StationController.java` and update `UseCaseConfig.java`.

- [ ] **Step 4: Run test to verify pass**

Run: `mvn -pl backend/weather-api test -Dtest=StationControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/weather-api
git commit -m "feat(api): add StationController and wire use cases"
```

---

### Task 5: Frontend API, Hooks & UI Components

**Files:**
- Modify: `web/src/api/weather.ts`
- Create: `web/src/hooks/useStationsQuery.ts`
- Create: `web/src/components/StationsTab.tsx`
- Modify: `web/src/components/ForecastTabs.tsx`
- Modify: `web/src/App.tsx` (to wire the tab)

**Interfaces:**
- Produces: `fetchStations`, `fetchStationObservations`, `useStationsQuery`, `StationsTab`.

- [ ] **Step 1: Update API client in `web/src/api/weather.ts`**

Add `fetchStations(city: string)` and `fetchStationObservations(stationId: string)`.

- [ ] **Step 2: Create `useStationsQuery.ts` and `StationsTab.tsx`**

Implement hooks and UI components for displaying station lists, selected station METAR data, and observation trends.

- [ ] **Step 3: Run frontend typecheck and tests**

Run: `cd web && npm run typecheck && npm test`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add web
git commit -m "feat(web): add stations API, hooks, and StationsTab component"
```
