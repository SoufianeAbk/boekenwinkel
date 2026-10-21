package be.ehb.gebruikersservice.web;

import be.ehb.gebruikersservice.dto.ReservatieDto;
import be.ehb.gebruikersservice.dto.ReservatieRequest;
import be.ehb.gebruikersservice.service.ReservatieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Reservaties van de ingelogde klant. */
@RestController
@RequestMapping("/api/reservaties")
@RequiredArgsConstructor
public class ReservatieController {

    private final ReservatieService reservatieService;

    @GetMapping
    public List<ReservatieDto> mijnReservaties(@AuthenticationPrincipal Jwt jwt) {
        return reservatieService.reservatiesVan(IngelogdeGebruiker.id(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservatieDto reserveer(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReservatieRequest request) {
        return reservatieService.maakReservatie(IngelogdeGebruiker.id(jwt), request);
    }

    @PostMapping("/{id}/annuleren")
    public ReservatieDto annuleer(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return reservatieService.annuleer(IngelogdeGebruiker.id(jwt), id);
    }
}
