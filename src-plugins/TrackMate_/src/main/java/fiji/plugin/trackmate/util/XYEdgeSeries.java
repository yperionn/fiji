package fiji.plugin.trackmate.util;

import org.jfree.data.general.Series;
import org.jfree.data.xy.XYSeries;

public class XYEdgeSeries extends Series {

	/*
	 * FIELDS
	 */

	private static final long serialVersionUID = -3716934680176727207L;
	private final XYSeries startSeries = new XYSeries("StartPoints", false, true);
	private final XYSeries endSeries = new XYSeries("EndPoints", false, true);

	/*
	 * CONSTRUCTOR
	 */

	@SuppressWarnings("rawtypes")
	public XYEdgeSeries(final Comparable key) {
		super(key);
	}

	@SuppressWarnings("rawtypes")
	public XYEdgeSeries(final Comparable key, final String description) {
		super(key, description);
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public int getItemCount() {
		return startSeries.getItemCount();
	}

	public void addEdge(final double x0, final double y0, final double x1, final double y1) {
		startSeries.add(x0, y0, false);
		endSeries.add(x1, y1, false);
	}

	public Number getEdgeXStart(final int index) {
		return startSeries.getX(index);
	}

	public Number getEdgeYStart(final int index) {
		return startSeries.getY(index);
	}

	public Number getEdgeXEnd(final int index) {
		return endSeries.getX(index);
	}

	public Number getEdgeYEnd(final int index) {
		return endSeries.getY(index);
	}

}
