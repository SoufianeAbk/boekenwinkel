package be.ehb.catalogusservice.web;

import be.ehb.catalogusservice.dto.BoekDto;
import be.ehb.catalogusservice.dto.ReviewDto;
import be.ehb.catalogusservice.dto.ReviewRequest;
import be.ehb.catalogusservice.dto.StockVerminderingRequest;
import be.ehb.catalogusservice.service.BoekService;
import be.ehb.catalogusservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints die enkel door andere services (de gebruikers-service) aangeroepen worden.
 * Ze zijn afgeschermd met een API-sleutel (zie ApiKeyFilter).
 */
@RestController
@RequestMapping("/api/intern")
@RequiredArgsConstructor
public class InternController {

    private final BoekService boekService;
    private final ReviewService reviewService;

    @PostMapping("/stock/verminderen")
    public List<BoekDto> verminderStock(@Valid @RequestBody StockVerminderingRequest request) {
        return boekService.verminderStock(request.lijnen());
    }

    @PostMapping("/boeken/{isbn}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto plaatsReview(@PathVariable String isbn, @Valid @RequestBody ReviewRequest request) {
        return reviewService.plaatsReview(isbn, request);
    }
}
