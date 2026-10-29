package edu.umg.guali.admin.controlador;

import java.time.LocalDate;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.umg.guali.admin.servicio.Dtos.Estadisticas;
import edu.umg.guali.admin.servicio.EstadisticasService;
import edu.umg.guali.seguridad.ClienteIdResolver;

/** Registro de visitas (público) y módulo de estadísticas de uso (administradores). */
@RestController
public class EstadisticasController {

    private final EstadisticasService estadisticas;
    private final ClienteIdResolver clientes;

    public EstadisticasController(EstadisticasService estadisticas, ClienteIdResolver clientes) {
        this.estadisticas = estadisticas;
        this.clientes = clientes;
    }

    @PostMapping("/api/visitas")
    public ResponseEntity<Void> visita(HttpServletRequest request) {
        estadisticas.registrarVisita(clientes.resolver(request));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/estadisticas")
    public Estadisticas entre(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : estadisticas.hoy();
        LocalDate inicio = desde != null ? desde : fin.minusDays(29);
        return estadisticas.entre(inicio, fin);
    }
}
