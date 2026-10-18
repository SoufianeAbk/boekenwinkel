package be.ehb.catalogusservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Gegevens om een bestaand boek te wijzigen. De stock wijzigt enkel via leveringen en bevestigde reservaties. */
public record BoekWijzigRequest(
        @NotBlank(message = "Titel is verplicht")
        @Size(max = 255)
        String titel,

        @NotEmpty(message = "Minstens één auteur is verplicht")
        List<@NotBlank(message = "Auteur mag niet leeg zijn") String> auteurs,

        @Size(max = 2000, message = "Omschrijving mag maximaal 2000 tekens bevatten")
        String omschrijving,

        @NotNull(message = "Prijs is verplicht")
        @DecimalMin(value = "0.00", message = "Prijs mag niet negatief zijn")
        @Digits(integer = 8, fraction = 2)
        BigDecimal prijs
) {
}
