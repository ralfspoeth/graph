package io.github.ralfspoeth.graph;

public class Path<T, L extends Comparable<? super L>> {

    private record Elem<L, T>(L label, Elem<L, T> next){}

    private final T anchor;
    private final Elem next;


    public Path(T anchor) {
        this.anchor = anchor;
        this.next = null;
    }

    private Path(T anchor, L label, T next) {
        this(anchor, new Elem(label, next));
    }

    public PathOld append(L label, T item) {

    }
}
