package edu.umg.guali.lecciones.servicio;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

/**
 * Convierte el markdown de una lección en HTML seguro.
 *
 * Dos capas contra XSS: commonmark escapa cualquier HTML escrito dentro del markdown y luego
 * el sanitizador OWASP deja pasar solo una lista blanca de etiquetas y atributos.
 * Las imágenes relativas (imagen.jpg) se reescriben a la ruta de la API que las sirve desde la base de datos.
 */
@Component
public class RenderizadorMarkdown {

    private static final List<Extension> EXTENSIONES = List.of(TablesExtension.create());
    private static final Pattern IMAGEN_RELATIVA = Pattern.compile("^(?:\\./)?([A-Za-z0-9][A-Za-z0-9._-]{0,149})$");
    private static final int LARGO_RESUMEN = 160;

    private final Parser parser = Parser.builder().extensions(EXTENSIONES).build();

    private final PolicyFactory politica = new HtmlPolicyBuilder()
            .allowElements("h1", "h2", "h3", "h4", "h5", "h6", "p", "br", "hr", "ul", "ol", "li",
                    "blockquote", "pre", "code", "em", "strong", "del", "table", "thead", "tbody",
                    "tr", "th", "td", "a", "img")
            .allowAttributes("class").matching(Pattern.compile("language-[A-Za-z0-9_+-]{1,30}")).onElements("code")
            .allowAttributes("align").matching(true, "left", "center", "right").onElements("th", "td")
            .allowAttributes("start").matching(Pattern.compile("\\d{1,4}")).onElements("ol")
            .allowUrlProtocols("https", "http")
            .allowAttributes("href").onElements("a")
            .allowAttributes("target").matching(true, "_blank").onElements("a")
            .requireRelsOnLinks("noopener", "noreferrer", "nofollow")
            .allowAttributes("src").matching(Pattern.compile("/api/lecciones/\\d+/recursos/[A-Za-z0-9._-]+")).onElements("img")
            .allowAttributes("alt", "title").onElements("img")
            .allowAttributes("loading").matching(true, "lazy").onElements("img")
            .toFactory();

    /**
     * @param recursos nombres de las imágenes que existen en la lección
     */
    public String html(String markdown, long leccionId, Set<String> recursos) {
        HtmlRenderer renderer = HtmlRenderer.builder()
                .extensions(EXTENSIONES)
                .escapeHtml(true)
                .sanitizeUrls(true)
                .attributeProviderFactory(contexto -> (node, tag, atributos) -> {
                    if (node instanceof Image imagen) {
                        atributos.put("src", rutaImagen(imagen.getDestination(), leccionId, recursos));
                        atributos.put("loading", "lazy");
                    } else if (node instanceof Link) {
                        atributos.put("target", "_blank");
                    }
                })
                .build();
        return politica.sanitize(renderer.render(parser.parse(markdown)));
    }

    /** Texto del primer párrafo, para mostrar en la lista de lecciones. */
    public String resumen(String markdown) {
        StringBuilder texto = new StringBuilder();
        parser.parse(markdown).accept(new AbstractVisitor() {
            private boolean listo;

            @Override
            public void visit(Paragraph paragraph) {
                if (listo) return;
                visitChildren(paragraph);
                listo = !texto.toString().isBlank();
            }

            @Override
            public void visit(Text text) {
                if (!listo && dentroDeParrafo(text)) texto.append(text.getLiteral());
            }

            @Override
            public void visit(Code code) {
                if (!listo && dentroDeParrafo(code)) texto.append(code.getLiteral());
            }

            @Override
            public void visit(SoftLineBreak softLineBreak) {
                if (!listo) texto.append(' ');
            }
        });
        String limpio = texto.toString().replaceAll("\\s+", " ").strip();
        if (limpio.isEmpty()) return "Lección de práctica.";
        return limpio.length() <= LARGO_RESUMEN ? limpio : limpio.substring(0, LARGO_RESUMEN - 1).strip() + "…";
    }

    /** Texto de un párrafo, excluyendo el texto alternativo de las imágenes. */
    private static boolean dentroDeParrafo(Node node) {
        for (Node n = node.getParent(); n != null; n = n.getParent()) {
            if (n instanceof Image) return false;
            if (n instanceof Paragraph) return true;
        }
        return false;
    }

    private static String rutaImagen(String destino, long leccionId, Set<String> recursos) {
        Matcher m = IMAGEN_RELATIVA.matcher(destino == null ? "" : destino);
        if (m.matches() && recursos.contains(m.group(1))) {
            return "/api/lecciones/" + leccionId + "/recursos/" + m.group(1);
        }
        return "";
    }
}
