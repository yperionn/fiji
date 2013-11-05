package fiji.selection;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import net.imglib2.algorithm.pathfinding.AStar;
import fiji.selection.PathFinder.PathType;
import fiji.tool.AbstractTool;
import fiji.tool.ToolWithOptions;

/**
 * A simple ImageJ plugin that generate paths over an image, following "veins"
 * of high or low intensities.
 * <p>
 * We rely on the {@link AStar} algorithm to find the path. The tool GUI offers
 * simple parameter tuning of the algorithm and exporting facilities.
 * <p>
 * How to use: <br>
 * Click and drag in the image. The path found is displayed as free line ROI.
 * Double-click on the tool icon display the configuration panel, which allows
 * to set a penalty for long path, and determine what path type search (across
 * high or low intensities). The last path is updated live when a setting is
 * changed in the configuration panel. An extra button allows exporting the path
 * to an ImageJ results table.
 * <p>
 * Limitations:<br>
 * The plugin implementation is very simple, and there is no proper task
 * handling, which might cause the plugin to be less responsive for very long
 * paths.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Oct-Nov 2013
 */
public class PathFindingTool_ extends AbstractTool implements MouseListener, MouseMotionListener, ToolWithOptions
{

	public static final String ICON = "C888D00C676L10e0C888Df0" + "C676D01Df1" + "D02CffaD22Cfb7D32Cf76D42Cfe8D52Cff9D62Cdd5D72CacfD82C9beD92C9cfDa2C8aeDb2C8beDc2C7aeDd2CdfeDe2C676Df2" + "D03Cff9D23Cee7D33Cf65D43Cea5D53Cff7D63Ccc4D73CaccD83C8adD93C9beDa3C8adLb3c3C7adDd3CdfdDe3C676Df3" + "D04CffaD24Cff8D34Cfd8D44Cf65D54Cff8D64Cde6D74CddaD84CabeD94CbcfDa4C9beDb4CaceDc4C9beDd4CdfdDe4C676Df4" + "D05Cff9D25Cee8D35Cff8D45Ce75D55Cfa6D65Cdd6L7585C9bdD95CaceDa5C9bdDb5C9beDc5C8adDd5CdfdDe5C676Df5" + "D06CffaD26Cff9D36CffaD46Cfd8D56Cf76D66Cfe7D76Cee7D86CbccD96CcdfDa6CbceDb6CbcfDc6CaceDd6CdfdDe6C676Df6" + "D07CffaD27Cee8D37Cff9D47Cee7D57Cf96D67Ce85D77Cee7D87Cbc7D97CbdeDa7CaceLb7c7CabeDd7CefdDe7C676Df7" + "D08CffbD28Cff9D38CffaD48Cff9L5868Cf66D78Cee8D88Cdd6D98CdeeDa8CddbDb8Cde9Dc8Cdd7Dd8CefdDe8C676Df8" + "D09CffaD29Cee9D39Cff9D49Cbd7D59Cad7D69Cb85D79Cd86D89Cbc5D99Cdd9Da9Ccc5Db9Ced8Dc9Cea6Dd9CeedDe9C676Df9" + "D0aCffcD2aCeeaD3aCce9D4aC9d7D5aCbe9D6aCac7D7aCf87D8aCbb7D9aCee8DaaCea6DbaCe95DcaCca6DdaCefdDeaC676Dfa" + "D0bCceaD2bC9c7D3bCad8D4bC9c7D5bCad8D6bC9c7D7bCca7D8bCe65D9bCd85DabCb95DbbCac7DcbC9c7DdbCefdDebC676Dfb" + "D0cCbe9D2cCad8D3cCbe9D4cC9d8D5cCbd9D6cCba6D7cCd95D8cCe65D9cCdb8DacCad8LbcdcCefdDecC676Dfc" + "D0dCceaD2dCad9D3dCbdaD4dCad8D5dCcb7D6dCc85D7dCbc8D8dCbb7D9dCf77DadCac8DbdCad9DcdCad8DddCdfdDedC676Dfd" + "D0eCdfdL2e3eCefdL4e9eCfedDaeCdedDbeCefdDceCefeDdeCefdDeeC676Dfe" + "C888D0fC676L1fefC888Dff";

	private PathFinder pathFinder;

	private PathType pathType = PathType.HIGH_INTENSITIES;

	private final DisplayUpdater displayUpdater;

	private double heuristicStrength = 0.5d;

	private PathFindingToolDialog dialog;

	public PathFindingTool_()
	{
		this.displayUpdater = new DisplayUpdater();
	}

	public void setMode( final PathType mode )
	{
		this.pathType = mode;
		if ( pathFinder != null )
		{
			pathFinder.setPathType( mode );
		}
		IJ.showStatus( mode.toString() );
	}


	@Override
	public void showOptionDialog()
	{
		if ( dialog == null )
		{
			dialog = new PathFindingToolDialog( heuristicStrength, pathType );
			dialog.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					if ( e == dialog.SETTINGS_CHANGED )
					{
						pathType = dialog.getPathType();
						heuristicStrength = dialog.getSliderValue();
						pathFinder.setPathType( pathType );
						pathFinder.setHeuristicStrength( heuristicStrength );
						displayUpdater.doUpdate();
					}
					else if ( e == dialog.RESULT_BUTTON_PUSHED )
					{
						generateResultTable();
					}

				}
			} );
		}
		dialog.setVisible( true );
	}

	@Override
	public void mouseDragged( final MouseEvent e )
	{
		final int x = getOffscreenX( e );
		final int y = getOffscreenY( e );
		pathFinder.setEnd( x, y );
		displayUpdater.doUpdate();
	}

	@Override
	public void mouseMoved( final MouseEvent arg0 )
	{}

	@Override
	public void mouseClicked( final MouseEvent e )
	{}

	@Override
	public void mouseEntered( final MouseEvent e )
	{}

	@Override
	public void mouseExited( final MouseEvent e )
	{}

	@Override
	public void mousePressed( final MouseEvent e )
	{
		final ImagePlus image = getImagePlus( e );
		if ( image == null )
			return;
		if ( pathFinder == null || image != pathFinder.getImage() )
		{
			pathFinder = new PathFinder( image, pathType, heuristicStrength );
		}
		final int x = getOffscreenX( e );
		final int y = getOffscreenY( e );
		pathFinder.setStart( x, y );
	}

	@Override
	public void mouseReleased( final MouseEvent e )
	{}

	@Override
	public String getToolName()
	{
		return "PathFinder";
	}

	@Override
	public String getToolIcon()
	{
		return ICON;
	}

	@Override
	protected void unregisterTool()
	{
		super.unregisterTool();
		displayUpdater.quit();
	}

	private void generateResultTable()
	{
		final ImagePlus image = pathFinder.getImage();
		if ( image == null ) { return; }

		final Roi roi = image.getRoi();
		if ( !PolygonRoi.class.isInstance( roi ) ) { return; }

		final ResultsTable table = new ResultsTable();

		final PolygonRoi proi = ( PolygonRoi ) roi;
		final int npoints = proi.getPolygon().npoints;
		final int[] xpoints = proi.getPolygon().xpoints;
		final int[] ypoints = proi.getPolygon().ypoints;

		double pathLength = 0;
		for ( int i = 0; i < npoints; i++ )
		{
			if (i >= 1) {
				pathLength += Math.sqrt( ( xpoints[ i ] - xpoints[ i - 1 ] ) * ( xpoints[ i ] - xpoints[ i - 1 ] ) * image.getCalibration().pixelWidth * image.getCalibration().pixelWidth + ( ypoints[ i ] - ypoints[ i - 1 ] ) * ( ypoints[ i ] - ypoints[ i - 1 ] ) * image.getCalibration().pixelHeight * image.getCalibration().pixelHeight );
			}

			final int[] pixelPos = new int[] { xpoints[ i ], ypoints[ i ] };
			final double[] calibratedPos = new double[ 2 ];
			calibratedPos[ 0 ] = pixelPos[ 0 ] * image.getCalibration().pixelWidth;
			calibratedPos[ 1 ] = pixelPos[ 1 ] * image.getCalibration().pixelHeight;

			table.incrementCounter();
			table.addLabel( "" + i );
			table.addValue( "Xi", pixelPos[ 0 ] );
			table.addValue( "Yi", pixelPos[ 1 ] );
			if ( pixelPos.length > 2 )
			{
				table.addValue( "Zi", pixelPos[ 2 ] );
			}
			table.addValue( "X (" + image.getCalibration().getUnit() + ")", calibratedPos[ 0 ] );
			table.addValue( "Y (" + image.getCalibration().getUnit() + ")", calibratedPos[ 1 ] );
			table.addValue( "Path length (" + image.getCalibration().getUnit() + ")", pathLength );
			table.addValue( "Intensity", image.getProcessor().get( xpoints[ i ], ypoints[ i ] ) );

		}
		table.show( "Path" );
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
			super( "ProfilePathFinding displayer thread" );
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
					if ( r > 0 && pathFinder != null )
						pathFinder.compute();
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
		new ImagePlus( "/Users/tinevez/Desktop/test3DPath.tif" ).show();
		new PathFindingTool_().run( "" );
	}

}
