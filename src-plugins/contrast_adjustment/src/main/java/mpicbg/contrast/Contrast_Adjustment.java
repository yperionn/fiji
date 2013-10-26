package mpicbg.contrast;
/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.filter.GaussianBlur;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import mpicbg.models.AffineModel1D;
import mpicbg.models.IdentityModel;
import mpicbg.models.InterpolatedAffineModel1D;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.Tile;
import mpicbg.models.TileConfiguration;
import mpicbg.models.TranslationModel1D;



/**
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class Contrast_Adjustment implements PlugIn
{
	protected String folder = "/home/saalfeld/tmp/corinna/Data/";
	protected String folderOut = "/home/saalfeld/tmp/corinna/CA/";
	protected String tileConfigPath = "/home/saalfeld/tmp/corinna/Data/TileConfiguration.registered.txt";
	
	public interface ImageFetcher
	{
		ImageProcessor fetchSmoothedImage( int i );
		String getFileName( int i );
		double[] getCoordinate( int i );
		int getNImages();
	}
	
	/* needs RAM */
	public class LazyFetcher implements ImageFetcher
	{
		final protected ArrayList< ImageProcessor > images = new ArrayList< ImageProcessor >();
		final protected ArrayList< double[] > coordinates = new ArrayList< double[] >();
		final protected ArrayList< String > fileNames = new ArrayList< String >();

		public LazyFetcher(
				final ArrayList< String > fileNames,
				final ArrayList< double[] > coordinates )
		{
			this.coordinates.addAll( coordinates );
			this.fileNames.addAll( fileNames );
			for ( final String fileName : fileNames )
			{
				final ImagePlus img = new ImagePlus( fileName );
				final ImageProcessor ip = img.getProcessor();
				new GaussianBlur().blurGaussian( ip, 2.0, 2.0, 0.0002 );
				images.add( ip );
			}
		}
		
		public LazyFetcher( final String tileConfigPath ) throws IOException
		{
			final BufferedReader input = new BufferedReader( new FileReader( tileConfigPath ) );
			String line = null;
			
			while ( ( line = input.readLine() ) != null )
			{
				if ( !( line.startsWith( "#" ) || line.startsWith( "dim" ) || line.startsWith( "\n" ) ) )
				{
					final String[] fields = line.split( "; " );

					if ( fields.length > 1 )
					{
						final String fileName = folder + fields[ 0 ];
						
						fileNames.add( fileName );
						
						final String coords = fields[ fields.length - 1 ].replace( "(", "" ).replace( ")", "" ).replace( " ", "" );
						final String[] xy = coords.split( "," );

						coordinates.add( new double[]{ Double.parseDouble( xy[ 0 ] ), Double.parseDouble( xy[ 1 ] ) } );
						
						final ImagePlus img = new ImagePlus( fileName );
						final ImageProcessor ip = img.getProcessor();
						new GaussianBlur().blurGaussian( ip, 2.0, 2.0, 0.0002 );
						images.add( ip );
					}	
				}
			}
			
			input.close();
		}

		@Override
		public ImageProcessor fetchSmoothedImage( final int i )
		{
			return images.get( i );
		}

		@Override
		public double[] getCoordinate( final int i )
		{
			return coordinates.get( i );
		}

		@Override
		public int getNImages()
		{
			return images.size();
		}

		@Override
		public String getFileName( final int i )
		{
			return fileNames.get( i );
		}
	}
	
	final static private void applyToFloat( final FloatProcessor ip, final double[] a )
	{
		final int nPixels = ip.getWidth() * ip.getHeight();
		for( int index = 0; index < nPixels; ++index )
		{
			final double val = ip.getf( index );
			final double newVal = ( a[ 0 ]  * val ) + a[ 1 ];
			ip.setf( index, ( float )newVal );
		}
	}
	
	final static private void applyToByte( final ByteProcessor ip, final double[] a )
	{
		final int nPixels = ip.getWidth() * ip.getHeight();
		for( int index = 0; index < nPixels; ++index )
		{
			final double val = ip.getf( index );
			final double newVal = ( a[ 0 ]  * val ) + a[ 1 ];
			final int newValInt = newVal < 0 ? 0 : newVal > 255 ? 255 : ( int )Math.round( newVal );
			ip.set( index, newValInt );
		}
	}
	
	final static private void applyToShort( final ShortProcessor ip, final double[] a )
	{
		final int nPixels = ip.getWidth() * ip.getHeight();
		for( int index = 0; index < nPixels; ++index )
		{
			final double val = ip.getf( index );
			final double newVal = ( a[ 0 ]  * val ) + a[ 1 ];
			final int newValInt = newVal < 0 ? 0 : newVal > 65535 ? 65535 : ( int )Math.round( newVal );
			ip.set( index, newValInt );
		}
	}
	
	final static private void applyToRGB( final ColorProcessor ip, final double[] a )
	{
		final int nPixels = ip.getWidth() * ip.getHeight();
		for( int index = 0; index < nPixels; ++index )
		{
			final int rgb = ip.get( index );
			final int r = ( rgb >> 16 ) & 0xff;
			final int g = ( rgb >> 8 ) & 0xff;
			final int b = rgb & 0xff;
			
			final double newR = ( a[ 0 ]  * r ) + a[ 1 ];
			final double newG = ( a[ 0 ]  * g ) + a[ 1 ];
			final double newB = ( a[ 0 ]  * b ) + a[ 1 ];
			final int newRInt = newR < 0 ? 0 : newR > 255 ? 255 : ( int )Math.round( newR );
			final int newGInt = newG < 0 ? 0 : newG > 255 ? 255 : ( int )Math.round( newG );
			final int newBInt = newB < 0 ? 0 : newB > 255 ? 255 : ( int )Math.round( newB );
			ip.set( index, ( ( ( newRInt << 8 ) | newGInt ) << 8 ) | newBInt | 0xff000000 );
		}
	}
	
	
	@Override
	public void run( final String arg )
	{
		final GenericDialog gd = new GenericDialog( "Give me some input" );
		gd.addStringField( "Folder :", folder );
		gd.addStringField( "Output_Folder :", folderOut );
		gd.addStringField( "Tile_Configuration :", tileConfigPath );
		
		gd.addNumericField( "minimum_intensity :", 10, 0 );
		gd.addNumericField( "maximum_intensity :", 254, 0 );
		gd.addNumericField( "number_of_samples :", 100, 0 );
		gd.addNumericField( "lambda_1 :", 0.1, 2 );
		gd.addNumericField( "lambda_2 :", 0.1, 2 );
		
		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;
		
		final long t = System.currentTimeMillis();
		
		folder = gd.getNextString();
		folderOut = gd.getNextString();
		tileConfigPath = gd.getNextString();
		
		final int min = ( int )gd.getNextNumber();
		final int max = ( int )gd.getNextNumber();;
		final int nSamples = ( int )gd.getNextNumber();;
		final float lambda1 = ( float )gd.getNextNumber();
		final float lambda2 = ( float )gd.getNextNumber();

		final ImageFetcher fetcher;
		try
		{
			if ( true )  // has enough RAM
				fetcher = new LazyFetcher( tileConfigPath );
			else
				fetcher = null;
		}
		catch ( final IOException e )
		{
			System.out.println( "Could not open tile configuration file." );
			e.printStackTrace();
			return;
		}

		//////////////////////////// Main ////////////////////////////////


		final ArrayList< Tile< InterpolatedAffineModel1D< AffineModel1D, InterpolatedAffineModel1D< TranslationModel1D, IdentityModel > > > > tiles =
				new ArrayList< Tile< InterpolatedAffineModel1D< AffineModel1D, InterpolatedAffineModel1D< TranslationModel1D, IdentityModel > > > >();
		
		final int n = fetcher.getNImages();

		for ( int i = 0; i < n; ++i )
		{
			final InterpolatedAffineModel1D< AffineModel1D, InterpolatedAffineModel1D< TranslationModel1D, IdentityModel > > model =
					new InterpolatedAffineModel1D< AffineModel1D, InterpolatedAffineModel1D< TranslationModel1D, IdentityModel > >(
					new AffineModel1D(),
					new InterpolatedAffineModel1D< TranslationModel1D, IdentityModel >(
						new TranslationModel1D(),
						new IdentityModel(),
						lambda1
					),
					lambda2
			);
			tiles.add( new Tile< InterpolatedAffineModel1D< AffineModel1D, InterpolatedAffineModel1D< TranslationModel1D, IdentityModel > > >( model ) );
		}


		for ( int i = 0; i < n; ++i )
		{
			final ImageProcessor ip1 = fetcher.fetchSmoothedImage( i );
			final double[] coordinate1 = fetcher.getCoordinate( i );
			
			final Rectangle2D.Double rec1 = new Rectangle2D.Double(
					Math.ceil( coordinate1[ 0 ] ),
					Math.ceil( coordinate1[ 1 ] ),
					Math.floor( ip1.getWidth() ),
					Math.floor( ip1.getHeight() ) );
			
			for ( int j = i + 1; j < n; ++j )
			{
				final ImageProcessor ip2 = fetcher.fetchSmoothedImage( j );
				final double[] coordinate2 = fetcher.getCoordinate( j );
				
				final Rectangle2D.Double rec2 = new Rectangle2D.Double(
						Math.ceil( coordinate2[ 0 ] ),
						Math.ceil( coordinate2[ 1 ] ),
						Math.floor( ip2.getWidth() ),
						Math.floor( ip2.getHeight() ) );

				final Rectangle2D intersection = rec1.createIntersection( rec2 );

				if ( intersection.getWidth() >= 0 && intersection.getHeight() >= 0 )
				{
					final Roi roi1 = new Roi(
							( int )( intersection.getX() - ( coordinate1[ 0 ] ) ),
							( int )( intersection.getY() - Math.ceil( coordinate1[ 1 ] ) ),
							( int )intersection.getWidth(),
							( int )intersection.getHeight() );
					final Roi roi2 = new Roi(
							( int )( intersection.getX() - ( coordinate2[ 0 ] ) ),
							( int )( intersection.getY() - Math.ceil( coordinate2[ 1 ] ) ),
							( int )intersection.getWidth(),
							( int )intersection.getHeight() );
					
					ip1.setRoi( roi1 );
					ip2.setRoi( roi2 );

//					final int[] histogramIp1 = ip1.getHistogram();
//					double pixelCount = intersection.getWidth() * intersection.getHeight();
//					for ( int k = 0; k < min; ++k )
//						pixelCount -= histogramIp1[ k ];
//					for ( int k = max + 1; k < 256; ++k )
//						pixelCount -= histogramIp1[ k ];

					final Random generatorX = new java.util.Random( 100 );
					final Random generatorY = new java.util.Random( 100 );

					final ArrayList< PointMatch > matches = new ArrayList< PointMatch >();

					final float[] xCoord1 = new float[ nSamples ];
					final float[] yCoord1 = new float[ nSamples ];
					final float[] xCoord2 = new float[ nSamples ];
					final float[] yCoord2 = new float[ nSamples ];

					int currentMin = min;
					int rejectedSamples = 1;
					
					for ( int k = 0; k < nSamples; ++k )
					{
						final int randomX = generatorX.nextInt( ( int )intersection.getWidth() );
						final int randomY = generatorY.nextInt( ( int )intersection.getHeight() );

						final double x1 = intersection.getX() - coordinate1[ 0 ] + randomX;
						final double y1 = intersection.getY() - coordinate1[ 1 ] + randomY;
						final double x2 = intersection.getX() - coordinate2[ 0 ] + randomX;
						final double y2 = intersection.getY() - coordinate2[ 1 ] + randomY;
						
						final double v1 = ip1.getInterpolatedPixel( x1, y1 );
						final double v2 = ip2.getInterpolatedPixel( x2, y2 );

						xCoord1[ k ] = ( float )x1;
						yCoord1[ k ] = ( float )y1;
						xCoord2[ k ] = ( float )x2;
						yCoord2[ k ] = ( float )y2;

						if ( v1 < currentMin || v2 < currentMin )
						{
							--k;
							++rejectedSamples;
							if ( rejectedSamples % nSamples == 0 )
							{
								currentMin = Math.max( 0, currentMin - 2 );
							}
						}
						else
						{
//							weight = histogramIp1[v1] / pixelCount;
							final double weight = 1;
						
							final Point p = new Point( new float[]{ ( float )v1 } );
							final Point q = new Point( new float[]{ ( float )v2 } );
					
							matches.add( new PointMatch( p, q, ( float )weight ) );
						}
					}

					final ArrayList< PointMatch >inliers = new ArrayList< PointMatch >();
					
					final AffineModel1D pairWiseModel = new AffineModel1D();
					
					try
					{
						pairWiseModel.filterRansac(
							matches,
							inliers,
							1000,
							5,
							0.1f,
							20,
							2 );
					
						tiles.get( i ).connect( tiles.get( j ), inliers );
					}
					catch ( final NotEnoughDataPointsException e ) {}
				}
			}
		}


		final TileConfiguration tileConfig = new TileConfiguration();
		tileConfig.addTiles( tiles );
		try
		{
			tileConfig.optimize( 0, 1000, 1000 );
		}
		catch ( final Exception e )
		{
			System.out.println( "Exception during optimizing.  Sorry." );
			e.printStackTrace();
			return;
		}

		/*
		IJ.log("min:  " + tileConfig.getMinError());
		IJ.log("mean:  " + tileConfig.getError());
		IJ.log("max:  " + tileConfig.getMaxError());

		for (i=0; i<n; ++i){
			a = new double[2];
			tiles.get(i).getModel().toArray(a);
			IJ.log((i+1) + ": a=" + a[0] + " b=" + a[1] + "  (" + tiles.get(i).getMatches().size() + " inliers)");
		}*/


		
		for ( int i = 0; i < n; ++i )
		{
			final double[] a = new double[ 2 ];
			tiles.get( i ).getModel().toArray( a );
			final String fileName = fetcher.getFileName( i );
			
			final ImagePlus imp = new ImagePlus( fileName );
			final ImageProcessor ip = imp.getProcessor();
			
			switch ( imp.getType() )
			{
			case ImagePlus.COLOR_RGB:
				applyToRGB( ( ColorProcessor )ip, a );
				break;
			case ImagePlus.GRAY16:
				applyToShort( ( ShortProcessor )ip, a );
				break;
			case ImagePlus.GRAY32:
				applyToFloat( ( FloatProcessor )ip, a );
				break;
			default:
				applyToByte( ( ByteProcessor )ip, a );
			}
			
			IJ.save( imp, folderOut + new File( fileName ).getName() );

			//IJ.log(folderOut + filenames[i]);
			
			//save(img2, folderOut + filenames[i]);

			imp.close();
		}

		final long u = System.currentTimeMillis() - t;
		System.out.println( String.format( "%.3f", ( u * 0.001 ) ) + "s" );
	}
	
	final static public void main( final String... args )
	{
		new ImageJ();
		new Contrast_Adjustment().run( "" );
	}
}
