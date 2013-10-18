package fiji.selection;

import fiji.tool.SliceListener;
import fiji.tool.SliceObserver;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageStatistics;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.pathfinding.AStar;
import net.imglib2.algorithm.pathfinding.DefaultAStar;
import net.imglib2.algorithm.pathfinding.InvertedCostAStar;
import net.imglib2.algorithm.pathfinding.ListPathIterable;
import net.imglib2.algorithm.pathfinding.PathCursor;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;

/**
 * A simple ImageJ plugin that generate paths over an image, following "veins"
 * of high or low intensities.
 * <p>
 * We rely on the {@link AStar} algorithm to find the path. The plugin GUI
 * offers simple parameter tuning of the algorithm and exporting facilities.
 * <p>
 * How to use: <br>
 * Select the target image, move to the desired plane. Launch the plugin. Using
 * ImageJ point tool, create two ROI points on the image. Adjust parameter on
 * the GUI, and path is searched and displayed as a red overlay on the image.
 * Moving one of the control ROI points causes the path to be recalculated live.
 * <p>
 * Limitations:<br>
 * The plugin implementation is very simple, and there is no proper task
 * handling, which might cause the plugin to be less responsive for very long
 * paths.
 *
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Oct 2013
 *
 */
public class ProfilePathFinding implements PlugIn
{

	private PathType pathType = PathType.HIGH_INTENSITIES;

	private int heuristicStrength = 0;

	private ImagePlus imp;

	private ListPathIterable< ? > pathIterable;

	private final DisplayUpdater updater = new DisplayUpdater();

	@SuppressWarnings( "rawtypes" )
	private RandomAccessibleInterval source;

	private double maxPixelVal;

	@Override
	public void run( final String arg )
	{
		imp = WindowManager.getCurrentImage();
		if ( null == imp ) { return; }
		Toolbar.getInstance().setTool( Toolbar.POINT );

		new SliceObserver( imp, new SliceListener()
		{
			@Override
			public void sliceChanged( final ImagePlus image )
			{
				regenerateProcessor();
			}
		} );

		imp.setOverlay( new Overlay() );
		imp.getOverlay().setStrokeColor( Color.RED );
		final MouseAdapter ma = new MouseAdapter()
		{
			@Override
			public void mouseDragged( final java.awt.event.MouseEvent e )
			{
				updater.doUpdate();
			};

		};
		imp.getCanvas().addMouseMotionListener( ma );

		final int max = ( int ) imp.getProcessor().getMax();
		final ProfilePathFindingFrame frame = new ProfilePathFindingFrame( 0, max, imp.getShortTitle() );
		frame.setVisible( true );
		final ActionListener al = new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				if ( e == frame.RESULT_BUTTON_PUSHED )
				{
					generateResultTable();
				}
				else if ( e == frame.SETTINGS_CHANGED )
				{
					pathType = frame.getPathType();
					heuristicStrength = frame.getSliderValue();
					updater.doUpdate();
				}
				else
				{
					System.err.println( "Unknown event: " + e );
				}
			}
		};
		frame.addActionListener( al );
		heuristicStrength = frame.getSliderValue();
		pathType = frame.getPathType();

		imp.getWindow().addWindowListener( new WindowAdapter()
		{

			@Override
			public void windowClosing( final WindowEvent e )
			{
				frame.dispose();
				updater.quit();
			}
		} );

		frame.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent arg0 )
			{
				imp.getCanvas().removeMouseMotionListener( ma );
				updater.quit();
			}
		} );
	}

	protected void regenerateProcessor()
	{

		final int pos = imp.getSlice();
		final ImagePlus dup = new ImagePlus( "dup", imp.getProcessor() );
		source = ImageJFunctions.wrap( dup );

		final ImageStatistics statistics = ImageStatistics.getStatistics( imp.getProcessor(), ImageStatistics.MIN_MAX, null );
		maxPixelVal = statistics.max;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	protected void compute()
	{
		final Roi roi = imp.getRoi();
		if ( !PolygonRoi.class.isInstance( roi ) ) { return; }

		final PolygonRoi pointRoi = ( PolygonRoi ) roi;

		final int nPoints = pointRoi.getNCoordinates();
		if ( nPoints < 2 ) { return; }

		final Polygon polygon = pointRoi.getPolygon();
		final long[] start = new long[] { polygon.xpoints[ 0 ], polygon.ypoints[ 0 ] };
		final long[] end = new long[] { polygon.xpoints[ nPoints - 1 ], polygon.ypoints[ nPoints - 1 ] };

		if ( start[ 0 ] < 0 || start[ 1 ] < 0 || start[ 0 ] >= imp.getWidth() || start[ 1 ] >= imp.getHeight() || end[ 0 ] < 0 || end[ 1 ] < 0 || end[ 0 ] >= imp.getWidth() || end[ 1 ] >= imp.getHeight() ) { return; }


		final AStar pathfinder;
		switch ( pathType )
		{
		case HIGH_INTENSITIES:
		{
			pathfinder = new InvertedCostAStar( source, start, end, heuristicStrength, maxPixelVal );
			break;
		}

		case LOW_INTENSITIES:
			pathfinder = new DefaultAStar( source, start, end, heuristicStrength );
			break;

		default:
			System.err.println( "Unknown path type: " + pathType );
			return;
		}

		pathfinder.process();

		final List< long[] > result = pathfinder.getResult();
		pathIterable = new ListPathIterable( source, result );

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

		final PolygonRoi nroi = new PolygonRoi( xPoints, yPoints, nSteps, Roi.POLYLINE );
		if ( null != imp.getOverlay() )
		{
			imp.getOverlay().clear();
			imp.getOverlay().add( nroi );
		}
		else
		{
			imp.setOverlay( new Overlay( nroi ) );
		}
		imp.updateAndDraw();
	}

	private void generateResultTable()
	{
		if ( null == pathIterable ) { return; }
		final ResultsTable table = new ResultsTable();

		final PathCursor< ? > cursor = pathIterable.cursor();
		final int index = 0;
		while ( cursor.hasNext() )
		{
			cursor.fwd();

			final double pathLength = cursor.length();
			final RealType< ? > type = ( RealType< ? > ) cursor.get();
			final long[] pixelPos = new long[ cursor.numDimensions() ];
			cursor.localize( pixelPos );
			final double[] calibratedPos = new double[ pixelPos.length ];
			calibratedPos[ 0 ] = pixelPos[ 0 ] * imp.getCalibration().pixelWidth;
			calibratedPos[ 1 ] = pixelPos[ 1 ] * imp.getCalibration().pixelHeight;
			if ( calibratedPos.length > 2 )
			{
				calibratedPos[ 2 ] = pixelPos[ 2 ] * imp.getCalibration().pixelDepth;
			}

			table.incrementCounter();
			table.addLabel( "" + index );
			table.addValue( "Path length", pathLength );
			table.addValue( "Xi", pixelPos[ 0 ] );
			table.addValue( "Yi", pixelPos[ 1 ] );
			if ( pixelPos.length > 2 )
			{
				table.addValue( "Zi", pixelPos[ 2 ] );
			}
			table.addValue( "X (" + imp.getCalibration().getUnit() + ")", calibratedPos[ 0 ] );
			table.addValue( "Y (" + imp.getCalibration().getUnit() + ")", calibratedPos[ 1 ] );
			if ( pixelPos.length > 2 )
			{
				table.addValue( "Z (" + imp.getCalibration().getUnit() + ")", calibratedPos[ 2 ] );
			}
			table.addValue( "Intensity", type.getRealDouble() );
		}
		table.show( "Path" );
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

	/**
	 * This is a helper class modified after a class by Albert Cardona
	 */
	private final class DisplayUpdater extends Thread
	{
		private long request = 0;

		// Constructor autostarts thread
		private DisplayUpdater()
		{
			super( "TrackMate displayer thread" );
			setPriority( Thread.NORM_PRIORITY );
			start();
		}

		private void doUpdate()
		{
			if ( isInterrupted() )
				return;
			synchronized ( this )
			{
				request++;
				notify();
			}
		}

		private void quit()
		{
			interrupt();
			synchronized ( this )
			{
				notify();
			}
		}

		@Override
		public void run()
		{
			while ( !isInterrupted() )
			{
				try
				{
					final long r;
					synchronized ( this )
					{
						r = request;
					}
					if ( r > 0 )
						compute();
					synchronized ( this )
					{
						if ( r == request )
						{
							request = 0; // reset
							wait();
						}
						// else loop through to update again
					}
				}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
			}
		}
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		// new ImagePlus( "/Users/tinevez/Desktop/Data/PathExample.tif"
		// ).show();
		new ImagePlus( "/Users/tinevez/Desktop/test3DPath.tif" ).show();
		new ProfilePathFinding().run( "" );
	}

}
