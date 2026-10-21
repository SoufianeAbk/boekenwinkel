package be.ehb.gebruikersservice.web;

import org.springframework.security.oauth2.jwt.Jwt;

final class IngelogdeGebruiker {

    private IngelogdeGebruiker() {
    }

    /** Het subject van de JWT is het id van de gebruiker (zie TokenService). */
    static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
