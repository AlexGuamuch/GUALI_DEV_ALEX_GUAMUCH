package edu.umg.guali.admin.servicio;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Objetos que viajan en la API REST (JSON). */
public final class Dtos {

    private Dtos() {
    }

    public record Credenciales(@NotBlank @Size(max = 30) String username, @NotBlank @Size(max = 128) String password) {
    }

    public record Sesion(boolean authenticated, String username, String fullName, Instant expiraEn) {
        public static Sesion anonima() {
            return new Sesion(false, null, null, null);
        }
    }

    public record UsuarioDto(long id, String username, String nombreCompleto, boolean activo,
                             Instant ultimoAcceso, Instant creadoEn) {
    }

    public record UsuarioForm(
            @NotBlank(message = "Escribe el nombre completo.")
            @Size(min = 3, max = 80, message = "El nombre debe tener entre 3 y 80 caracteres.") String nombreCompleto,
            @NotBlank(message = "Escribe el usuario.")
            @Pattern(regexp = "[a-z0-9._-]{3,30}",
                    message = "Usuario: de 3 a 30 caracteres en minúsculas, números, punto, guion o guion bajo.")
            String username,
            @Size(max = 128) String password,
            boolean activo) {
    }

    public record Estadisticas(List<Dia> porDia, Totales totales, List<LeccionPopular> masAbiertas, boolean parcial) {
    }

    public record Dia(String fecha, long visitantes, long visitas, long abiertas, long completadas) {
    }

    public record Totales(long visitantes, long visitas, long abiertas, long completadas) {
    }

    public record LeccionPopular(String nombre, long abiertas) {
    }

    /** Respuesta de lecciones-service (/interno/estadisticas). */
    public record EstadisticasLecciones(List<DiaLecciones> porDia, long abiertas, long completadas,
                                        List<LeccionPopular> masAbiertas) {
    }

    public record DiaLecciones(String fecha, long abiertas, long completadas) {
    }
}
