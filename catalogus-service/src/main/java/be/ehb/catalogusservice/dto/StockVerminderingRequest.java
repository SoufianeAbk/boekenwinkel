package be.ehb.catalogusservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Verzoek van de gebruikers-service om de stock te verminderen wanneer een reservatie bevestigd wordt.
 * Alle lijnen worden samen verwerkt: lukt één lijn niet, dan wordt er niets aangepast.
 */
public record StockVerminderingRequest(
        @NotEmpty(message = "Minstens één lijn is verplicht")
        List<@Valid Lijn> lijnen
) {

    public record Lijn(
            @NotBlank(message = "ISBN is verplicht")
            String isbn,

            @Min(value = 1, message = "Aantal moet minstens 1 zijn")
            int aantal
    ) {
    }
}
