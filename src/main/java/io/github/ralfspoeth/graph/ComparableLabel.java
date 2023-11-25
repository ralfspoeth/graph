package io.github.ralfspoeth.basix.graph;

public record ComparableLabel<T extends Comparable<? super T>>(T label) implements Comparable<ComparableLabel<T>> {
    @Override
    public int compareTo(ComparableLabel<T> o) {
        return label.compareTo(o.label);
    }
}
