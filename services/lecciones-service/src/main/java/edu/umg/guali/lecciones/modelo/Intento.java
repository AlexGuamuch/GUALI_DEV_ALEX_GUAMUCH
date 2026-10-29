package edu.umg.guali.lecciones.modelo;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Registro del historial: una lección abierta por un navegador.
 * Fecha final = cuando se completó, se cerró o se abandonó (otra lección abierta o pestaña cerrada).
 */
@Entity
@Table(name = "intento")
public class Intento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false, length = 36)
    private String clienteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leccion_id")
    private Leccion leccion;

    @Column(name = "leccion_nombre", nullable = false, length = 100)
    private String leccionNombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Objetivo objetivo;

    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;

    @Column(name = "fecha_fin")
    private Instant fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoIntento estado = EstadoIntento.PENDIENTE;

    protected Intento() {
    }

    public Intento(String clienteId, Leccion leccion, Instant inicio) {
        this.clienteId = clienteId;
        this.leccion = leccion;
        this.leccionNombre = leccion.getNombre();
        this.objetivo = leccion.getObjetivo();
        this.fechaInicio = inicio;
    }

    public boolean estaAbierto() {
        return fechaFin == null;
    }

    /** Cierra el intento sin alcanzar el objetivo (queda Pendiente). */
    public void cerrar(Instant ahora) {
        if (fechaFin == null) fechaFin = ahora;
    }

    /** El objetivo se alcanzó: queda Completada y se registra la fecha final. */
    public void completar(Instant ahora) {
        estado = EstadoIntento.COMPLETADA;
        if (fechaFin == null) fechaFin = ahora;
    }

    public Long getId() {
        return id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public String getLeccionNombre() {
        return leccionNombre;
    }

    public Objetivo getObjetivo() {
        return objetivo;
    }

    public Instant getFechaInicio() {
        return fechaInicio;
    }

    public Instant getFechaFin() {
        return fechaFin;
    }

    public EstadoIntento getEstado() {
        return estado;
    }
}
