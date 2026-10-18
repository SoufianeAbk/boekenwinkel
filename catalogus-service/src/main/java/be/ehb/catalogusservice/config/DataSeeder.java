package be.ehb.catalogusservice.config;

import be.ehb.catalogusservice.model.Boek;
import be.ehb.catalogusservice.repository.BoekRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vult een lege catalogus met een aantal boeken zodat de applicatie meteen bruikbaar is.
 * Uit te schakelen met {@code app.seed.enabled=false} (gebeurt in de testen).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final BoekRepository boekRepository;

    @Override
    public void run(String... args) {
        if (boekRepository.count() > 0) {
            return;
        }
        List<Boek> boeken = List.of(
                boek("9789023417354", "De avonden", List.of("Gerard Reve"),
                        "Tien dagen uit het leven van Frits van Egters, een jonge kantoorklerk in het naoorlogse Amsterdam die zich door lange, lege avonden worstelt met ironie en verveling.",
                        "19.99", 6),
                boek("9789023403913", "Het verdriet van België", List.of("Hugo Claus"),
                        "Louis Seynaeve groeit op in West-Vlaanderen voor en tijdens de Tweede Wereldoorlog. Een grote roman over opgroeien, collaboratie en de Vlaamse kleinburgerlijkheid.",
                        "29.99", 4),
                boek("9789028900141", "Max Havelaar", List.of("Multatuli"),
                        "Een felle aanklacht tegen de uitbuiting van de Javaanse bevolking onder het Nederlandse koloniale bestuur, verpakt in een verhaal vol ironie en stijlwisselingen.",
                        "14.50", 3),
                boek("9789023496007", "De ontdekking van de hemel", List.of("Harry Mulisch"),
                        "Een monumentale roman over vriendschap, liefde en het lot, waarin hemelse wezens de opdracht krijgen om de stenen tafelen van de wet terug te halen.",
                        "27.50", 5),
                boek("9789026473807", "Het diner", List.of("Herman Koch"),
                        "Twee broers en hun vrouwen dineren in een chic restaurant. Tussen de gangen door komt een gruwelijk geheim over hun zonen naar boven.",
                        "12.99", 8),
                boek("9789023436614", "Oorlog en terpentijn", List.of("Stefan Hertmans"),
                        "Aan de hand van de schriften van zijn grootvader reconstrueert de verteller diens leven als arbeiderskind, soldaat aan het IJzerfront en zondagsschilder.",
                        "22.99", 2),
                boek("9789023458807", "Tirza", List.of("Arnon Grunberg"),
                        "Jörgen Hofmeester organiseert een afscheidsfeest voor zijn jongste dochter Tirza. Een beklemmende roman over vaderliefde en de kloof tussen schijn en werkelijkheid.",
                        "21.99", 0),
                boek("9780141036144", "Nineteen Eighty-Four", List.of("George Orwell"),
                        "In een totalitaire staat waar Big Brother alles ziet, probeert Winston Smith zijn eigen gedachten en herinneringen te bewaren. Een klassieker over surveillance en waarheid.",
                        "11.50", 10),
                boek("9780552171892", "Good Omens", List.of("Terry Pratchett", "Neil Gaiman"),
                        "Een engel en een demon die al eeuwen op aarde wonen, besluiten samen het einde van de wereld tegen te houden. Absurd, geestig en verrassend hartelijk.",
                        "13.99", 7),
                boek("9780132350884", "Clean Code", List.of("Robert C. Martin"),
                        "Praktische principes en voorbeelden om code te schrijven die leesbaar, testbaar en onderhoudbaar blijft. Verplichte kost voor elke ontwikkelaar.",
                        "44.95", 5),
                boek("9780201633610", "Design Patterns", List.of("Erich Gamma", "Richard Helm", "Ralph Johnson", "John Vlissides"),
                        "Het standaardwerk van de 'Gang of Four' met 23 beproefde ontwerppatronen voor objectgeoriënteerde software, van Singleton tot Observer.",
                        "54.99", 3),
                boek("9789027410009", "De kleine prins", List.of("Antoine de Saint-Exupéry"),
                        "Een piloot strandt in de woestijn en ontmoet een prinsje van een andere planeet. Een poëtisch verhaal over vriendschap en wat werkelijk belangrijk is.",
                        "9.99", 12),
                boek("9789076832340", "Gödel, Escher, Bach", List.of("Douglas Hofstadter"),
                        "Een speelse verkenning van zelfverwijzing en bewustzijn via de wiskunde van Gödel, de prenten van Escher en de muziek van Bach.",
                        "39.99", 1),
                boek("9789044690125", "Sapiens", List.of("Yuval Noah Harari"),
                        "Een vlot geschreven geschiedenis van de mensheid: van de cognitieve revolutie over de landbouw tot de wetenschappelijke revolutie.",
                        "24.99", 9)
        );
        boekRepository.saveAll(boeken);
        log.info("Catalogus gevuld met {} boeken", boeken.size());
    }

    private static Boek boek(String isbn, String titel, List<String> auteurs, String omschrijving, String prijs, int stock) {
        return new Boek(isbn, titel, auteurs, omschrijving, new BigDecimal(prijs), stock);
    }
}
