package edu.umg.guali.lecciones.modelo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** Lección importada desde un compendio .zip. El nombre de su carpeta es su título. */
@Entity
@Table(name = "leccion")
public class Leccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Objetivo objetivo;

    @Column(nullable = false, length = 300)
    private String resumen;

    @Column(name = "contenido_md", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String contenidoMd;

    /** HTML ya sanitizado, generado al importar a partir del markdown. */
    @Column(name = "contenido_html", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String contenidoHtml;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    @OneToMany(mappedBy = "leccion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecursoLeccion> recursos = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Objetivo getObjetivo() {
        return objetivo;
    }

    public void setObjetivo(Objetivo objetivo) {
        this.objetivo = objetivo;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public String getContenidoMd() {
        return contenidoMd;
    }

    public void setContenidoMd(String contenidoMd) {
        this.contenidoMd = contenidoMd;
    }

    public String getContenidoHtml() {
        return contenidoHtml;
    }

    public void setContenidoHtml(String contenidoHtml) {
        this.contenidoHtml = contenidoHtml;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(Instant creadaEn) {
        this.creadaEn = creadaEn;
    }

    public Instant getActualizadaEn() {
        return actualizadaEn;
    }

    public void setActualizadaEn(Instant actualizadaEn) {
        this.actualizadaEn = actualizadaEn;
    }

    public List<RecursoLeccion> getRecursos() {
        return recursos;
    }

    public void agregarRecurso(RecursoLeccion recurso) {
        recurso.setLeccion(this);
        recursos.add(recurso);
    }
}
