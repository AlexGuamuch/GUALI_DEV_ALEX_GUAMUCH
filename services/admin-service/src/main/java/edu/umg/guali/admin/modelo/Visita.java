package edu.umg.guali.admin.modelo;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Visita a la aplicación, para el módulo de estadísticas. */
@Entity
@Table(name = "visita")
public class Visita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String visitante;

    /** Día de la visita en hora de Guatemala. */
    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    protected Visita() {
    }

    public Visita(String visitante, LocalDate fecha, Instant creadaEn) {
        this.visitante = visitante;
        this.fecha = fecha;
        this.creadaEn = creadaEn;
    }
}
