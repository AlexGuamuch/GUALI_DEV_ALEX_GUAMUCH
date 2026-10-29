package edu.umg.guali.lecciones.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.umg.guali.lecciones.modelo.Leccion;

public interface LeccionRepository extends JpaRepository<Leccion, Long> {

    Optional<Leccion> findByNombre(String nombre);

    List<Leccion> findAllByOrderByNombreAsc();
}
