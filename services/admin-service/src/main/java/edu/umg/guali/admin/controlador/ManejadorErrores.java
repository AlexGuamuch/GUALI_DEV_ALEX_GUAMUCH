package edu.umg.guali.admin.controlador;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.umg.guali.admin.servicio.ReglaException;

/** Convierte errores conocidos en respuestas JSON con un mensaje claro y sin detalles internos. */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ReglaException.class)
    ProblemDetail regla(ReglaException e) {
        return ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalido(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getDefaultMessage())
                .orElse("Datos inválidos.");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
    }
}
