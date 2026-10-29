package edu.umg.guali.lecciones.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import edu.umg.guali.lecciones.modelo.EstadoIntento;
import edu.umg.guali.lecciones.modelo.Objetivo;
import edu.umg.guali.lecciones.servicio.CompendioLeido.Rechazo;

/** Objetos que viajan en la API REST (JSON). */
public final class Dtos {

    private Dtos() {
    }

    public record LeccionResumen(long id, String nombre, Objetivo objetivo, String resumen) {
    }

    public record LeccionDetalle(long id, String nombre, Objetivo objetivo, String resumen, String html) {
    }

    public record LeccionAdmin(long id, String nombre, Objetivo objetivo, long imagenes,
                               Instant creadaEn, Instant actualizadaEn) {
    }

    public record ResultadoImportacion(List<String> creadas, List<String> actualizadas,
                                       List<Rechazo> rechazadas, List<String> advertencias) {
    }

    public record IntentoDto(long id, String leccionNombre, Objetivo objetivo, Instant fechaInicio,
                             Instant fechaFin, EstadoIntento estado) {
    }

    public record NuevoIntento(@NotNull Long leccionId) {
    }

    public record SolicitudCodigo(
            @NotNull @Size(max = 20_000, message = "El código no puede superar 20 000 caracteres.") String codigo,
            Long intentoId) {
    }

    public record PreferenciaDto(
            @NotNull @Pattern(regexp = "light|dark") String tema,
            @Min(12) @Max(24) int tamanoFuente) {
    }

    /** Respuesta de Compilar y Ejecutar. "ejecucion" es null cuando solo se compiló. */
    public record RespuestaCodigo(Compilacion compilacion, Ejecucion ejecucion, boolean objetivoCumplido) {
    }

    public record Compilacion(boolean exitosa, String salida, String archivo, long duracionMs) {
    }

    public record Ejecucion(boolean exitosa, String salida, String errores, boolean tiempoAgotado, long duracionMs) {
    }

    public record EstadisticasLecciones(List<DiaLecciones> porDia, long abiertas, long completadas,
                                        List<LeccionPopular> masAbiertas) {
    }

    public record DiaLecciones(String fecha, long abiertas, long completadas) {
    }

    public record LeccionPopular(String nombre, long abiertas) {
    }
}
