package be.ehb.catalogusservice.service;

import be.ehb.catalogusservice.dto.BoekDetailDto;
import be.ehb.catalogusservice.dto.BoekDto;
import be.ehb.catalogusservice.dto.BoekRequest;
import be.ehb.catalogusservice.dto.BoekWijzigRequest;
import be.ehb.catalogusservice.dto.ReviewDto;
import be.ehb.catalogusservice.dto.ReviewStatistiek;
import be.ehb.catalogusservice.dto.StockVerminderingRequest;
import be.ehb.catalogusservice.exception.BoekBestaatAlException;
import be.ehb.catalogusservice.exception.BoekNietGevondenException;
import be.ehb.catalogusservice.exception.OnvoldoendeStockException;
import be.ehb.catalogusservice.model.Boek;
import be.ehb.catalogusservice.model.Isbn;
import be.ehb.catalogusservice.model.Review;
import be.ehb.catalogusservice.repository.BoekRepository;
import be.ehb.catalogusservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoekService {

    private final BoekRepository boekRepository;
    private final ReviewRepository reviewRepository;

    public List<BoekDto> zoekBoeken(String zoekterm) {
        List<Boek> boeken = zoekterm == null || zoekterm.isBlank()
                ? boekRepository.findAllByOrderByTitelAsc()
                : boekRepository.zoek(zoekterm.trim());

        Map<String, ReviewStatistiek> statistieken = reviewRepository.statistiekenPerBoek().stream()
                .collect(Collectors.toMap(ReviewStatistiek::isbn, Function.identity()));

        return boeken.stream()
                .map(boek -> BoekDto.van(boek, statistieken.get(boek.getIsbn())))
                .toList();
    }

    public BoekDetailDto haalBoek(String isbn) {
        Boek boek = vindBoek(isbn);
        List<ReviewDto> reviews = reviewRepository.findByBoekIsbnOrderByAangemaaktOpDesc(boek.getIsbn()).stream()
                .map(ReviewDto::van)
                .toList();
        return BoekDetailDto.van(boek, reviews);
    }

    @Transactional
    public BoekDto voegBoekToe(BoekRequest request) {
        String isbn = Isbn.normaliseer(request.isbn());
        if (boekRepository.existsById(isbn)) {
            throw new BoekBestaatAlException(isbn);
        }
        Boek boek = new Boek(isbn, request.titel().trim(), trim(request.auteurs()), request.omschrijving(),
                request.prijs(), request.stock());
        return BoekDto.van(boekRepository.save(boek), null);
    }

    @Transactional
    public BoekDto wijzigBoek(String isbn, BoekWijzigRequest request) {
        Boek boek = vindBoek(isbn);
        boek.setTitel(request.titel().trim());
        boek.setAuteurs(trim(request.auteurs()));
        boek.setOmschrijving(request.omschrijving());
        boek.setPrijs(request.prijs());
        return metStatistiek(boek);
    }

    /** Een levering van de uitgever komt binnen: de stock in de catalogus stijgt meteen mee. */
    @Transactional
    public BoekDto registreerLevering(String isbn, int aantal) {
        Boek boek = vindBoek(isbn);
        boek.verhoogStock(aantal);
        return metStatistiek(boek);
    }

    /**
     * Vermindert de stock voor alle lijnen van een bevestigde reservatie.
     * Eerst wordt alles gecontroleerd en pas daarna aangepast, zodat er nooit maar een deel
     * van de reservatie afgeboekt wordt. Dubbele ISBN's worden samengeteld.
     */
    @Transactional
    public List<BoekDto> verminderStock(List<StockVerminderingRequest.Lijn> lijnen) {
        Map<String, Integer> aantalPerIsbn = lijnen.stream()
                .collect(Collectors.groupingBy(lijn -> Isbn.normaliseer(lijn.isbn()), LinkedHashMap::new,
                        Collectors.summingInt(StockVerminderingRequest.Lijn::aantal)));

        List<Boek> boeken = aantalPerIsbn.keySet().stream().map(this::vindBoek).toList();

        for (Boek boek : boeken) {
            int gevraagd = aantalPerIsbn.get(boek.getIsbn());
            if (gevraagd > boek.getStock()) {
                throw new OnvoldoendeStockException(boek.getIsbn(), boek.getTitel(), boek.getStock(), gevraagd);
            }
        }
        boeken.forEach(boek -> boek.verminderStock(aantalPerIsbn.get(boek.getIsbn())));

        return boeken.stream().map(boek -> BoekDto.van(boek, null)).toList();
    }

    Boek vindBoek(String isbn) {
        String genormaliseerd = Isbn.normaliseer(isbn);
        return boekRepository.findById(genormaliseerd)
                .orElseThrow(() -> new BoekNietGevondenException(genormaliseerd));
    }

    private BoekDto metStatistiek(Boek boek) {
        List<Integer> scores = reviewRepository.findByBoekIsbnOrderByAangemaaktOpDesc(boek.getIsbn()).stream()
                .map(Review::getScore)
                .toList();
        return BoekDto.van(boek, ReviewStatistiek.van(boek.getIsbn(), scores));
    }

    private static List<String> trim(List<String> waarden) {
        return waarden.stream().map(String::trim).toList();
    }
}
