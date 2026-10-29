package edu.umg.guali.admin.controlador;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.umg.guali.admin.modelo.Usuario;
import edu.umg.guali.admin.servicio.AutenticacionService;
import edu.umg.guali.admin.servicio.Dtos.Credenciales;
import edu.umg.guali.admin.servicio.Dtos.Sesion;
import edu.umg.guali.admin.servicio.EmisorTokens;
import edu.umg.guali.admin.servicio.EmisorTokens.Token;
import edu.umg.guali.seguridad.CookieBearerTokenResolver;
import edu.umg.guali.seguridad.SeguridadProperties;

/** Inicio y cierre de sesión de administradores; el JWT viaja solo en una cookie HttpOnly. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AutenticacionService autenticacion;
    private final EmisorTokens tokens;
    private final SeguridadProperties props;

    public AuthController(AutenticacionService autenticacion, EmisorTokens tokens, SeguridadProperties props) {
        this.autenticacion = autenticacion;
        this.tokens = tokens;
        this.props = props;
    }

    @PostMapping("/login")
    public ResponseEntity<Sesion> login(@Valid @RequestBody Credenciales credenciales) {
        Usuario usuario = autenticacion.login(credenciales.username(), credenciales.password());
        Token token = tokens.emitir(usuario);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokens.cookie(token).toString())
                .body(new Sesion(true, usuario.getUsername(), usuario.getNombreCompleto(), token.expiraEn()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Sesion> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokens.cookieVacia().toString())
                .body(Sesion.anonima());
    }

    /**
     * Estado de la sesión. Si el token está por vencer se renueva (sesión deslizante de 30 minutos).
     * También entrega la cookie XSRF-TOKEN que el frontend necesita para las peticiones POST/PUT/DELETE.
     */
    @GetMapping("/sesion")
    public ResponseEntity<Sesion> sesion(HttpServletRequest request) {
        String cookie = CookieBearerTokenResolver.leerCookie(request, props.nombreCookie());
        return autenticacion.sesion(cookie)
                .map(s -> {
                    ResponseEntity.BodyBuilder ok = ResponseEntity.ok();
                    var expira = s.expiraEn();
                    if (s.renovar()) {
                        Token nuevo = tokens.emitir(s.usuario());
                        ok.header(HttpHeaders.SET_COOKIE, tokens.cookie(nuevo).toString());
                        expira = nuevo.expiraEn();
                    }
                    return ok.body(new Sesion(true, s.usuario().getUsername(), s.usuario().getNombreCompleto(), expira));
                })
                .orElseGet(() -> {
                    ResponseEntity.BodyBuilder ok = ResponseEntity.ok();
                    if (cookie != null) ok.header(HttpHeaders.SET_COOKIE, tokens.cookieVacia().toString());
                    return ok.body(Sesion.anonima());
                });
    }
}
