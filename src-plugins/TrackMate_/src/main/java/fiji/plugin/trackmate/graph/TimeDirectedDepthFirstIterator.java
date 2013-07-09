/**
 *
 */
package fiji.plugin.trackmate.graph;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;

public class TimeDirectedDepthFirstIterator extends SortedDepthFirstIterator<Spot, DefaultWeightedEdge> {

	public TimeDirectedDepthFirstIterator(final Graph<Spot, DefaultWeightedEdge> g, final Spot startVertex) {
		super(g, startVertex, null);
	}

	@Override
	protected void addUnseenChildrenOf(final Spot vertex) {

		final int ts = vertex.getFeature(Spot.FRAME).intValue();
		for (final DefaultWeightedEdge edge : specifics.edgesOf(vertex)) {
			if (nListeners != 0) {
				fireEdgeTraversed(createEdgeTraversalEvent(edge));
			}

			final Spot oppositeV = Graphs.getOppositeVertex(graph, edge, vertex);
			final int tt = oppositeV.getFeature(Spot.FRAME).intValue();
			if (tt <= ts) {
				continue;
			}

			if (seen.containsKey(oppositeV)) {
				encounterVertexAgain(oppositeV, edge);
			} else {
				encounterVertex(oppositeV, edge);
			}
		}
	}

}