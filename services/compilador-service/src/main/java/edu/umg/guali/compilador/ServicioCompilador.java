package edu.umg.guali.compilador;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.umg.guali.compilador.Modelos.Compilacion;
import edu.umg.guali.compilador.Modelos.Ejecucion;
import edu.umg.guali.compilador.Modelos.Respuesta;
import edu.umg.guali.compiler.CompileResult;
import edu.umg.guali.compiler.ExecuteResult;
import edu.umg.guali.compiler.GualiDev;

/**
 * Usa guali-dev.jar para compilar y ejecutar.
 *
 * guali-dev escribe en una única carpeta temporal compartida (java.io.tmpdir/guali-dev) y usa el nombre
 * fijo "app25" para la sintaxis compacta, así que dos solicitudes simultáneas se pisarían. Por eso:
 * las solicitudes se atienden de una en una (fila justa), la carpeta se limpia antes de cada una y
 * "Ejecutar" vuelve a compilar el mismo código dentro del mismo turno.
 */
@Service
public class ServicioCompilador {

    private static final Pattern CLASE_PUBLICA = Pattern.compile("public\\s+class\\s+(\\w+)");
    private static final Pattern CLASE = Pattern.compile("\\bclass\\s+(\\w+)");
    private static final Pattern AVISO_JVM = Pattern.compile("(?m)^(NOTE: )?Picked up (JAVA_TOOL_OPTIONS|JDK_JAVA_OPTIONS):.*\\R?");

    private final ReentrantLock turno = new ReentrantLock(true);
    private final CompiladorProperties props;
    private final Path carpetaTemporal = Path.of(System.getProperty("java.io.tmpdir"), "guali-dev");

    public ServicioCompilador(CompiladorProperties props) {
        this.props = props;
    }

    public Respuesta procesar(String codigo, boolean ejecutar) {
        boolean tengoTurno;
        try {
            tengoTurno = turno.tryLock(props.esperaMaximaSegundos(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ocupado();
        }
        if (!tengoTurno) throw ocupado();

        try {
            limpiarCarpetaTemporal();
            long inicio = System.nanoTime();
            CompileResult cr = GualiDev.compile(codigo, props.jdkHome());
            Compilacion compilacion = new Compilacion(cr.isExitoso(), limpiar(cr.getSalida()),
                    archivo(cr, codigo), milis(inicio));

            if (!ejecutar || !cr.isExitoso()) return new Respuesta(compilacion, null);

            inicio = System.nanoTime();
            ExecuteResult er = GualiDev.execute(cr.getNombreClase(), props.jdkHome(), props.tiempoLimiteSegundos());
            Ejecucion ejecucion = new Ejecucion(er.isExitoso(), recortar(er.getSalida()), limpiar(er.getErrores()),
                    er.isTimeoutAlcanzado(), milis(inicio));
            return new Respuesta(compilacion, ejecucion);
        } finally {
            terminarProcesosHuerfanos();
            limpiarCarpetaTemporal();
            turno.unlock();
        }
    }

    /**
     * Un programa puede lanzar otros procesos (Runtime.exec) que sobreviven cuando guali-dev termina el
     * suyo. Dentro del contenedor solo deben existir init (PID 1) y este servicio: se termina todo lo demás
     * y se borra lo que el programa haya dejado en /tmp.
     */
    private void terminarProcesosHuerfanos() {
        if (!props.aislado()) return;
        // Solo procesos colgados de init o de este servicio (los de "docker exec", como el healthcheck, no).
        long propio = ProcessHandle.current().pid();
        ProcessHandle.allProcesses()
                .filter(p -> p.pid() != propio && p.pid() != 1)
                .filter(p -> p.parent().map(padre -> padre.pid() == 1 || padre.pid() == propio).orElse(false))
                .forEach(p -> {
                    p.descendants().forEach(ProcessHandle::destroyForcibly);
                    p.destroyForcibly();
                });

        Path tmp = Path.of("/tmp");
        try (Stream<Path> entradas = Files.list(tmp)) {
            entradas.filter(p -> {
                        String nombre = p.getFileName().toString();
                        return !nombre.startsWith("tomcat") && !nombre.startsWith("hsperfdata") && !nombre.equals("guali-dev");
                    })
                    .forEach(ServicioCompilador::borrar);
        } catch (IOException ignorado) {
            // Se intentará de nuevo en la siguiente solicitud.
        }
    }

    private static void borrar(Path raiz) {
        try (Stream<Path> todo = Files.walk(raiz)) {
            todo.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignorado) {
            // Archivo en uso o ya borrado.
        }
    }

    /**
     * Las rutas internas del servidor no se muestran al estudiante (solo el nombre del archivo),
     * ni los avisos que la JVM imprime cuando se configuran opciones por variables de entorno.
     */
    private String limpiar(String texto) {
        if (texto == null) return "";
        String sinRutas = texto.replace(carpetaTemporal + java.io.File.separator, "")
                .replace(carpetaTemporal.toString(), "");
        String sinAvisos = AVISO_JVM.matcher(sinRutas).replaceAll("");
        return recortar(sinAvisos);
    }

    private String recortar(String texto) {
        if (texto == null) return "";
        int max = props.salidaMaximaCaracteres();
        return texto.length() <= max ? texto : texto.substring(0, max) + "\n… (salida recortada)";
    }

    private static String archivo(CompileResult cr, String codigo) {
        if (cr.getNombreClase() != null) return cr.getNombreClase() + ".java";
        Matcher m = CLASE_PUBLICA.matcher(codigo);
        if (m.find()) return m.group(1) + ".java";
        m = CLASE.matcher(codigo);
        return (m.find() ? m.group(1) : "app25") + ".java";
    }

    private void limpiarCarpetaTemporal() {
        if (!Files.isDirectory(carpetaTemporal)) return;
        try (Stream<Path> archivos = Files.walk(carpetaTemporal)) {
            archivos.sorted(Comparator.reverseOrder())
                    .filter(p -> !p.equals(carpetaTemporal))
                    .forEach(p -> p.toFile().delete());
        } catch (IOException ignorado) {
            // La siguiente solicitud lo intentará de nuevo.
        }
    }

    private static long milis(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000;
    }

    private static ResponseStatusException ocupado() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El compilador está ocupado.");
    }
}
