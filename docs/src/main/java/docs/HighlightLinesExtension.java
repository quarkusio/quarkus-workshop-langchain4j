package docs;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;

import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Adds the {@code hl_lines} option to fenced code blocks, the same syntax the
 * workshop used before the move to Roq:
 *
 * <pre>
 * ```java hl_lines="37-61 114-127"
 * </pre>
 *
 * The option is written to a {@code data-hl-lines} attribute on the rendered
 * {@code <code>} element. The browser then wraps each line in its own element
 * and marks the selected ones, so the highlighting survives syntax
 * highlighting and the copy button still copies the original text.
 * <p>
 * The extension is a CDI bean. The Markdown converter in
 * {@code quarkus-qute-web-markdown} injects every {@code org.commonmark.Extension}
 * bean and registers it with both the parser and the renderer.
 */
@ApplicationScoped
public class HighlightLinesExtension implements HtmlRenderer.HtmlRendererExtension {

    /**
     * Accepts {@code hl_lines="1-3 7"}, {@code hl_lines='1-3 7'} and
     * {@code hl_lines=1-3}. The last form stops at the first space, which is
     * all a bare option can safely cover.
     */
    private static final Pattern HL_LINES = Pattern.compile("hl_lines\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|(\\S+))");

    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder) {
        rendererBuilder.attributeProviderFactory(context -> new AttributeProvider() {
            @Override
            public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
                if (node instanceof FencedCodeBlock code && "code".equals(tagName)) {
                    String lines = lineSpec(code.getInfo());
                    if (lines != null) {
                        attributes.put("data-hl-lines", lines);
                    }
                }
            }
        });
    }

    /**
     * Extracts the line list from a fence info string, or {@code null} when
     * the block does not ask for highlighting.
     */
    static String lineSpec(String info) {
        if (info == null || info.isEmpty()) {
            return null;
        }
        Matcher matcher = HL_LINES.matcher(info);
        if (!matcher.find()) {
            return null;
        }
        String lines = matcher.group(1) != null ? matcher.group(1)
                : matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
        lines = lines == null ? "" : lines.trim();
        return lines.isEmpty() ? null : lines;
    }
}
