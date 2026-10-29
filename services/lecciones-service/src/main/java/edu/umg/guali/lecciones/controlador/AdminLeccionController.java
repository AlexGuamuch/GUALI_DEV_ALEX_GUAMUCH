package edu.umg.guali.lecciones.controlador;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.umg.guali.lecciones.dto.Dtos.LeccionAdmin;
import edu.umg.guali.lecciones.dto.Dtos.ResultadoImportacion;
import edu.umg.guali.lecciones.servicio.ImportacionException;
import edu.umg.guali.lecciones.servicio.LeccionService;

/** Gestión de lecciones: solo administradores autenticados (rol ADMIN). */
@RestController
@RequestMapping("/api/admin/lecciones")
public class AdminLeccionController {

    private static final Logger auditoria = LoggerFactory.getLogger("auditoria");

    private final LeccionService lecciones;

    public AdminLeccionController(LeccionService lecciones) {
        this.lecciones = lecciones;
    }

    @GetMapping
    public List<LeccionAdmin> listar() {
        return lecciones.listarParaAdmin();
    }

    /** Importar un compendio (bundle) .zip con una o más lecciones. */
    @PostMapping(path = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultadoImportacion importar(@RequestParam("archivo") MultipartFile archivo,
                                         @AuthenticationPrincipal Jwt admin) throws IOException {
        if (archivo.isEmpty()) throw new ImportacionException("Selecciona un archivo .zip.");
        try (InputStream in = archivo.getInputStream()) {
            ResultadoImportacion r = lecciones.importar(in);
            auditoria.info("{} importó {}: creadas={} actualizadas={} rechazadas={}", admin.getSubject(),
                    archivo.getOriginalFilename(), r.creadas().size(), r.actualizadas().size(), r.rechazadas().size());
            return r;
        }
    }

    /** Exportar y descargar todas las lecciones en un .zip con el mismo formato de importación. */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@AuthenticationPrincipal Jwt admin) {
        byte[] zip = lecciones.exportar();
        auditoria.info("{} exportó las lecciones ({} bytes)", admin.getSubject(), zip.length);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("lecciones-" + LocalDate.now() + ".zip").build().toString())
                .body(zip);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable long id, @AuthenticationPrincipal Jwt admin) {
        String nombre = lecciones.eliminar(id);
        auditoria.info("{} eliminó la lección {}", admin.getSubject(), nombre);
        return ResponseEntity.noContent().build();
    }
}
