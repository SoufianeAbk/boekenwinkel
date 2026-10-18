package be.ehb.catalogusservice.service;

import be.ehb.catalogusservice.dto.ReviewDto;
import be.ehb.catalogusservice.dto.ReviewRequest;
import be.ehb.catalogusservice.model.Boek;
import be.ehb.catalogusservice.model.Review;
import be.ehb.catalogusservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final BoekService boekService;
    private final ReviewRepository reviewRepository;
    private final Clock clock;

    /** Plaatst een review, of overschrijft de vorige review van dezelfde gebruiker voor dit boek. */
    @Transactional
    public ReviewDto plaatsReview(String isbn, ReviewRequest request) {
        Boek boek = boekService.vindBoek(isbn);
        Review review = reviewRepository.findByBoekIsbnAndGebruikerId(boek.getIsbn(), request.gebruikerId())
                .orElseGet(() -> new Review(boek, request.gebruikerId()));

        review.setGebruikersnaam(request.gebruikersnaam());
        review.setScore(request.score());
        review.setTekst(request.tekst() == null ? null : request.tekst().trim());
        review.setAangemaaktOp(LocalDateTime.now(clock));

        return ReviewDto.van(reviewRepository.save(review));
    }
}
