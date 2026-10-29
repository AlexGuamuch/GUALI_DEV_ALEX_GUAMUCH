package edu.umg.guali.lecciones.servicio;

import static edu.umg.guali.lecciones.servicio.EvaluadorObjetivo.Accion.COMPILAR;
import static edu.umg.guali.lecciones.servicio.EvaluadorObjetivo.Accion.EJECUTAR;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import edu.umg.guali.lecciones.dto.Dtos.Compilacion;
import edu.umg.guali.lecciones.dto.Dtos.Ejecucion;
import edu.umg.guali.lecciones.dto.Dtos.RespuestaCodigo;
import edu.umg.guali.lecciones.modelo.Objetivo;

class EvaluadorObjetivoTest {

    private static final RespuestaCodigo COMPILA = new RespuestaCodigo(new Compilacion(true, "", "app25.java", 1), null, false);
    private static final RespuestaCodigo NO_COMPILA = new RespuestaCodigo(new Compilacion(false, "error", "app25.java", 1), null, false);
    private static final RespuestaCodigo EJECUTA_BIEN = new RespuestaCodigo(COMPILA.compilacion(), new Ejecucion(true, "Hola", "", false, 1), false);
    private static final RespuestaCodigo EJECUTA_MAL = new RespuestaCodigo(COMPILA.compilacion(), new Ejecucion(false, "", "ArithmeticException", false, 1), false);
    private static final RespuestaCodigo TIEMPO = new RespuestaCodigo(COMPILA.compilacion(), new Ejecucion(false, "", "", true, 5000), false);

    @Test
    void compilarConError() {
        assertThat(EvaluadorObjetivo.cumple(Objetivo.COMPILAR_CON_ERROR, COMPILAR, NO_COMPILA)).isTrue();
        assertThat(EvaluadorObjetivo.cumple(Objetivo.COMPILAR_CON_ERROR, COMPILAR, COMPILA)).isFalse();
    }

    @Test
    void compilarExitoso() {
        assertThat(EvaluadorObjetivo.cumple(Objetivo.COMPILAR_EXITOSO, COMPILAR, COMPILA)).isTrue();
        assertThat(EvaluadorObjetivo.cumple(Objetivo.COMPILAR_EXITOSO, EJECUTAR, EJECUTA_BIEN)).isFalse();
    }

    @Test
    void ejecutarConErrorNoCuentaElTiempoAgotado() {
        assertThat(EvaluadorObjetivo.cumple(Objetivo.EJECUTAR_CON_ERROR, EJECUTAR, EJECUTA_MAL)).isTrue();
        assertThat(EvaluadorObjetivo.cumple(Objetivo.EJECUTAR_CON_ERROR, EJECUTAR, TIEMPO)).isFalse();
        assertThat(EvaluadorObjetivo.cumple(Objetivo.EJECUTAR_CON_ERROR, COMPILAR, NO_COMPILA)).isFalse();
    }

    @Test
    void ejecutarExitoso() {
        assertThat(EvaluadorObjetivo.cumple(Objetivo.EJECUTAR_EXITOSO, EJECUTAR, EJECUTA_BIEN)).isTrue();
        assertThat(EvaluadorObjetivo.cumple(Objetivo.EJECUTAR_EXITOSO, EJECUTAR, EJECUTA_MAL)).isFalse();
    }
}
