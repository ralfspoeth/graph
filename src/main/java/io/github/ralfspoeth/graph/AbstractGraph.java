package io.github.ralfspoeth.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

abstract class AbstractGraph<T, L extends Comparable<? super L>> implements Graph<T, L>  {
    protected final Set<T> nodes;
    protected final Set<T> sources = new HashSet<>();
    protected final Set<T> sinks = new HashSet<>();

    AbstractGraph(Set<T> elems, List<Edge<T, L>> edges) {
        nodes = elems;
    }

}
