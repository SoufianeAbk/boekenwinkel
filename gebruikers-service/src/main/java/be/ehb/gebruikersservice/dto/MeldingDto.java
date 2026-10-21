package be.ehb.gebruikersservice.dto;

import be.ehb.gebruikersservice.model.Melding;

import java.time.LocalDateTime;

public record MeldingDto(Long id, String tekst, LocalDateTime aangemaaktOp, boolean gelezen, Long reservatieId) {

    public static MeldingDto van(Melding melding) {
        return new MeldingDto(
                melding.getId(),
                melding.getTekst(),
                melding.getAangemaaktOp(),
                melding.isGelezen(),
                melding.getReservatieId()
        );
    }
}
