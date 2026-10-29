package edu.umg.guali.admin.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.umg.guali.admin.modelo.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByUsername(String username);

    long countByActivoTrue();

    List<Usuario> findAllByOrderByNombreCompletoAsc();
}
