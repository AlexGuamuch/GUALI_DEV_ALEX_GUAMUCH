package edu.umg.guali.admin.servicio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import edu.umg.guali.admin.modelo.Visita;
import edu.umg.guali.admin.repositorio.VisitaRepository;
import edu.umg.guali.admin.servicio.Dtos.Dia;
import edu.umg.guali.admin.servicio.Dtos.DiaLecciones;
import edu.umg.guali.admin.servicio.Dtos.Estadisticas;
import edu.umg.guali.admin.servicio.Dtos.EstadisticasLecciones;
import edu.umg.guali.admin.servicio.Dtos.Totales;
import edu.umg.guali.seguridad.SeguridadProperties;
import edu.umg.guali.seguridad.TokenInternoFilter;

/** Módulo de estadísticas: visitas (este servicio) + uso de lecciones (lecciones-service). */
@Service
public class EstadisticasService {

    public static final ZoneId ZONA = ZoneId.of("America/Guatemala");
    private static final Duration VENTANA_VISITA = Duration.ofMinutes(30);
    private static final long MAX_DIAS = 366;
    private static final Logger log = LoggerFactory.getLogger(EstadisticasService.class);

    private final VisitaRepository visitas;
    private final RestClient lecciones;
    private final Clock reloj;
    private final byte[] sal;

    public EstadisticasService(VisitaRepository visitas, RestClient.Builder builder, SeguridadProperties seguridad,
                               Clock reloj, @Value("${guali.lecciones.url}") String leccionesUrl,
                               @Value("${guali.visitas.sal}") String sal) {
        if (sal == null || sal.length() < 16) {
            throw new IllegalStateException("VISITAS_SAL debe tener al menos 16 caracteres");
        }
        this.visitas = visitas;
        this.reloj = reloj;
        this.sal = sal.getBytes(StandardCharsets.UTF_8);
        this.lecciones = builder.baseUrl(leccionesUrl)
                .defaultHeader(TokenInternoFilter.CABECERA, seguridad.tokenInterno())
                .build();
    }

    /** Registra una visita del navegador; recargas dentro de 30 minutos cuentan como la misma visita. */
    @Transactional
    public void registrarVisita(String clienteId) {
        Instant ahora = reloj.instant();
        String visitante = seudonimo(clienteId);
        if (visitas.existsByVisitanteAndCreadaEnAfter(visitante, ahora.minus(VENTANA_VISITA))) return;
        visitas.save(new Visita(visitante, ahora.atZone(ZONA).toLocalDate(), ahora));
    }

    @Transactional(readOnly = true)
    public Estadisticas entre(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new ReglaException(HttpStatus.BAD_REQUEST, "La fecha inicial debe ser anterior a la final.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) >= MAX_DIAS) {
            throw new ReglaException(HttpStatus.BAD_REQUEST, "El intervalo máximo es de un año.");
        }

        Map<LocalDate, long[]> porDiaVisitas = new HashMap<>();
        for (Object[] fila : visitas.porDia(desde, hasta)) {
            porDiaVisitas.put((LocalDate) fila[0], new long[]{(Long) fila[1], (Long) fila[2]});
        }

        EstadisticasLecciones deLecciones = null;
        try {
            deLecciones = lecciones.get()
                    .uri(u -> u.path("/interno/estadisticas").queryParam("desde", desde).queryParam("hasta", hasta).build())
                    .retrieve()
                    .body(EstadisticasLecciones.class);
        } catch (RestClientException e) {
            log.warn("No se pudieron obtener las estadísticas de lecciones: {}", e.getMessage());
        }
        Map<String, DiaLecciones> porDiaLecciones = new HashMap<>();
        if (deLecciones != null) deLecciones.porDia().forEach(d -> porDiaLecciones.put(d.fecha(), d));

        List<Dia> dias = new ArrayList<>();
        long totalVisitas = 0;
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            long[] v = porDiaVisitas.getOrDefault(d, new long[2]);
            DiaLecciones l = porDiaLecciones.get(d.toString());
            dias.add(new Dia(d.toString(), v[0], v[1], l == null ? 0 : l.abiertas(), l == null ? 0 : l.completadas()));
            totalVisitas += v[1];
        }

        Totales totales = new Totales(visitas.visitantesDistintos(desde, hasta), totalVisitas,
                deLecciones == null ? 0 : deLecciones.abiertas(),
                deLecciones == null ? 0 : deLecciones.completadas());
        return new Estadisticas(dias, totales, deLecciones == null ? List.of() : deLecciones.masAbiertas(),
                deLecciones == null);
    }

    public LocalDate hoy() {
        return reloj.instant().atZone(ZONA).toLocalDate();
    }

    /** SHA-256 con sal: permite contar visitantes distintos sin guardar su identificador real. */
    private String seudonimo(String clienteId) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(sal);
            return HexFormat.of().formatHex(sha.digest(clienteId.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
