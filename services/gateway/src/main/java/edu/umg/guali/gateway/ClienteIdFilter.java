package edu.umg.guali.gateway;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Identificador anónimo del navegador del estudiante (no requiere cuenta).
 *
 * Se guarda en la cookie HttpOnly "guali_cliente" y se reenvía a los servicios en X-Cliente-Id.
 * Las cabeceras internas que lleguen desde Internet se eliminan para que nadie pueda suplantar
 * a otro navegador ni a otro servicio.
 */
@Component
public class ClienteIdFilter implements GlobalFilter, Ordered {

    static final String COOKIE = "guali_cliente";
    static final String CABECERA = "X-Cliente-Id";
    static final String CABECERA_GATEWAY = "X-Gateway-Token";

    private final boolean cookieSegura;
    private final String tokenGateway;

    public ClienteIdFilter(@Value("${guali.cookie-segura:false}") boolean cookieSegura,
                           @Value("${guali.token-gateway}") String tokenGateway) {
        if (tokenGateway == null || tokenGateway.length() < 24) {
            throw new IllegalStateException("Falta TOKEN_GATEWAY (mínimo 24 caracteres). "
                    + "Ejecuta: bash deploy/generar-secretos.sh");
        }
        this.cookieSegura = cookieSegura;
        this.tokenGateway = tokenGateway;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpCookie cookie = request.getCookies().getFirst(COOKIE);
        String id = cookie != null && esUuid(cookie.getValue()) ? cookie.getValue() : null;

        if (id == null) {
            id = UUID.randomUUID().toString();
            exchange.getResponse().addCookie(ResponseCookie.from(COOKIE, id)
                    .httpOnly(true)
                    .secure(cookieSegura)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ofDays(730))
                    .build());
        }

        String clienteId = id;
        ServerHttpRequest limpia = request.mutate()
                .headers(h -> {
                    h.remove(CABECERA);
                    h.remove(CABECERA_GATEWAY);
                    h.remove("X-Interno-Token");
                    h.set(CABECERA, clienteId);
                    h.set(CABECERA_GATEWAY, tokenGateway);
                })
                .build();
        return chain.filter(exchange.mutate().request(limpia).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private static boolean esUuid(String valor) {
        try {
            return UUID.fromString(valor).toString().equals(valor);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
