package be.ehb.gebruikersservice.web;

import be.ehb.gebruikersservice.dto.GekochtBoekDto;
import be.ehb.gebruikersservice.dto.MeldingDto;
import be.ehb.gebruikersservice.dto.ProfielDto;
import be.ehb.gebruikersservice.dto.ProfielWijzigRequest;
import be.ehb.gebruikersservice.dto.ReviewRequest;
import be.ehb.gebruikersservice.service.MeldingService;
import be.ehb.gebruikersservice.service.ProfielService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Het profiel van de ingelogde gebruiker. */
@RestController
@RequestMapping("/api/profiel")
@RequiredArgsConstructor
public class ProfielController {

    private final ProfielService profielService;
    private final MeldingService meldingService;

    @GetMapping
    public ProfielDto haalProfiel(@AuthenticationPrincipal Jwt jwt) {
        return profielService.haalProfiel(IngelogdeGebruiker.id(jwt));
    }

    @PutMapping
    public ProfielDto wijzigProfiel(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfielWijzigRequest request) {
        return profielService.wijzigProfiel(IngelogdeGebruiker.id(jwt), request);
    }

    @PostMapping("/gekochte-boeken/{isbn}/review")
    public GekochtBoekDto schrijfReview(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable String isbn,
                                        @Valid @RequestBody ReviewRequest request) {
        return profielService.schrijfReview(IngelogdeGebruiker.id(jwt), isbn, request);
    }

    @GetMapping("/meldingen")
    public List<MeldingDto> meldingen(@AuthenticationPrincipal Jwt jwt) {
        return meldingService.meldingenVan(IngelogdeGebruiker.id(jwt));
    }

    @PostMapping("/meldingen/gelezen")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markeerMeldingenGelezen(@AuthenticationPrincipal Jwt jwt) {
        meldingService.markeerAllesGelezen(IngelogdeGebruiker.id(jwt));
    }
}
