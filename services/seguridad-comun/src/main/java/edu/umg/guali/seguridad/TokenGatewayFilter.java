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
 * Las rutas públicas /api/** solo se aceptan si llegan a través del gateway, que agrega este token.
 * Así nadie dentro de la red de Docker (por ejemplo, un programa de estudiante en el compilador)
 * puede llamar a los servicios saltándose el gateway, sus límites y su identificador anónimo.
 */
public class TokenGatewayFilter extends OncePerRequestFilter {

    public static final String CABECERA = "X-Gateway-Token";

    private final byte[] esperado;

    public TokenGatewayFilter(String token) {
        if (token == null || token.length() < 24) {
            throw new IllegalStateException("guali.seguridad.token-gateway debe tener al menos 24 caracteres");
        }
        this.esperado = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
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
