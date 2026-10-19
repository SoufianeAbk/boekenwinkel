package be.ehb.catalogusservice.web;

import be.ehb.catalogusservice.model.Boek;
import be.ehb.catalogusservice.repository.BoekRepository;
import be.ehb.catalogusservice.repository.ReviewRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functionele testen van de REST-API van de catalogus, inclusief beveiliging en database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogusApiTest {

    private static final String ISBN = "9789026473807";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoekRepository boekRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Value("${app.intern.api-key}")
    private String apiKey;

    @Value("${app.jwt.secret}")
    private String jwtGeheim;

    @Value("${app.jwt.issuer}")
    private String jwtIssuer;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        boekRepository.deleteAll();
        boekRepository.save(new Boek(ISBN, "Het diner", List.of("Herman Koch"), "Twee broers gaan uit eten.",
                new BigDecimal("12.99"), 5));
        boekRepository.save(new Boek("9780552171892", "Good Omens", List.of("Terry Pratchett", "Neil Gaiman"),
                "Een engel en een demon.", new BigDecimal("13.99"), 7));
    }

    @Test
    void catalogusIsPubliekRaadpleegbaar() throws Exception {
        mockMvc.perform(get("/api/boeken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].titel").value("Good Omens"))
                .andExpect(jsonPath("$[0].auteurs.length()").value(2))
                .andExpect(jsonPath("$[1].isbn").value(ISBN))
                .andExpect(jsonPath("$[1].stock").value(5));
    }

    @Test
    void zoekenVindtBoekenOpAuteur() throws Exception {
        mockMvc.perform(get("/api/boeken").param("zoek", "gaiman"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titel").value("Good Omens"));
    }

    @Test
    void onbekendBoekGeeft404() throws Exception {
        mockMvc.perform(get("/api/boeken/9780000000002"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Geen boek gevonden met ISBN 9780000000002"));
    }

    @Test
    void bevestigdeReservatieVermindertDeStockInDeCatalogus() throws Exception {
        mockMvc.perform(post("/api/intern/stock/verminderen")
                        .header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lijnen": [{"isbn": "%s", "aantal": 2}]}
                                """.formatted(ISBN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stock").value(3));

        // de catalogus toont meteen de nieuwe stock
        mockMvc.perform(get("/api/boeken/" + ISBN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(3));
        assertThat(boekRepository.findById(ISBN).orElseThrow().getStock()).isEqualTo(3);
    }

    @Test
    void stockVerminderenZonderGeldigeApiSleutelWordtGeweigerd() throws Exception {
        mockMvc.perform(post("/api/intern/stock/verminderen")
                        .header("X-API-KEY", "verkeerde-sleutel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lijnen": [{"isbn": "%s", "aantal": 2}]}
                                """.formatted(ISBN)))
                .andExpect(status().isUnauthorized());

        assertThat(boekRepository.findById(ISBN).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void teveelVerminderenGeeftConflictEnLaatDeStockOngewijzigd() throws Exception {
        mockMvc.perform(post("/api/intern/stock/verminderen")
                        .header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lijnen": [
                                    {"isbn": "9780552171892", "aantal": 1},
                                    {"isbn": "%s", "aantal": 6}
                                ]}
                                """.formatted(ISBN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isbn").value(ISBN))
                .andExpect(jsonPath("$.beschikbaar").value(5))
                .andExpect(jsonPath("$.gevraagd").value(6));

        assertThat(boekRepository.findById(ISBN).orElseThrow().getStock()).isEqualTo(5);
        assertThat(boekRepository.findById("9780552171892").orElseThrow().getStock()).isEqualTo(7);
    }

    @Test
    void leveringRegistrerenVereistDeRolAdmin() throws Exception {
        String levering = """
                {"aantal": 10}
                """;

        mockMvc.perform(post("/api/boeken/{isbn}/levering", ISBN)
                        .contentType(MediaType.APPLICATION_JSON).content(levering))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/boeken/{isbn}/levering", ISBN)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_KLANT")))
                        .contentType(MediaType.APPLICATION_JSON).content(levering))
                .andExpect(status().isForbidden());

        assertThat(boekRepository.findById(ISBN).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void leveringMetEchteAdminTokenVanDeGebruikersServiceVerhoogtDeStock() throws Exception {
        mockMvc.perform(post("/api/boeken/{isbn}/levering", ISBN)
                        .header("Authorization", "Bearer " + tokenMetRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"aantal": 10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(15));

        assertThat(boekRepository.findById(ISBN).orElseThrow().getStock()).isEqualTo(15);
    }

    @Test
    void nieuwBoekToevoegenAlsAdmin() throws Exception {
        String boek = """
                {"isbn": "978-0-13-235088-4", "titel": "Clean Code", "auteurs": ["Robert C. Martin"],
                 "omschrijving": "Over leesbare code.", "prijs": 44.95, "stock": 3}
                """;

        mockMvc.perform(post("/api/boeken")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(boek))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value("9780132350884"));

        mockMvc.perform(post("/api/boeken")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(boek))
                .andExpect(status().isConflict());
    }

    @Test
    void boekZonderTitelWordtGeweigerd() throws Exception {
        mockMvc.perform(post("/api/boeken")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "9780132350884", "titel": "", "auteurs": ["Robert C. Martin"], "prijs": 44.95, "stock": 3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Titel is verplicht"));
    }

    @Test
    void reviewsVerschijnenOpDeDetailpaginaMetGemiddeldeScore() throws Exception {
        plaatsReview(1L, "jana", 4, "Spannend tot de laatste bladzijde");
        plaatsReview(2L, "tom", 5, "Meesterlijk");

        mockMvc.perform(get("/api/boeken/" + ISBN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aantalReviews").value(2))
                .andExpect(jsonPath("$.gemiddeldeScore").value(4.5))
                .andExpect(jsonPath("$.reviews.length()").value(2));

        mockMvc.perform(get("/api/boeken"))
                .andExpect(jsonPath("$[1].gemiddeldeScore").value(4.5));
    }

    @Test
    void eenTweedeReviewVanDezelfdeGebruikerVervangtDeEerste() throws Exception {
        plaatsReview(1L, "jana", 2, "Viel tegen");
        plaatsReview(1L, "jana", 4, "Bij herlezing toch erg goed");

        mockMvc.perform(get("/api/boeken/" + ISBN))
                .andExpect(jsonPath("$.aantalReviews").value(1))
                .andExpect(jsonPath("$.reviews[0].score").value(4))
                .andExpect(jsonPath("$.reviews[0].tekst").value("Bij herlezing toch erg goed"));
    }

    @Test
    void reviewMetOngeldigeScoreWordtGeweigerd() throws Exception {
        mockMvc.perform(post("/api/intern/boeken/{isbn}/reviews", ISBN)
                        .header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gebruikerId": 1, "gebruikersnaam": "jana", "score": 6, "tekst": "Super"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private void plaatsReview(Long gebruikerId, String gebruikersnaam, int score, String tekst) throws Exception {
        mockMvc.perform(post("/api/intern/boeken/{isbn}/reviews", ISBN)
                        .header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gebruikerId": %d, "gebruikersnaam": "%s", "score": %d, "tekst": "%s"}
                                """.formatted(gebruikerId, gebruikersnaam, score, tekst)))
                .andExpect(status().isCreated());
    }

    /** Maakt een token zoals de gebruikers-service dat doet, ondertekend met hetzelfde gedeelde geheim. */
    private String tokenMetRol(String rol) {
        var sleutel = new SecretKeySpec(jwtGeheim.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(sleutel));
        Instant nu = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtIssuer)
                .subject("1")
                .issuedAt(nu)
                .expiresAt(nu.plus(1, ChronoUnit.HOURS))
                .claim("roles", List.of(rol))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
