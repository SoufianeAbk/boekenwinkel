package be.ehb.catalogusservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Een levering van de uitgever: zoveel exemplaren komen bij in de stock. */
public record LeveringRequest(
        @Min(value = 1, message = "Een levering bevat minstens 1 exemplaar")
        @Max(value = 10_000, message = "Een levering bevat maximaal 10000 exemplaren")
        int aantal
) {
}
