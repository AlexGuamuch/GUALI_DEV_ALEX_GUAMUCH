package edu.umg.guali.lecciones.modelo;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tema y tamaño de fuente elegidos en un navegador. */
@Entity
@Table(name = "preferencia")
public class Preferencia {

    @Id
    @Column(name = "cliente_id", length = 36)
    private String clienteId;

    @Column(nullable = false, length = 10)
    private String tema;

    @Column(name = "tamano_fuente", nullable = false)
    private short tamanoFuente;

    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    protected Preferencia() {
    }

    public Preferencia(String clienteId) {
        this.clienteId = clienteId;
    }

    public void actualizar(String tema, int tamanoFuente, Instant ahora) {
        this.tema = tema;
        this.tamanoFuente = (short) tamanoFuente;
        this.actualizadaEn = ahora;
    }

    public String getTema() {
        return tema;
    }

    public int getTamanoFuente() {
        return tamanoFuente;
    }
}
