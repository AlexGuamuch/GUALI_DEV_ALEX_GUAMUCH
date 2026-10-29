package edu.umg.guali.admin.servicio;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.umg.guali.admin.modelo.Usuario;
import edu.umg.guali.admin.repositorio.UsuarioRepository;
import edu.umg.guali.admin.servicio.Dtos.UsuarioDto;
import edu.umg.guali.admin.servicio.Dtos.UsuarioForm;

/** Módulo CRUD de usuarios administradores. */
@Service
public class UsuarioService {

    private static final Logger auditoria = LoggerFactory.getLogger("auditoria");

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final Clock reloj;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder, Clock reloj) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        return usuarios.findAllByOrderByNombreCompletoAsc().stream().map(UsuarioService::dto).toList();
    }

    @Transactional
    public UsuarioDto crear(UsuarioForm form, String actor) {
        if (usuarios.existsByUsername(form.username())) {
            throw new ReglaException(HttpStatus.CONFLICT, "Ese usuario ya existe.");
        }
        PoliticaContrasenas.validar(form.password(), form.username());
        Usuario nuevo = usuarios.save(new Usuario(form.username(), form.nombreCompleto().strip(),
                encoder.encode(form.password()), form.activo(), reloj.instant()));
        auditoria.info("{} creó al administrador {}", actor, nuevo.getUsername());
        return dto(nuevo);
    }

    @Transactional
    public UsuarioDto actualizar(long id, UsuarioForm form, String actor) {
        Usuario usuario = buscar(id);
        boolean esActor = usuario.getUsername().equals(actor);
        if (usuarios.existsByUsernameAndIdNot(form.username(), id)) {
            throw new ReglaException(HttpStatus.CONFLICT, "Ese usuario ya existe.");
        }
        if (esActor && !form.activo()) {
            throw new ReglaException(HttpStatus.BAD_REQUEST, "No puedes desactivar tu propia cuenta.");
        }
        if (usuario.isActivo() && !form.activo() && usuarios.countByActivoTrue() <= 1) {
            throw new ReglaException(HttpStatus.BAD_REQUEST, "Debe quedar al menos un administrador activo.");
        }

        Instant ahora = reloj.instant();
        usuario.actualizar(form.username(), form.nombreCompleto().strip(), form.activo(), ahora);
        if (form.password() != null && !form.password().isEmpty()) {
            PoliticaContrasenas.validar(form.password(), form.username());
            usuario.cambiarPassword(encoder.encode(form.password()), ahora);
        }
        auditoria.info("{} modificó al administrador {}", actor, usuario.getUsername());
        return dto(usuario);
    }

    @Transactional
    public void eliminar(long id, String actor) {
        Usuario usuario = buscar(id);
        if (usuario.getUsername().equals(actor)) {
            throw new ReglaException(HttpStatus.BAD_REQUEST, "No puedes eliminar tu propia cuenta.");
        }
        if (usuario.isActivo() && usuarios.countByActivoTrue() <= 1) {
            throw new ReglaException(HttpStatus.BAD_REQUEST, "Debe quedar al menos un administrador activo.");
        }
        usuarios.delete(usuario);
        auditoria.info("{} eliminó al administrador {}", actor, usuario.getUsername());
    }

    /** Crea el primer administrador desde variables de entorno si no existe ninguno. */
    @Transactional
    public boolean crearInicialSiNoHay(String username, String nombre, String password) {
        if (usuarios.count() > 0) return false;
        PoliticaContrasenas.validar(password, username);
        usuarios.save(new Usuario(username, nombre, encoder.encode(password), true, reloj.instant()));
        return true;
    }

    private Usuario buscar(long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new ReglaException(HttpStatus.NOT_FOUND, "El administrador no existe."));
    }

    private static UsuarioDto dto(Usuario u) {
        return new UsuarioDto(u.getId(), u.getUsername(), u.getNombreCompleto(), u.isActivo(),
                u.getUltimoAcceso(), u.getCreadoEn());
    }
}
