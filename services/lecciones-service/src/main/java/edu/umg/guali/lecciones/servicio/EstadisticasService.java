package edu.umg.guali.lecciones.servicio;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.umg.guali.lecciones.dto.Dtos.DiaLecciones;
import edu.umg.guali.lecciones.dto.Dtos.EstadisticasLecciones;
import edu.umg.guali.lecciones.dto.Dtos.LeccionPopular;
import edu.umg.guali.lecciones.modelo.EstadoIntento;
import edu.umg.guali.lecciones.repositorio.IntentoRepository;
import edu.umg.guali.lecciones.repositorio.IntentoRepository.IntentoResumen;

/** Agregados de uso de lecciones para el módulo de estadísticas (consumido por admin-service). */
@Service
public class EstadisticasService {

    public static final ZoneId ZONA = ZoneId.of("America/Guatemala");

    private final IntentoRepository intentos;

    public EstadisticasService(IntentoRepository intentos) {
        this.intentos = intentos;
    }

    @Transactional(readOnly = true)
    public EstadisticasLecciones entre(LocalDate desde, LocalDate hasta) {
        List<IntentoResumen> filas = intentos.resumenEntre(
                desde.atStartOfDay(ZONA).toInstant(), hasta.plusDays(1).atStartOfDay(ZONA).toInstant());

        Map<LocalDate, long[]> porDia = new TreeMap<>();
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) porDia.put(d, new long[2]);
        Map<String, Long> porLeccion = new HashMap<>();
        long completadas = 0;

        for (IntentoResumen fila : filas) {
            long[] dia = porDia.get(fila.getFechaInicio().atZone(ZONA).toLocalDate());
            boolean completada = fila.getEstado() == EstadoIntento.COMPLETADA;
            if (dia != null) {
                dia[0]++;
                if (completada) dia[1]++;
            }
            if (completada) completadas++;
            porLeccion.merge(fila.getLeccionNombre(), 1L, Long::sum);
        }

        List<DiaLecciones> dias = new ArrayList<>();
        porDia.forEach((fecha, v) -> dias.add(new DiaLecciones(fecha.toString(), v[0], v[1])));
        List<LeccionPopular> top = porLeccion.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(e -> new LeccionPopular(e.getKey(), e.getValue()))
                .toList();
        return new EstadisticasLecciones(dias, filas.size(), completadas, top);
    }
}
