package io.github.ralfspoeth.graph;

import java.util.*;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.toMap;

public class ImmutableGraph<T, L extends Comparable<? super L>> extends AbstractGraph<T, L> {

    private ImmutableGraph(Collection<T> nodeCollection, List<Edge<T, L>> edges) {
        super(Set.copyOf(requireNonNullElse(nodeCollection, Set.of())), edges);

    }

    private Set<L> newLabelSet(L label) {
        Set<L> tmp = HashSet.newHashSet(1);
        tmp.add(label);
        return tmp;
    }

    private record FromTo<T>(T from, T to){}

    private final Map<FromTo<T>, Set<L>> adjacency = new HashMap<>();



    public static <T, L extends Comparable<? super L>> ImmutableGraph<T, L> usingLabelMap(Collection<T> elements, Function<T, Map<L, T>> successors) {
        List<Edge<T, L>> edges = new ArrayList<>();
        elements.forEach(e ->
                successors.apply(e).entrySet()
                        .stream()
                        .map(se -> new Edge(e, se.getValue(), se.getKey()))
                        .forEach(edges::add)
        );
        return new ImmutableGraph<>(elements, edges);
    }


    public static <T> ImmutableGraph<T, EquiLabel<Void>> usingUnlabeledCollection(Collection<T> elements, Function<T, Collection<T>> successors) {
        return usingLabelMap(elements, e -> Map.of(EquiLabel.CONSTANT, e));
    }

    public static <T, L extends Comparable<? super L>> ImmutableGraph<T, ComparableLabel<L>> usingComparableLabeledSuccessors(Collection<T> elements, Function<T, Map<L, T>> successors) {
        return usingLabelMap(elements, successors.andThen(m -> m.entrySet().stream().collect(toMap(e -> new ComparableLabel<>(e.getKey()), Map.Entry::getValue))));
    }

}