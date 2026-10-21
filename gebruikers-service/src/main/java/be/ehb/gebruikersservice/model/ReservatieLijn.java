package be.ehb.gebruikersservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Eén boek in een reservatie. Titel en prijs worden bewaard op het moment van reserveren,
 * zodat een latere prijswijziging in de catalogus de reservatie niet verandert.
 */
@Entity
@Getter
@NoArgsConstructor
public class ReservatieLijn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reservatie_id")
    private Reservatie reservatie;

    @Column(nullable = false, length = 13)
    private String isbn;

    @Column(nullable = false)
    private String titel;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prijs;

    @Column(nullable = false)
    private int aantal;

    ReservatieLijn(Reservatie reservatie, String isbn, String titel, BigDecimal prijs, int aantal) {
        this.reservatie = reservatie;
        this.isbn = isbn;
        this.titel = titel;
        this.prijs = prijs;
        this.aantal = aantal;
    }

    public BigDecimal subtotaal() {
        return prijs.multiply(BigDecimal.valueOf(aantal));
    }
}
