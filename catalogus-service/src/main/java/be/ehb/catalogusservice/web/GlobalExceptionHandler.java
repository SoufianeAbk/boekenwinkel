package be.ehb.catalogusservice.web;

import be.ehb.catalogusservice.exception.BoekBestaatAlException;
import be.ehb.catalogusservice.exception.BoekNietGevondenException;
import be.ehb.catalogusservice.exception.OnvoldoendeStockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Zet fouten om naar een ProblemDetail (RFC 9457) zodat de frontend en de gebruikers-service
 * altijd een leesbare foutboodschap krijgen in het veld "detail".
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BoekNietGevondenException.class)
    public ProblemDetail boekNietGevonden(BoekNietGevondenException e) {
        return probleem(HttpStatus.NOT_FOUND, "Boek niet gevonden", e.getMessage());
    }

    @ExceptionHandler(OnvoldoendeStockException.class)
    public ProblemDetail onvoldoendeStock(OnvoldoendeStockException e) {
        ProblemDetail probleem = probleem(HttpStatus.CONFLICT, "Onvoldoende stock", e.getMessage());
        probleem.setProperty("isbn", e.getIsbn());
        probleem.setProperty("beschikbaar", e.getBeschikbaar());
        probleem.setProperty("gevraagd", e.getGevraagd());
        return probleem;
    }

    @ExceptionHandler(BoekBestaatAlException.class)
    public ProblemDetail boekBestaatAl(BoekBestaatAlException e) {
        return probleem(HttpStatus.CONFLICT, "Boek bestaat al", e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail gelijktijdigeWijziging(ObjectOptimisticLockingFailureException e) {
        return probleem(HttpStatus.CONFLICT, "Gelijktijdige wijziging",
                "Het boek werd ondertussen door iemand anders gewijzigd. Probeer opnieuw.");
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
