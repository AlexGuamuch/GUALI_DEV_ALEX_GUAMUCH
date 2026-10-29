package edu.umg.guali.seguridad;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Obtiene el JWT de la cookie HttpOnly de sesión. El navegador nunca expone el token a JavaScript,
 * lo que impide robarlo mediante XSS.
 *
 * Solo se lee en rutas de administración (/api/admin/**): así, una cookie vencida no bloquea las
 * funciones públicas de los estudiantes.
 */
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private static final String PREFIJO_PROTEGIDO = "/api/admin/";

    private final String cookieName;

    public CookieBearerTokenResolver(String cookieName) {
        this.cookieName = cookieName;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        if (!request.getRequestURI().startsWith(PREFIJO_PROTEGIDO)) return null;
        return leerCookie(request, cookieName);
    }

    public static String leerCookie(HttpServletRequest request, String nombre) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (nombre.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
