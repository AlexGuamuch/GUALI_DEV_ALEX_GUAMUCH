package edu.umg.guali.lecciones.servicio;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import edu.umg.guali.lecciones.dto.Dtos.RespuestaCodigo;
import edu.umg.guali.lecciones.servicio.EvaluadorObjetivo.Accion;
import edu.umg.guali.seguridad.TokenInternoFilter;

/** Cliente REST del microservicio compilador (solo accesible dentro de la red interna). */
@Component
public class ClienteCompilador {

    private final RestClient rest;

    public ClienteCompilador(RestClient.Builder builder,
                             @Value("${guali.compilador.url}") String url,
                             @Value("${guali.compilador.token}") String token) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        // Las solicitudes se atienden de una en una en el compilador: se espera un poco más que su tiempo límite.
        fabrica.setReadTimeout(Duration.ofSeconds(45));
        this.rest = builder
                .baseUrl(url)
                .requestFactory(fabrica)
                .defaultHeader(TokenInternoFilter.CABECERA, token)
                .build();
    }

    public RespuestaCodigo procesar(String codigo, Accion accion) {
        try {
            RespuestaCompilador r = rest.post()
                    .uri("/interno/procesar")
                    .body(new SolicitudCompilador(codigo, accion.name()))
                    .retrieve()
                    .body(RespuestaCompilador.class);
            if (r == null) throw new RestClientException("Respuesta vacía");
            return new RespuestaCodigo(r.compilacion(), r.ejecucion(), false);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El compilador está ocupado o no responde. Intenta de nuevo en unos segundos.");
        }
    }

    record SolicitudCompilador(String codigo, String accion) {
    }

    record RespuestaCompilador(edu.umg.guali.lecciones.dto.Dtos.Compilacion compilacion,
                               edu.umg.guali.lecciones.dto.Dtos.Ejecucion ejecucion) {
    }
}
