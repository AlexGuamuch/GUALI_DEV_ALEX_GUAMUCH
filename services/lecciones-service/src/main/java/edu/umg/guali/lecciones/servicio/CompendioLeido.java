package edu.umg.guali.lecciones.servicio;

import java.util.List;
import java.util.Map;

import edu.umg.guali.lecciones.modelo.Objetivo;

/** Resultado de leer y validar un compendio .zip, antes de guardarlo. */
public record CompendioLeido(List<LeccionEntrante> lecciones, List<Rechazo> rechazadas, List<String> advertencias) {

    /** Lección válida lista para guardarse. */
    public record LeccionEntrante(String nombre, Objetivo objetivo, String markdown, Map<String, Imagen> imagenes) {
    }

    /** Imagen validada por su firma binaria (no solo por la extensión). */
    public record Imagen(String nombre, String tipo, byte[] datos) {
    }

    /** Lección que no se importó y el motivo, para mostrarlo al administrador. */
    public record Rechazo(String leccion, String motivo) {
    }
}
