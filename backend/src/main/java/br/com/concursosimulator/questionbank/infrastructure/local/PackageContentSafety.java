package br.com.concursosimulator.questionbank.infrastructure.local;

import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.shared.infrastructure.QuestionTextSafety;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

final class PackageContentSafety {
    private static final Pattern CORRECTIVE = Pattern.compile("gabarito|answer|correct|corret[ao]|resolu[cç][aã]o", Pattern.CASE_INSENSITIVE);
    static void check(Path root, QuestionContent content) throws java.io.IOException {
        text(root, content.statement(), true);
        for (var option : content.alternatives()) text(root, option.content(), true);
        text(root, content.resolution(), false);
        for (var ref : content.references()) {
            var url = URLDecoder.decode(ref.url().toString(), StandardCharsets.UTF_8);
            if (url.contains("\\") || CORRECTIVE.matcher(url).find()) throw new IllegalArgumentException("Link corretivo/inseguro não permitido");
        }
    }
    private static void text(Path root, TextContent text, boolean publicContent) throws java.io.IOException {
        if (QuestionTextSafety.looksLikeMarkup(text.text())) throw new IllegalArgumentException("Conteúdo deve ser texto, não markup");
        for (var resource : text.resources()) {
            if (!resource.path().startsWith("assets/") || QuestionTextSafety.looksLikeMarkup(resource.description())
                    || (publicContent && CORRECTIVE.matcher(resource.path() + resource.description()).find())) throw new IllegalArgumentException("Recurso inseguro/corretivo");
            var bytes = LocalFiles.source(root, resource.path());
            if (!LocalFiles.sha(bytes).equals(resource.sha256())) throw new IllegalArgumentException("Hash de recurso divergente");
            switch (resource.mediaType()) {
                case "image/png" -> { if (bytes.length < 8 || !java.util.Arrays.equals(java.util.Arrays.copyOf(bytes,8), new byte[]{(byte)137,80,78,71,13,10,26,10})) throw new IllegalArgumentException("PNG inválido"); }
                case "image/jpeg" -> { if (bytes.length < 3 || bytes[0] != (byte)255 || bytes[1] != (byte)216 || bytes[2] != (byte)255) throw new IllegalArgumentException("JPEG inválido"); }
                case "image/svg+xml" -> svg(bytes);
                default -> throw new IllegalArgumentException("Tipo não permitido");
            }
        }
    }
    private static void svg(byte[] bytes) {
        try {
            var factory = DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, ""); factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var builder = factory.newDocumentBuilder();
            // O handler padrão pode imprimir trechos privados em System.err antes da tradução pelo launcher.
            builder.setErrorHandler(new ErrorHandler() {
                @Override public void warning(SAXParseException error) throws SAXException { throw error; }
                @Override public void error(SAXParseException error) throws SAXException { throw error; }
                @Override public void fatalError(SAXParseException error) throws SAXException { throw error; }
            });
            var document = builder.parse(new java.io.ByteArrayInputStream(bytes));
            if (!"svg".equals(document.getDocumentElement().getLocalName())) throw new IllegalArgumentException("Raiz SVG inválida");
            var tags = Set.of("svg","g","path","rect","circle","ellipse","line","polyline","polygon","text","tspan","title","desc","defs","marker");
            var nodes = document.getElementsByTagName("*");
            for (int i=0; i<nodes.getLength(); i++) {
                var node = nodes.item(i);
                if (!"http://www.w3.org/2000/svg".equals(node.getNamespaceURI()) || !tags.contains(node.getLocalName())) throw new IllegalArgumentException("SVG ativo/não permitido");
                var attrs = node.getAttributes();
                for (int j=0; j<attrs.getLength(); j++) {
                    var attr = attrs.item(j); var value = (attr.getNodeName() + attr.getNodeValue()).toLowerCase(java.util.Locale.ROOT);
                    if (attr.getNodeName().toLowerCase(java.util.Locale.ROOT).startsWith("on") || value.matches("(?s).*?(href|url\\s*\\(|javascript:|data:).*")) throw new IllegalArgumentException("SVG ativo");
                }
            }
        } catch (Exception error) { throw new IllegalArgumentException("SVG recusado", error); }
    }
}
