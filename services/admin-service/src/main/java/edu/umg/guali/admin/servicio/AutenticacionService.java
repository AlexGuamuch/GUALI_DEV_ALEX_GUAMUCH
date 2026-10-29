package edu.umg.guali.admin.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.umg.guali.admin.modelo.Usuario;
import edu.umg.guali.admin.repositorio.UsuarioRepository;

/** Inicio de sesión de administradores con bloqueo por intentos fallidos. */
@Service
public class AutenticacionService {

    private static final Logger auditoria = LoggerFactory.getLogger("auditoria");
    private static final Duration RENOVAR_ANTES = Duration.ofMinutes(15);
    private static final String CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos.";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock reloj;
    /** Hash de referencia: se compara aunque el usuario no exista, para no revelarlo por el tiempo de respuesta. */
    private final String hashFicticio;

    public AutenticacionService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtDecoder decoder, Clock reloj) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.decoder = decoder;
        this.reloj = reloj;
        this.hashFicticio = encoder.encode("contraseña-de-referencia-no-valida");
    }

    @Transactional(noRollbackFor = ReglaException.class)
    public Usuario login(String username, String password) {
        Instant ahora = reloj.instant();
        Optional<Usuario> encontrado = usuarios.findByUsername(username.trim().toLowerCase());

        if (encontrado.isEmpty()) {
            encoder.matches(password, hashFicticio);
            auditoria.warn("Inicio de sesión fallido: usuario inexistente");
            throw new ReglaException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS);
        }

        Usuario usuario = encontrado.get();
        if (usuario.estaBloqueado(ahora)) {
            auditoria.warn("Inicio de sesión rechazado: {} está bloqueado", usuario.getUsername());
            throw new ReglaException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Espera 15 minutos e inténtalo de nuevo.");
        }
        if (!encoder.matches(password, usuario.getPasswordHash()) || !usuario.isActivo()) {
            usuario.registrarFallo(ahora);
            auditoria.warn("Inicio de sesión fallido para {}", usuario.getUsername());
            throw new ReglaException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS);
        }

        usuario.registrarAcceso(ahora);
        auditoria.info("Inicio de sesión de {}", usuario.getUsername());
        return usuario;
    }

    /**
     * Valida el token de la cookie y que el administrador siga activo.
     * Devuelve vacío si no hay sesión válida.
     */
    @Transactional(readOnly = true)
    public Optional<SesionValida> sesion(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            Jwt jwt = decoder.decode(token);
            return usuarios.findByUsername(jwt.getSubject())
                    .filter(Usuario::isActivo)
                    .map(u -> new SesionValida(u, jwt.getExpiresAt(),
                            jwt.getExpiresAt().isBefore(reloj.instant().plus(RENOVAR_ANTES))));
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public boolean estaActivo(String username) {
        return usuarios.findByUsername(username).map(Usuario::isActivo).orElse(false);
    }

    public record SesionValida(Usuario usuario, Instant expiraEn, boolean renovar) {
    }
}
