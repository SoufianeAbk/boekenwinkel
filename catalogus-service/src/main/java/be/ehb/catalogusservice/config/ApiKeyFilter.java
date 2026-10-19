package be.ehb.catalogusservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Herkent service-naar-service verkeer aan de header {@value #HEADER}. Een verzoek met de juiste
 * sleutel krijgt de rol SERVICE, waarmee de interne endpoints bereikbaar worden.
 */
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-KEY";

    private final byte[] apiKey;

    public ApiKeyFilter(String apiKey) {
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String meegestuurd = request.getHeader(HEADER);
        // MessageDigest.isEqual vergelijkt in constante tijd, zodat de sleutel niet te raden valt via timing
        if (meegestuurd != null && MessageDigest.isEqual(apiKey, meegestuurd.getBytes(StandardCharsets.UTF_8))) {
            var authenticatie = UsernamePasswordAuthenticationToken.authenticated(
                    "gebruikers-service", null, AuthorityUtils.createAuthorityList("ROLE_SERVICE"));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authenticatie);
            SecurityContextHolder.setContext(context);
        }
        chain.doFilter(request, response);
    }
}
