package edu.umg.guali.configuracion;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.env.Environment;

/**
 * Explica en español por qué no se pudo conectar a MariaDB al arrancar, en lugar de una traza larga.
 * Indica con qué servidor, puerto y usuario se intentó y qué archivo hay que corregir.
 */
public class ConexionMariaDbFailureAnalyzer extends AbstractFailureAnalyzer<SQLException> {

    private static final Pattern URL = Pattern.compile("jdbc:mariadb://([^:/]+)(?::(\\d+))?/([^?]+)");
    private static final String ARCHIVOS = "configuracion.properties (o configuracion.local.properties)";

    private final Environment env;

    public ConexionMariaDbFailureAnalyzer(Environment env) {
        this.env = env;
    }

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, SQLException cause) {
        String estado = cause.getSQLState() == null ? "" : cause.getSQLState();
        String mensaje = String.valueOf(cause.getMessage());
        String destino = destino();
        String usuario = env.getProperty("spring.datasource.username", "?");

        if (estado.startsWith("28") || mensaje.contains("Access denied") || mensaje.contains("authentication")) {
            return new FailureAnalysis(
                    "MariaDB rechazó el usuario o la contraseña.\n\n"
                            + "    Servidor: " + destino + "\n"
                            + "    Usuario:  " + usuario + "\n"
                            + "    Detalle:  " + mensaje,
                    "Revisa db.usuario y db.clave en " + ARCHIVOS + " con la contraseña que pusiste al "
                            + "instalar MariaDB. Una línea vacía como \"db.clave=\" deja la contraseña en blanco.",
                    cause);
        }
        if (estado.startsWith("08") || mensaje.contains("Connection refused") || mensaje.contains("Socket fail")) {
            return new FailureAnalysis(
                    "No hay un servidor MariaDB respondiendo en " + destino + ".\n\n    Detalle: " + mensaje,
                    "Verifica que el servicio MariaDB esté iniciado y que db.servidor y db.puerto en " + ARCHIVOS
                            + " sean correctos (el puerto se elige al instalar MariaDB; por defecto 3306).",
                    cause);
        }
        return null;
    }

    private String destino() {
        String url = env.getProperty("spring.datasource.url", "");
        Matcher m = URL.matcher(url);
        if (!m.find()) return url;
        String puerto = m.group(2) == null ? "3306" : m.group(2);
        return m.group(1) + ":" + puerto + " (base de datos " + m.group(3) + ")";
    }
}
