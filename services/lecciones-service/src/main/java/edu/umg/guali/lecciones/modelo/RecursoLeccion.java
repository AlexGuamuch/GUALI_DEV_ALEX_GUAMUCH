package edu.umg.guali.lecciones.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Imagen que forma parte del contenido de una lección; se guarda en la base de datos. */
@Entity
@Table(name = "recurso_leccion")
public class RecursoLeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leccion_id", nullable = false)
    private Leccion leccion;

    @Column(name = "nombre_archivo", nullable = false, length = 150)
    private String nombreArchivo;

    @Column(name = "tipo_contenido", nullable = false, length = 40)
    private String tipoContenido;

    @Column(nullable = false)
    private int tamano;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] datos;

    protected RecursoLeccion() {
    }

    public RecursoLeccion(String nombreArchivo, String tipoContenido, byte[] datos) {
        this.nombreArchivo = nombreArchivo;
        this.tipoContenido = tipoContenido;
        this.datos = datos;
        this.tamano = datos.length;
    }

    public Long getId() {
        return id;
    }

    public Leccion getLeccion() {
        return leccion;
    }

    void setLeccion(Leccion leccion) {
        this.leccion = leccion;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getTipoContenido() {
        return tipoContenido;
    }

    public int getTamano() {
        return tamano;
    }

    public byte[] getDatos() {
        return datos;
    }
}
