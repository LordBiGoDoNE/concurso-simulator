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
    private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
    private static final String SVG_NUMBER = "[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?";
    private static final String SVG_NUMBER_LIST = SVG_NUMBER + "(?:[ ,\\t\\r\\n]+" + SVG_NUMBER + ")*";
    private static final String SVG_IDENTIFIER = "[A-Za-z_][A-Za-z0-9_.-]*";
    private static final String SVG_TRANSFORM = "(?:matrix|translate|scale|rotate|skewX|skewY)\\(" + SVG_NUMBER_LIST + "\\)";
    private static final java.util.Map<String,Pattern> SVG_ATTRIBUTES = svgAttributes();
    private static java.util.Map<String,Pattern> svgAttributes() {
        var rules = new java.util.HashMap<String,String>();
        for (var name : "x y dx dy x1 y1 x2 y2 cx cy r rx ry width height refX refY markerWidth markerHeight font-size stroke-width stroke-dashoffset".split(" "))
            rules.put(name, SVG_NUMBER + "(?:px|%)?");
        rules.put("viewBox", SVG_NUMBER + ("[ ,\\t\\r\\n]+" + SVG_NUMBER).repeat(3));
        rules.put("points", SVG_NUMBER_LIST); rules.put("d", "[MmZzLlHhVvCcSsQqTtAa0-9eE+., \\t\\r\\n-]+");
        rules.put("transform", SVG_TRANSFORM + "(?:[ ,\\t\\r\\n]*" + SVG_TRANSFORM + ")*");
        rules.put("id", SVG_IDENTIFIER); rules.put("aria-labelledby", SVG_IDENTIFIER + "(?:[ \\t]+" + SVG_IDENTIFIER + ")*");
        rules.put("role", "img");
        rules.put("fill", "(?:#[0-9A-Fa-f]{3}|#[0-9A-Fa-f]{4}|#[0-9A-Fa-f]{6}|#[0-9A-Fa-f]{8}|(?i:none|currentColor|transparent|black|white|gray|grey|red|green|blue|yellow|orange|purple|pink|brown|cyan|magenta))");
        rules.put("stroke", rules.get("fill"));
        for (var name : java.util.List.of("opacity", "fill-opacity", "stroke-opacity")) rules.put(name, SVG_NUMBER);
        rules.put("stroke-dasharray", "(?:none|" + SVG_NUMBER_LIST + ")");
        rules.put("stroke-linecap", "(?:butt|round|square)"); rules.put("stroke-linejoin", "(?:miter|round|bevel)");
        rules.put("fill-rule", "(?:nonzero|evenodd)"); rules.put("font-family", "[A-Za-z][A-Za-z0-9 ,_-]*");
        rules.put("font-weight", "(?:normal|bold|bolder|lighter|[1-9]00)"); rules.put("font-style", "(?:normal|italic|oblique)");
        rules.put("text-anchor", "(?:start|middle|end)"); rules.put("dominant-baseline", "(?:auto|middle|central|hanging|alphabetic)");
        rules.put("orient", "(?:auto|auto-start-reverse|" + SVG_NUMBER + "(?:deg)?)");
        var patterns = new java.util.HashMap<String,Pattern>(); rules.forEach((name, regex) -> patterns.put(name, Pattern.compile(regex)));
        return java.util.Map.copyOf(patterns);
    }
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
            rejectProcessingInstructions(document);
            if (!"svg".equals(document.getDocumentElement().getLocalName())) throw new IllegalArgumentException("Raiz SVG inválida");
            var tags = Set.of("svg","g","path","rect","circle","ellipse","line","polyline","polygon","text","tspan","title","desc","defs","marker");
            var nodes = document.getElementsByTagName("*");
            for (int i=0; i<nodes.getLength(); i++) {
                var node = nodes.item(i);
                if (!SVG_NAMESPACE.equals(node.getNamespaceURI()) || !tags.contains(node.getLocalName())) throw new IllegalArgumentException("SVG ativo/não permitido");
                var attrs = node.getAttributes();
                for (int j=0; j<attrs.getLength(); j++) {
                    var attr = attrs.item(j);
                    if (javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())) {
                        if (!SVG_NAMESPACE.equals(attr.getNodeValue())) throw new IllegalArgumentException("Namespace SVG não permitido");
                    } else {
                        var pattern = SVG_ATTRIBUTES.get(attr.getNodeName());
                        if (attr.getNamespaceURI() != null || pattern == null || !pattern.matcher(attr.getNodeValue().trim()).matches())
                            throw new IllegalArgumentException("Atributo/valor SVG não permitido");
                    }
                }
            }
        } catch (Exception error) { throw new IllegalArgumentException("SVG recusado", error); }
    }
    private static void rejectProcessingInstructions(org.w3c.dom.Node root) {
        // Percorrer o documento inteiro, inclusive antes/depois da raiz; não só os elementos SVG.
        var pending = new java.util.ArrayDeque<org.w3c.dom.Node>(); pending.push(root);
        while (!pending.isEmpty()) {
            var node = pending.pop();
            if (node.getNodeType() == org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE)
                throw new IllegalArgumentException("SVG com instrução de processamento");
            for (var child = node.getFirstChild(); child != null; child = child.getNextSibling()) pending.push(child);
        }
    }
}
