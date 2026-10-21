package be.ehb.gebruikersservice.dto;

import be.ehb.gebruikersservice.model.Gebruiker;
import be.ehb.gebruikersservice.model.Reservatie;
import be.ehb.gebruikersservice.model.ReservatieLijn;
import be.ehb.gebruikersservice.model.ReservatieStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReservatieDto(
        Long id,
        ReservatieStatus status,
        LocalDateTime aangemaaktOp,
        LocalDateTime bevestigdOp,
        LocalDate afhaalDatum,
        BigDecimal totaal,
        Klant klant,
        List<Lijn> lijnen
) {

    public static ReservatieDto van(Reservatie reservatie) {
        Gebruiker g = reservatie.getGebruiker();
        return new ReservatieDto(
                reservatie.getId(),
                reservatie.getStatus(),
                reservatie.getAangemaaktOp(),
                reservatie.getBevestigdOp(),
                reservatie.getAfhaalDatum(),
                reservatie.totaal(),
                new Klant(g.getId(), g.getGebruikersnaam(), g.getVoornaam() + " " + g.getAchternaam(), g.getEmail()),
                reservatie.getLijnen().stream().map(Lijn::van).toList()
        );
    }

    public record Klant(Long id, String gebruikersnaam, String naam, String email) {
    }

    public record Lijn(String isbn, String titel, BigDecimal prijs, int aantal, BigDecimal subtotaal) {

        static Lijn van(ReservatieLijn lijn) {
            return new Lijn(lijn.getIsbn(), lijn.getTitel(), lijn.getPrijs(), lijn.getAantal(), lijn.subtotaal());
        }
    }
}
