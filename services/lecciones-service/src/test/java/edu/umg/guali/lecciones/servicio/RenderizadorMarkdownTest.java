package edu.umg.guali.lecciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class RenderizadorMarkdownTest {

    private final RenderizadorMarkdown renderizador = new RenderizadorMarkdown();

    @Test
    void reescribeImagenesDeLaLeccionHaciaLaApi() {
        String html = renderizador.html("![Editor](./imagen.png)", 7, Set.of("imagen.png"));
        assertThat(html).contains("src=\"/api/lecciones/7/recursos/imagen.png\"").contains("alt=\"Editor\"");
    }

    @Test
    void eliminaHtmlYEnlacesPeligrosos() {
        String md = """
                <script>alert('xss')</script>
                <img src=x onerror=alert(1)>

                [clic](javascript:alert(1)) [externa](https://dev.java)

                ![remota](https://evil.example/x.png)
                """;
        String html = renderizador.html(md, 1, Set.of());
        // El HTML escrito en el markdown queda como texto escapado, nunca como etiqueta o atributo.
        assertThat(html).doesNotContain("<script").doesNotContain("<img src=x").doesNotContain("onerror=\"")
                .doesNotContain("javascript:").doesNotContain("evil.example");
        assertThat(html).contains("&lt;script&gt;");
        assertThat(html).contains("href=\"https://dev.java\"").contains("rel=\"noopener noreferrer nofollow\"");
    }

    @Test
    void conservaBloquesDeCodigoJava() {
        String html = renderizador.html("```java\nvoid main() {\n    IO.println(\"<b>\");\n}\n```", 1, Set.of());
        assertThat(html).contains("<pre><code class=\"language-java\">").contains("&lt;b&gt;");
    }

    @Test
    void resumenUsaElPrimerParrafoSinTextoDeImagenes() {
        String md = "![Una captura](img.png)\n\n# Título\n\nTodo programador empieza **aquí**.\n\nOtro párrafo.";
        assertThat(renderizador.resumen(md)).isEqualTo("Todo programador empieza aquí.");
    }
}
