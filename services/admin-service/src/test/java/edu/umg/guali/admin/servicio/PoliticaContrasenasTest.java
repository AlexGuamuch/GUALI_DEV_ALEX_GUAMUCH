package edu.umg.guali.admin.servicio;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import edu.umg.guali.admin.modelo.Usuario;

class PoliticaContrasenasTest {

    @Test
    void aceptaContrasenasFuertes() {
        assertThatCode(() -> PoliticaContrasenas.validar("Laboratorio2026x", "aguamuch")).doesNotThrowAnyException();
    }

    @Test
    void rechazaContrasenasDebilesOConElUsuario() {
        assertThatThrownBy(() -> PoliticaContrasenas.validar("corta1A", "x")).isInstanceOf(ReglaException.class);
        assertThatThrownBy(() -> PoliticaContrasenas.validar("sinmayusculas123", "x")).isInstanceOf(ReglaException.class);
        assertThatThrownBy(() -> PoliticaContrasenas.validar("SinNumerosAqui", "x")).isInstanceOf(ReglaException.class);
        assertThatThrownBy(() -> PoliticaContrasenas.validar("Aguamuch2026xx", "aguamuch")).isInstanceOf(ReglaException.class);
    }

    @Test
    void bloqueaLaCuentaDespuesDeCincoFallos() {
        Instant ahora = Instant.parse("2026-10-28T12:00:00Z");
        Usuario u = new Usuario("admin", "Admin", "hash", true, ahora);
        for (int i = 0; i < Usuario.MAX_INTENTOS - 1; i++) u.registrarFallo(ahora);
        org.assertj.core.api.Assertions.assertThat(u.estaBloqueado(ahora)).isFalse();
        u.registrarFallo(ahora);
        org.assertj.core.api.Assertions.assertThat(u.estaBloqueado(ahora)).isTrue();
        org.assertj.core.api.Assertions.assertThat(u.estaBloqueado(ahora.plus(Usuario.BLOQUEO))).isFalse();
    }
}
