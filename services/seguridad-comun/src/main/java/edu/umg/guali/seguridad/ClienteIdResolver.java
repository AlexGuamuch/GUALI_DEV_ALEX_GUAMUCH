package edu.umg.guali.seguridad;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Identificador anónimo del navegador del estudiante.
 * El gateway asigna la cookie HttpOnly "guali_cliente" y reenvía su valor en la cabecera
 * {@value #CABECERA}; cualquier cabecera con ese nombre que venga del exterior se descarta en el gateway.
 */
public class ClienteIdResolver {

    public static final String CABECERA = "X-Cliente-Id";

    public String resolver(HttpServletRequest request) {
        String valor = request.getHeader(CABECERA);
        if (valor == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el identificador del navegador.");
        }
        try {
            return UUID.fromString(valor).toString();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador del navegador inválido.");
        }
    }
}
