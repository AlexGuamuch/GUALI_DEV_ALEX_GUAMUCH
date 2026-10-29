package edu.umg.guali.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

class ConfiguracionLocalTest {

    @TempDir
    Path temporal;

    @Test
    void generaLosSecretosUnaVezYLuegoLosReutiliza() throws Exception {
        SecretosLocales primera = SecretosLocales.obtenerOCrear(temporal);
        SecretosLocales segunda = SecretosLocales.obtenerOCrear(temporal);

        assertThat(primera.tokenGateway()).hasSize(48).isEqualTo(segunda.tokenGateway());
        assertThat(primera.tokenInterno()).isNotEqualTo(primera.tokenCompilador());
        assertThat(Files.readString(primera.clavePublica())).startsWith("-----BEGIN PUBLIC KEY-----");
        assertThat(Files.readString(primera.clavePrivada())).startsWith("-----BEGIN PRIVATE KEY-----");
        assertThat(Files.readString(segunda.clavePrivada())).isEqualTo(Files.readString(primera.clavePrivada()));
    }

    @Test
    void encuentraLaCarpetaDelProyectoDesdeLaCarpetaDeUnServicio() throws Exception {
        Files.writeString(temporal.resolve(ConfiguracionLocal.ARCHIVO), "db.puerto=3306");
        Path servicio = Files.createDirectories(temporal.resolve("services/lecciones-service"));

        assertThat(ConfiguracionLocal.buscarRaiz(servicio)).isEqualTo(temporal);
        assertThat(ConfiguracionLocal.buscarRaiz(temporal)).isEqualTo(temporal);
    }

    @Test
    void elArchivoPersonalTienePrioridadPeroUnValorVacioNoBorraElGeneral() throws Exception {
        Files.writeString(temporal.resolve(ConfiguracionLocal.ARCHIVO), "db.puerto=3306\ndb.clave=root\n");
        Files.writeString(temporal.resolve(ConfiguracionLocal.ARCHIVO_PERSONAL), "db.puerto=3307\ndb.clave=\n");
        Path servicio = Files.createDirectories(temporal.resolve("services/admin-service"));

        String dirOriginal = System.getProperty("user.dir");
        String homeOriginal = System.getProperty("user.home");
        try {
            System.setProperty("user.dir", servicio.toString());
            System.setProperty("user.home", temporal.toString());
            StandardEnvironment env = new StandardEnvironment();
            new ConfiguracionLocal().postProcessEnvironment(env, null);

            assertThat(env.getProperty("db.puerto")).isEqualTo("3307");
            assertThat(env.getProperty("db.clave")).isEqualTo("root");
            assertThat(env.getProperty("guali.local.frontend")).isNull();
        } finally {
            System.setProperty("user.dir", dirOriginal);
            System.setProperty("user.home", homeOriginal);
        }
    }

    @Test
    void explicaEnEspanolUnaContrasenaRechazada() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mariadb://localhost:3307/guali_admin?createDatabaseIfNotExist=true")
                .withProperty("spring.datasource.username", "root");
        ConexionMariaDbFailureAnalyzer analizador = new ConexionMariaDbFailureAnalyzer(env);

        FailureAnalysis clave = analizador.analyze(new IllegalStateException("arranque"),
                new SQLException("GSS-API authentication exception", "28000"));
        assertThat(clave.getDescription()).contains("rechazó", "localhost:3307", "guali_admin", "root");
        assertThat(clave.getAction()).contains("db.clave");

        FailureAnalysis apagado = analizador.analyze(new IllegalStateException("arranque"),
                new SQLNonTransientConnectionException("Socket fail to connect to localhost:3307", "08000"));
        assertThat(apagado.getDescription()).contains("No hay un servidor MariaDB");
    }
}
