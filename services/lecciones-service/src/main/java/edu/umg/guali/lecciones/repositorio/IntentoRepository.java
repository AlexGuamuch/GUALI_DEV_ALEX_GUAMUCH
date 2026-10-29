package edu.umg.guali.lecciones.repositorio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.umg.guali.lecciones.modelo.EstadoIntento;
import edu.umg.guali.lecciones.modelo.Intento;

public interface IntentoRepository extends JpaRepository<Intento, Long> {

    List<Intento> findByClienteIdOrderByFechaInicioDesc(String clienteId, Limit limit);

    List<Intento> findByClienteIdAndFechaFinIsNull(String clienteId);

    Optional<Intento> findByIdAndClienteId(Long id, String clienteId);

    /** Intentos abiertos hace demasiado tiempo (pestaña cerrada sin aviso): se cierran como Pendiente. */
    @Modifying
    @Query("update Intento i set i.fechaFin = :ahora where i.fechaFin is null and i.fechaInicio < :limite")
    int cerrarAbandonados(@Param("limite") Instant limite, @Param("ahora") Instant ahora);

    /** Datos mínimos para las estadísticas de administración. */
    @Query("select i.fechaInicio as fechaInicio, i.estado as estado, i.leccionNombre as leccionNombre "
            + "from Intento i where i.fechaInicio >= :desde and i.fechaInicio < :hasta")
    List<IntentoResumen> resumenEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    interface IntentoResumen {
        Instant getFechaInicio();

        EstadoIntento getEstado();

        String getLeccionNombre();
    }
}
