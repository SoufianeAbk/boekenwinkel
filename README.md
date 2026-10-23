# De Leeshoek – boekenwinkel

Applicatie voor een fictieve boekenwinkel, opgebouwd uit kleine services die via REST met elkaar communiceren, plus een aparte frontend. Klanten reserveren boeken online, de winkel bevestigt de reservatie, en de klant haalt de boeken af en betaalt in de winkel.

## Inhoud

- [Overzicht](#overzicht)
- [Architectuur](#architectuur)
- [Functionaliteit](#functionaliteit)
- [Flow van een reservatie](#flow-van-een-reservatie)
- [Domeinmodel](#domeinmodel)
- [Rollen en beveiliging](#rollen-en-beveiliging)
- [REST-API](#rest-api)
- [Frontend](#frontend)
- [Projectstructuur](#projectstructuur)
- [Opstarten](#opstarten)
- [Demo-accounts](#demo-accounts)
- [Testen](#testen)
- [Configuratie](#configuratie)
- [Probleemoplossing](#probleemoplossing)
- [Ontwerpkeuzes en beperkingen](#ontwerpkeuzes-en-beperkingen)

## Overzicht

| Project | Technologie | Poort | Verantwoordelijkheid |
|---|---|---|---|
| `catalogus-service` | Spring Boot 4.1, Java 21, Spring Data JPA, Spring Security (OAuth2 resource server), H2 | 8081 | Boeken, stock en reviews (de catalogus) |
| `gebruikers-service` | Spring Boot 4.1, Java 21, Spring Data JPA, Spring Security (JWT), RestClient, H2 | 8082 | Registreren/inloggen (JWT), reservaties, profiel, meldingen |
| `boekenwinkel-frontend` | React 19, TypeScript, Vite 7, React Router 7 | 5173 | Webapplicatie voor klanten en winkelbeheer |

De taal in de code (klassen, velden, endpoints, foutboodschappen) is grotendeels Nederlands. Er is geen gezamenlijke parent-pom: elke service is een zelfstandig Maven-project met eigen `pom.xml` en Maven Wrapper.

## Architectuur

```
                     +------------------------------+
                     |     boekenwinkel-frontend    |
                     |        (React, :5173)        |
                     +------------------------------+
        /api/boeken/**   |                  |   /api/auth, /api/profiel,
        (+ JWT admin)    |                  |   /api/reservaties, /api/admin
                         v                  v
 +--------------------------+  REST + API-sleutel  +----------------------------+
 |    catalogus-service     |<---------------------|     gebruikers-service     |
 |         (:8081)          |   boek opvragen,     |          (:8082)           |
 |  Boek, Review            |   stock verminderen, |  Gebruiker, Reservatie,    |
 |  H2: data/catalogus      |   review plaatsen    |  GekochtBoek, Melding      |
 +--------------------------+                      |  H2: data/gebruikers       |
                                                   +----------------------------+
```

- **Eigen database per service.** De gebruikers-service kent de boeken enkel via de REST-API van de catalogus. Titel en prijs worden bij een reservatie mee opgeslagen, zodat een latere prijswijziging een reservatie niet verandert.
- **Authenticatie met JWT.** De gebruikers-service maakt bij registreren/inloggen een token (HS256) met het gebruikers-id (`sub`), de gebruikersnaam en de rol (`KLANT` of `ADMIN`, in de claim `roles`). De catalogus-service valideert dezelfde token (handtekening, uitgever en vervaldatum) met het gedeelde geheim, zodat een beheerder rechtstreeks boeken kan toevoegen en leveringen kan registreren.
- **Service-naar-service verkeer** gaat naar `/api/intern/**` van de catalogus en is afgeschermd met een API-sleutel in de header `X-API-KEY`. De sleutel wordt in constante tijd vergeleken.
- **Eén adres voor de frontend.** De Vite-dev-server stuurt elk API-pad door naar de juiste service (zie `boekenwinkel-frontend/vite.config.ts`). Daardoor zijn er in de ontwikkelomgeving geen CORS-problemen.
- **Stateless.** Geen sessies op de server; elk verzoek draagt zijn eigen token (`Authorization: Bearer ...`).

## Functionaliteit

| Vereiste | Waar |
|---|---|
| Catalogus met ISBN, titel, auteur(s), omschrijving, prijs en stock | catalogus-service → `GET /api/boeken`, `GET /api/boeken/{isbn}` · frontend: catalogus + detailpagina |
| Stock aanpassen wanneer er een levering binnenkomt | `POST /api/boeken/{isbn}/levering` (admin) · frontend: Beheer → Catalogus en stock |
| Registreren en inloggen | `POST /api/auth/registreer`, `POST /api/auth/login` |
| Boeken reserveren | `POST /api/reservaties` (stock wordt gecontroleerd bij de catalogus) |
| Bevestiging naar de gebruiker wanneer hij mag afhalen en betalen | `POST /api/admin/reservaties/{id}/bevestigen` → maakt een melding met afhaaldatum en totaal |
| Bij bevestiging daalt de stock in de catalogus | de bevestiging roept `POST /api/intern/stock/verminderen` van de catalogus aan |
| Profiel met lijst gekochte boeken | bij bevestiging komen de boeken in het profiel (alles wat bevestigd is, wordt afgehaald) · `GET /api/profiel` |
| Review (score + tekst) voor een gekocht boek | `POST /api/profiel/gekochte-boeken/{isbn}/review` → wordt doorgestuurd naar de catalogus |
| Reviews op de detailpagina van een boek | `GET /api/boeken/{isbn}` bevat de reviews en de gemiddelde score |

**Extra:**

- zoeken op titel, auteur of ISBN (deelmatch, hoofdletterongevoelig);
- profielgegevens aanpassen (voornaam, achternaam, e-mailadres);
- een openstaande reservatie annuleren;
- nieuwe boeken toevoegen (frontend: Beheer → Catalogus en stock) en bestaande boeken wijzigen (`PUT /api/boeken/{isbn}`, enkel via de API, zonder scherm in de frontend);
- meldingen in de applicatie, met teller voor ongelezen meldingen en alles-als-gelezen-markeren;
- een tweede review van dezelfde gebruiker voor hetzelfde boek vervangt de eerste;
- genormaliseerde ISBN's (streepjes en spaties worden verwijderd, 10 of 13 cijfers);
- optimistic locking op de stock (twee gelijktijdige bevestigingen kunnen elkaar niet overschrijven);
- voor elk boek wordt in de frontend een unieke cover getekend op basis van het ISBN (er zijn geen afbeeldingen).

## Flow van een reservatie

1. De klant voegt boeken toe aan zijn reservatie (het mandje) en plaatst ze. Daarvoor moet hij ingelogd zijn. De gebruikers-service vraagt titel, prijs en stock op bij de catalogus en weigert de reservatie als er te weinig stock is. De reservatie krijgt de status `GERESERVEERD`. De stock verandert nog niet.
2. Een beheerder bevestigt de reservatie en kiest een afhaaldatum (zonder datum wordt dat morgen; een datum in het verleden wordt geweigerd). De gebruikers-service vraagt de catalogus om de stock van alle lijnen samen te verminderen (alles of niets). Lukt dat niet, bv. door onvoldoende stock, dan blijft de reservatie openstaan.
3. Daarna krijgt de reservatie de status `BEVESTIGD`, komen de boeken in de lijst gekochte boeken van de klant en krijgt de klant een melding: vanaf welke datum hij de boeken mag afhalen en betalen (met het totaalbedrag).
4. De klant kan vanuit zijn profiel een review schrijven voor elk gekocht boek. Die review verschijnt op de detailpagina in de catalogus.

Een klant kan zijn reservatie annuleren zolang ze `GERESERVEERD` is. Een geannuleerde of al bevestigde reservatie kan niet (opnieuw) bevestigd worden.

Statussen van een reservatie:

| Status | Betekenis |
|---|---|
| `GERESERVEERD` | Door de klant aangevraagd, wacht op bevestiging door de winkel |
| `BEVESTIGD` | Bevestigd door de winkel: de boeken liggen klaar om af te halen en te betalen |
| `GEANNULEERD` | Door de klant geannuleerd vóór de bevestiging |

## Domeinmodel

**catalogus-service**

| Entiteit | Belangrijkste velden |
|---|---|
| `Boek` | `isbn` (id, genormaliseerd), `titel`, `auteurs` (geordende lijst), `omschrijving` (max. 2000 tekens), `prijs`, `stock`, `versie` (optimistic locking) |
| `Review` | `boek`, `gebruikerId`, `gebruikersnaam`, `score` (1–5), `tekst` (max. 2000 tekens), `aangemaaktOp`. Uniek per (boek, gebruiker) |

**gebruikers-service**

| Entiteit | Belangrijkste velden |
|---|---|
| `Gebruiker` | `gebruikersnaam` (uniek), `email` (uniek), `voornaam`, `achternaam`, `wachtwoordHash` (BCrypt), `rol` (`KLANT`/`ADMIN`), lijst `gekochteBoeken` |
| `Reservatie` | `gebruiker`, `status`, `aangemaaktOp`, `bevestigdOp`, `afhaalDatum`, lijst `lijnen` |
| `ReservatieLijn` | `isbn`, `titel`, `prijs`, `aantal`. Titel en prijs zijn een momentopname bij het reserveren |
| `GekochtBoek` | `isbn`, `titel`, `aantal`, `laatstGekochtOp`, `reviewScore`, `reviewTekst`. Uniek per (gebruiker, ISBN). Koopt een klant een boek opnieuw, dan stijgt het aantal |
| `Melding` | `tekst`, `aangemaaktOp`, `gelezen`, `reservatieId` |

## Rollen en beveiliging

| Wie | Hoe herkend | Mag |
|---|---|---|
| Iedereen (anoniem) | geen token | catalogus raadplegen (`GET /api/boeken/**`), registreren, inloggen |
| `KLANT` | JWT met rol `KLANT` | reserveren, eigen reservaties/profiel/meldingen, reviews voor gekochte boeken |
| `ADMIN` | JWT met rol `ADMIN` | reservaties bevestigen en bekijken, boeken toevoegen/wijzigen, leveringen registreren |
| `SERVICE` | header `X-API-KEY` | enkel `/api/intern/**` van de catalogus (gebruikers-service naar catalogus) |

- Wachtwoorden worden met BCrypt gehasht. Een nieuw account krijgt altijd de rol `KLANT`.
- CSRF is uitgeschakeld omdat de API stateless is en geen cookies gebruikt.
- CORS staat enkel de origin(s) uit `app.cors.allowed-origins` toe (standaard `http://localhost:5173`).
- De H2-console (`/h2-console`) is open en enkel bedoeld voor lokale ontwikkeling.

## REST-API

Foutboodschappen komen als `ProblemDetail` (RFC 9457) terug; de leesbare tekst staat in het veld `detail`. De frontend toont die tekst rechtstreeks aan de gebruiker.

| Status | Wanneer |
|---|---|
| 400 | ongeldige invoer (validatie, ongeldig ISBN, onleesbare JSON, afhaaldatum in het verleden) |
| 401 | geen of ongeldige/verlopen token, ongeldige API-sleutel, fout wachtwoord bij inloggen |
| 403 | onvoldoende rechten (bv. klant op een admin-endpoint, review voor een niet-gekocht boek) |
| 404 | boek, reservatie of gebruiker niet gevonden |
| 409 | gebruikersnaam/e-mail/ISBN bestaat al, onvoldoende stock, reservatie heeft een ongeldige status, gelijktijdige wijziging |
| 503 | de gebruikers-service kan de catalogus niet bereiken |

### catalogus-service (`:8081`)

| Methode | Pad | Toegang | Beschrijving |
|---|---|---|---|
| GET | `/api/boeken?zoek=` | publiek | Alle boeken op titel gesorteerd, of gefilterd op titel/auteur/ISBN. Met gemiddelde score en aantal reviews |
| GET | `/api/boeken/{isbn}` | publiek | Detail van één boek met alle reviews (nieuwste eerst), gemiddelde score (1 decimaal) en aantal reviews |
| POST | `/api/boeken` | ADMIN | Nieuw boek toevoegen (`isbn`, `titel`, `auteurs`, `omschrijving`, `prijs`, `stock`). 201, of 409 als het ISBN al bestaat |
| PUT | `/api/boeken/{isbn}` | ADMIN | Titel, auteurs, omschrijving en prijs wijzigen. De stock wijzigt enkel via leveringen en bevestigde reservaties |
| POST | `/api/boeken/{isbn}/levering` | ADMIN | Levering registreren: `{"aantal": 10}` (1 tot 10000) verhoogt de stock |
| POST | `/api/intern/stock/verminderen` | SERVICE | `{"lijnen": [{"isbn": "...", "aantal": 2}]}`. Alles of niets; dubbele ISBN's worden samengeteld. Bij te weinig stock 409 met `isbn`, `beschikbaar` en `gevraagd` |
| POST | `/api/intern/boeken/{isbn}/reviews` | SERVICE | Review plaatsen of de vorige review van dezelfde gebruiker overschrijven. 201 |

### gebruikers-service (`:8082`)

| Methode | Pad | Toegang | Beschrijving |
|---|---|---|---|
| POST | `/api/auth/registreer` | publiek | Account aanmaken (rol `KLANT`). Antwoord: `{token, gebruiker}`. 201, of 409 bij bestaande gebruikersnaam/e-mail |
| POST | `/api/auth/login` | publiek | Inloggen met gebruikersnaam (hoofdletterongevoelig) en wachtwoord. Antwoord: `{token, gebruiker}` |
| GET | `/api/profiel` | ingelogd | Eigen gegevens met de lijst gekochte boeken |
| PUT | `/api/profiel` | ingelogd | Voornaam, achternaam en e-mailadres aanpassen (e-mail moet uniek blijven) |
| POST | `/api/profiel/gekochte-boeken/{isbn}/review` | ingelogd | `{"score": 1-5, "tekst": "..."}` voor een gekocht boek. 403 als het boek niet gekocht is. Wordt doorgestuurd naar de catalogus; opnieuw schrijven vervangt de vorige review |
| GET | `/api/profiel/meldingen` | ingelogd | Eigen meldingen, nieuwste eerst |
| POST | `/api/profiel/meldingen/gelezen` | ingelogd | Alle meldingen als gelezen markeren (204) |
| GET | `/api/reservaties` | ingelogd | Eigen reservaties, nieuwste eerst |
| POST | `/api/reservaties` | ingelogd | `{"lijnen": [{"isbn": "...", "aantal": 1}]}`. Maximaal 20 exemplaren per boek; stock wordt gecontroleerd. 201 |
| POST | `/api/reservaties/{id}/annuleren` | ingelogd | Eigen openstaande reservatie annuleren |
| GET | `/api/admin/reservaties?status=` | ADMIN | Alle reservaties (nieuwste eerst), of gefilterd op `GERESERVEERD`, `BEVESTIGD` of `GEANNULEERD` (oudste eerst) |
| POST | `/api/admin/reservaties/{id}/bevestigen` | ADMIN | Optionele body `{"afhaalDatum": "2026-10-20"}`. Vermindert de stock, zet de status op `BEVESTIGD`, vult het profiel van de klant en maakt een melding |

### JWT

| Claim | Inhoud |
|---|---|
| `iss` | `app.jwt.issuer` (standaard `boekenwinkel-gebruikers-service`) |
| `sub` | id van de gebruiker |
| `gebruikersnaam` | gebruikersnaam |
| `roles` | `["KLANT"]` of `["ADMIN"]` (wordt `ROLE_KLANT` / `ROLE_ADMIN`) |
| `iat` / `exp` | uitgifte en vervaldatum (standaard 8 uur geldig, `app.jwt.geldigheid`) |

### Validatieregels

| Veld | Regel |
|---|---|
| ISBN | 10 cijfers (laatste mag `X`) of 13 cijfers; streepjes en spaties worden verwijderd |
| Gebruikersnaam | 3–50 tekens, enkel letters, cijfers, punt, `-` en `_` |
| Wachtwoord | 8–100 tekens |
| E-mailadres | geldig formaat; wordt in kleine letters bewaard |
| Boek | titel verplicht (max. 255), minstens één auteur, prijs ≥ 0 (max. 2 decimalen), stock ≥ 0, omschrijving max. 2000 tekens |
| Reservatielijn | aantal 1–20 per boek |
| Levering | aantal 1–10000 |
| Review | score 1–5, tekst max. 2000 tekens (optioneel) |

## Frontend

Eén-pagina-applicatie in React 19 met TypeScript (strikte modus), React Router 7 en Vite 7. Er is geen UI-bibliotheek: de opmaak staat in `src/styles.css`. De lettertypes (Fraunces en Inter) worden via Google Fonts geladen; zonder internet valt de browser terug op standaardlettertypes.

| Route | Pagina | Toegang |
|---|---|---|
| `/` | Catalogus: zoekveld (zoekt pas wanneer je even stopt met typen, `?zoek=`), stockbadge en gemiddelde score per boek | iedereen |
| `/boeken/:isbn` | Detail: omschrijving, stock, reviews van lezers, knop "Toevoegen aan reservatie" | iedereen |
| `/mandje` | Mijn reservatie (mandje): aantallen aanpassen, reservatie plaatsen | iedereen; plaatsen vereist inloggen |
| `/login`, `/registreer` | Inloggen en registreren | iedereen |
| `/profiel` | Tabs Meldingen, Reservaties (met annuleren), Gekochte boeken (met review schrijven) en Mijn gegevens (`?tab=`) | ingelogd |
| `/beheer` | Tabs Reservaties (filter en bevestigen met afhaaldatum) en Catalogus en stock (levering registreren, nieuw boek) (`?tab=catalogus`) | enkel `ADMIN` |

Werking:

- **Sessie.** Token en gebruiker staan in `localStorage` (`leeshoek.sessie`). Een verlopen token wordt bij het laden genegeerd; krijgt de frontend een 401 terwijl hij ingelogd is, dan wordt de gebruiker automatisch uitgelogd.
- **Mandje.** Het mandje staat in `localStorage` (`leeshoek.mandje`) en blijft dus bewaard na het sluiten van de browser. Het aantal wordt begrensd door de stock en door 20 per boek. De server gebruikt bij het reserveren altijd de actuele prijs uit de catalogus.
- **Meldingen.** De frontend haalt de meldingen elke 30 seconden op; het aantal ongelezen meldingen staat bij "Mijn profiel" in de navigatie.
- **API-laag.** `src/api/` bevat de fetch-wrapper (`client.ts`, met foutvertaling uit `detail`), de calls per service (`catalogus.ts`, `gebruikers.ts`) en de types die overeenkomen met de DTO's van de backend (`types.ts`).

Beschikbare scripts (`cd boekenwinkel-frontend`):

| Commando | Doet |
|---|---|
| `npm run dev` | Dev-server op http://localhost:5173 met proxy naar de services |
| `npm run typecheck` | Typecontrole (`tsc --noEmit`) |
| `npm run build` | Typecontrole en productiebuild in `dist/` |
| `npm run preview` | De productiebuild lokaal bekijken |

De proxy staat in `vite.config.ts`. Voor een echte deployment heb je een reverse proxy nodig die dezelfde paden naar de services doorstuurt, of je moet `app.cors.allowed-origins` aanpassen. De frontend heeft geen geautomatiseerde testen.

## Projectstructuur

```
boekenwinkel/
├── catalogus-service/
│   ├── pom.xml, mvnw, mvnw.cmd
│   └── src/
│       ├── main/java/be/ehb/catalogusservice/
│       │   ├── config/      beveiliging (JWT + API-sleutel), CORS, data-seeder
│       │   ├── model/       Boek, Review, Isbn (normalisatie)
│       │   ├── repository/  Spring Data-repositories
│       │   ├── service/     BoekService (stock), ReviewService
│       │   ├── web/         BoekController, InternController, foutafhandeling
│       │   ├── dto/         request- en responseobjecten (records)
│       │   └── exception/   domeinexceptions
│       ├── main/resources/application.properties
│       └── test/            unit- en functionele testen
├── gebruikers-service/
│   ├── pom.xml, mvnw, mvnw.cmd
│   └── src/
│       ├── main/java/be/ehb/gebruikersservice/
│       │   ├── client/      CatalogusClient (REST naar de catalogus)
│       │   ├── config/      beveiliging (JWT, BCrypt), CORS, data-seeder
│       │   ├── model/       Gebruiker, Reservatie, ReservatieLijn, GekochtBoek, Melding, enums
│       │   ├── repository/  Spring Data-repositories
│       │   ├── service/     AuthService, TokenService, ReservatieService, ProfielService, MeldingService
│       │   ├── web/         Auth-, Profiel-, Reservatie- en AdminReservatieController
│       │   ├── dto/         request- en responseobjecten (records)
│       │   └── exception/   domeinexceptions
│       ├── main/resources/application.properties
│       └── test/            unit- en functionele testen
└── boekenwinkel-frontend/
    ├── index.html, package.json, vite.config.ts, tsconfig.json
    └── src/
        ├── api/         fetch-wrapper, calls per service, types
        ├── components/  Layout, BoekCover, StockBadge, Sterren, ReservatieKaart, ...
        ├── context/     AuthContext, MandjeContext, MeldingenContext
        ├── pages/       Catalogus-, BoekDetail-, Mandje-, Login-, Registreer-, Profiel- en BeheerPagina
        ├── util/        opmaak van datums en prijzen, useData-hook
        └── styles.css
```

## Opstarten

### Vereisten

- JDK 21 of hoger (`JAVA_HOME` moet ernaar wijzen als je via de command line werkt)
- Node.js 20.19+ (of 22.12+) met npm
- Internet bij de eerste start: de Maven Wrapper downloadt Maven en de dependencies, `npm install` downloadt de packages.
- Een IDE met Lombok-ondersteuning (IntelliJ: Lombok-plugin en *Enable annotation processing*). De services gebruiken Lombok.

### 1. Backend-services

Start beide services, elk in een eigen terminal (of via IntelliJ: `CatalogusServiceApplication` en `GebruikersServiceApplication`). Start de catalogus-service best eerst. In IntelliJ moet je daarvoor eerst beide `pom.xml`-bestanden als Maven-project laden (rechtsklik op `pom.xml` → *Add as Maven Project*).

Windows (PowerShell):

```powershell
cd catalogus-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd gebruikers-service
.\mvnw.cmd spring-boot:run
```

macOS / Linux:

```bash
cd catalogus-service && ./mvnw spring-boot:run
```

```bash
cd gebruikers-service && ./mvnw spring-boot:run
```

Bij de eerste start wordt de catalogus gevuld met 14 boeken en worden er twee accounts aangemaakt. De data wordt bewaard in `data/` binnen elke service (H2-bestand, relatief aan de map waarin je de service start). Verwijder die map om opnieuw te beginnen. De H2-console is bereikbaar op http://localhost:8081/h2-console en http://localhost:8082/h2-console (JDBC-URL `jdbc:h2:file:./data/catalogus` resp. `jdbc:h2:file:./data/gebruikers`, gebruiker `sa`, geen wachtwoord).

### 2. Frontend

```bash
cd boekenwinkel-frontend
npm install
npm run dev
```

Open daarna http://localhost:5173.

## Demo-accounts

De wachtwoorden staan in `gebruikers-service/src/main/resources/application.properties` (`app.seed.admin-wachtwoord` en `app.seed.klant-wachtwoord`) en zijn enkel bedoeld voor lokale ontwikkeling.

| Gebruikersnaam | Rol | Kan |
|---|---|---|
| `admin` | `ADMIN` | reservaties bevestigen, boeken toevoegen, leveringen registreren |
| `klant` | `KLANT` | reserveren, profiel, reviews |

Je kan ook zelf een nieuw klantaccount registreren.

## Testen

```bash
cd catalogus-service && ./mvnw test      # Windows: .\mvnw.cmd test
cd gebruikers-service && ./mvnw test
```

De testen draaien op een lege in-memory H2-database zonder seed-data (`src/test/resources/application.properties`). Er zijn geen draaiende services of netwerkverbindingen nodig: de catalogus wordt in de gebruikers-service gemockt.

**catalogus-service (22 testen)**

| Klasse | Testen | Wat wordt getest |
|---|---|---|
| `BoekServiceTest` | 8 | Unittesten met Mockito: stock verminderen voor alle lijnen, dubbele lijnen samentellen, bij onvoldoende stock wordt geen enkel boek aangepast, onbekend boek, levering verhoogt de stock, ongeldig aantal, nieuw boek met ISBN-normalisatie, bestaand ISBN geweigerd |
| `CatalogusApiTest` | 13 | Functionele testen via MockMvc met echte database en beveiliging: catalogus publiek raadpleegbaar, zoeken op auteur, onbekend boek (404), stock verminderen via de interne API is meteen zichtbaar in de catalogus, zonder geldige API-sleutel geweigerd (401), te veel verminderen geeft 409 en laat de stock ongewijzigd, levering vereist rol ADMIN (401/403), een echte JWT zoals de gebruikers-service die maakt wordt aanvaard, nieuw boek toevoegen als admin (en dubbel boek geweigerd), validatiefout, reviews verschijnen op de detailpagina met de juiste gemiddelde score, een tweede review van dezelfde gebruiker vervangt de eerste, ongeldige score geweigerd |
| `CatalogusServiceApplicationTests` | 1 | De Spring-context start |

**gebruikers-service (27 testen)**

| Klasse | Testen | Wat wordt getest |
|---|---|---|
| `ReservatieServiceTest` | 11 | Unittesten met een gemockte catalogus: bevestigen stuurt de juiste stockvermindering naar de catalogus, zet status en afhaaldatum, voegt de boeken toe aan het profiel (bij een herhaalde aankoop stijgt het aantal), maakt een melding met datum en totaal, standaard afhaaldatum morgen, afhaaldatum in het verleden geweigerd, een bevestigde reservatie kan niet opnieuw bevestigd worden; bij onvoldoende stock blijft de reservatie ongewijzigd; reserveren neemt titel en prijs over uit de catalogus zonder de stock te verminderen en weigert meer dan de beschikbare stock |
| `CatalogusClientTest` | 5 | REST-communicatie met de catalogus (`MockRestServiceServer`): API-sleutel en body worden meegestuurd, boek opvragen, fouten van de catalogus (409, 404, 500) worden correct vertaald |
| `GebruikersApiTest` | 10 | Functionele testen via MockMvc: registreren (wachtwoord gehasht), dubbele gebruikersnaam, te kort wachtwoord, inloggen (juist en fout wachtwoord), profiel enkel na inloggen, profiel correct aangepast en opgeslagen, e-mailadres van een ander account geweigerd, de volledige flow reserveren → bevestigen → gekochte boeken in profiel → melding → review (klant mag niet zelf bevestigen), review voor een niet-gekocht boek geweigerd, een openstaande reservatie annuleren |
| `GebruikersServiceApplicationTests` | 1 | De Spring-context start |

De frontend heeft geen testen; gebruik `npm run typecheck` of `npm run build` om hem te controleren.

## Configuratie

Alle instellingen staan in `application.properties` en kunnen overschreven worden met omgevingsvariabelen (Spring *relaxed binding*: punten worden underscores, streepjes verdwijnen, alles in hoofdletters).

| Property | Omgevingsvariabele | Service | Opmerking |
|---|---|---|---|
| `server.port` | `SERVER_PORT` | beide | standaard 8081 (catalogus) en 8082 (gebruikers) |
| `app.jwt.secret` | `APP_JWT_SECRET` | beide | min. 32 tekens, **gelijk in beide services** |
| `app.jwt.issuer` | `APP_JWT_ISSUER` | beide | moet gelijk zijn in beide services |
| `app.jwt.geldigheid` | `APP_JWT_GELDIGHEID` | gebruikers | geldigheid van een token, standaard `8h` |
| `app.intern.api-key` (catalogus) / `app.catalogus.api-key` (gebruikers) | `APP_INTERN_APIKEY` / `APP_CATALOGUS_APIKEY` | catalogus / gebruikers | moeten gelijk zijn |
| `app.catalogus.url` | `APP_CATALOGUS_URL` | gebruikers | adres van de catalogus-service, standaard `http://localhost:8081` |
| `app.cors.allowed-origins` | `APP_CORS_ALLOWEDORIGINS` | beide | frontend-origin(s), komma-gescheiden |
| `app.seed.enabled` | `APP_SEED_ENABLED` | beide | `false` schakelt de startdata uit (14 boeken, resp. twee accounts) |
| `app.seed.admin-wachtwoord` / `app.seed.klant-wachtwoord` | `APP_SEED_ADMINWACHTWOORD` / `APP_SEED_KLANTWACHTWOORD` | gebruikers | wachtwoorden van de demo-accounts |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | beide | standaard een H2-bestand in `./data/` |
| `spring.h2.console.enabled` | `SPRING_H2_CONSOLE_ENABLED` | beide | standaard `true`; zet uit buiten lokale ontwikkeling |

## Probleemoplossing

| Probleem | Oorzaak en oplossing |
|---|---|
| `mvnw` meldt dat `JAVA_HOME` niet gevonden is | Stel `JAVA_HOME` in op een JDK 21 of hoger |
| Poort 8081, 8082 of 5173 is bezet | Stop het andere programma of wijzig de poort (`server.port`, `vite.config.ts`; pas bij een andere backendpoort ook de proxy en `app.catalogus.url` aan) |
| De frontend toont "De server is niet bereikbaar" | De backend-services draaien niet, of de proxy in `vite.config.ts` wijst naar de verkeerde poort |
| Een beheerder wordt na een actie in de catalogus (boek toevoegen, levering) uitgelogd | `app.jwt.secret` of `app.jwt.issuer` verschilt tussen de services: de catalogus weigert de token met 401 |
| Bevestigen of een review plaatsen geeft "Catalogus niet beschikbaar" | De catalogus-service draait niet, of de API-sleutels verschillen (`app.intern.api-key` en `app.catalogus.api-key`) |
| Na een wijziging aan een entiteit start de service niet meer of ontbreken kolommen | Verwijder de `data/`-map van die service (het schema wordt met `ddl-auto=update` bijgewerkt, maar niet alles kan automatisch) |
| H2-console of externe tool meldt dat de database al in gebruik is | Een H2-bestand kan maar door één proces tegelijk geopend worden: gebruik de console van de draaiende service of stop de service eerst |
| Compilatiefouten over ontbrekende getters/constructors | Lombok-annotatieverwerking staat uit in je IDE |

## Ontwerpkeuzes en beperkingen

- **Geen gedistribueerde transactie** tussen de services. Bij een bevestiging wordt eerst de stock in de catalogus verminderd en pas daarna de reservatie lokaal bevestigd. Faalt de catalogus, dan verandert er niets. Faalt de lokale opslag nadat de catalogus al geantwoord heeft (zeldzaam), dan is de stock al verminderd. In een productiesysteem zou je dit oplossen met een saga of een outbox-patroon met berichten. Hetzelfde geldt voor een review: eerst naar de catalogus, daarna lokaal bewaard.
- **De stock in de catalogus is de fysieke stock in de winkel**: openstaande (nog niet bevestigde) reservaties houden geen stock vast. Bij het bevestigen wordt opnieuw gecontroleerd. Reserveren de klanten tegelijk het laatste exemplaar, dan lukt de bevestiging maar voor één reservatie; de andere blijft openstaan.
- **Alles wat bevestigd is, wordt afgehaald.** De boeken komen bij het bevestigen in het profiel van de klant; er is geen aparte stap voor afhalen of betalen. Betalen gebeurt in de winkel, niet in de applicatie.
- **Meldingen zijn berichten in de applicatie** (profiel → meldingen). Een e-mail versturen kan later als extra kanaal toegevoegd worden.
- **Alleen de gebruikers-service kent de reservaties en gebruikers**, alleen de catalogus de boeken. Titel en prijs worden gekopieerd (reservatielijn, gekocht boek) zodat het profiel en oude reservaties zonder de catalogus kunnen getoond worden.
- **Beperkingen van deze versie:** geen paginering van de catalogus, geen scherm om een boek te wijzigen (de API ervoor bestaat wel), en een reservatie kan enkel door de klant zelf geannuleerd worden, niet door een beheerder, geen refresh-tokens of intrekken van tokens (een token blijft geldig tot hij verloopt), token en mandje staan in `localStorage`, en er zijn geen frontendtesten.
- **Voor productie:** vervang H2 door een echte database, zet `spring.h2.console.enabled=false`, kies eigen waarden voor `app.jwt.secret`, de API-sleutel en de demo-wachtwoorden (of schakel de seed uit), en zet de frontend achter een reverse proxy met HTTPS.