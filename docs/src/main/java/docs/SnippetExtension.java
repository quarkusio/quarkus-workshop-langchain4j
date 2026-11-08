package docs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.qute.RawString;
import io.quarkus.qute.TemplateExtension;

@TemplateExtension(namespace = "snippet")
public class SnippetExtension {

    /**
     * Section markers as used by pymdownx snippets, e.g.
     * {@code <!-- -8<- [start:tag] -->} in XML/properties files or
     * {@code // --8<-- [start:tag] } in Java files.
     */
    private static final Pattern MARKER = Pattern.compile("(?:--8<--|-8<-)\\s*\\[\\s*(start|end)\\s*:\\s*([^\\]]+?)\\s*\\]");

    private static final Path SNIPPET_ROOT = findSnippetRoot();

    /**
     * The snippets reference files relative to the repository root.
     * Depending on how the site is started (from the repository root with
     * {@code ./mvnw -f docs quarkus:dev} or from the {@code docs} directory),
     * the working directory differs, so we look for the root instead of
     * assuming a fixed location.
     */
    private static Path findSnippetRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("section-1"))
                    && Files.isDirectory(dir.resolve("docs"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        return Path.of("..");
    }

    /**
     * Inserts a whole file, e.g. {@code {snippet:insert("section-1/step-01/pom.xml")}}.
     */
    static RawString insert(String file) throws IOException {
        Path path = resolve(file);
        if (path == null) {
            return raw("<!-- snippet not found: " + file + " -->");
        }
        return raw(Files.readString(path));
    }

    /**
     * Inserts a part of a file.
     *
     * <p>
     * A named section between {@code --8<-- [start:tag]} and {@code --8<-- [end:tag]}
     * markers, e.g. {@code {snippet:insert("section-1/step-05/pom.xml", "easy-rag")}}.
     *
     * <p>
     * A line range (1-based, inclusive), encoded with the start line appended to the
     * file path and the end line as second argument, e.g.
     * {@code {snippet:insert("section-3/step-05/pom.xml:38", "73")}} for lines 38 to 73.
     */
    static RawString insert(String file, String tag) throws IOException {
        int colon = file.lastIndexOf(':');
        if (colon > 0 && tag.chars().allMatch(Character::isDigit)
                && file.substring(colon + 1).chars().allMatch(Character::isDigit)) {
            Path path = resolve(file.substring(0, colon));
            int startLine = Integer.parseInt(file.substring(colon + 1));
            int endLine = Integer.parseInt(tag);
            if (path == null) {
                return raw("<!-- snippet not found: " + file + " -->");
            }
            return raw(lines(path, startLine, endLine));
        }
        Path path = resolve(file);
        if (path == null) {
            return raw("<!-- snippet not found: " + file + " -->");
        }
        return raw(section(path, tag));
    }

    /**
     * Wraps snippet text without the newline that ends it. Each call sits on
     * its own line in the fence, and the line break after the call already
     * ends the last snippet line, so keeping it would leave an empty line
     * between stacked snippets.
     */
    private static RawString raw(String content) {
        if (content.endsWith("\n")) {
            content = content.substring(0, content.length() - 1);
        }
        return new RawString(content);
    }

    private static Path resolve(String file) {
        Path path = SNIPPET_ROOT.resolve(file);
        return Files.exists(path) ? path : null;
    }

    private static String lines(Path path, int startLine, int endLine) throws IOException {
        List<String> all = Files.readAllLines(path);
        if (startLine < 1 || endLine < startLine || startLine > all.size()) {
            return "<!-- invalid line range " + startLine + "-" + endLine + " in " + path + " -->";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = startLine; i <= Math.min(endLine, all.size()); i++) {
            sb.append(all.get(i - 1)).append("\n");
        }
        return sb.toString();
    }

    private static String section(Path path, String tag) throws IOException {
        boolean[] recording = { false };
        boolean[] found = { false };
        StringBuilder sb = new StringBuilder();
        for (String line : Files.readAllLines(path)) {
            Matcher m = MARKER.matcher(line);
            if (m.find()) {
                if (!recording[0] && "start".equals(m.group(1)) && tag.equals(m.group(2))) {
                    recording[0] = true;
                    found[0] = true;
                    continue;
                } else if (recording[0] && "end".equals(m.group(1)) && tag.equals(m.group(2))) {
                    recording[0] = false;
                    continue;
                }
            }
            if (recording[0]) {
                sb.append(line).append("\n");
            }
        }
        if (!found[0]) {
            return "<!-- tag '" + tag + "' not found in " + path + " -->";
        }
        return sb.toString();
    }
}
