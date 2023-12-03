package io.github.ralfspoeth.graph;

import java.util.*;
import java.util.function.Function;

/**
 * General purpose implementation of a directed Graph.
 *
 * <p>
 * A directed graph is a structure which contains a set
 * of nodes which themselves may be interconnected.
 * A node may be a successor or predecessor of another
 * node.
 * </p>
 *
 * <p>
 * The class is thread-safe.
 * </p>
 *
 * <p>
 * The implementation is very efficient in terms of time
 * complexity, but may be weak when strongly interconnected
 * and big. Every lookup of a Node for an object or a node
 * is performed in <i>t&nbsp;=&nbsp;O</i>(1), (order of 1).
 * The set of sources and sinks are always synchronized with
 * the current state of the graph.
 */
abstract class AbstractGraph<T, L extends Comparable<? super L>> implements Graph<T, L> {

    final static class Node<T, L extends Comparable<? super L>>  {

        final T item;

        Node(T item) {
            this.item = item;
        }

        Map<L, Node<T, L>> successors = new HashMap<>();

        Map<L, Node<T, L>> predecessors = new HashMap<>();

        /**
         * Calculates all predecessors of this node excluding this, which
         * is the union of its own predecessors and the all predecessors
         * of its predecessors, recursively.
         * <p>
         * Note: this is a calculation, thus follows not the naming
         * convention for property getters. the number of all successors
         * may change without notificatio to this node.
         * <p>
         * the calculation time is O(n) where n is the number of nodes in the
         * graph.
         * /
        Set<Node> allPredecessors() {
            Set<Node> temp = new HashSet<Node>(predecessors);
            for (Node n : predecessors) {
                temp.addAll(n.allPredecessors());
            }
            return temp;
        }


        /**
         * Calculates all successors of this node excluding this, which
         * is the union of its own successors and the all successors
         * of its successors, recursively.
         * <p>
         * Note: this is a calculation, thus follows not the naming
         * convention for property getters. the number of all successors
         * may change without notificatio to this node.
         * <p>
         * the calculation time is O(n) where n is the number of nodes in the
         * graph.
         * /
        Set<Node> allSuccessors() {
            Set<Node> temp = new HashSet<Node>(successors);
            for (Node n : successors) {
                temp.addAll(n.allSuccessors());
            }
            return temp;

        }*/

        boolean isSink() {
            return successors.isEmpty();
        }

        boolean isSource() {
            return predecessors.isEmpty();
        }
    }


    record Edge<T, L extends Comparable<? super L>>(Node<T, L> from, Node<T, L> to, L weight) {}

    record EdgeKey<T, L extends Comparable<? super L>>(Node<T, L> from, Node<T, L> to) {}


    /**
     * internally used for faster lookups of nodes
     */
    Map<T, Node> lookup = new HashMap<>();


    protected Set<Node> nodes = new HashSet<Node>();


    protected Set<Node> sources = new HashSet<Node>();

    protected Set<Node> sinks = new HashSet<Node>();


    /**
     * maintaing a List of edges.
     * <p>
     * it is often more efficient to iterate
     * through the edges rather than through nodes.
     */
    public Map<EdgeKey, Edge> edges = new HashMap<EdgeKey, Edge>();

    /**
     * Getter of property "edges"
     *
     * @return an immutable, ordered, indexed set of edges.
     */
    public synchronized Collection<Edge> getEdges() {
        return Collections.unmodifiableCollection(edges.values());
    }

    /**
     * Add a <code>Node</code> to the graph.
     * <p>
     * Each instance of <code>Node</code> may be a member of a single
     * graph only, and not be added twice. After adding the node to the
     * graph, both the number of sources and sinks are increased by 1.
     *
     * @param n the <code>Node</code> to be added.
     * @throws IllegalArgumentException when either the node is already
     *                                  contained in this or another graph.
     */
    public synchronized void addNode(Node n) {
        // add to the set of nodes
        nodes.add(n);
        // a new node MUST be a source and a sink
        sources.add(n);
        sinks.add(n);
        // add to lookup dictionary
        lookup.put(n.item, n);
    }

    /**
     * Convenience method; add an arbitray object to the graph.
     * <p>
     * If the object is already represented by another node, that
     * one is returned; the newly created otherwise.
     * <p>
     * (Each user object may only be added once to a graph therefore!)
     *
     * @param anObject the object to be added.
     * @return the newly created node or the node which already holds
     * the object.
     */
    public synchronized Node add(T anObject) {
        Node n = lookup.get(anObject);
        if (n == null) {
            Node temp = new Node(anObject);
            addNode(temp);
            return temp;
        } else {
            return n;
        }
    }


	/*public synchronized boolean add(T anObject) {
		Node n = lookup.get(anObject);
		if(n == null) {
			addNode(new Node(anObject));
			return true;
		}
		else {
			return false;
		}
	}*/


    /**
     * Remove a node from the graph.
     * <p>
     * If the node is not a member of the graph,
     * nothing happens.
     *
     * @param n the node to be removed (may not be <code>null</code>.
     * @throws NullPointerException when <code>n</code> is <code>null</code>.
     */
    public synchronized void removeNode(Node n) {
        // remove node from node list
        // this tells us wether the graph is changed.
        if (nodes.remove(n)) {
            // remove node from caches
            sources.remove(n);
            sinks.remove(n);
            // remove dependencies
            for (Node temp : n.successors) {
                temp.predecessors.remove(n);
                // if no predecessors, it's a source
                if (temp.predecessors.size() == 0) {
                    sources.add(temp);
                }
            }

            for (Node temp : n.predecessors) {
                temp.successors.remove(n);
                // temp may have no successors any more.
                if (temp.successors.size() == 0) {
                    sinks.add(temp);
                }
            }

            // there may be edges pointing to the node
            for (Iterator<EdgeKey> keys = edges.keySet().iterator(); keys.hasNext(); ) {
                EdgeKey key = keys.next();
                if (key.from == n || key.to == n) {
                    keys.remove();
                }
            }

            // just in case someone wants to reuse the node...
            n.successors.clear();
            n.predecessors.clear();

            // remove node from lookup
            lookup.remove(n.item);
        }
    }

    /**
     * convenience method; remove an arbitray object.
     *
     * @param anObject the object used to identify the node to be removed
     * @return a Node when found, <code>null</code>otherwise.
     * @throws NullPointerException when <code>anObject</code> is
     *                              <code>null</code>.
     */
    public synchronized boolean remove(T anObject) {
        Node t = lookup.get(anObject);
        if (t != null) {
            removeNode(t);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Lookup a <code>Node</code> for object <code>userObject</code>.
     * <p>
     * The lookup is fast (order of 1).
     *
     * @param userObject the object for which the node shall be found.
     * @return the Node which wraps the given object, or <code>null</code>
     * if not found.
     */
    public synchronized Node getNodeFor(T userObject) {
        return lookup.get(userObject);
    }


    public synchronized Edge getEdge(Node from, Node to) {
        return edge(from, to);
    }

    public synchronized List<Edge> getOutboundEdges(Node from) {
        List<Edge> tmp = new ArrayList<Edge>(from.successors.size());
        for (Node n : from.successors) {
            tmp.add(edge(from, n));
        }
        return Collections.unmodifiableList(tmp);
    }

    public synchronized List<Edge> getInboundEdges(Node to) {
        List<Edge> tmp = new ArrayList<Edge>(to.predecessors.size());
        for (Node n : to.predecessors) {
            tmp.add(edge(n, to));
        }
        return Collections.unmodifiableList(tmp);
    }

    public synchronized Edge getEdge(T from, T to) {
        return edge(getNodeFor(from), getNodeFor(to));
    }

    /**
     * Link two nodes, meaning make <code>from</code> a predecessor
     * of <code>to</code> and <code>to</code> a successor of
     * <code>from</code>.
     *
     * @param from the starting point of the link.
     * @param to   the end point of the link.
     * @throws IllegalArgumentException when either node is not
     *                                  a node of the graph.
     */
    public synchronized void link(Node from, Node to, double weight) {

        // test whether in my list of nodes.
        if (from == null || to == null) {
            throw new IllegalArgumentException
                    ("Both nodes must be in THIS graph.");
        }

        // make "to" a successor of "from"
        // and "from" a predecessor of "to"
        from.successors.add(to);
        to.predecessors.add(from);

        // "from" is not a sink anymore
        sinks.remove(from);

        // to is not a source anymore
        sources.remove(to);

        // plus: we have a new age... uhm, edge.
        edges.put(key(from, to), new Edge(from, to, weight));
    }

    public synchronized void link(T from, T to, double weight) {
        link(getNodeFor(from), getNodeFor(to), weight);
    }

    /**
     * Unlink two nodes.
     * <p>
     * If the given two nodes were not connected before (or in the
     * opposite direction instead), the method does nothing at all.
     *
     * @param from the starting point of the link.
     * @param to   the end point of the link.
     * @throws IllegalArgumentException when either node is not
     *                                  in the graph.
     */
    public synchronized void unlink(Node from, Node to) {

        // first, test whether these are my nodes...
        if (from == null || to == null)
            throw new IllegalArgumentException
                    ("Both nodes must be in THIS graph.");

        // remove the pointers, and test whether a link existed
        if (from.successors.remove(to) && to.predecessors.remove(from)) {

            // when from has no successors anymore, it's a sink
            if (from.successors.size() == 0)
                sinks.add(from);

            // when to has no predecessors anymore, it's a source
            if (to.predecessors.size() == 0)
                sources.add(to);

            // we must remove it from the edges
            edges.remove(key(from, to));
        }
    }

    public synchronized void unlink(T from, T to) {
        unlink(getNodeFor(from), getNodeFor(to));
    }

    // testing

    /**
     * Getter of property "empty".
     * <p>
     * the graph is empty when the number of nodes contained
     * in the graph is 0.
     *
     * @return true when the number of nodes is 0, false otherwise.
     */
    public synchronized boolean isEmpty() {
        return nodes.size() == 0;
    }

    /**
     * Getter of property "size".
     *
     * @return the number of nodes contained in the graph.
     */
    public synchronized int getSize() {
        return nodes.size();
    }

    /**
     * Check whether the graph has cyclics, i.e. whether
     * there are nodes in the graph which are reachable
     * through at least a single other node from themselves.
     *
     * <p>
     * Emptry graphs are non-cyclic by definition.
     * </p>
     *
     * <p>
     * The method is expensive yet efficient
     * and may block other threads. Time complexity is linear
     * in the number of nodes.
     * </p>
     *
     * <p>
     * The return value should be cached. The property doesn't
     * change when objects are added and removed. A cyclic graph
     * cannot become non-cyclic when new edges are added, a non-cyclic
     * graph cannot become cyclic when edges are removed.
     * </p>
     *
     * <p>
     * The algorithm works as follows:<br>
     * 1: clone this graph (testing is destructive!)<br>
     * 2: if the clone has no nodes, the graph is non-cyclic. terminate.<br>
     * 3: if the clone has no sinks, the graph is cyclic. terminate.<br>
     * 4: remove a source without successors from the graph.<br>
     * 5: continue with step 2.
     * </p>
     *
     * <p>
     * This is the fastest algorithm for larger graphs
     * with either sparse or dense connections possible,
     * provided we use effective data structures what we do.
     * </p>
     *
     * @return true when the graph has no cycles, false otherwise.
     */
    public synchronized boolean hasCycles() {
        // before we clone, we check what we can
        // determine WITHOUT destroying the graph.
        // cloning is not TOO inexpensive.

        // the graph is empty: no cycles
        if (nodes.size() == 0) {
            return false;
        }
        // the graph is not empty but has no sinks: it has cycles
        else if (sinks.size() == 0) {
            return true;
        }
        // the is not empty but all nodes are sinks: it has no cycles
        else if (sinks.size() == nodes.size()) {
            return false;
        }
        // other tests ARE destructive; we need to clone
        // the graph and test the clone.
        else {
            Graph<T> clone = this.clone();
            return clone.cyclic();
        }
    }

    /**
     * method may and will only be performed on
     * a clone.
     */
    private boolean cyclic() {
        // an empty graph is non-cyclic
        if (nodes.size() == 0) {
            return false;
        }
        // a non-empty graph with no sinks IS cyclic!
        else if (sinks.size() == 0) {
            return true;
        }
        // a non-empty graph with no links is acyclic!
        else if (sinks.size() == nodes.size()) {
            return false;
        } else {
            // pick a single node from the sinks
            List<Node> s = new ArrayList<Node>(sinks);
            for (Node n : s) {
                removeNode(n);
            }
            return cyclic();
        }

    }


    /**
     * Split the graph if it has <i>clowds</i>.
     *
     * <p>
     * A clowd is a set of nodes which have no
     * connection to other nodes in the graph.
     * </p>
     *
     * <p>
     * The method is expensive yet efficient.
     * </p>
     *
     * @return null if graph has no nodes, an array containing
     * <code>this.clone()</code> when the graph is fully connected,
     * or an array of graphs each of which represents a clowd.
     */
    public synchronized List<Graph<T>> split() {
        if (nodes.size() == 0) {
            return null;
        } else if (sources.size() < 2) {
            return Collections.singletonList(this.clone());

        } else {
            // array of graphs to be returned
            List<Graph<T>> temp = new ArrayList<Graph<T>>();

            List<Set<Node>> ssuc = new ArrayList<Set<Node>>(sources.size());

            // graph sources
            List<Set<Node>> gsources = new ArrayList<Set<Node>>(sources.size());
            int k = 0;

            // clowds must have a source as their starting point.
            for (Node source : sources) {
                Set<Node> snt = new HashSet<Node>();
                snt.add(source);
                gsources.add(snt);

                // set of all successors for each source
                ssuc.add(new HashSet<Node>(source.allSuccessors()));
            }

            // now, find all sets which don't have an intersection
            // with any of the other sets.
            for (int i = 0, len = gsources.size() - 1; i < len; i++) {

                // only when not nulled out previously
                if (gsources.get(i) != null) {
                    // all others...
                    for (int j = i + 1; j <= len; j++) {
                        // may have been nulled out before...
                        if (gsources.get(j) != null) {
                            // when two sets are disjoint, the
                            // next operation returns false;
                            // removeAll returns true iff
                            // it modifies s.
                            boolean joint = (ssuc.get(i).removeAll(ssuc.get(j)));
                            // if not disjoint,
                            // add jth source to ith,
                            // and jth successors to ith,
                            // and set jth sources to
                            if (joint) {
                                gsources.get(i).addAll(gsources.get(j));
                                gsources.set(j, null);
                                ssuc.get(i).addAll(ssuc.get(j));
                                ssuc.set(j, null);
                            }
                        }
                    }
                }
            }

            // count graphs in clowd
            int num = 0;
            for (int i = 0, len = gsources.size(); i < len; i++) {
                if (gsources.get(i) != null)
                    num++;
            }

            temp = new ArrayList<Graph<T>>(num);
            num = 0;

            // let's build a sub-graph for each disjoint set of nodes
            for (int i = 0, len = gsources.size(); i < len; i++) {
                if (ssuc.get(i) != null) {
                    // create graph induced by the disjoint subset of nodes.
                    ssuc.get(i).addAll(gsources.get(i));
                    temp.add(new Graph<T>(ssuc.get(i)));
                    // remove these nodes from the set of remaining nodes
                    num++;
                }
            }
            return temp;
        }
    }


    /**
     * Calculates all paths from the given "source" to the given
     * "sink" nodes. An implicit prerequisite is that the graph
     * is non-cyclic; if it has cycles, the method may not return
     * or may not return all potential paths (in the best case).
     * <p>
     * The method is comparably time-consuming.
     */
    public synchronized List<AbstractGraph.Path> paths(Node source, Node sink) {
        if (source == null || sink == null) {
            throw new IllegalArgumentException(
                    "source and sink must both be non-null members of the graph"
            );
        } else if (source == sink) {
            return Collections.singletonList(new AbstractGraph.Path(source));
        } else {
            List<AbstractGraph.Path> target = new LinkedList<AbstractGraph.Path>();
            target.add(new AbstractGraph.Path(source));
            searchPaths(sink, target);
            return Collections.unmodifiableList(target);
        }
    }


    public synchronized List<AbstractGraph.Path> paths(T from, T to) {
        return paths(getNodeFor(from), getNodeFor(to));
    }

    private void searchPaths(Node sink, List<AbstractGraph.Path> result) {

        // number of hits
        int hits = 0;

        // number of elements
        int elem = 0;

        // cruise through all paths
        for (ListIterator<AbstractGraph.Path> li = result.listIterator(); li.hasNext(); ) {

            elem++;

            AbstractGraph.Path p = li.next();

            Node endPoint = p.last();

            if (endPoint == sink) {
                hits++;
            }
            // if end-point of path is a sink
            // the path cannot lead to the target node
            else if (endPoint.isSink()) {
                elem--;
                li.remove();
            }
            // if end-point is the target node,
            // the path is complete, otherwise,
            // travel on.
            else {
                li.remove();
                elem--;
                for (Node suc : endPoint.successors) {
                    AbstractGraph.Path np = p.duplicate();
                    np.append(edge(endPoint, suc));
                    li.add(np);
                    elem++;
                }
            }
        }
        if (hits < elem) {
            searchPaths(sink, result);
        } else {
            assert hits == elem;
        }

    }


    public synchronized void clear() {
        nodes.clear();
        edges.clear();
        lookup.clear();
        sources.clear();
        sinks.clear();
    }

    public synchronized void removeAll(Collection<Node> nodes) {
        for (Node n : nodes) {
            removeNode(n);
        }
    }

    public synchronized void retainAll(Collection<Node> nodes) {
        for (Node n : new ArrayList<Node>(nodes)) {
            if (!this.nodes.contains(n)) {
                removeNode(n);
            }
        }
    }


    /**
     * This operation returns a copy of the graph which contains
     * a shallow copy of the internal structures.
     *
     * <p>
     * The cloned graph contains a set of newly created nodes.
     * These nodes reference the same user object as the nodes
     * in the original.
     * </p>
     *
     * <p>
     * Unfortunately, the cloned graph <code>clone = g.clone();</code>
     * of graph <code>g</code> returns <code>false</code> when
     * tested for equality: <code>clone.equals(g)</code> is <code>false</code>.
     * This is in compliance with the specification of <code>clone()</code>,
     * though not recommended.
     * </p>
     *
     * @return a copy of the graph.
     * @see java.lang.Object
     */
    public Graph<T> clone() {
        synchronized (this) {

            // new graph
            Graph<T> clone = new Graph<T>(this);
            return clone;
        }
    }

    /**
     * dump to console
     */
    public synchronized void dump() {
        for (Node source : sources) {
            source.dump(0);
        }
    }


}

    protected final Set<T> nodes;
    protected final Set<T> sources = new HashSet<>();
    protected final Set<T> sinks = new HashSet<>();

    protected AbstractGraph(Set<T> elems, Function<T, Map<L, T>> deps) {
        nodes = elems;
        sources.addAll(elems);
        sinks.addAll(elems);

    }

    public final class PathOld implements Comparable<PathOld> {

        /**
         * A path-element.
         * <p>
         * Each path element contains of a <code>weight</code>
         * which represents the cost on the way to
         * the next node, or <code>target</code>.
         * </p>
         * <p>
         * Note that the first node of a path is not
         * enumerated as an element, instead, it's the
         * anchor or first node of the path.
         * </p>
         */
        public final class Element {
            /**
             * the next node in this path.
             */
            public final Node target;
            /**
             * the weight of the edge to this path.
             */
            public final double weight;

            Element(Node n, double w) {
                this.target = n;
                this.weight = w;
            }
        }

        // edges
        private LinkedList<Node.Edge> edges = new LinkedList<Node.Edge>();

        // anchor, necessary for empty paths
        private Node anchor = null;

        // current pointers
        private transient int len = Integer.MIN_VALUE;
        private transient Iterator<Node.Edge> iter = null;
        private transient Node.Edge current = null;

        // package private
        PathOld(Node startingPoint) {
            this.anchor = startingPoint;
        }

        // package private append method
        PathOld append(Node.Edge e) {
            if (iter != null) throw new IllegalStateException(
                    "Path in use!"
            );
            else if (e == null) {
                throw new NullPointerException(
                        "Edge may not be null"
                );
            } else if (e.from != last()) {
                throw new IllegalArgumentException(
                        "Last edge.to not given e.from"
                );
            }
            len = Double.NEGATIVE_INFINITY;
            edges.add(e);
            return this;
        }

        PathOld append(PathOld p) {
            if (iter != null) {
                throw new IllegalStateException(
                        "Path in use!"
                );
            } else if (p == null) {
                throw new NullPointerException(
                        "Path added may not be null"
                );
            } else if (p.anchor != last()) {
                throw new IllegalArgumentException(
                        "Path's anchor not end node of this path"
                );
            } else {
                for (Node.Edge e : p.edges) {
                    edges.add(e);
                }
                return this;
            }
        }

        PathOld pop() {
            if (iter != null) throw new IllegalStateException(
                    "Path in use!"
            );
            else if (edges.isEmpty()) {
                throw new IllegalStateException(
                        "first node cannot be removed"
                );
            } else {
                edges.removeLast();
                return this;
            }
        }

        /**
         * length of the path, calculated from
         * the weight of the edges.
         */
        public int length() {
            if (len < 0) {
                int d = 0;
                for (Node.Edge e : edges) {
                    d += 1;
                }
                len = d;
            }
            return len;
        }

        /**
         * compare the length of two paths.
         */
        public int compareTo(PathOld p) {
            double distance = length() - p.length();
            int intdist = (int) distance;
            if (intdist != 0) return intdist;
            else {
                if (distance < 0) return -1;
                else return 1;
            }
        }


        /**
         * The starting, first node, or anchor node of
         * the path.
         */
        public Node first() {
            iter = edges.iterator();
            return anchor;
        }


        public Node last() {
            if (edges.isEmpty()) {
                return anchor;
            } else {
                return edges.getLast().to;
            }
        }

        /**
         *
         */
        public Element next() {
            if (iter == null) {
                throw new IllegalStateException("Must call 'first' before using 'next'");
            } else if (!iter.hasNext()) {
                throw new IllegalStateException("No more elements");
            }
            // move forward
            current = iter.next();
            return new Element(current.to, current.weight);
        }


        PathOld duplicate() {
            PathOld dup = new PathOld(anchor);
            for (Node.Edge e : edges) {
                dup.edges.add(e);
            }
            return dup;
        }


        public boolean hasNext() {
            return iter.hasNext();
        }


        /**
         * Convenience method, lists all nodes from the source
         * to the target, starting with the source in the path's order.
         */
        public List<Node> nodes() {
            List<Node> ln = new ArrayList<Node>(edges.size() + 1);
            ln.add(first());
            for (Node.Edge e : edges) {
                ln.add(e.getTo());
            }
            return ln;
        }


        public void dump() {
            System.out.print(first());
            for (Node.Edge e : edges) {
                System.out.print("-");
                System.out.print(e.weight);
                System.out.print("->");
                System.out.print(e.to);
            }
            System.out.println("; length: " + length());
        }

    }
}
