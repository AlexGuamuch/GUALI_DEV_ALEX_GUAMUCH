package edu.umg.guali.lecciones.controlador;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.umg.guali.lecciones.dto.Dtos.EstadisticasLecciones;
import edu.umg.guali.lecciones.servicio.EstadisticasService;

/** API interna entre microservicios (no publicada en el gateway, protegida con token interno). */
@RestController
@RequestMapping("/interno")
public class InternoController {

    private final EstadisticasService estadisticas;

    public InternoController(EstadisticasService estadisticas) {
        this.estadisticas = estadisticas;
    }

    @GetMapping("/estadisticas")
    public EstadisticasLecciones estadisticas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return estadisticas.entre(desde, hasta);
    }
}
