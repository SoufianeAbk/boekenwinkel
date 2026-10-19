package be.ehb.catalogusservice.service;

import be.ehb.catalogusservice.dto.BoekDto;
import be.ehb.catalogusservice.dto.BoekRequest;
import be.ehb.catalogusservice.dto.StockVerminderingRequest.Lijn;
import be.ehb.catalogusservice.exception.BoekBestaatAlException;
import be.ehb.catalogusservice.exception.BoekNietGevondenException;
import be.ehb.catalogusservice.exception.OnvoldoendeStockException;
import be.ehb.catalogusservice.model.Boek;
import be.ehb.catalogusservice.repository.BoekRepository;
import be.ehb.catalogusservice.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unittesten voor het aanpassen van de stock in de catalogus. */
@ExtendWith(MockitoExtension.class)
class BoekServiceTest {

    private static final String ISBN_AVONDEN = "9789023417354";
    private static final String ISBN_DINER = "9789026473807";

    @Mock
    private BoekRepository boekRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private BoekService boekService;

    private Boek avonden;
    private Boek diner;

    @BeforeEach
    void setUp() {
        avonden = new Boek(ISBN_AVONDEN, "De avonden", List.of("Gerard Reve"), "Roman", new BigDecimal("19.99"), 5);
        diner = new Boek(ISBN_DINER, "Het diner", List.of("Herman Koch"), "Roman", new BigDecimal("12.99"), 2);
    }

    @Test
    void verminderStockVerlaagtDeStockVanElkBoekInDeReservatie() {
        when(boekRepository.findById(ISBN_AVONDEN)).thenReturn(Optional.of(avonden));
        when(boekRepository.findById(ISBN_DINER)).thenReturn(Optional.of(diner));

        List<BoekDto> resultaat = boekService.verminderStock(List.of(new Lijn(ISBN_AVONDEN, 3), new Lijn(ISBN_DINER, 2)));

        assertThat(avonden.getStock()).isEqualTo(2);
        assertThat(diner.getStock()).isZero();
        assertThat(resultaat).extracting(BoekDto::stock).containsExactly(2, 0);
    }

    @Test
    void verminderStockTeltLijnenVoorHetzelfdeBoekSamen() {
        when(boekRepository.findById(ISBN_AVONDEN)).thenReturn(Optional.of(avonden));

        boekService.verminderStock(List.of(new Lijn(ISBN_AVONDEN, 2), new Lijn("978-90-234-1735-4", 2)));

        assertThat(avonden.getStock()).isEqualTo(1);
    }

    @Test
    void verminderStockBijOnvoldoendeStockPastGeenEnkelBoekAan() {
        when(boekRepository.findById(ISBN_AVONDEN)).thenReturn(Optional.of(avonden));
        when(boekRepository.findById(ISBN_DINER)).thenReturn(Optional.of(diner));

        assertThatThrownBy(() -> boekService.verminderStock(List.of(new Lijn(ISBN_AVONDEN, 1), new Lijn(ISBN_DINER, 3))))
                .isInstanceOf(OnvoldoendeStockException.class)
                .hasMessageContaining("Het diner")
                .hasMessageContaining("nog 2 beschikbaar");

        // alles of niets: ook het boek met voldoende stock blijft ongewijzigd
        assertThat(avonden.getStock()).isEqualTo(5);
        assertThat(diner.getStock()).isEqualTo(2);
    }

    @Test
    void verminderStockVanOnbekendBoekGeeftBoekNietGevonden() {
        when(boekRepository.findById(ISBN_DINER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boekService.verminderStock(List.of(new Lijn(ISBN_DINER, 1))))
                .isInstanceOf(BoekNietGevondenException.class);
    }

    @Test
    void leveringVerhoogtDeStock() {
        when(boekRepository.findById(ISBN_DINER)).thenReturn(Optional.of(diner));
        when(reviewRepository.findByBoekIsbnOrderByAangemaaktOpDesc(ISBN_DINER)).thenReturn(List.of());

        BoekDto resultaat = boekService.registreerLevering(ISBN_DINER, 10);

        assertThat(diner.getStock()).isEqualTo(12);
        assertThat(resultaat.stock()).isEqualTo(12);
    }

    @Test
    void leveringMetOngeldigAantalWordtGeweigerd() {
        when(boekRepository.findById(ISBN_DINER)).thenReturn(Optional.of(diner));

        assertThatThrownBy(() -> boekService.registreerLevering(ISBN_DINER, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(diner.getStock()).isEqualTo(2);
    }

    @Test
    void nieuwBoekKrijgtGenormaliseerdIsbn() {
        when(boekRepository.existsById(ISBN_DINER)).thenReturn(false);
        when(boekRepository.save(any(Boek.class))).thenAnswer(aanroep -> aanroep.getArgument(0));

        BoekDto resultaat = boekService.voegBoekToe(new BoekRequest("978-90-264-7380-7", " Het diner ",
                List.of("Herman Koch"), "Roman", new BigDecimal("12.99"), 4));

        assertThat(resultaat.isbn()).isEqualTo(ISBN_DINER);
        assertThat(resultaat.titel()).isEqualTo("Het diner");
        assertThat(resultaat.stock()).isEqualTo(4);
    }

    @Test
    void boekMetBestaandIsbnWordtGeweigerd() {
        when(boekRepository.existsById(ISBN_DINER)).thenReturn(true);

        assertThatThrownBy(() -> boekService.voegBoekToe(new BoekRequest(ISBN_DINER, "Het diner",
                List.of("Herman Koch"), null, new BigDecimal("12.99"), 1)))
                .isInstanceOf(BoekBestaatAlException.class);
        verify(boekRepository, never()).save(any());
    }
}
