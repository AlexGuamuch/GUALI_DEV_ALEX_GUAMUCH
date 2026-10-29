package edu.umg.guali.admin.controlador;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.umg.guali.admin.servicio.Dtos.UsuarioDto;
import edu.umg.guali.admin.servicio.Dtos.UsuarioForm;
import edu.umg.guali.admin.servicio.UsuarioService;

/** Módulo CRUD de administradores (requiere rol ADMIN). */
@RestController
@RequestMapping("/api/admin/usuarios")
public class UsuarioController {

    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return usuarios.listar();
    }

    @PostMapping
    public ResponseEntity<UsuarioDto> crear(@Valid @RequestBody UsuarioForm form, @AuthenticationPrincipal Jwt admin) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarios.crear(form, admin.getSubject()));
    }

    @PutMapping("/{id}")
    public UsuarioDto actualizar(@PathVariable long id, @Valid @RequestBody UsuarioForm form,
                                 @AuthenticationPrincipal Jwt admin) {
        return usuarios.actualizar(id, form, admin.getSubject());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable long id, @AuthenticationPrincipal Jwt admin) {
        usuarios.eliminar(id, admin.getSubject());
        return ResponseEntity.noContent().build();
    }
}
