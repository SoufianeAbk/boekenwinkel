package be.ehb.gebruikersservice.web;

import be.ehb.gebruikersservice.client.BoekInfo;
import be.ehb.gebruikersservice.client.CatalogusClient;
import be.ehb.gebruikersservice.client.CatalogusReview;
import be.ehb.gebruikersservice.client.StockLijn;
import be.ehb.gebruikersservice.model.Gebruiker;
import be.ehb.gebruikersservice.model.Rol;
import be.ehb.gebruikersservice.repository.GebruikerRepository;
import be.ehb.gebruikersservice.repository.MeldingRepository;
import be.ehb.gebruikersservice.repository.ReservatieRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functionele testen van de gebruikers-service: registreren, inloggen, profiel aanpassen
 * en de volledige flow reserveren → bevestigen → gekochte boeken → review.
 * Enkel de catalogus-service wordt gemockt; de rest (security, database) is echt.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GebruikersApiTest {

    private static final String ISBN = "9789026473807";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GebruikerRepository gebruikerRepository;

    @Autowired
    private ReservatieRepository reservatieRepository;

    @Autowired
    private MeldingRepository meldingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CatalogusClient catalogusClient;

    @BeforeEach
    void opkuisen() {
        meldingRepository.deleteAll();
        reservatieRepository.deleteAll();
        gebruikerRepository.deleteAll();
    }

    @Test
    void registrerenGeeftEenTokenEnBewaartHetWachtwoordGehasht() throws Exception {
        mockMvc.perform(post("/api/auth/registreer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registratie("jana", "jana@test.be")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.gebruiker.gebruikersnaam").value("jana"))
                .andExpect(jsonPath("$.gebruiker.rol").value("KLANT"));

        Gebruiker opgeslagen = gebruikerRepository.findByGebruikersnaamIgnoreCase("jana").orElseThrow();
        assertThat(opgeslagen.getWachtwoordHash()).isNotEqualTo("geheim123");
        assertThat(passwordEncoder.matches("geheim123", opgeslagen.getWachtwoordHash())).isTrue();
    }

    @Test
    void registrerenMetBestaandeGebruikersnaamGeeftConflict() throws Exception {
        registreer("jana");

        mockMvc.perform(post("/api/auth/registreer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registratie("Jana", "andere@test.be")))
                .andExpect(status().isConflict());
    }

    @Test
    void registrerenMetTeKortWachtwoordWordtGeweigerd() throws Exception {
        mockMvc.perform(post("/api/auth/registreer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gebruikersnaam": "jana", "email": "jana@test.be", "voornaam": "Jana",
                                 "achternaam": "Peeters", "wachtwoord": "kort"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Wachtwoord moet minstens 8 tekens lang zijn"));
    }

    @Test
    void inloggenMetJuistEnFoutWachtwoord() throws Exception {
        registreer("jana");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gebruikersnaam": "jana", "wachtwoord": "geheim123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gebruikersnaam": "jana", "wachtwoord": "fout-wachtwoord"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profielIsEnkelBereikbaarNaInloggen() throws Exception {
        mockMvc.perform(get("/api/profiel")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/profiel").header("Authorization", "Bearer ongeldig.token.waarde"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profielAanpassenWordtCorrectOpgeslagen() throws Exception {
        String token = registreer("jana");

        mockMvc.perform(metToken(put("/api/profiel"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"voornaam": "Janneke", "achternaam": "Janssens", "email": "Janneke@Voorbeeld.be"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voornaam").value("Janneke"));

        mockMvc.perform(metToken(get("/api/profiel"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gebruikersnaam").value("jana"))
                .andExpect(jsonPath("$.voornaam").value("Janneke"))
                .andExpect(jsonPath("$.achternaam").value("Janssens"))
                .andExpect(jsonPath("$.email").value("janneke@voorbeeld.be"));

        Gebruiker opgeslagen = gebruikerRepository.findByGebruikersnaamIgnoreCase("jana").orElseThrow();
        assertThat(opgeslagen.getVoornaam()).isEqualTo("Janneke");
        assertThat(opgeslagen.getAchternaam()).isEqualTo("Janssens");
        assertThat(opgeslagen.getEmail()).isEqualTo("janneke@voorbeeld.be");
    }

    @Test
    void profielAanpassenNaarHetEmailadresVanEenAnderAccountWordtGeweigerd() throws Exception {
        registreer("tom");
        String token = registreer("jana");

        mockMvc.perform(metToken(put("/api/profiel"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"voornaam": "Jana", "achternaam": "Peeters", "email": "tom@test.be"}
                                """))
                .andExpect(status().isConflict());

        assertThat(gebruikerRepository.findByGebruikersnaamIgnoreCase("jana").orElseThrow().getEmail())
                .isEqualTo("jana@test.be");
    }

    @Test
    void bevestigdeReservatieVerschijntInHetProfielEnDeKlantKrijgtEenMelding() throws Exception {
        when(catalogusClient.haalBoek(ISBN))
                .thenReturn(new BoekInfo(ISBN, "Het diner", List.of("Herman Koch"), new BigDecimal("12.99"), 5));
        String klantToken = registreer("jana");
        String adminToken = adminToken();

        // 1. klant reserveert
        String antwoord = mockMvc.perform(metToken(post("/api/reservaties"), klantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lijnen": [{"isbn": "%s", "aantal": 2}]}
                                """.formatted(ISBN)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("GERESERVEERD"))
                .andExpect(jsonPath("$.totaal").value(25.98))
                .andReturn().getResponse().getContentAsString();
        Integer reservatieId = JsonPath.read(antwoord, "$.id");

        // nog niet bevestigd: nog geen gekochte boeken en de stock is niet aangepast
        mockMvc.perform(metToken(get("/api/profiel"), klantToken))
                .andExpect(jsonPath("$.gekochteBoeken.length()").value(0));
        verify(catalogusClient, never()).verminderStock(anyList());

        // 2. een klant mag zelf niet bevestigen
        mockMvc.perform(metToken(post("/api/admin/reservaties/{id}/bevestigen", reservatieId), klantToken))
                .andExpect(status().isForbidden());

        // 3. de winkel bevestigt
        String afhaalDatum = LocalDate.now().plusDays(2).toString();
        mockMvc.perform(metToken(post("/api/admin/reservaties/{id}/bevestigen", reservatieId), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"afhaalDatum": "%s"}
                                """.formatted(afhaalDatum)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BEVESTIGD"))
                .andExpect(jsonPath("$.afhaalDatum").value(afhaalDatum));

        // de catalogus kreeg de opdracht om de stock te verminderen
        verify(catalogusClient).verminderStock(List.of(new StockLijn(ISBN, 2)));

        // het profiel van de klant bevat nu het gekochte boek
        mockMvc.perform(metToken(get("/api/profiel"), klantToken))
                .andExpect(jsonPath("$.gekochteBoeken.length()").value(1))
                .andExpect(jsonPath("$.gekochteBoeken[0].isbn").value(ISBN))
                .andExpect(jsonPath("$.gekochteBoeken[0].titel").value("Het diner"))
                .andExpect(jsonPath("$.gekochteBoeken[0].aantal").value(2));

        // en de klant kreeg een melding om de boeken af te halen en te betalen
        mockMvc.perform(metToken(get("/api/profiel/meldingen"), klantToken))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].gelezen").value(false))
                .andExpect(jsonPath("$[0].tekst").value(containsString("bevestigd")))
                .andExpect(jsonPath("$[0].tekst").value(containsString("afhalen en betalen")));

        // 4. de klant schrijft een review voor het gekochte boek
        mockMvc.perform(metToken(post("/api/profiel/gekochte-boeken/{isbn}/review", ISBN), klantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score": 5, "tekst": "Kon het niet wegleggen"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewScore").value(5));

        Long klantId = gebruikerRepository.findByGebruikersnaamIgnoreCase("jana").orElseThrow().getId();
        verify(catalogusClient).plaatsReview(ISBN, new CatalogusReview(klantId, "jana", 5, "Kon het niet wegleggen"));
    }

    @Test
    void eenReviewVoorEenNietGekochtBoekWordtGeweigerd() throws Exception {
        String token = registreer("jana");

        mockMvc.perform(metToken(post("/api/profiel/gekochte-boeken/{isbn}/review", ISBN), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score": 5, "tekst": "Nooit gelezen maar vast goed"}
                                """))
                .andExpect(status().isForbidden());

        verify(catalogusClient, never()).plaatsReview(any(), any());
    }

    @Test
    void eenKlantKanZijnOpenstaandeReservatieAnnuleren() throws Exception {
        when(catalogusClient.haalBoek(ISBN))
                .thenReturn(new BoekInfo(ISBN, "Het diner", List.of("Herman Koch"), new BigDecimal("12.99"), 5));
        String token = registreer("jana");
        String antwoord = mockMvc.perform(metToken(post("/api/reservaties"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lijnen": [{"isbn": "%s", "aantal": 1}]}
                                """.formatted(ISBN)))
                .andReturn().getResponse().getContentAsString();
        Integer reservatieId = JsonPath.read(antwoord, "$.id");

        mockMvc.perform(metToken(post("/api/reservaties/{id}/annuleren", reservatieId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GEANNULEERD"));

        // een geannuleerde reservatie kan niet meer bevestigd worden
        mockMvc.perform(metToken(post("/api/admin/reservaties/{id}/bevestigen", reservatieId), adminToken()))
                .andExpect(status().isConflict());
        verify(catalogusClient, never()).verminderStock(anyList());
    }

    private String registreer(String gebruikersnaam) throws Exception {
        String antwoord = mockMvc.perform(post("/api/auth/registreer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registratie(gebruikersnaam, gebruikersnaam + "@test.be")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(antwoord, "$.token");
    }

    private String adminToken() throws Exception {
        if (!gebruikerRepository.existsByGebruikersnaamIgnoreCase("beheerder")) {
            gebruikerRepository.save(new Gebruiker("beheerder", "beheerder@test.be", "Winkel", "Beheerder",
                    passwordEncoder.encode("admin-wachtwoord"), Rol.ADMIN));
        }
        String antwoord = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gebruikersnaam": "beheerder", "wachtwoord": "admin-wachtwoord"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(antwoord, "$.token");
    }

    private static String registratie(String gebruikersnaam, String email) {
        return """
                {"gebruikersnaam": "%s", "email": "%s", "voornaam": "Jana", "achternaam": "Peeters",
                 "wachtwoord": "geheim123"}
                """.formatted(gebruikersnaam, email);
    }

    private static MockHttpServletRequestBuilder metToken(MockHttpServletRequestBuilder verzoek, String token) {
        return verzoek.header("Authorization", "Bearer " + token);
    }
}
