package io.ythalorossy.weatherapi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.ythalorossy.weatherapi.domain.model.AfdProduct;

import java.time.Instant;

/**
 * REST response for {@code GET /api/v1/weather/forecast/discussion?city=…}.
 *
 * <p>Carries the latest Area Forecast Discussion issued by the WFO
 * responsible for the resolved city. The {@code body} field is the raw
 * NWS {@code productText}, paragraph-preserved.
 */
@Schema(description = "Latest Area Forecast Discussion for a city: the issuing WFO id, when it was issued, and the raw discussion body.")
public record DiscussionResponse(

        @Schema(description = "Three-letter WFO identifier that issued this AFD.",
                example = "LWX")
        String officeId,

        @Schema(description = "When the AFD was issued (UTC).",
                example = "2026-08-10T14:35:00Z")
        Instant issuanceTime,

        @Schema(description = "Raw NWS productText. Paragraph breaks are preserved as \\n\\n.",
                example = "KLWX AFD\\n\\n.SHORT TERM...")
        String body
) {

    public static DiscussionResponse from(AfdProduct product) {
        return new DiscussionResponse(
                product.officeId(),
                product.issuanceTime(),
                product.body());
    }
}
