package be.ehb.catalogusservice.dto;

import be.ehb.catalogusservice.model.Boek;

import java.math.BigDecimal;
import java.util.List;

public record BoekDetailDto(
        String isbn,
        String titel,
        List<String> auteurs,
        String omschrijving,
        BigDecimal prijs,
        int stock,
        Double gemiddeldeScore,
        long aantalReviews,
        List<ReviewDto> reviews
) {

    public static BoekDetailDto van(Boek boek, List<ReviewDto> reviews) {
        ReviewStatistiek statistiek = ReviewStatistiek.van(boek.getIsbn(), reviews.stream().map(ReviewDto::score).toList());
        return new BoekDetailDto(
                boek.getIsbn(),
                boek.getTitel(),
                List.copyOf(boek.getAuteurs()),
                boek.getOmschrijving(),
                boek.getPrijs(),
                boek.getStock(),
                statistiek.afgerondGemiddelde(),
                statistiek.aantal(),
                reviews
        );
    }
}
