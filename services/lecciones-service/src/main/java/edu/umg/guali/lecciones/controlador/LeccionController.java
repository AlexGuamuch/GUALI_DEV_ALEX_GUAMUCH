package edu.umg.guali.lecciones.controlador;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.umg.guali.lecciones.dto.Dtos.LeccionDetalle;
import edu.umg.guali.lecciones.dto.Dtos.LeccionResumen;
import edu.umg.guali.lecciones.modelo.RecursoLeccion;
import edu.umg.guali.lecciones.servicio.LeccionService;

/** Consulta pública de lecciones (estudiantes, sin autenticación). */
@RestController
@RequestMapping("/api/lecciones")
public class LeccionController {

    private final LeccionService lecciones;

    public LeccionController(LeccionService lecciones) {
        this.lecciones = lecciones;
    }

    @GetMapping
    public List<LeccionResumen> listar() {
        return lecciones.listar();
    }

    @GetMapping("/{id}")
    public LeccionDetalle obtener(@PathVariable long id) {
        return lecciones.obtener(id);
    }

    /** Imágenes de la lección guardadas en la base de datos. */
    @GetMapping("/{id}/recursos/{archivo:[A-Za-z0-9][A-Za-z0-9._-]{0,149}}")
    public ResponseEntity<byte[]> recurso(@PathVariable long id, @PathVariable String archivo) {
        RecursoLeccion recurso = lecciones.recurso(id, archivo);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(recurso.getTipoContenido()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .header("Content-Disposition", ContentDisposition.inline().filename(archivo).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(recurso.getDatos());
    }
}
