package fiji.plugin.trackmate;

public enum Dimension {
	NONE, QUALITY, INTENSITY, INTENSITY_SQUARED, POSITION, VELOCITY,
	/* We separate length and position so that x,y,z are plotted on a different
	 * graph from spot sizes for non-numeric features. */
	LENGTH, TIME, ANGLE, STRING;

}