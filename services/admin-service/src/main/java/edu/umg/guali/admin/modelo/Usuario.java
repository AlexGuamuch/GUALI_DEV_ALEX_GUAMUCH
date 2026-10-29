package edu.umg.guali.admin.modelo;

import java.time.Duration;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Usuario administrador. Los estudiantes no tienen cuenta. */
@Entity
@Table(name = "usuario")
public class Usuario {

    public static final int MAX_INTENTOS = 5;
    public static final Duration BLOQUEO = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String username;

    @Column(name = "nombre_completo", nullable = false, length = 80)
    private String nombreCompleto;

    /** Hash BCrypt; la contraseña nunca se guarda ni se registra en texto plano. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "ultimo_acceso")
    private Instant ultimoAcceso;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Usuario() {
    }

    public Usuario(String username, String nombreCompleto, String passwordHash, boolean activo, Instant ahora) {
        this.username = username;
        this.nombreCompleto = nombreCompleto;
        this.passwordHash = passwordHash;
        this.activo = activo;
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
    }

    public boolean estaBloqueado(Instant ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    /** Tras 5 intentos fallidos la cuenta se bloquea 15 minutos (protección contra fuerza bruta). */
    public void registrarFallo(Instant ahora) {
        intentosFallidos++;
        if (intentosFallidos >= MAX_INTENTOS) {
            bloqueadoHasta = ahora.plus(BLOQUEO);
            intentosFallidos = 0;
        }
    }

    public void registrarAcceso(Instant ahora) {
        intentosFallidos = 0;
        bloqueadoHasta = null;
        ultimoAcceso = ahora;
    }

    public void actualizar(String username, String nombreCompleto, boolean activo, Instant ahora) {
        this.username = username;
        this.nombreCompleto = nombreCompleto;
        this.activo = activo;
        this.actualizadoEn = ahora;
    }

    public void cambiarPassword(String passwordHash, Instant ahora) {
        this.passwordHash = passwordHash;
        this.bloqueadoHasta = null;
        this.intentosFallidos = 0;
        this.actualizadoEn = ahora;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getUltimoAcceso() {
        return ultimoAcceso;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
