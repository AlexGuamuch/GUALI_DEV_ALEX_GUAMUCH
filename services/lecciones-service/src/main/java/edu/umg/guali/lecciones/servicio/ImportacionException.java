package edu.umg.guali.lecciones.servicio;

/** El compendio .zip completo no se puede importar; el mensaje se muestra tal cual al administrador. */
public class ImportacionException extends RuntimeException {

    public ImportacionException(String mensaje) {
        super(mensaje);
    }
}
