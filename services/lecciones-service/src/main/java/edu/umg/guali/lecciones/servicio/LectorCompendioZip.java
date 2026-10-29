package edu.umg.guali.lecciones.servicio;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Component;

import edu.umg.guali.lecciones.modelo.Objetivo;
import edu.umg.guali.lecciones.servicio.CompendioLeido.Imagen;
import edu.umg.guali.lecciones.servicio.CompendioLeido.LeccionEntrante;
import edu.umg.guali.lecciones.servicio.CompendioLeido.Rechazo;

/**
 * Lee un compendio de lecciones (.zip) y lo valida sin escribir nada en disco.
 *
 * Formato: cada lección es una carpeta (su nombre es el título) con leccion.md, config.txt
 * e imágenes opcionales. Las carpetas pueden venir en la raíz del zip o dentro de otra carpeta
 * (por ejemplo "lecciones/").
 *
 * Protecciones: límite de entradas y de bytes descomprimidos (zip bomb), rechazo de rutas
 * absolutas o con ".." (zip slip), validación de UTF-8 y de la firma binaria de cada imagen.
 */
@Component
public class LectorCompendioZip {

    static final int MAX_ENTRADAS = 1000;
    static final long MAX_DESCOMPRIMIDO = 60L * 1024 * 1024;
    static final int MAX_MARKDOWN = 256 * 1024;
    static final int MAX_CONFIG = 1024;
    static final int MAX_ARCHIVO = 3 * 1024 * 1024;
    static final int MAX_LECCIONES = 200;

    private static final Pattern NOMBRE_LECCION = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,99}");
    private static final Pattern NOMBRE_IMAGEN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,149}");
    private static final Pattern LINEA_OBJETIVO = Pattern.compile("(?i)objetivo\\s*=\\s*(\\S+)");
    private static final Set<String> EXTENSIONES_IMAGEN = Set.of("png", "jpg", "jpeg", "gif", "webp");
    private static final String VALORES_OBJETIVO = Arrays.stream(Objetivo.values())
            .map(Enum::name).collect(Collectors.joining(", "));

    public CompendioLeido leer(InputStream entrada) {
        BufferedInputStream in = new BufferedInputStream(entrada);
        verificarFirmaZip(in);

        Map<String, Map<String, byte[]>> porCarpeta = new LinkedHashMap<>();
        List<String> advertencias = new ArrayList<>();
        int entradas = 0;
        long total = 0;

        try (ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entradas > MAX_ENTRADAS) {
                    throw new ImportacionException("El zip tiene demasiados archivos (máximo " + MAX_ENTRADAS + ").");
                }
                String ruta = entry.getName().replace('\\', '/');
                validarRuta(ruta);
                if (entry.isDirectory()) continue;

                // Todo se lee con límite, incluso lo que se ignora: así se acota la descompresión total.
                byte[] datos = leerConLimite(zip, MAX_ARCHIVO, MAX_DESCOMPRIMIDO - total, ruta);
                total += datos.length;
                if (esIgnorable(ruta)) continue;

                int barra = ruta.lastIndexOf('/');
                String carpeta = barra < 0 ? "" : ruta.substring(0, barra);
                String archivo = ruta.substring(barra + 1);
                porCarpeta.computeIfAbsent(carpeta, k -> new LinkedHashMap<>()).put(archivo, datos);
            }
        } catch (ZipException | IllegalArgumentException e) {
            throw new ImportacionException(
                    "El archivo .zip está dañado o usa nombres de archivo con una codificación no soportada.");
        } catch (IOException e) {
            throw new ImportacionException("No se pudo leer el archivo .zip.");
        }

        return construir(porCarpeta, advertencias);
    }

    private CompendioLeido construir(Map<String, Map<String, byte[]>> porCarpeta, List<String> advertencias) {
        Set<String> carpetasLeccion = porCarpeta.entrySet().stream()
                .filter(e -> buscar(e.getValue(), "leccion.md") != null || buscar(e.getValue(), "config.txt") != null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        if (carpetasLeccion.contains("")) {
            throw new ImportacionException("leccion.md y config.txt deben ir dentro de una carpeta con el nombre "
                    + "de la lección, por ejemplo leccion-01-hola-mundo/leccion.md.");
        }
        if (carpetasLeccion.isEmpty()) {
            throw new ImportacionException("No se encontró ninguna lección. Cada lección debe ser una carpeta "
                    + "con leccion.md y config.txt.");
        }
        if (carpetasLeccion.size() > MAX_LECCIONES) {
            throw new ImportacionException("El compendio tiene más de " + MAX_LECCIONES + " lecciones.");
        }

        for (String carpeta : porCarpeta.keySet()) {
            if (carpetasLeccion.contains(carpeta)) continue;
            boolean dentroDeLeccion = carpetasLeccion.stream().anyMatch(l -> carpeta.startsWith(l + "/"));
            for (String archivo : porCarpeta.get(carpeta).keySet()) {
                String ruta = carpeta.isEmpty() ? archivo : carpeta + "/" + archivo;
                advertencias.add(dentroDeLeccion
                        ? "Se ignoró " + ruta + ": las imágenes deben estar directamente en la carpeta de la lección."
                        : "Se ignoró " + ruta + ": no pertenece a ninguna lección.");
            }
        }

        List<LeccionEntrante> validas = new ArrayList<>();
        List<Rechazo> rechazadas = new ArrayList<>();
        Set<String> vistos = new HashSet<>();

        for (String carpeta : carpetasLeccion) {
            String nombre = carpeta.substring(carpeta.lastIndexOf('/') + 1);
            try {
                if (!NOMBRE_LECCION.matcher(nombre).matches()) {
                    throw new ImportacionException("El nombre de la carpeta solo puede tener letras sin tilde, "
                            + "números, punto, guion y guion bajo (máximo 100 caracteres).");
                }
                if (!vistos.add(nombre.toLowerCase(Locale.ROOT))) {
                    throw new ImportacionException("Hay otra carpeta con el mismo nombre en el zip.");
                }
                validas.add(leerLeccion(nombre, porCarpeta.get(carpeta), advertencias));
            } catch (ImportacionException e) {
                rechazadas.add(new Rechazo(nombre, e.getMessage()));
            }
        }
        return new CompendioLeido(validas, rechazadas, advertencias);
    }

    private LeccionEntrante leerLeccion(String nombre, Map<String, byte[]> archivos, List<String> advertencias) {
        String nombreMd = buscar(archivos, "leccion.md");
        String nombreConfig = buscar(archivos, "config.txt");
        if (nombreMd == null) throw new ImportacionException("Falta el archivo leccion.md.");
        if (nombreConfig == null) throw new ImportacionException("Falta el archivo config.txt.");

        byte[] md = archivos.get(nombreMd);
        if (md.length > MAX_MARKDOWN) {
            throw new ImportacionException("leccion.md supera el máximo de " + (MAX_MARKDOWN / 1024) + " KB.");
        }
        String markdown = utf8(md, "leccion.md");
        if (markdown.isBlank()) throw new ImportacionException("leccion.md está vacío.");

        Objetivo objetivo = leerObjetivo(archivos.get(nombreConfig));

        Map<String, Imagen> imagenes = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> archivo : archivos.entrySet()) {
            String nombreArchivo = archivo.getKey();
            if (nombreArchivo.equals(nombreMd) || nombreArchivo.equals(nombreConfig)) continue;

            String extension = extension(nombreArchivo);
            if (!EXTENSIONES_IMAGEN.contains(extension)) {
                advertencias.add("Se ignoró " + nombre + "/" + nombreArchivo
                        + ": solo se aceptan imágenes PNG, JPG, GIF o WEBP.");
                continue;
            }
            if (!NOMBRE_IMAGEN.matcher(nombreArchivo).matches()) {
                throw new ImportacionException("La imagen \"" + nombreArchivo + "\" tiene un nombre no permitido: "
                        + "usa letras sin tilde, números, punto, guion y guion bajo.");
            }
            String tipo = tipoImagen(archivo.getValue());
            if (tipo == null) {
                throw new ImportacionException("\"" + nombreArchivo + "\" no es una imagen PNG, JPG, GIF o WEBP válida.");
            }
            imagenes.put(nombreArchivo, new Imagen(nombreArchivo, tipo, archivo.getValue()));
        }
        return new LeccionEntrante(nombre, objetivo, markdown, imagenes);
    }

    /** config.txt debe contener exactamente una línea objetivo=VALOR. */
    static Objetivo leerObjetivo(byte[] datos) {
        if (datos.length > MAX_CONFIG) throw new ImportacionException("config.txt es demasiado grande.");
        List<String> lineas = utf8(datos, "config.txt").lines()
                .map(String::strip)
                .filter(l -> !l.isEmpty())
                .toList();
        String ayuda = "config.txt debe contener exactamente una línea objetivo=VALOR, donde VALOR es "
                + VALORES_OBJETIVO + ".";
        if (lineas.size() != 1) throw new ImportacionException(ayuda);
        Matcher m = LINEA_OBJETIVO.matcher(lineas.getFirst());
        if (!m.matches()) throw new ImportacionException(ayuda);
        try {
            return Objetivo.valueOf(m.group(1).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ImportacionException("Objetivo desconocido \"" + m.group(1) + "\". " + ayuda);
        }
    }

    static String tipoImagen(byte[] d) {
        if (d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') return "image/png";
        if (d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (d.length > 6 && d[0] == 'G' && d[1] == 'I' && d[2] == 'F' && d[3] == '8') return "image/gif";
        if (d.length > 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') return "image/webp";
        return null;
    }

    private static void verificarFirmaZip(BufferedInputStream in) {
        try {
            in.mark(4);
            byte[] firma = in.readNBytes(4);
            in.reset();
            if (firma.length < 4 || firma[0] != 'P' || firma[1] != 'K' || firma[2] != 3 || firma[3] != 4) {
                throw new ImportacionException("El archivo no es un .zip válido.");
            }
        } catch (IOException e) {
            throw new ImportacionException("No se pudo leer el archivo .zip.");
        }
    }

    private static void validarRuta(String ruta) {
        boolean peligrosa = ruta.startsWith("/")
                || ruta.matches("^[A-Za-z]:.*")
                || ruta.indexOf('\0') >= 0
                || Arrays.asList(ruta.split("/")).contains("..");
        if (peligrosa) {
            throw new ImportacionException("El zip contiene rutas no permitidas (absolutas o con \"..\"). "
                    + "Por seguridad no se importó nada.");
        }
    }

    /** Archivos que crean macOS/Windows al comprimir y archivos ocultos. */
    private static boolean esIgnorable(String ruta) {
        for (String segmento : ruta.split("/")) {
            if (segmento.equals("__MACOSX") || segmento.startsWith(".")) return true;
        }
        String archivo = ruta.substring(ruta.lastIndexOf('/') + 1);
        return archivo.equalsIgnoreCase("Thumbs.db") || archivo.equalsIgnoreCase("desktop.ini");
    }

    private static byte[] leerConLimite(InputStream in, int maxArchivo, long restante, String ruta) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long leidos = 0;
        int n;
        while ((n = in.read(buffer)) != -1) {
            leidos += n;
            if (leidos > restante) {
                throw new ImportacionException("El contenido descomprimido del zip supera el máximo de "
                        + (MAX_DESCOMPRIMIDO / 1024 / 1024) + " MB.");
            }
            if (leidos > maxArchivo) {
                throw new ImportacionException("El archivo " + ruta + " supera el máximo de "
                        + (maxArchivo / 1024 / 1024) + " MB.");
            }
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private static String buscar(Map<String, byte[]> archivos, String nombre) {
        return archivos.keySet().stream().filter(a -> a.equalsIgnoreCase(nombre)).findFirst().orElse(null);
    }

    private static String utf8(byte[] datos, String archivo) {
        int inicio = datos.length >= 3 && (datos[0] & 0xFF) == 0xEF && (datos[1] & 0xFF) == 0xBB
                && (datos[2] & 0xFF) == 0xBF ? 3 : 0;
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(datos, inicio, datos.length - inicio))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new ImportacionException(archivo + " debe estar guardado en UTF-8.");
        }
    }

    private static String extension(String archivo) {
        int punto = archivo.lastIndexOf('.');
        return punto < 0 ? "" : archivo.substring(punto + 1).toLowerCase(Locale.ROOT);
    }
}
