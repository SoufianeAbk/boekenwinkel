package be.ehb.gebruikersservice.service;

import be.ehb.gebruikersservice.dto.AuthAntwoord;
import be.ehb.gebruikersservice.dto.GebruikerDto;
import be.ehb.gebruikersservice.dto.LoginRequest;
import be.ehb.gebruikersservice.dto.RegistratieRequest;
import be.ehb.gebruikersservice.exception.GebruikerBestaatAlException;
import be.ehb.gebruikersservice.exception.OngeldigeLoginException;
import be.ehb.gebruikersservice.model.Gebruiker;
import be.ehb.gebruikersservice.model.Rol;
import be.ehb.gebruikersservice.repository.GebruikerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GebruikerRepository gebruikerRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public AuthAntwoord registreer(RegistratieRequest request) {
        String gebruikersnaam = request.gebruikersnaam().trim();
        String email = request.email().trim().toLowerCase();

        if (gebruikerRepository.existsByGebruikersnaamIgnoreCase(gebruikersnaam)) {
            throw new GebruikerBestaatAlException("De gebruikersnaam '" + gebruikersnaam + "' is al in gebruik");
        }
        if (gebruikerRepository.existsByEmailIgnoreCase(email)) {
            throw new GebruikerBestaatAlException("Er bestaat al een account met dit e-mailadres");
        }

        Gebruiker gebruiker = gebruikerRepository.save(new Gebruiker(
                gebruikersnaam,
                email,
                request.voornaam().trim(),
                request.achternaam().trim(),
                passwordEncoder.encode(request.wachtwoord()),
                Rol.KLANT));

        return new AuthAntwoord(tokenService.maakToken(gebruiker), GebruikerDto.van(gebruiker));
    }

    @Transactional(readOnly = true)
    public AuthAntwoord login(LoginRequest request) {
        Gebruiker gebruiker = gebruikerRepository.findByGebruikersnaamIgnoreCase(request.gebruikersnaam().trim())
                .filter(g -> passwordEncoder.matches(request.wachtwoord(), g.getWachtwoordHash()))
                .orElseThrow(OngeldigeLoginException::new);

        return new AuthAntwoord(tokenService.maakToken(gebruiker), GebruikerDto.van(gebruiker));
    }
}
