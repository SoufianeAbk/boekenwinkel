package be.ehb.gebruikersservice.web;

import be.ehb.gebruikersservice.exception.BoekNietGekochtException;
import be.ehb.gebruikersservice.exception.CatalogusNietBereikbaarException;
import be.ehb.gebruikersservice.exception.GebruikerBestaatAlException;
import be.ehb.gebruikersservice.exception.NietGevondenException;
import be.ehb.gebruikersservice.exception.OngeldigeLoginException;
import be.ehb.gebruikersservice.exception.OnvoldoendeStockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/** Zet fouten om naar een ProblemDetail (RFC 9457) met een leesbare boodschap in "detail". */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NietGevondenException.class)
    public ProblemDetail nietGevonden(NietGevondenException e) {
        return probleem(HttpStatus.NOT_FOUND, "Niet gevonden", e.getMessage());
    }

    @ExceptionHandler(GebruikerBestaatAlException.class)
    public ProblemDetail bestaatAl(GebruikerBestaatAlException e) {
        return probleem(HttpStatus.CONFLICT, "Bestaat al", e.getMessage());
    }

    @ExceptionHandler(OngeldigeLoginException.class)
    public ProblemDetail ongeldigeLogin(OngeldigeLoginException e) {
        return probleem(HttpStatus.UNAUTHORIZED, "Inloggen mislukt", e.getMessage());
    }

    @ExceptionHandler(BoekNietGekochtException.class)
    public ProblemDetail nietGekocht(BoekNietGekochtException e) {
        return probleem(HttpStatus.FORBIDDEN, "Niet toegestaan", e.getMessage());
    }

    @ExceptionHandler(OnvoldoendeStockException.class)
    public ProblemDetail onvoldoendeStock(OnvoldoendeStockException e) {
        return probleem(HttpStatus.CONFLICT, "Onvoldoende stock", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail ongeldigeStatus(IllegalStateException e) {
        return probleem(HttpStatus.CONFLICT, "Actie niet mogelijk", e.getMessage());
    }

    @ExceptionHandler(CatalogusNietBereikbaarException.class)
    public ProblemDetail catalogusNietBereikbaar(CatalogusNietBereikbaarException e) {
        log.warn("Probleem bij communicatie met de catalogus-service", e);
        return probleem(HttpStatus.SERVICE_UNAVAILABLE, "Catalogus niet beschikbaar", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validatieFout(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(", "));
        return probleem(HttpStatus.BAD_REQUEST, "Ongeldige invoer", detail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail ongeldigeInvoer(IllegalArgumentException e) {
        return probleem(HttpStatus.BAD_REQUEST, "Ongeldige invoer", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail ongeldigeParameter(MethodArgumentTypeMismatchException e) {
        return probleem(HttpStatus.BAD_REQUEST, "Ongeldige invoer",
                "Ongeldige waarde '%s' voor '%s'".formatted(e.getValue(), e.getName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail onleesbareInvoer(HttpMessageNotReadableException e) {
        return probleem(HttpStatus.BAD_REQUEST, "Ongeldige invoer", "De verstuurde gegevens konden niet gelezen worden");
    }

    private static ProblemDetail probleem(HttpStatus status, String titel, String detail) {
        ProblemDetail probleem = ProblemDetail.forStatusAndDetail(status, detail);
        probleem.setTitle(titel);
        return probleem;
    }
}
