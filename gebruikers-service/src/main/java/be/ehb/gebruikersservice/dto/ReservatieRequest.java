package be.ehb.gebruikersservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReservatieRequest(
        @NotEmpty(message = "Een reservatie bevat minstens één boek")
        List<@Valid Lijn> lijnen
) {

    public record Lijn(
            @NotBlank(message = "ISBN is verplicht")
            String isbn,

            @Min(value = 1, message = "Aantal moet minstens 1 zijn")
            @Max(value = 20, message = "Je kan maximaal 20 exemplaren van een boek reserveren")
            int aantal
    ) {
    }
}
