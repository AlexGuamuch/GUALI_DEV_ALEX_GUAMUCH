package edu.umg.guali.admin.repositorio;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.umg.guali.admin.modelo.Visita;

public interface VisitaRepository extends JpaRepository<Visita, Long> {

    boolean existsByVisitanteAndCreadaEnAfter(String visitante, Instant desde);

    /** Por día: [fecha, visitantes distintos, visitas]. */
    @Query("select v.fecha, count(distinct v.visitante), count(v) from Visita v "
            + "where v.fecha between :desde and :hasta group by v.fecha")
    List<Object[]> porDia(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("select count(distinct v.visitante) from Visita v where v.fecha between :desde and :hasta")
    long visitantesDistintos(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
