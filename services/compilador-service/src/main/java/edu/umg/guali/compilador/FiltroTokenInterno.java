package edu.umg.guali.compilador;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * El compilador no usa Spring Security ni base de datos (superficie mínima en el servicio que ejecuta
 * código ajeno). Exige el token interno que solo conoce lecciones-service.
 */
@Component
public class FiltroTokenInterno extends OncePerRequestFilter {

    private final byte[] esperado;

    public FiltroTokenInterno(@Value("${guali.seguridad.token-interno}") String token) {
        if (token == null || token.length() < 24) {
            throw new IllegalStateException("TOKEN_INTERNO debe tener al menos 24 caracteres");
        }
        this.esperado = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibido = request.getHeader("X-Interno-Token");
        if (recibido == null || !MessageDigest.isEqual(esperado, recibido.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
