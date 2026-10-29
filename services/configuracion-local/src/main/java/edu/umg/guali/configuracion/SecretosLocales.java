package edu.umg.guali.configuracion;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;

/**
 * Secretos de una instalación local, generados al azar la primera vez y reutilizados después.
 * Los cuatro servicios pueden arrancar al mismo tiempo: un candado de archivo evita que dos los generen.
 */
public record SecretosLocales(String tokenInterno, String tokenCompilador, String tokenGateway,
                              String salVisitas, Path clavePublica, Path clavePrivada) {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom AZAR = new SecureRandom();

    public static SecretosLocales obtenerOCrear(Path carpeta) {
        try {
            Files.createDirectories(carpeta);
            Path archivo = carpeta.resolve("secretos.properties");
            Path publica = carpeta.resolve("jwt-publica.pem");
            Path privada = carpeta.resolve("jwt-privada.pem");

            try (FileChannel canal = FileChannel.open(carpeta.resolve(".candado"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock candado = canal.lock()) {

                if (!Files.isRegularFile(publica) || !Files.isRegularFile(privada)) {
                    KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
                    generador.initialize(2048, AZAR);
                    KeyPair par = generador.generateKeyPair();
                    escribir(privada, pem("PRIVATE KEY", par.getPrivate().getEncoded()));
                    escribir(publica, pem("PUBLIC KEY", par.getPublic().getEncoded()));
                }

                Properties p = new Properties();
                if (Files.isRegularFile(archivo)) {
                    try (Reader r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                        p.load(r);
                    }
                }
                boolean cambio = false;
                for (String clave : new String[] {"token.interno", "token.compilador", "token.gateway", "visitas.sal"}) {
                    if (p.getProperty(clave, "").length() < 24) {
                        p.setProperty(clave, aleatorio(48));
                        cambio = true;
                    }
                }
                if (cambio) {
                    Path temporal = carpeta.resolve("secretos.properties.tmp");
                    try (Writer w = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                        p.store(w, "Secretos de Codifica con Guali en esta PC (generados automáticamente)");
                    }
                    Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                }

                return new SecretosLocales(p.getProperty("token.interno"), p.getProperty("token.compilador"),
                        p.getProperty("token.gateway"), p.getProperty("visitas.sal"), publica, privada);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudieron preparar los secretos locales en " + carpeta, e);
        }
    }

    private static String aleatorio(int largo) {
        StringBuilder sb = new StringBuilder(largo);
        for (int i = 0; i < largo; i++) sb.append(ALFABETO.charAt(AZAR.nextInt(ALFABETO.length())));
        return sb.toString();
    }

    private static String pem(String tipo, byte[] der) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        return "-----BEGIN " + tipo + "-----\n" + base64 + "\n-----END " + tipo + "-----\n";
    }

    private static void escribir(Path destino, String contenido) throws IOException {
        Path temporal = destino.resolveSibling(destino.getFileName() + ".tmp");
        Files.writeString(temporal, contenido, StandardCharsets.US_ASCII);
        Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
