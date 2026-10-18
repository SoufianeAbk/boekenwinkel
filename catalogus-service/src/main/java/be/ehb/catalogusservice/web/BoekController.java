package be.ehb.catalogusservice.web;

import be.ehb.catalogusservice.dto.BoekDetailDto;
import be.ehb.catalogusservice.dto.BoekDto;
import be.ehb.catalogusservice.dto.BoekRequest;
import be.ehb.catalogusservice.dto.BoekWijzigRequest;
import be.ehb.catalogusservice.dto.LeveringRequest;
import be.ehb.catalogusservice.service.BoekService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Publieke catalogus. Lezen mag iedereen, wijzigen enkel een beheerder (zie SecurityConfig).
 */
@RestController
@RequestMapping("/api/boeken")
@RequiredArgsConstructor
public class BoekController {

    private final BoekService boekService;

    @GetMapping
    public List<BoekDto> zoekBoeken(@RequestParam(required = false) String zoek) {
        return boekService.zoekBoeken(zoek);
    }

    @GetMapping("/{isbn}")
    public BoekDetailDto haalBoek(@PathVariable String isbn) {
        return boekService.haalBoek(isbn);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BoekDto voegBoekToe(@Valid @RequestBody BoekRequest request) {
        return boekService.voegBoekToe(request);
    }

    @PutMapping("/{isbn}")
    public BoekDto wijzigBoek(@PathVariable String isbn, @Valid @RequestBody BoekWijzigRequest request) {
        return boekService.wijzigBoek(isbn, request);
    }

    @PostMapping("/{isbn}/levering")
    public BoekDto registreerLevering(@PathVariable String isbn, @Valid @RequestBody LeveringRequest request) {
        return boekService.registreerLevering(isbn, request.aantal());
    }
}
