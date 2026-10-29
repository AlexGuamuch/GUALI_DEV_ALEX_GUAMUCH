package edu.umg.guali.seguridad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Las rutas /interno/** solo se usan entre microservicios dentro de la red privada de Docker.
 * Además de no publicarse en el gateway, exigen un token compartido (defensa en profundidad).
 */
public class TokenInternoFilter extends OncePerRequestFilter {

    public static final String CABECERA = "X-Interno-Token";

    private final byte[] esperado;

    public TokenInternoFilter(String token) {
        if (token == null || token.length() < 24) {
            throw new IllegalStateException("guali.seguridad.token-interno debe tener al menos 24 caracteres");
        }
        this.esperado = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/interno/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibido = request.getHeader(CABECERA);
        if (recibido == null || !MessageDigest.isEqual(esperado, recibido.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
