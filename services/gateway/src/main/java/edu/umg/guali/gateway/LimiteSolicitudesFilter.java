package edu.umg.guali.gateway;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Límite de solicitudes por IP (ventana fija de un minuto), segunda capa después de Nginx y Cloudflare.
 * Protege sobre todo el compilador y el inicio de sesión.
 */
@Component
public class LimiteSolicitudesFilter implements GlobalFilter, Ordered {

    private static final Duration VENTANA = Duration.ofMinutes(1);

    private static final List<Regla> REGLAS = List.of(
            new Regla("/api/auth/login", 10),
            new Regla("/api/codigo/", 30),
            new Regla("/api/admin/lecciones/importar", 10),
            new Regla("/api/", 300));

    private final Map<String, Contador> contadores = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String ruta = request.getPath().value();
        Regla regla = REGLAS.stream().filter(r -> ruta.startsWith(r.prefijo())).findFirst().orElse(null);
        if (regla == null) return chain.filter(exchange);

        long ventana = System.currentTimeMillis() / VENTANA.toMillis();
        String clave = regla.prefijo() + "|" + ipCliente(request);
        Contador contador = contadores.compute(clave, (k, actual) ->
                actual == null || actual.ventana() != ventana ? new Contador(ventana, new AtomicInteger()) : actual);

        if (contador.cuenta().incrementAndGet() > regla.maximo()) {
            return rechazar(exchange.getResponse());
        }
        return chain.filter(exchange);
    }

    /** Nginx reemplaza X-Real-IP con la IP real (detrás de Cloudflare, CF-Connecting-IP). */
    private static String ipCliente(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip;
        return request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "desconocida";
    }

    private static Mono<Void> rechazar(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.getHeaders().set("Retry-After", "60");
        byte[] cuerpo = ("{\"status\":429,\"title\":\"Too Many Requests\","
                + "\"detail\":\"Demasiadas solicitudes. Espera un minuto e inténtalo de nuevo.\"}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(cuerpo);
        return response.writeWith(Mono.just(buffer));
    }

    @Scheduled(fixedDelay = 120_000)
    void limpiar() {
        long actual = System.currentTimeMillis() / VENTANA.toMillis();
        contadores.values().removeIf(c -> c.ventana() < actual);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    record Regla(String prefijo, int maximo) {
    }

    record Contador(long ventana, AtomicInteger cuenta) {
    }
}
