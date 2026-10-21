package be.ehb.gebruikersservice.dto;

import java.time.LocalDate;

/** Optionele afhaaldatum bij het bevestigen van een reservatie. Zonder datum wordt het morgen. */
public record BevestigRequest(LocalDate afhaalDatum) {
}
