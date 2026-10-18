package be.ehb.catalogusservice.dto;

import be.ehb.catalogusservice.model.Review;

import java.time.LocalDateTime;

public record ReviewDto(Long id, String gebruikersnaam, int score, String tekst, LocalDateTime aangemaaktOp) {

    public static ReviewDto van(Review review) {
        return new ReviewDto(
                review.getId(),
                review.getGebruikersnaam(),
                review.getScore(),
                review.getTekst(),
                review.getAangemaaktOp()
        );
    }
}
