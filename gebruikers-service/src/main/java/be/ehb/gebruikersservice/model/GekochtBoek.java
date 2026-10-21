package be.ehb.gebruikersservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Een boek in de lijst "gekochte boeken" van een gebruiker. De titel en de eigen review worden hier
 * ook bewaard zodat het profiel getoond kan worden zonder de catalogus-service te moeten bevragen.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"gebruiker_id", "isbn"}))
@Getter
@Setter
@NoArgsConstructor
public class GekochtBoek {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "gebruiker_id")
    private Gebruiker gebruiker;

    @Column(nullable = false, length = 13)
    private String isbn;

    @Column(nullable = false)
    private String titel;

    @Column(nullable = false)
    private int aantal;

    @Column(nullable = false)
    private LocalDateTime laatstGekochtOp;

    /** Score (1-5) van de review die de gebruiker schreef, of null als er nog geen review is. */
    private Integer reviewScore;

    @Column(length = 2000)
    private String reviewTekst;

    public GekochtBoek(Gebruiker gebruiker, String isbn, String titel) {
        this.gebruiker = gebruiker;
        this.isbn = isbn;
        this.titel = titel;
    }

    void voegAankoopToe(int extra, LocalDateTime tijdstip) {
        this.aantal += extra;
        this.laatstGekochtOp = tijdstip;
    }
}
