package edu.umg.guali.admin.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea el primer administrador a partir de variables de entorno (ADMIN_USUARIO, ADMIN_NOMBRE, ADMIN_CLAVE)
 * cuando la tabla de usuarios está vacía. Después se gestionan desde el módulo de administradores.
 */
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioService usuarios;
    private final String usuario;
    private final String nombre;
    private final String clave;

    public AdminInicial(UsuarioService usuarios,
                        @Value("${guali.admin-inicial.usuario:}") String usuario,
                        @Value("${guali.admin-inicial.nombre:Administrador}") String nombre,
                        @Value("${guali.admin-inicial.clave:}") String clave) {
        this.usuarios = usuarios;
        this.usuario = usuario;
        this.nombre = nombre;
        this.clave = clave;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuario.isBlank() || clave.isBlank()) {
            log.info("ADMIN_USUARIO/ADMIN_CLAVE no definidos: no se crea administrador inicial.");
            return;
        }
        if (usuarios.crearInicialSiNoHay(usuario.trim().toLowerCase(), nombre.trim(), clave)) {
            log.info("Administrador inicial creado: {}", usuario);
        }
    }
}
