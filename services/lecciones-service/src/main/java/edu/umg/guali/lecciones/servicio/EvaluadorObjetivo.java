package edu.umg.guali.lecciones.servicio;

import edu.umg.guali.lecciones.dto.Dtos.RespuestaCodigo;
import edu.umg.guali.lecciones.modelo.Objetivo;

/** Decide si la acción del estudiante cumple el objetivo de la lección (tabla del enunciado). */
public final class EvaluadorObjetivo {

    public enum Accion { COMPILAR, EJECUTAR }

    private EvaluadorObjetivo() {
    }

    public static boolean cumple(Objetivo objetivo, Accion accion, RespuestaCodigo r) {
        boolean compilo = r.compilacion() != null && r.compilacion().exitosa();
        return switch (objetivo) {
            case COMPILAR_CON_ERROR -> accion == Accion.COMPILAR && r.compilacion() != null && !compilo;
            case COMPILAR_EXITOSO -> accion == Accion.COMPILAR && compilo;
            // Un programa detenido por tiempo límite no cuenta como error de ejecución.
            case EJECUTAR_CON_ERROR -> accion == Accion.EJECUTAR && compilo && r.ejecucion() != null
                    && !r.ejecucion().exitosa() && !r.ejecucion().tiempoAgotado();
            case EJECUTAR_EXITOSO -> accion == Accion.EJECUTAR && compilo && r.ejecucion() != null
                    && r.ejecucion().exitosa();
        };
    }
}
