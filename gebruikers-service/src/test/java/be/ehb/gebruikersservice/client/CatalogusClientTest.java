package be.ehb.gebruikersservice.client;

import be.ehb.gebruikersservice.exception.CatalogusNietBereikbaarException;
import be.ehb.gebruikersservice.exception.NietGevondenException;
import be.ehb.gebruikersservice.exception.OnvoldoendeStockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Test de communicatie met de catalogus-service: welke verzoeken we sturen
 * en hoe de antwoorden van de catalogus vertaald worden.
 */
@RestClientTest(CatalogusClient.class)
class CatalogusClientTest {

    private static final String ISBN = "9789026473807";

    @Autowired
    private CatalogusClient catalogusClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void verminderStockStuurtDeLijnenMetDeApiSleutelNaarDeCatalogus() {
        server.expect(requestTo("http://catalogus.test/api/intern/stock/verminderen"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-KEY", "test-api-sleutel"))
                .andExpect(content().json("""
                        {"lijnen": [{"isbn": "%s", "aantal": 2}]}
                        """.formatted(ISBN)))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        catalogusClient.verminderStock(List.of(new StockLijn(ISBN, 2)));

        server.verify();
    }

    @Test
    void onvoldoendeStockInDeCatalogusWordtVertaaldMetDeFoutboodschap() {
        server.expect(requestTo("http://catalogus.test/api/intern/stock/verminderen"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"title": "Onvoldoende stock", "status": 409,
                                 "detail": "Onvoldoende stock voor 'Het diner': 3 gevraagd, nog 2 beschikbaar",
                                 "isbn": "%s", "beschikbaar": 2, "gevraagd": 3}
                                """.formatted(ISBN)));

        assertThatThrownBy(() -> catalogusClient.verminderStock(List.of(new StockLijn(ISBN, 3))))
                .isInstanceOf(OnvoldoendeStockException.class)
                .hasMessage("Onvoldoende stock voor 'Het diner': 3 gevraagd, nog 2 beschikbaar");
    }

    @Test
    void haalBoekLeestDeGegevensUitDeCatalogus() {
        server.expect(requestTo("http://catalogus.test/api/boeken/" + ISBN))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"isbn": "%s", "titel": "Het diner", "auteurs": ["Herman Koch"], "omschrijving": "Roman",
                         "prijs": 12.99, "stock": 4, "gemiddeldeScore": null, "aantalReviews": 0, "reviews": []}
                        """.formatted(ISBN), MediaType.APPLICATION_JSON));

        BoekInfo boek = catalogusClient.haalBoek(ISBN);

        assertThat(boek.titel()).isEqualTo("Het diner");
        assertThat(boek.prijs()).isEqualByComparingTo("12.99");
        assertThat(boek.stock()).isEqualTo(4);
    }

    @Test
    void onbekendBoekGeeftNietGevonden() {
        server.expect(requestTo("http://catalogus.test/api/boeken/9780000000002"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"status": 404, "detail": "Geen boek gevonden met ISBN 9780000000002"}
                                """));

        assertThatThrownBy(() -> catalogusClient.haalBoek("9780000000002"))
                .isInstanceOf(NietGevondenException.class)
                .hasMessage("Geen boek gevonden met ISBN 9780000000002");
    }

    @Test
    void eenServerfoutVanDeCatalogusGeeftCatalogusNietBereikbaar() {
        server.expect(requestTo("http://catalogus.test/api/intern/boeken/" + ISBN + "/reviews"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> catalogusClient.plaatsReview(ISBN, new CatalogusReview(1L, "jana", 5, "Top")))
                .isInstanceOf(CatalogusNietBereikbaarException.class);
    }
}
