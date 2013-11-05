package fiji.selection;

import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ImageStatistics;

import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.pathfinding.AStar;
import net.imglib2.algorithm.pathfinding.DefaultAStar;
import net.imglib2.algorithm.pathfinding.InvertedCostAStar;
import net.imglib2.algorithm.pathfinding.ListPathIterable;
import net.imglib2.img.display.imagej.ImageJFunctions;

public class PathFinder
{

	private final ImagePlus imp;

	private PathType pathType;

	private int xStart;

	private int yStart;

	private int xEnd;

	private int yEnd;

	private double heuristicStrength;

	private double maxPixelVal;

	@SuppressWarnings( "rawtypes" )
	private RandomAccessibleInterval source;

	public PathFinder( final ImagePlus imp, final PathType pathType, final double heuristicStrength )
	{
		this.imp = imp;
		this.pathType = pathType;
		this.heuristicStrength = heuristicStrength;
		final ImagePlus dup = new ImagePlus( "dup", imp.getProcessor() );
		this.source = ImageJFunctions.wrap( dup );
		final ImageStatistics statistics = ImageStatistics.getStatistics( imp.getProcessor(), ImageStatistics.MIN_MAX, null );
		this.maxPixelVal = statistics.max;
	}

	public ImagePlus getImage()
	{
		return imp;
	}

	public static enum PathType
	{
		HIGH_INTENSITIES( "high intensities" ), LOW_INTENSITIES( "low intensities" );

		private final String name;

		private PathType( final String name )
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	public void setPathType( final PathType pathType )
	{
		this.pathType = pathType;
	}

	public void setStart( final int x, final int y )
	{
		this.xStart = x;
		this.yStart = y;
		final ImagePlus dup = new ImagePlus( "dup", imp.getProcessor() );
		source = ImageJFunctions.wrap( dup );
		final ImageStatistics statistics = ImageStatistics.getStatistics( imp.getProcessor(), ImageStatistics.MIN_MAX, null );
		maxPixelVal = statistics.max;
	}

	public void setEnd( final int x, final int y )
	{
		this.xEnd = x;
		this.yEnd = y;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public void compute()
	{
		final long[] start = new long[] { xStart, yStart };
		final long[] end = new long[] { xEnd, yEnd };

		if ( start[ 0 ] < 0 || start[ 1 ] < 0 || start[ 0 ] >= imp.getWidth() || start[ 1 ] >= imp.getHeight() || end[ 0 ] < 0 || end[ 1 ] < 0 || end[ 0 ] >= imp.getWidth() || end[ 1 ] >= imp.getHeight() ) { return; }

		final int hs = ( int ) ( heuristicStrength * maxPixelVal );

		final AStar pathfinder;
		switch ( pathType )
		{
		case HIGH_INTENSITIES:
		{
			pathfinder = new InvertedCostAStar( source, start, end, hs, maxPixelVal );
			break;
		}

		case LOW_INTENSITIES:
			pathfinder = new DefaultAStar( source, start, end, hs );
			break;

		default:
			System.err.println( "Unknown path type: " + pathType );
			return;
		}

		pathfinder.process();

		final List< long[] > result = pathfinder.getResult();
		final ListPathIterable pathIterable = new ListPathIterable( source, result );

		final int nSteps = result.size();
		final int[] xPoints = new int[ nSteps ];
		final int[] yPoints = new int[ nSteps ];
		final Cursor< ? > cursor = pathIterable.cursor();
		int index = 0;
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			xPoints[ index ] = cursor.getIntPosition( 0 );
			yPoints[ index ] = cursor.getIntPosition( 1 );
			index++;
		}

		final PolygonRoi nroi = new PolygonRoi( xPoints, yPoints, nSteps, Roi.FREELINE );
		imp.setRoi( nroi );
		imp.updateAndDraw();
	}

	public void setHeuristicStrength( final double heuristicStrength )
	{
		this.heuristicStrength = heuristicStrength;
	}

}
