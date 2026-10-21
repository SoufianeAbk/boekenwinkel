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
import lombok.Setter;

import java.time.LocalDateTime;

/** Een bericht voor de gebruiker, bv. de bevestiging dat gereserveerde boeken klaarliggen. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Melding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "gebruiker_id")
    private Gebruiker gebruiker;

    @Column(nullable = false, length = 1000)
    private String tekst;

    @Column(nullable = false)
    private LocalDateTime aangemaaktOp;

    private boolean gelezen;

    private Long reservatieId;

    public Melding(Gebruiker gebruiker, String tekst, LocalDateTime aangemaaktOp, Long reservatieId) {
        this.gebruiker = gebruiker;
        this.tekst = tekst;
        this.aangemaaktOp = aangemaaktOp;
        this.reservatieId = reservatieId;
    }
}
