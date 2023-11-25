package io.github.ralfspoeth.basix.graph;

import java.util.Collection;
import java.util.Set;

public interface Graph<T, L extends Comparable<? super L>> extends Iterable<T> {

    record Edge<T, L extends Comparable<? super L>>(T to, L label){}

    boolean isEmpty();

    Set<Edge<T, L>> successorsOf(T from);
    Set<Edge<T, L>> predeccessorsOf(T from);

    Graph<T, L> subgraphFrom(T node);

    Graph<T, L> subgraphTo(T node);

    Set<T> sources();

    Set<T> sinks();

    Collection<Graph<T, L>> subgraphs();
}
