package be.ehb.catalogusservice.dto;

import be.ehb.catalogusservice.model.Boek;

import java.math.BigDecimal;
import java.util.List;

public record BoekDto(
        String isbn,
        String titel,
        List<String> auteurs,
        String omschrijving,
        BigDecimal prijs,
        int stock,
        Double gemiddeldeScore,
        long aantalReviews
) {

    public static BoekDto van(Boek boek, ReviewStatistiek statistiek) {
        ReviewStatistiek s = statistiek == null ? ReviewStatistiek.GEEN : statistiek;
        return new BoekDto(
                boek.getIsbn(),
                boek.getTitel(),
                List.copyOf(boek.getAuteurs()),
                boek.getOmschrijving(),
                boek.getPrijs(),
                boek.getStock(),
                s.afgerondGemiddelde(),
                s.aantal()
        );
    }
}
