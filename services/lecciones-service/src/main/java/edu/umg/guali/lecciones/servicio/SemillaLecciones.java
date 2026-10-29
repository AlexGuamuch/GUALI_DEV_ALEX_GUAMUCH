package edu.umg.guali.lecciones.servicio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import edu.umg.guali.lecciones.dto.Dtos.ResultadoImportacion;

/**
 * Si la base de datos no tiene lecciones, importa las lecciones de ejemplo empaquetadas en el servicio.
 * Pasan por el mismo importador que un .zip subido por un administrador.
 */
@Component
public class SemillaLecciones implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SemillaLecciones.class);
    private static final String PREFIJO = "semilla/";

    private final LeccionService lecciones;
    private final boolean activa;

    public SemillaLecciones(LeccionService lecciones, @Value("${guali.lecciones.semilla:true}") boolean activa) {
        this.lecciones = lecciones;
        this.activa = activa;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!activa || lecciones.hayLecciones()) return;

        Resource[] recursos = new PathMatchingResourcePatternResolver().getResources("classpath*:" + PREFIJO + "**/*");
        ByteArrayOutputStream zip = new ByteArrayOutputStream();
        int archivos = 0;
        try (ZipOutputStream out = new ZipOutputStream(zip, StandardCharsets.UTF_8)) {
            for (Resource recurso : recursos) {
                String url = recurso.getURL().toString();
                int i = url.lastIndexOf(PREFIJO);
                if (i < 0 || !recurso.isReadable() || url.endsWith("/")) continue;
                String ruta = url.substring(i + PREFIJO.length());
                if (!ruta.contains("/")) continue;
                try (InputStream in = recurso.getInputStream()) {
                    out.putNextEntry(new ZipEntry(ruta));
                    in.transferTo(out);
                    out.closeEntry();
                    archivos++;
                }
            }
        }
        if (archivos == 0) return;

        ResultadoImportacion r = lecciones.importar(new ByteArrayInputStream(zip.toByteArray()));
        log.info("Lecciones de ejemplo importadas: {} (rechazadas: {})", r.creadas(), r.rechazadas());
    }
}
