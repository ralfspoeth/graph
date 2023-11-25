package io.github.ralfspoeth.basix.graph;

public record EquiLabel<T>(T label) implements Comparable<EquiLabel<? super T>> {

    public static final EquiLabel<Void> CONSTANT = new EquiLabel(null);
    @Override
    public int compareTo(EquiLabel o) {
        return 0;
    }
}
