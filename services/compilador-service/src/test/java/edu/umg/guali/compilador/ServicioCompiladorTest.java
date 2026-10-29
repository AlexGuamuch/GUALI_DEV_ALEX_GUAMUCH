package edu.umg.guali.compilador;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import edu.umg.guali.compilador.Modelos.Respuesta;

/** Prueba de integración con guali-dev.jar real (requiere ejecutar Maven con JDK 25). */
class ServicioCompiladorTest {

    private final ServicioCompilador servicio = new ServicioCompilador(
            new CompiladorProperties(System.getProperty("java.home"), 2, 10, 65_536, false));

    @Test
    void compilaYEjecutaSintaxisCompactaDeJava25() {
        Respuesta r = servicio.procesar("void main() {\n    IO.println(\"¡Hola, Mundo!\");\n}\n", true);
        assertThat(r.compilacion().exitosa()).isTrue();
        assertThat(r.compilacion().archivo()).isEqualTo("app25.java");
        assertThat(r.ejecucion().exitosa()).isTrue();
        assertThat(r.ejecucion().salida()).contains("¡Hola, Mundo!");
    }

    @Test
    void reportaErroresDeCompilacionSinRutasInternas() {
        Respuesta r = servicio.procesar("void main() {\n    IO.println(\"Hola\")\n}\n", false);
        assertThat(r.compilacion().exitosa()).isFalse();
        assertThat(r.compilacion().salida()).contains("app25.java:2").contains("';' expected")
                .doesNotContain("guali-dev");
        assertThat(r.ejecucion()).isNull();
    }

    @Test
    void detectaErroresDeEjecucion() {
        Respuesta r = servicio.procesar("void main() {\n    int a = 10;\n    int b = 0;\n    IO.println(a / b);\n}\n", true);
        assertThat(r.compilacion().exitosa()).isTrue();
        assertThat(r.ejecucion().exitosa()).isFalse();
        assertThat(r.ejecucion().tiempoAgotado()).isFalse();
        assertThat(r.ejecucion().errores()).contains("ArithmeticException");
    }

    @Test
    void detieneProgramasQueNoTerminan() {
        Respuesta r = servicio.procesar("void main() {\n    while (true) { }\n}\n", true);
        assertThat(r.ejecucion().tiempoAgotado()).isTrue();
        assertThat(r.ejecucion().exitosa()).isFalse();
    }

    @Test
    void aceptaClasesTradicionales() {
        String codigo = "public class Saludo {\n    public static void main(String[] args) {\n"
                + "        System.out.println(\"Hola desde una clase\");\n    }\n}\n";
        Respuesta r = servicio.procesar(codigo, true);
        assertThat(r.compilacion().archivo()).isEqualTo("Saludo.java");
        assertThat(r.ejecucion().salida()).contains("Hola desde una clase");
    }
}
