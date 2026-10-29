package edu.umg.guali.lecciones.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.umg.guali.lecciones.dto.Dtos.IntentoDto;
import edu.umg.guali.lecciones.dto.Dtos.RespuestaCodigo;
import edu.umg.guali.lecciones.modelo.Intento;
import edu.umg.guali.lecciones.repositorio.IntentoRepository;

/** Historial de sesión: intentos por navegador, que sobreviven entre visitas. */
@Service
public class IntentoService {

    private static final int MAX_HISTORIAL = 200;
    private static final Duration ABANDONO = Duration.ofHours(6);

    private final IntentoRepository intentos;
    private final LeccionService lecciones;
    private final Clock reloj;

    public IntentoService(IntentoRepository intentos, LeccionService lecciones, Clock reloj) {
        this.intentos = intentos;
        this.lecciones = lecciones;
        this.reloj = reloj;
    }

    /** Abrir una lección cierra cualquier otro intento abierto del mismo navegador (queda Pendiente). */
    @Transactional
    public IntentoDto iniciar(String clienteId, long leccionId) {
        Instant ahora = reloj.instant();
        intentos.findByClienteIdAndFechaFinIsNull(clienteId).forEach(i -> i.cerrar(ahora));
        Intento nuevo = intentos.save(new Intento(clienteId, lecciones.buscar(leccionId), ahora));
        return dto(nuevo);
    }

    /** Cerrar la lección o abandonarla (cerrar la pestaña). */
    @Transactional
    public IntentoDto finalizar(String clienteId, long intentoId) {
        Intento intento = intentos.findByIdAndClienteId(intentoId, clienteId)
                .orElseThrow(() -> new NoSuchElementException("El intento no existe."));
        intento.cerrar(reloj.instant());
        return dto(intento);
    }

    @Transactional(readOnly = true)
    public List<IntentoDto> historial(String clienteId) {
        return intentos.findByClienteIdOrderByFechaInicioDesc(clienteId, Limit.of(MAX_HISTORIAL)).stream()
                .map(IntentoService::dto)
                .toList();
    }

    /**
     * Después de compilar o ejecutar: si el intento sigue abierto y la acción cumple su objetivo,
     * queda Completada con la fecha final. Devuelve true solo la primera vez que se cumple.
     */
    @Transactional
    public boolean evaluar(String clienteId, Long intentoId, EvaluadorObjetivo.Accion accion, RespuestaCodigo resultado) {
        if (intentoId == null) return false;
        Optional<Intento> abierto = intentos.findByIdAndClienteId(intentoId, clienteId).filter(Intento::estaAbierto);
        if (abierto.isEmpty() || !EvaluadorObjetivo.cumple(abierto.get().getObjetivo(), accion, resultado)) {
            return false;
        }
        abierto.get().completar(reloj.instant());
        return true;
    }

    @Scheduled(fixedDelayString = "PT30M", initialDelayString = "PT1M")
    @Transactional
    public void cerrarAbandonados() {
        Instant ahora = reloj.instant();
        intentos.cerrarAbandonados(ahora.minus(ABANDONO), ahora);
    }

    static IntentoDto dto(Intento i) {
        return new IntentoDto(i.getId(), i.getLeccionNombre(), i.getObjetivo(), i.getFechaInicio(),
                i.getFechaFin(), i.getEstado());
    }
}
