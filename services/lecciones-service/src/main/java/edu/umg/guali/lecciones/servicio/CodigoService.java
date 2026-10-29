package edu.umg.guali.lecciones.servicio;

import org.springframework.stereotype.Service;

import edu.umg.guali.lecciones.dto.Dtos.RespuestaCodigo;
import edu.umg.guali.lecciones.dto.Dtos.SolicitudCodigo;
import edu.umg.guali.lecciones.servicio.EvaluadorObjetivo.Accion;

/**
 * Compilar y Ejecutar: delega en el microservicio compilador y evalúa en el servidor
 * si se cumplió el objetivo de la lección activa (el navegador no decide).
 * La llamada al compilador ocurre fuera de cualquier transacción para no retener conexiones a la base de datos.
 */
@Service
public class CodigoService {

    private final ClienteCompilador compilador;
    private final IntentoService intentos;

    public CodigoService(ClienteCompilador compilador, IntentoService intentos) {
        this.compilador = compilador;
        this.intentos = intentos;
    }

    public RespuestaCodigo procesar(String clienteId, SolicitudCodigo solicitud, Accion accion) {
        RespuestaCodigo resultado = compilador.procesar(solicitud.codigo(), accion);
        boolean cumplido = intentos.evaluar(clienteId, solicitud.intentoId(), accion, resultado);
        return new RespuestaCodigo(resultado.compilacion(), resultado.ejecucion(), cumplido);
    }
}
