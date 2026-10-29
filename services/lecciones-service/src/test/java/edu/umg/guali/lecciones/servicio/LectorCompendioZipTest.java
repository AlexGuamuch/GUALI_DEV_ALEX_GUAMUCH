package edu.umg.guali.lecciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

import edu.umg.guali.lecciones.modelo.Objetivo;

class LectorCompendioZipTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    private final LectorCompendioZip lector = new LectorCompendioZip();

    @Test
    void leeLeccionesValidasEIgnoraArchivosDeMacOs() throws IOException {
        Map<String, byte[]> archivos = new LinkedHashMap<>();
        archivos.put("lecciones/leccion-01-hola-mundo/leccion.md", texto("# Hola\n\n![x](imagen.png)"));
        archivos.put("lecciones/leccion-01-hola-mundo/config.txt", texto("objetivo=EJECUTAR_EXITOSO\n"));
        archivos.put("lecciones/leccion-01-hola-mundo/imagen.png", PNG);
        archivos.put("lecciones/leccion-02-errores/leccion.md", texto("# Errores"));
        archivos.put("lecciones/leccion-02-errores/config.txt", texto("﻿ objetivo = compilar_con_error \r\n\r\n"));
        archivos.put("__MACOSX/lecciones/._leccion.md", texto("basura"));
        archivos.put("lecciones/.DS_Store", texto("basura"));

        CompendioLeido r = lector.leer(zip(archivos));

        assertThat(r.rechazadas()).isEmpty();
        assertThat(r.lecciones()).extracting(CompendioLeido.LeccionEntrante::nombre)
                .containsExactly("leccion-01-hola-mundo", "leccion-02-errores");
        assertThat(r.lecciones().get(0).imagenes()).containsKey("imagen.png");
        assertThat(r.lecciones().get(0).imagenes().get("imagen.png").tipo()).isEqualTo("image/png");
        assertThat(r.lecciones().get(1).objetivo()).isEqualTo(Objetivo.COMPILAR_CON_ERROR);
    }

    @Test
    void rechazaLeccionesIncompletasConMotivo() throws IOException {
        Map<String, byte[]> archivos = new LinkedHashMap<>();
        archivos.put("sin-config/leccion.md", texto("# Hola"));
        archivos.put("objetivo-malo/leccion.md", texto("# Hola"));
        archivos.put("objetivo-malo/config.txt", texto("objetivo=APROBAR"));
        archivos.put("imagen-falsa/leccion.md", texto("# Hola"));
        archivos.put("imagen-falsa/config.txt", texto("objetivo=COMPILAR_EXITOSO"));
        archivos.put("imagen-falsa/foto.png", texto("<svg onload=alert(1)>"));
        archivos.put("con-tilde-ó/leccion.md", texto("# Hola"));
        archivos.put("con-tilde-ó/config.txt", texto("objetivo=COMPILAR_EXITOSO"));

        CompendioLeido r = lector.leer(zip(archivos));

        assertThat(r.lecciones()).isEmpty();
        assertThat(r.rechazadas()).extracting(CompendioLeido.Rechazo::leccion)
                .containsExactlyInAnyOrder("sin-config", "objetivo-malo", "imagen-falsa", "con-tilde-ó");
        assertThat(r.rechazadas()).anySatisfy(x -> assertThat(x.motivo()).contains("config.txt"));
        assertThat(r.rechazadas()).anySatisfy(x -> assertThat(x.motivo()).contains("APROBAR"));
    }

    @Test
    void rechazaRutasConPuntoPunto() {
        Map<String, byte[]> archivos = Map.of("../../etc/leccion.md", texto("x"));
        assertThatThrownBy(() -> lector.leer(zip(archivos)))
                .isInstanceOf(ImportacionException.class)
                .hasMessageContaining("rutas no permitidas");
    }

    @Test
    void rechazaArchivosQueNoSonZip() {
        assertThatThrownBy(() -> lector.leer(new ByteArrayInputStream(texto("no soy un zip"))))
                .isInstanceOf(ImportacionException.class)
                .hasMessageContaining("no es un .zip");
    }

    @Test
    void detieneZipBombAntesDeDescomprimirTodo() {
        // 80 MB de ceros comprimen a unos pocos KB.
        Map<String, byte[]> archivos = new LinkedHashMap<>();
        for (int i = 0; i < 30; i++) archivos.put("bomba/relleno-" + i + ".png", new byte[3 * 1024 * 1024 - 1]);
        assertThatThrownBy(() -> lector.leer(zip(archivos)))
                .isInstanceOf(ImportacionException.class)
                .hasMessageContaining("descomprimido");
    }

    @Test
    void exigeQueLasLeccionesEstenEnCarpetas() {
        Map<String, byte[]> archivos = new LinkedHashMap<>();
        archivos.put("leccion.md", texto("# Hola"));
        archivos.put("config.txt", texto("objetivo=COMPILAR_EXITOSO"));
        assertThatThrownBy(() -> lector.leer(zip(archivos)))
                .isInstanceOf(ImportacionException.class)
                .hasMessageContaining("dentro de una carpeta");
    }

    private static byte[] texto(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static ByteArrayInputStream zip(Map<String, byte[]> archivos) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, byte[]> e : archivos.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue());
                zip.closeEntry();
            }
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }
}
