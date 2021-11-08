package mermaid;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public enum MermaidDiagram {
    Graph {
        public GraphFunction of() {
            return of("LR");
        }

        public GraphFunction of(String dir) {
            return () -> "graph " + dir;
        }
    },
    Arrow {
        public GraphFunction of(String l, String r) {
            return () -> toName(l) + " --> " + toName(r);
        }

        public GraphFunction of(String l, String a, String r) {
            return () -> toName(l) + " --> |" + a + "| " + toName(r);
        }
    };

    GraphFunction of(String l, String a, String r) {
        return () -> "";
    }

    GraphFunction of(String left, String right) {
        return () -> "";
    }

    GraphFunction of(String of) {
        return () -> "";
    }

    GraphFunction of() {
        return () -> "";
    }

    static String toName(String fqn) {
        var i = fqn.lastIndexOf('.');
        return i != -1 ? fqn.substring(i + 1) : fqn;
    }
}

interface GraphFunction extends GraphUtil {
    String print();

    default GraphFunction add(GraphFunction of) {
        return () -> String.join("\n", print(), of.print());
    }

    default void save(String filename) {
        save(print() + "\n", filename);
    }
}

interface GraphUtil {
    default void save(String content, String filename) {
        try {
            var path = Paths.get(filename);
            Files.createDirectories(path.getParent());
            Files.write(path, printEmbedHtml(content).getBytes());
            System.out.println("saved " + path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String printEmbedHtml(String print) {
        return (
            "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "  <meta charset=\"utf-8\">\n" +
            "</head>\n" +
            "<body>\n" +
            "<div class=\"mermaid\">\n" +
            print +
            "</div>\n" +
            "<script src=\"https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js\"></script>\n" +
            "<script>mermaid.initialize({startOnLoad:true});</script>\n" +
            "</body>\n" +
            "</html>\n"
        );
    }
}
