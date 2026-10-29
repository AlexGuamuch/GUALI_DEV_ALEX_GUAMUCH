package edu.umg.guali.lecciones.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import edu.umg.guali.lecciones.modelo.RecursoLeccion;

public interface RecursoLeccionRepository extends JpaRepository<RecursoLeccion, Long> {

    Optional<RecursoLeccion> findByLeccionIdAndNombreArchivo(Long leccionId, String nombreArchivo);

    @Query("select r.leccion.id, count(r) from RecursoLeccion r group by r.leccion.id")
    List<Object[]> contarPorLeccion();
}
