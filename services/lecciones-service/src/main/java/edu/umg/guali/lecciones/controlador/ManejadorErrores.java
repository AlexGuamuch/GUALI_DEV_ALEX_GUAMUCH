package edu.umg.guali.lecciones.controlador;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import edu.umg.guali.lecciones.servicio.ImportacionException;

/** Convierte errores conocidos en respuestas JSON con un mensaje claro y sin detalles internos. */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ImportacionException.class)
    ProblemDetail importacion(ImportacionException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail noEncontrado(NoSuchElementException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail muyGrande() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, "El archivo supera el máximo de 10 MB.");
    }

    @ExceptionHandler(MultipartException.class)
    ProblemDetail multipart() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "No se recibió un archivo válido.");
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
