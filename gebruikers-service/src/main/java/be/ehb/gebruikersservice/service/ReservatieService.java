package be.ehb.gebruikersservice.service;

import be.ehb.gebruikersservice.client.BoekInfo;
import be.ehb.gebruikersservice.client.CatalogusClient;
import be.ehb.gebruikersservice.client.StockLijn;
import be.ehb.gebruikersservice.dto.ReservatieDto;
import be.ehb.gebruikersservice.dto.ReservatieRequest;
import be.ehb.gebruikersservice.exception.NietGevondenException;
import be.ehb.gebruikersservice.exception.OnvoldoendeStockException;
import be.ehb.gebruikersservice.model.Gebruiker;
import be.ehb.gebruikersservice.model.Melding;
import be.ehb.gebruikersservice.model.Reservatie;
import be.ehb.gebruikersservice.model.ReservatieStatus;
import be.ehb.gebruikersservice.repository.GebruikerRepository;
import be.ehb.gebruikersservice.repository.MeldingRepository;
import be.ehb.gebruikersservice.repository.ReservatieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservatieService {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale BELGIE = Locale.of("nl", "BE");

    private final ReservatieRepository reservatieRepository;
    private final GebruikerRepository gebruikerRepository;
    private final MeldingRepository meldingRepository;
    private final CatalogusClient catalogusClient;
    private final Clock clock;

    /**
     * Een klant reserveert boeken. Titel, prijs en beschikbaarheid komen uit de catalogus-service.
     * De stock wordt pas verminderd wanneer de winkel de reservatie bevestigt.
     */
    @Transactional
    public ReservatieDto maakReservatie(Long gebruikerId, ReservatieRequest request) {
        Gebruiker gebruiker = vindGebruiker(gebruikerId);
        Map<String, Integer> aantalPerIsbn = request.lijnen().stream()
                .collect(Collectors.groupingBy(lijn -> lijn.isbn().trim(), LinkedHashMap::new,
                        Collectors.summingInt(ReservatieRequest.Lijn::aantal)));

        Reservatie reservatie = new Reservatie(gebruiker, LocalDateTime.now(clock));
        aantalPerIsbn.forEach((isbn, aantal) -> {
            BoekInfo boek = catalogusClient.haalBoek(isbn);
            if (aantal > boek.stock()) {
                throw new OnvoldoendeStockException(boek.stock() == 0
                        ? "'%s' is momenteel niet op voorraad".formatted(boek.titel())
                        : "Er zijn nog maar %d exemplaren van '%s' beschikbaar".formatted(boek.stock(), boek.titel()));
            }
            reservatie.voegLijnToe(boek.isbn(), boek.titel(), boek.prijs(), aantal);
        });

        return ReservatieDto.van(reservatieRepository.save(reservatie));
    }

    @Transactional(readOnly = true)
    public List<ReservatieDto> reservatiesVan(Long gebruikerId) {
        return reservatieRepository.findByGebruikerIdOrderByAangemaaktOpDesc(gebruikerId).stream()
                .map(ReservatieDto::van)
                .toList();
    }

    @Transactional
    public ReservatieDto annuleer(Long gebruikerId, Long reservatieId) {
        Reservatie reservatie = reservatieRepository.findByIdAndGebruikerId(reservatieId, gebruikerId)
                .orElseThrow(() -> new NietGevondenException("Reservatie #" + reservatieId + " niet gevonden"));
        reservatie.annuleer();
        return ReservatieDto.van(reservatie);
    }

    @Transactional(readOnly = true)
    public List<ReservatieDto> alleReservaties(ReservatieStatus status) {
        List<Reservatie> reservaties = status == null
                ? reservatieRepository.findAllByOrderByAangemaaktOpDesc()
                : reservatieRepository.findByStatusOrderByAangemaaktOpAsc(status);
        return reservaties.stream().map(ReservatieDto::van).toList();
    }

    /**
     * De winkel bevestigt een reservatie:
     * <ol>
     *     <li>de stock in de catalogus wordt verminderd;</li>
     *     <li>de reservatie krijgt een afhaaldatum;</li>
     *     <li>de boeken komen in de lijst gekochte boeken van de klant
     *     (we gaan ervan uit dat alles wat gereserveerd is ook afgehaald wordt);</li>
     *     <li>de klant krijgt een melding wanneer hij de boeken mag afhalen en betalen.</li>
     * </ol>
     */
    @Transactional
    public ReservatieDto bevestig(Long reservatieId, LocalDate gevraagdeAfhaalDatum) {
        Reservatie reservatie = reservatieRepository.findById(reservatieId)
                .orElseThrow(() -> new NietGevondenException("Reservatie #" + reservatieId + " niet gevonden"));
        reservatie.controleerBevestigbaar();

        LocalDate vandaag = LocalDate.now(clock);
        LocalDate afhaalDatum = gevraagdeAfhaalDatum != null ? gevraagdeAfhaalDatum : vandaag.plusDays(1);
        if (afhaalDatum.isBefore(vandaag)) {
            throw new IllegalArgumentException("De afhaaldatum kan niet in het verleden liggen");
        }

        // Eerst de stock in de catalogus verminderen. Faalt dat (bv. onvoldoende stock of de catalogus
        // is onbereikbaar), dan stopt de bevestiging hier en blijft de reservatie ongewijzigd openstaan.
        catalogusClient.verminderStock(reservatie.getLijnen().stream()
                .map(lijn -> new StockLijn(lijn.getIsbn(), lijn.getAantal()))
                .toList());

        LocalDateTime nu = LocalDateTime.now(clock);
        reservatie.bevestig(afhaalDatum, nu);

        Gebruiker klant = reservatie.getGebruiker();
        reservatie.getLijnen().forEach(lijn ->
                klant.registreerAankoop(lijn.getIsbn(), lijn.getTitel(), lijn.getAantal(), nu));

        meldingRepository.save(new Melding(klant, bevestigingstekst(reservatie), nu, reservatie.getId()));
        return ReservatieDto.van(reservatie);
    }

    private static String bevestigingstekst(Reservatie reservatie) {
        String totaal = NumberFormat.getCurrencyInstance(BELGIE).format(reservatie.totaal());
        return "Je reservatie #%d is bevestigd! Je boeken liggen vanaf %s voor je klaar in de winkel. Je kan ze dan afhalen en betalen (totaal %s)."
                .formatted(reservatie.getId(), reservatie.getAfhaalDatum().format(DATUM), totaal);
    }

    private Gebruiker vindGebruiker(Long gebruikerId) {
        return gebruikerRepository.findById(gebruikerId)
                .orElseThrow(() -> new NietGevondenException("Gebruiker niet gevonden"));
    }
}
