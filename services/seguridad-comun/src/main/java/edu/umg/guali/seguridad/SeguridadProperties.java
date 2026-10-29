package edu.umg.guali.seguridad;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Propiedades de seguridad compartidas por los microservicios.
 *
 * @param clavePublica  llave pública RSA (PEM) para validar los JWT emitidos por admin-service
 * @param emisor        valor esperado del claim "iss"
 * @param nombreCookie  cookie HttpOnly donde viaja el JWT
 * @param duracion      vigencia del token
 * @param cookieSegura  marca Secure en las cookies (true en producción con HTTPS)
 * @param tokenInterno  secreto compartido para las rutas /interno/** entre servicios
 * @param tokenGateway  secreto que agrega el gateway; sin él no se aceptan rutas /api/**
 */
@ConfigurationProperties(prefix = "guali.seguridad")
public record SeguridadProperties(
        Resource clavePublica,
        String emisor,
        String nombreCookie,
        Duration duracion,
        boolean cookieSegura,
        String tokenInterno,
        String tokenGateway) {

    public SeguridadProperties {
        if (emisor == null || emisor.isBlank()) emisor = "guali-admin";
        if (nombreCookie == null || nombreCookie.isBlank()) nombreCookie = "GUALI_SESION";
        if (duracion == null) duracion = Duration.ofMinutes(30);
    }
}
