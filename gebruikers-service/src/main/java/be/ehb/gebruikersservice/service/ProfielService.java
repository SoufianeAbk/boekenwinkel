package be.ehb.gebruikersservice.service;

import be.ehb.gebruikersservice.client.CatalogusClient;
import be.ehb.gebruikersservice.client.CatalogusReview;
import be.ehb.gebruikersservice.dto.GekochtBoekDto;
import be.ehb.gebruikersservice.dto.ProfielDto;
import be.ehb.gebruikersservice.dto.ProfielWijzigRequest;
import be.ehb.gebruikersservice.dto.ReviewRequest;
import be.ehb.gebruikersservice.exception.BoekNietGekochtException;
import be.ehb.gebruikersservice.exception.GebruikerBestaatAlException;
import be.ehb.gebruikersservice.exception.NietGevondenException;
import be.ehb.gebruikersservice.model.Gebruiker;
import be.ehb.gebruikersservice.model.GekochtBoek;
import be.ehb.gebruikersservice.repository.GebruikerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfielService {

    private final GebruikerRepository gebruikerRepository;
    private final CatalogusClient catalogusClient;

    @Transactional(readOnly = true)
    public ProfielDto haalProfiel(Long gebruikerId) {
        return ProfielDto.van(vindGebruiker(gebruikerId));
    }

    @Transactional
    public ProfielDto wijzigProfiel(Long gebruikerId, ProfielWijzigRequest request) {
        Gebruiker gebruiker = vindGebruiker(gebruikerId);
        String email = request.email().trim().toLowerCase();
        if (gebruikerRepository.existsByEmailIgnoreCaseAndIdNot(email, gebruikerId)) {
            throw new GebruikerBestaatAlException("Er bestaat al een ander account met dit e-mailadres");
        }
        gebruiker.setVoornaam(request.voornaam().trim());
        gebruiker.setAchternaam(request.achternaam().trim());
        gebruiker.setEmail(email);
        return ProfielDto.van(gebruiker);
    }

    /**
     * Een gebruiker schrijft een review voor een boek uit zijn lijst gekochte boeken.
     * De review wordt doorgestuurd naar de catalogus, waar ze op de detailpagina van het boek verschijnt.
     */
    @Transactional
    public GekochtBoekDto schrijfReview(Long gebruikerId, String isbn, ReviewRequest request) {
        Gebruiker gebruiker = vindGebruiker(gebruikerId);
        GekochtBoek boek = gebruiker.gekochtBoek(isbn).orElseThrow(() -> new BoekNietGekochtException(isbn));
        String tekst = request.tekst() == null || request.tekst().isBlank() ? null : request.tekst().trim();

        catalogusClient.plaatsReview(isbn,
                new CatalogusReview(gebruiker.getId(), gebruiker.getGebruikersnaam(), request.score(), tekst));

        boek.setReviewScore(request.score());
        boek.setReviewTekst(tekst);
        return GekochtBoekDto.van(boek);
    }

    private Gebruiker vindGebruiker(Long gebruikerId) {
        return gebruikerRepository.findById(gebruikerId)
                .orElseThrow(() -> new NietGevondenException("Gebruiker niet gevonden"));
    }
}
