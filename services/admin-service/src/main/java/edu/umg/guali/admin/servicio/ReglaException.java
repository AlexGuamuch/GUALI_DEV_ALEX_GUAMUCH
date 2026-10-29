package edu.umg.guali.admin.servicio;

import org.springframework.http.HttpStatus;

/** Error de negocio con un mensaje que se puede mostrar al administrador. */
public class ReglaException extends RuntimeException {

    private final HttpStatus status;

    public ReglaException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
