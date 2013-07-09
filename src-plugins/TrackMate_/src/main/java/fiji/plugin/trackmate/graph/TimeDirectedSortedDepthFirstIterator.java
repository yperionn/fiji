/**
 *
 */
package fiji.plugin.trackmate.graph;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;

public class TimeDirectedSortedDepthFirstIterator extends SortedDepthFirstIterator<Spot, DefaultWeightedEdge> {

	public TimeDirectedSortedDepthFirstIterator(final Graph<Spot, DefaultWeightedEdge> g, final Spot startVertex, final Comparator<Spot> comparator) {
		super(g, startVertex, comparator);
	}

	@Override
	protected void addUnseenChildrenOf(final Spot vertex) {

		// Retrieve target vertices, and sort them in a TreeSet
		final TreeSet<Spot> sortedChildren = new TreeSet<Spot>(comparator);
		// Keep a map of matching edges so that we can retrieve them in the same order
		final Map<Spot, DefaultWeightedEdge> localEdges = new HashMap<Spot, DefaultWeightedEdge>();

		final int ts = vertex.getFeature(Spot.FRAME).intValue();
		for (final DefaultWeightedEdge edge : specifics.edgesOf(vertex)) {

			final Spot oppositeV = Graphs.getOppositeVertex(graph, edge, vertex);
			final int tt = oppositeV.getFeature(Spot.FRAME).intValue();
			if (tt <= ts) {
				continue;
			}

			if (!seen.containsKey(oppositeV)) {
				sortedChildren.add(oppositeV);
			}
			localEdges.put(oppositeV, edge);
		}

		final Iterator<Spot> it = sortedChildren.descendingIterator();
		while (it.hasNext()) {
			final Spot child = it.next();

			if (nListeners != 0) {
				fireEdgeTraversed(createEdgeTraversalEvent(localEdges.get(child)));
			}

			if (seen.containsKey(child)) {
				encounterVertexAgain(child, localEdges.get(child));
			} else {
				encounterVertex(child, localEdges.get(child));
			}
		}
	}

}