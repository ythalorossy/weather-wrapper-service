package io.ythalorossy.weatherapi.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Latest Area Forecast Discussion (AFD) issued by a Weather Forecast Office.
 *
 * <p>The {@code body} is the raw {@code productText} returned by NWS,
 * paragraph-preserved ({@code \n\n} separators). Rendering decisions
 * (markdown vs plain, line wrap) live in the API/UI layers.
 */
public record AfdProduct(
        String officeId,
        Instant issuanceTime,
        String body) {

    public AfdProduct {
        Objects.requireNonNull(officeId, "officeId");
        Objects.requireNonNull(issuanceTime, "issuanceTime");
        Objects.requireNonNull(body, "body");
        if (officeId.isBlank()) {
            throw new IllegalArgumentException("officeId must not be blank");
        }
    }
}
