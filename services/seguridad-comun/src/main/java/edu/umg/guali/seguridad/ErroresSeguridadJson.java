package edu.umg.guali.seguridad;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Respuestas 401/403 en formato JSON (application/problem+json) en lugar de páginas HTML. */
public class ErroresSeguridadJson {

    public AuthenticationEntryPoint noAutenticado() {
        return (request, response, ex) -> escribir(response, HttpStatus.UNAUTHORIZED,
                "Inicia sesión como administrador para continuar.");
    }

    public AccessDeniedHandler accesoDenegado() {
        return (request, response, ex) -> escribir(response, HttpStatus.FORBIDDEN,
                "No tienes permiso para esta acción o la página expiró. Recarga e inténtalo de nuevo.");
    }

    private static void escribir(HttpServletResponse response, HttpStatus status, String detalle) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status.value()
                + ",\"title\":\"" + status.getReasonPhrase()
                + "\",\"detail\":\"" + detalle + "\"}");
    }
}
