package be.ehb.gebruikersservice.web;

import be.ehb.gebruikersservice.dto.BevestigRequest;
import be.ehb.gebruikersservice.dto.ReservatieDto;
import be.ehb.gebruikersservice.model.ReservatieStatus;
import be.ehb.gebruikersservice.service.ReservatieService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Beheer van reservaties door de winkel (enkel voor rol ADMIN, zie SecurityConfig). */
@RestController
@RequestMapping("/api/admin/reservaties")
@RequiredArgsConstructor
public class AdminReservatieController {

    private final ReservatieService reservatieService;

    @GetMapping
    public List<ReservatieDto> reservaties(@RequestParam(required = false) ReservatieStatus status) {
        return reservatieService.alleReservaties(status);
    }

    @PostMapping("/{id}/bevestigen")
    public ReservatieDto bevestig(@PathVariable Long id, @RequestBody(required = false) BevestigRequest request) {
        return reservatieService.bevestig(id, request == null ? null : request.afhaalDatum());
    }
}
