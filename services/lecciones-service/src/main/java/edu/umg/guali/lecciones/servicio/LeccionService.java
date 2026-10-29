package edu.umg.guali.lecciones.servicio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.umg.guali.lecciones.dto.Dtos.LeccionAdmin;
import edu.umg.guali.lecciones.dto.Dtos.LeccionDetalle;
import edu.umg.guali.lecciones.dto.Dtos.LeccionResumen;
import edu.umg.guali.lecciones.dto.Dtos.ResultadoImportacion;
import edu.umg.guali.lecciones.modelo.Leccion;
import edu.umg.guali.lecciones.modelo.RecursoLeccion;
import edu.umg.guali.lecciones.repositorio.LeccionRepository;
import edu.umg.guali.lecciones.repositorio.RecursoLeccionRepository;
import edu.umg.guali.lecciones.servicio.CompendioLeido.Imagen;
import edu.umg.guali.lecciones.servicio.CompendioLeido.LeccionEntrante;

/** Casos de uso de lecciones: consultar, importar, exportar y eliminar. */
@Service
public class LeccionService {

    private final LeccionRepository lecciones;
    private final RecursoLeccionRepository recursos;
    private final LectorCompendioZip lector;
    private final RenderizadorMarkdown renderizador;
    private final Clock reloj;

    public LeccionService(LeccionRepository lecciones, RecursoLeccionRepository recursos,
                          LectorCompendioZip lector, RenderizadorMarkdown renderizador, Clock reloj) {
        this.lecciones = lecciones;
        this.recursos = recursos;
        this.lector = lector;
        this.renderizador = renderizador;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<LeccionResumen> listar() {
        return lecciones.findAllByOrderByNombreAsc().stream()
                .map(l -> new LeccionResumen(l.getId(), l.getNombre(), l.getObjetivo(), l.getResumen()))
                .toList();
    }

    @Transactional(readOnly = true)
    public LeccionDetalle obtener(long id) {
        Leccion l = buscar(id);
        return new LeccionDetalle(l.getId(), l.getNombre(), l.getObjetivo(), l.getResumen(), l.getContenidoHtml());
    }

    @Transactional(readOnly = true)
    public RecursoLeccion recurso(long leccionId, String archivo) {
        return recursos.findByLeccionIdAndNombreArchivo(leccionId, archivo)
                .orElseThrow(() -> new NoSuchElementException("La imagen no existe."));
    }

    @Transactional(readOnly = true)
    public List<LeccionAdmin> listarParaAdmin() {
        Map<Long, Long> imagenes = new HashMap<>();
        for (Object[] fila : recursos.contarPorLeccion()) {
            imagenes.put((Long) fila[0], (Long) fila[1]);
        }
        return lecciones.findAllByOrderByNombreAsc().stream()
                .map(l -> new LeccionAdmin(l.getId(), l.getNombre(), l.getObjetivo(),
                        imagenes.getOrDefault(l.getId(), 0L), l.getCreadaEn(), l.getActualizadaEn()))
                .toList();
    }

    /**
     * Importa un compendio. Las lecciones válidas se guardan (si ya existe una con el mismo nombre,
     * se reemplaza con la nueva versión); las inválidas se informan con su motivo.
     */
    @Transactional
    public ResultadoImportacion importar(InputStream zip) {
        CompendioLeido compendio = lector.leer(zip);
        List<String> creadas = new ArrayList<>();
        List<String> actualizadas = new ArrayList<>();

        for (LeccionEntrante entrante : compendio.lecciones()) {
            boolean existe = guardar(entrante);
            (existe ? actualizadas : creadas).add(entrante.nombre());
        }
        return new ResultadoImportacion(creadas, actualizadas, compendio.rechazadas(), compendio.advertencias());
    }

    private boolean guardar(LeccionEntrante entrante) {
        Instant ahora = reloj.instant();
        Leccion leccion = lecciones.findByNombre(entrante.nombre()).orElse(null);
        boolean existe = leccion != null;

        if (existe) {
            // Se borran las imágenes anteriores antes de insertar las nuevas (mismo nombre de archivo).
            leccion.getRecursos().clear();
            lecciones.saveAndFlush(leccion);
        } else {
            leccion = new Leccion();
            leccion.setNombre(entrante.nombre());
            leccion.setCreadaEn(ahora);
        }

        leccion.setObjetivo(entrante.objetivo());
        leccion.setContenidoMd(entrante.markdown());
        leccion.setResumen(renderizador.resumen(entrante.markdown()));
        leccion.setActualizadaEn(ahora);
        leccion.setContenidoHtml("");
        for (Imagen imagen : entrante.imagenes().values()) {
            leccion.agregarRecurso(new RecursoLeccion(imagen.nombre(), imagen.tipo(), imagen.datos()));
        }
        leccion = lecciones.saveAndFlush(leccion);

        // El HTML necesita el id de la lección para las rutas de sus imágenes.
        leccion.setContenidoHtml(renderizador.html(entrante.markdown(), leccion.getId(),
                Set.copyOf(entrante.imagenes().keySet())));
        return existe;
    }

    /** Exporta todas las lecciones con el mismo formato que acepta la importación. */
    @Transactional(readOnly = true)
    public byte[] exportar() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            for (Leccion l : lecciones.findAllByOrderByNombreAsc()) {
                String carpeta = "lecciones/" + l.getNombre() + "/";
                escribir(zip, carpeta + "leccion.md", l.getContenidoMd().getBytes(StandardCharsets.UTF_8));
                escribir(zip, carpeta + "config.txt",
                        ("objetivo=" + l.getObjetivo().name() + "\n").getBytes(StandardCharsets.UTF_8));
                for (RecursoLeccion r : l.getRecursos()) {
                    escribir(zip, carpeta + r.getNombreArchivo(), r.getDatos());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el zip de exportación", e);
        }
        return bytes.toByteArray();
    }

    @Transactional
    public String eliminar(long id) {
        Leccion l = buscar(id);
        String nombre = l.getNombre();
        lecciones.delete(l);
        return nombre;
    }

    @Transactional(readOnly = true)
    public boolean hayLecciones() {
        return lecciones.count() > 0;
    }

    Leccion buscar(long id) {
        return lecciones.findById(id).orElseThrow(() -> new NoSuchElementException("La lección no existe."));
    }

    private static void escribir(ZipOutputStream zip, String ruta, byte[] datos) throws IOException {
        zip.putNextEntry(new ZipEntry(ruta));
        zip.write(datos);
        zip.closeEntry();
    }
}
