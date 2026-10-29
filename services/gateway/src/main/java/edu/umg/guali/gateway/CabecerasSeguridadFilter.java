package edu.umg.guali.gateway;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Cabeceras de seguridad en todas las respuestas del gateway (API y, en una PC, también el frontend).
 * En el servidor nginx las reemplaza por las suyas (proxy_hide_header), así no se duplican.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CabecerasSeguridadFilter implements WebFilter {

    static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
            + "font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; "
            + "frame-ancestors 'none'";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> {
            HttpHeaders h = exchange.getResponse().getHeaders();
            h.set("Content-Security-Policy", CSP);
            h.set("X-Content-Type-Options", "nosniff");
            h.set("X-Frame-Options", "DENY");
            h.set("Referrer-Policy", "strict-origin-when-cross-origin");
            h.set("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=(), usb=()");
            h.set("Cross-Origin-Opener-Policy", "same-origin");
            h.set("Cross-Origin-Resource-Policy", "same-origin");
            return Mono.empty();
        });
        return chain.filter(exchange);
    }
}
