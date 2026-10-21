package be.ehb.gebruikersservice.service;

import be.ehb.gebruikersservice.model.Gebruiker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Maakt JWT's aan. De token bevat het id van de gebruiker (subject), de gebruikersnaam en de rol.
 * De catalogus-service kan dezelfde token valideren met het gedeelde geheim.
 */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final String issuer;
    private final Duration geldigheid;

    public TokenService(JwtEncoder jwtEncoder,
                        Clock clock,
                        @Value("${app.jwt.issuer}") String issuer,
                        @Value("${app.jwt.geldigheid}") Duration geldigheid) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.issuer = issuer;
        this.geldigheid = geldigheid;
    }

    public String maakToken(Gebruiker gebruiker) {
        Instant nu = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(nu)
                .expiresAt(nu.plus(geldigheid))
                .subject(String.valueOf(gebruiker.getId()))
                .claim("gebruikersnaam", gebruiker.getGebruikersnaam())
                .claim("roles", List.of(gebruiker.getRol().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
