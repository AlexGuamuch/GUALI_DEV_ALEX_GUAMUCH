package edu.umg.guali.compilador;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Contrato JSON interno entre lecciones-service y compilador-service. */
public final class Modelos {

    private Modelos() {
    }

    public record Solicitud(
            @NotNull @Size(max = 20_000) String codigo,
            @NotNull @Pattern(regexp = "COMPILAR|EJECUTAR") String accion) {
    }

    public record Respuesta(Compilacion compilacion, Ejecucion ejecucion) {
    }

    public record Compilacion(boolean exitosa, String salida, String archivo, long duracionMs) {
    }

    public record Ejecucion(boolean exitosa, String salida, String errores, boolean tiempoAgotado, long duracionMs) {
    }
}
