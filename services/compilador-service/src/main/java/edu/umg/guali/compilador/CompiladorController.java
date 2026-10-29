package edu.umg.guali.compilador;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.umg.guali.compilador.Modelos.Respuesta;
import edu.umg.guali.compilador.Modelos.Solicitud;

/** Solo lo invoca lecciones-service por la red interna; no está publicado en el gateway. */
@RestController
@RequestMapping("/interno")
public class CompiladorController {

    private final ServicioCompilador compilador;

    public CompiladorController(ServicioCompilador compilador) {
        this.compilador = compilador;
    }

    @PostMapping("/procesar")
    public Respuesta procesar(@Valid @RequestBody Solicitud solicitud) {
        return compilador.procesar(solicitud.codigo(), "EJECUTAR".equals(solicitud.accion()));
    }
}
