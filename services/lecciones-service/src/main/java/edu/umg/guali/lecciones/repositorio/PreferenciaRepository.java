package edu.umg.guali.lecciones.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.umg.guali.lecciones.modelo.Preferencia;

public interface PreferenciaRepository extends JpaRepository<Preferencia, String> {
}
