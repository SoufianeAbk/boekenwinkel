package be.ehb.gebruikersservice.service;

import be.ehb.gebruikersservice.client.BoekInfo;
import be.ehb.gebruikersservice.client.CatalogusClient;
import be.ehb.gebruikersservice.client.StockLijn;
import be.ehb.gebruikersservice.dto.ReservatieDto;
import be.ehb.gebruikersservice.dto.ReservatieRequest;
import be.ehb.gebruikersservice.exception.OnvoldoendeStockException;
import be.ehb.gebruikersservice.model.Gebruiker;
import be.ehb.gebruikersservice.model.GekochtBoek;
import be.ehb.gebruikersservice.model.Melding;
import be.ehb.gebruikersservice.model.Reservatie;
import be.ehb.gebruikersservice.model.ReservatieStatus;
import be.ehb.gebruikersservice.model.Rol;
import be.ehb.gebruikersservice.repository.GebruikerRepository;
import be.ehb.gebruikersservice.repository.MeldingRepository;
import be.ehb.gebruikersservice.repository.ReservatieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unittesten voor het reserveren en bevestigen. De catalogus-service wordt gemockt,
 * zodat we kunnen nagaan welke stockaanpassing er naar de catalogus gestuurd wordt.
 */
@ExtendWith(MockitoExtension.class)
class ReservatieServiceTest {

    private static final Clock KLOK = Clock.fixed(Instant.parse("2026-10-17T10:00:00Z"), ZoneId.of("Europe/Brussels"));
    private static final String ISBN_DINER = "9789026473807";
    private static final String ISBN_1984 = "9780141036144";

    @Mock
    private ReservatieRepository reservatieRepository;

    @Mock
    private GebruikerRepository gebruikerRepository;

    @Mock
    private MeldingRepository meldingRepository;

    @Mock
    private CatalogusClient catalogusClient;

    private ReservatieService reservatieService;
    private Gebruiker klant;
    private Reservatie reservatie;

    @BeforeEach
    void setUp() {
        reservatieService = new ReservatieService(reservatieRepository, gebruikerRepository, meldingRepository,
                catalogusClient, KLOK);

        klant = new Gebruiker("jana", "jana@test.be", "Jana", "Peeters", "hash", Rol.KLANT);
        klant.setId(1L);

        reservatie = new Reservatie(klant, LocalDateTime.now(KLOK));
        reservatie.setId(10L);
        reservatie.voegLijnToe(ISBN_DINER, "Het diner", new BigDecimal("12.99"), 2);
        reservatie.voegLijnToe(ISBN_1984, "Nineteen Eighty-Four", new BigDecimal("11.50"), 1);
    }

    @Test
    void bevestigenVermindertDeStockInDeCatalogus() {
        when(reservatieRepository.findById(10L)).thenReturn(Optional.of(reservatie));

        reservatieService.bevestig(10L, LocalDate.of(2026, 10, 20));

        verify(catalogusClient).verminderStock(List.of(new StockLijn(ISBN_DINER, 2), new StockLijn(ISBN_1984, 1)));
    }

    @Test
    void bevestigenZetDeReservatieKlaarVoorAfhaling() {
        when(reservatieRepository.findById(10L)).thenReturn(Optional.of(reservatie));

        ReservatieDto resultaat = reservatieService.bevestig(10L, LocalDate.of(2026, 10, 20));

        assertThat(resultaat.status()).isEqualTo(ReservatieStatus.BEVESTIGD);
        assertThat(resultaat.afhaalDatum()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(resultaat.bevestigdOp()).isEqualTo(LocalDateTime.now(KLOK));
        assertThat(resultaat.totaal()).isEqualByComparingTo("37.48");
    }

    @Test
    void bevestigenVoegtDeBoekenToeAanHetProfielVanDeKlant() {
        when(reservatieRepository.findById(10L)).thenReturn(Optional.of(reservatie));

        reservatieService.bevestig(10L, null);

        assertThat(klant.getGekochteBoeken())
                .extracting(GekochtBoek::getIsbn, GekochtBoek::getTitel, GekochtBoek::getAantal)
                .containsExactlyInAnyOrder(
                        tuple(ISBN_DINER, "Het diner", 2),
                        tuple(ISBN_1984, "Nineteen Eighty-Four", 1));
    }

    @Test
    void eenBoekDatAlGekochtWasKrijgtEenHogerAantal() {
        klant.registreerAankoop(ISBN_DINER, "Het diner", 1, LocalDateTime.now(KLOK).minusDays(30));
        when(reservatieRepository.findById(10L)).thenReturn(Optional.of(reservatie));

        reservatieService.bevestig(10L, null);

        assertThat(klant.getGekochteBoeken()).hasSize(2);
        assertThat(klant.gekochtBoek(ISBN_DINER).orElseThrow().getAantal()).isEqualTo(3);
    }

    @Test
    void bevestigenStuurtDeKlantEenMeldingMetAfhaaldatumEnTotaal() {
        when(reservatieRepository.findById(10L)).thenReturn(Optional.of(reservatie));

        reservatieService.bevestig(10L, LocalDate.of(2026, 10, 20));

        ArgumentCaptor<Melding> melding = ArgumentCaptor.forClass(Melding.class);
        verify(meldingRepository).save(melding.capture());
        assertThat(melding.getValue().getGebruiker()).isSameAs(klant);
        assertThat(melding.getValue().getReservatieId()).isEqualTo(10L);
        assertThat(melding.getValue().isGelezen()).isFalse();
        assertThat(melding.getValue().getTekst())
                .contains("#10")
                .contains("20/10/2026")
                .contains("37,48");
    }

    @Test
    void zonderAfhaaldatumMagDeKlantMorgenAfhalen() {
        when(reservatieRepository.findById(10L)).thenReturn(Optional.of(reservatie));

        ReservatieDto resultaat = reservatieService.bevestig(10L, null);

        assertThat(resultaat.afhaalDatum()).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void bijOnvoldoendeStockInDeCatalogusBlijftDeReservatieOpenstaan() {
        when(reservatieRepository.findById(10L)).thenReturn(Optional.of(reservatie));
        doThrow(new OnvoldoendeStockException("Onvoldoende stock voor 'Het diner'"))
                .when(catalogusClient).verminderStock(anyList());

        assertThatThrownBy(() -> reservatieService.bevestig(10L, null))
                .isInstanceOf(OnvoldoendeStockException.class);

        assertThat(reservatie.getStatus()).isEqualTo(ReservatieStatus.GERESERVEERD);
        assertThat(klant.getGekochteBoeken()).isEmpty();
        verifyNoInteractions(meldingRepository);
    }

    @Test
    void eenBevestigdeReservatieKanNietOpnieuwBevestigdWorden() {
        reservatie.bevestig(LocalDate.of(2026, 10, 18), LocalDateTime.now(KLOK));
        when(reservatieRepository.findById(10L)).thenReturn(Optional.of(reservatie));

        assertThatThrownBy(() -> reservatieService.bevestig(10L, null))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(catalogusClient);
    }

    @Test
    void eenAfhaaldatumInHetVerledenWordtGeweigerd() {
        when(reservatieRepository.findById(10L)).thenReturn(Optional.of(reservatie));

        assertThatThrownBy(() -> reservatieService.bevestig(10L, LocalDate.of(2026, 10, 16)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(catalogusClient);
    }

    @Test
    void reserverenNeemtTitelEnPrijsOverUitDeCatalogusZonderStockTeVerminderen() {
        when(gebruikerRepository.findById(1L)).thenReturn(Optional.of(klant));
        when(catalogusClient.haalBoek(ISBN_DINER)).thenReturn(boekInfo(ISBN_DINER, "Het diner", "12.99", 5));
        when(reservatieRepository.save(any(Reservatie.class))).thenAnswer(aanroep -> aanroep.getArgument(0));

        ReservatieDto resultaat = reservatieService.maakReservatie(1L,
                new ReservatieRequest(List.of(new ReservatieRequest.Lijn(ISBN_DINER, 2))));

        assertThat(resultaat.status()).isEqualTo(ReservatieStatus.GERESERVEERD);
        assertThat(resultaat.lijnen()).singleElement()
                .satisfies(lijn -> {
                    assertThat(lijn.titel()).isEqualTo("Het diner");
                    assertThat(lijn.aantal()).isEqualTo(2);
                });
        assertThat(resultaat.totaal()).isEqualByComparingTo("25.98");
        verify(catalogusClient, never()).verminderStock(anyList());
    }

    @Test
    void reserverenMeerDanDeBeschikbareStockWordtGeweigerd() {
        when(gebruikerRepository.findById(1L)).thenReturn(Optional.of(klant));
        when(catalogusClient.haalBoek(ISBN_DINER)).thenReturn(boekInfo(ISBN_DINER, "Het diner", "12.99", 1));

        assertThatThrownBy(() -> reservatieService.maakReservatie(1L,
                new ReservatieRequest(List.of(new ReservatieRequest.Lijn(ISBN_DINER, 2)))))
                .isInstanceOf(OnvoldoendeStockException.class)
                .hasMessageContaining("nog maar 1");
        verify(reservatieRepository, never()).save(any());
    }

    private static BoekInfo boekInfo(String isbn, String titel, String prijs, int stock) {
        return new BoekInfo(isbn, titel, List.of("Auteur"), new BigDecimal(prijs), stock);
    }
}
