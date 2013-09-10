package fiji.plugin.trackmate.providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotRadiusEstimatorFactory;

/**
 * A provider for the spot analyzer factories provided in the GUI.
 */
public class SpotAnalyzerProvider
{

	/**
	 * The detector names, in the order they will appear in the GUI. These names
	 * will be used as keys to access relevant spot analyzer classes.
	 */
	protected List< String > analyzerNames;

	/** Create a {@link SpotAnalyzerFactory} given an {@link ImgPlus}. */
	public static interface FactoryCreator
	{
		public < T extends RealType< T > & NativeType< T >> SpotAnalyzerFactory< T > create( ImgPlus< T > img );
	}

	/**
	 * Map a spot analyzer key to a {@link FactoryCreator factory} for the spot
	 * analyzer factory.
	 */
	protected final HashMap< String, FactoryCreator > analyzerCreators;

	protected final Model model;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the model spotFeatureAnalyzers
	 * currently available in the TrackMate trackmate. Each spotFeatureAnalyzer
	 * is identified by a key String, which can be used to retrieve new instance
	 * of the spotFeatureAnalyzer.
	 * <p>
	 * If you want to add custom spotFeatureAnalyzers to TrackMate, a simple way
	 * is to extend this factory so that it is registered with the custom
	 * spotFeatureAnalyzers and provide this extended factory to the
	 * {@link TrackMate} trackmate.
	 */
	public SpotAnalyzerProvider( final Model model )
	{
		this.model = model;
		analyzerNames = new ArrayList< String >();
		analyzerCreators = new HashMap< String, FactoryCreator >();
		registerDefaultSpotFeatureAnalyzers();
	}

	/*
	 * METHODS
	 */

	/**
	 * Register a {@link SpotAnalyzerFactory} {@link FactoryCreator creator}.
	 */
	protected void registerSpotFeatureAnalyzer( final String key, final FactoryCreator creator )
	{
		analyzerNames.add( key );
		analyzerCreators.put( key, creator );
	}

	/**
	 * Register the standard spotFeatureAnalyzers shipped with TrackMate.
	 */
	private void registerDefaultSpotFeatureAnalyzers()
	{
		registerSpotFeatureAnalyzer( SpotIntensityAnalyzerFactory.KEY, new FactoryCreator()
		{
			@Override
			public < T extends RealType< T > & NativeType< T >> SpotAnalyzerFactory< T > create( final ImgPlus< T > img )
			{
				return new SpotIntensityAnalyzerFactory< T >( model, img );
			}
		} );
		registerSpotFeatureAnalyzer( SpotContrastAndSNRAnalyzerFactory.KEY, new FactoryCreator()
		{
			@Override
			public < T extends RealType< T > & NativeType< T >> SpotAnalyzerFactory< T > create( final ImgPlus< T > img )
			{
				return new SpotContrastAndSNRAnalyzerFactory< T >( model, img );
			}
		} );
		registerSpotFeatureAnalyzer( SpotRadiusEstimatorFactory.KEY, new FactoryCreator()
		{
			@Override
			public < T extends RealType< T > & NativeType< T >> SpotAnalyzerFactory< T > create( final ImgPlus< T > img )
			{
				return new SpotRadiusEstimatorFactory< T >( model, img );
			}
		} );
	}

	/**
	 * Returns a new instance of the target spotFeatureAnalyzer identified by
	 * the key parameter, and configured to operate on the specified
	 * {@link ImgPlus}. If the key is unknown to this provider,
	 * <code>null</code> is returned.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public SpotAnalyzerFactory getSpotFeatureAnalyzer( final String key, final ImgPlus< ? > img )
	{
		final FactoryCreator creator = analyzerCreators.get( key );
		return creator == null ? null : creator.create( ( ImgPlus ) img );
	}

	/**
	 * Returns a list of the {@link SpotAnalyzer} names available through this
	 * provider.
	 */
	public List< String > getAvailableSpotFeatureAnalyzers()
	{
		return analyzerNames;
	}

}
