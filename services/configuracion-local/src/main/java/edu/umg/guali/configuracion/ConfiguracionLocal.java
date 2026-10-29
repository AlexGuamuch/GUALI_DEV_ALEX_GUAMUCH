package edu.umg.guali.configuracion;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * Configuración para ejecutar los microservicios en una PC, sin Docker.
 *
 * 1. Busca configuracion.properties (y configuracion.local.properties, si existe) en la carpeta del
 *    proyecto, subiendo desde la carpeta de trabajo: funciona igual desde IntelliJ, desde la terminal
 *    o con iniciar.bat. Ahí están la conexión a MariaDB y el administrador inicial.
 * 2. Si los secretos no vienen como variables de entorno (en Docker sí vienen), los genera una sola vez
 *    en la carpeta del usuario (~/.guali): tokens entre servicios y el par de llaves RSA de los JWT.
 *
 * Los valores quedan disponibles como propiedades db.*, admin.* y guali.local.*, que application.yml
 * usa solo cuando no hay una variable de entorno con prioridad.
 */
public class ConfiguracionLocal implements EnvironmentPostProcessor, Ordered {

    static final String ARCHIVO = "configuracion.properties";
    static final String ARCHIVO_PERSONAL = "configuracion.local.properties";
    private static final int NIVELES = 4;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        Map<String, Object> locales = new HashMap<>();

        Path raiz = buscarRaiz(Path.of(System.getProperty("user.dir")).toAbsolutePath());
        if (raiz != null) {
            // configuracion.local.properties (personal, no se sube a git) tiene prioridad sobre la general.
            agregar(env, raiz.resolve(ARCHIVO_PERSONAL));
            agregar(env, raiz.resolve(ARCHIVO));
            Path frontend = raiz.resolve("frontend");
            if (Files.isDirectory(frontend)) {
                locales.put("guali.local.frontend", frontend.toUri().toString());
            }
        }

        if (!secretosExternos(env)) {
            Path carpeta = Path.of(System.getProperty("user.home"), ".guali");
            SecretosLocales s = SecretosLocales.obtenerOCrear(carpeta);
            locales.put("guali.local.token-interno", s.tokenInterno());
            locales.put("guali.local.token-compilador", s.tokenCompilador());
            locales.put("guali.local.token-gateway", s.tokenGateway());
            locales.put("guali.local.visitas-sal", s.salVisitas());
            locales.put("guali.local.clave-publica", s.clavePublica().toUri().toString());
            locales.put("guali.local.clave-privada", s.clavePrivada().toUri().toString());
        }

        env.getPropertySources().addLast(new MapPropertySource("guali-local", locales));
    }

    /** En Docker cada servicio recibe sus secretos como variables de entorno: no se genera nada. */
    private static boolean secretosExternos(ConfigurableEnvironment env) {
        return Stream.of("TOKEN_INTERNO", "TOKEN_COMPILADOR", "TOKEN_GATEWAY").anyMatch(env::containsProperty);
    }

    static Path buscarRaiz(Path desde) {
        Path actual = desde;
        for (int i = 0; i <= NIVELES && actual != null; i++) {
            if (Files.isRegularFile(actual.resolve(ARCHIVO))) return actual;
            actual = actual.getParent();
        }
        return null;
    }

    private static void agregar(ConfigurableEnvironment env, Path archivo) {
        if (!Files.isRegularFile(archivo)) return;
        Properties propiedades = new Properties();
        try (Reader reader = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            propiedades.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + archivo, e);
        }
        // Sin espacios que se cuelan al editar a mano. Una línea vacía ("db.clave=") cuenta como "no definida":
        // así el archivo personal no borra por accidente un valor de configuracion.properties.
        propiedades.replaceAll((k, v) -> v.toString().strip());
        propiedades.values().removeIf(v -> v.toString().isEmpty());
        env.getPropertySources().addLast(new PropertiesPropertySource(archivo.getFileName().toString(), propiedades));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
