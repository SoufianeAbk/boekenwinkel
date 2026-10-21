package be.ehb.gebruikersservice.dto;

import be.ehb.gebruikersservice.model.GekochtBoek;

import java.time.LocalDateTime;

public record GekochtBoekDto(
        String isbn,
        String titel,
        int aantal,
        LocalDateTime laatstGekochtOp,
        Integer reviewScore,
        String reviewTekst
) {

    public static GekochtBoekDto van(GekochtBoek boek) {
        return new GekochtBoekDto(
                boek.getIsbn(),
                boek.getTitel(),
                boek.getAantal(),
                boek.getLaatstGekochtOp(),
                boek.getReviewScore(),
                boek.getReviewTekst()
        );
    }
}
