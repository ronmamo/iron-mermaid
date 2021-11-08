package mermaid;

import static mermaid.MermaidDiagram.Arrow;
import static org.reflections.ReflectionUtils.Annotations;
import static org.reflections.ReflectionUtils.get;
import static org.reflections.scanners.Scanners.MethodsAnnotated;
import static org.reflections.scanners.Scanners.TypesAnnotated;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.reflections.Reflections;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.Scanners;
import org.reflections.util.AnnotationMergeCollector;
import org.reflections.util.NameHelper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public class MermaidDiagramBuilder implements NameHelper {

    public static void main(String[] args) {
        var basePackage = "com.mycompany.myapp";
        var reflections = new Reflections(
            basePackage,
            Scanners.values(),
            new MemberUsageScanner().filterResultsBy(s -> s.startsWith(basePackage))
        )
            .merge(new Reflections(RequestMapping.class));

        buildControllerGraph(reflections).save("./docs/controller-diagram.html");

        buildLayerGraph(reflections, basePackage).save("./docs/layer-diagram.html");
    }

    private static GraphFunction buildLayerGraph(Reflections reflections, String basePackage) {
        Map<String, Set<String>> usages = new HashMap<>();
        reflections
            .getStore()
            .get(MemberUsageScanner.class.getSimpleName())
            .forEach(
                (k, vs) ->
                    vs.forEach(
                        v -> {
                            var k1 = getPrefix(k, basePackage);
                            var v1 = getPrefix(v, basePackage);
                            if (!k1.isEmpty() && !k1.equals(v1)) usages.computeIfAbsent(v1, ignore -> new HashSet<>()).add(k1);
                        }
                    )
            );

        return usages
            .entrySet()
            .stream()
            .flatMap(e -> e.getValue().stream().map(v -> Arrow.of(e.getKey(), v)))
            .reduce(MermaidDiagram.Graph.of("LR"), GraphFunction::add);
    }

    private static GraphFunction buildControllerGraph(Reflections reflections) {
        // @RequestMapping meta annotated
        var requestMappings = reflections.get(
            TypesAnnotated
                .getAllIncluding(RequestMapping.class.getName())
                .asClass()
                .filter(t -> t.getPackage().equals(RequestMapping.class.getPackage()))
        );

        // merge annotations
        Set<Map<String, Object>> mergedAnnotations = reflections.get(
            MethodsAnnotated
                .with(requestMappings)
                .as(Method.class)
                .map(
                    method ->
                        get(
                            Annotations
                                .of(method)
                                .add(Annotations.of(method.getDeclaringClass()))
                                .filter(a -> requestMappings.contains(a.annotationType()))
                        )
                            .stream()
                            .collect(new AnnotationMergeCollector(method))
                )
        );

        // build graph
        return mergedAnnotations
            .stream()
            .flatMap(
                annotation ->
                    Stream
                        .of((Object[]) annotation.get("value"))
                        .map(
                            path ->
                                Arrow.of(
                                    path.toString().replace("{", ":").replace("}", ""),
                                    Stream
                                        .of(((RequestMethod[]) annotation.get("method")))
                                        .map(Enum::name)
                                        .collect(Collectors.joining(",")),
                                    ((Method) annotation.get("annotatedElement")).getName()
                                )
                        )
            )
            .reduce(MermaidDiagram.Graph.of(), GraphFunction::add);
    }

    //
    private static String getPrefix(String key, String prefix) {
        var split = (key.startsWith(prefix) ? key.substring(prefix.length() + 1) : "").split("\\.");
        return split.length > 1 ? split[0] : "";
    }

    private GraphFunction getGraphFunction(GraphFunction graph, String name, int i, Function<String, Set<String>> function) {
        if (i > 0) {
            for (String usage : function.apply(name)) {
                graph = graph.add(Arrow.of(name, usage));
                graph = getGraphFunction(graph, usage, i - 1, function);
            }
        }
        return graph;
    }
}
