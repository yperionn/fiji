package trainableSegmentation;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;

/**
 * The service that will calculate the features for the Advanced Weka Segmentation.
 * 
 * @author Johannes Schindelin
 */
@Plugin(type = FeatureService.class)
public class DefaultFeatureService extends AbstractService implements FeatureService {

	@Parameter
	private PluginService pluginService;

	@Parameter
	private LogService log;

	private Map<String, PluginInfo<Feature>> features;
	private Map<String, PluginInfo<Feature>> featuresByName;

	@Override
	public void initialize() {
		features = new TreeMap<String, PluginInfo<Feature>>();
		featuresByName = new TreeMap<String, PluginInfo<Feature>>();

		for (final PluginInfo<Feature> info :
				pluginService.getPluginsOfType(Feature.class)) {
			features.put(info.getClassName(), info);

			final String name = info.getName();
			if (name != null && !"".equals(name)) {
				if (featuresByName.containsKey(name)) {
						log.warn("Duplicate feature: " + name
								+ " is implemented by "
								+ info.getClassName() + " and "
								+ featuresByName.get(name).getClassName());
				}
				featuresByName.put(name, info);
			}
		}
	}

	private Feature getFeatureFromDescriptor(final String descriptor) {
		int paren = descriptor.indexOf('(');
		final String name = paren < 0 ? descriptor : descriptor.substring(0, paren);
		PluginInfo<Feature> info = features.get(name);
		if (info == null) {
			info = featuresByName.get(name);
		}
		if (info == null) return null;
		try {
			final Feature feature = info.createInstance();
			feature.setParametersFromDescriptor(descriptor);
			return feature;
		} catch (InstantiableException e) {
			log.warn("Could not instantiate feature from descriptor " + descriptor, e);
			return null;
		}
	}

	@Override
	public Collection<Feature> getAvailableFeatures() {
		final List<Feature> features = new ArrayList<Feature>();
		for (final PluginInfo<Feature> info : this.features.values()) try {
			features.add( info.createInstance() );
		} catch (InstantiableException e) {
			log.warn( "Could not instatiate " + info.getClassName(), e);
		}
		return features;
	}

	@Override
	public Feature cloneFeature(Feature feature) {
		try {
			final Feature clone = feature.getClass().newInstance();
			clone.setParametersFromDescriptor( feature.getDescriptor() );
			return clone;
		} catch (InstantiationException e) {
			log.error("Could not clone " + feature, e);
		} catch (IllegalAccessException e) {
			log.error("Could not clone " + feature, e);
		}
		return null;
	}

	@Override
	public ImageStack computeFromDescriptors(ImagePlus image,
			Iterable<String> featureDescriptors) {
		final List<Feature> features = new ArrayList<Feature>();
		for (final String descriptor : featureDescriptors) {
			features.add( getFeatureFromDescriptor( descriptor ) );
		}
		return compute( image, features );
	}

	@Override
	public ImageStack compute(ImagePlus image, Iterable<Feature> features) {
		final ImageProcessor ip = image.getProcessor();
		final ImageStack result = new ImageStack(image.getWidth(), image.getHeight());
		result.addSlice("original", ip);
		for (final Feature feature : features) {
			result.addSlice(feature.getDescriptor(), feature.compute(ip));
		}
		return result;
	}

	@Override
	public Collection<Feature> expandRange(final Iterable<Feature> features,
			final String parameterName, double... values) {
		final List<Feature> result = new ArrayList<Feature>();
		for (final Feature feature : features) {
			int index = feature.findParameterIndex(parameterName);
			if (index < 0) {
				result.add(feature);
			} else {
				for (final double value : values) {
					final Feature cloned = cloneFeature(feature);
					cloned.setParameter(index, value);
					result.add(cloned);
				}
			}
		}
		return result;
	}

	private static ImagePlus loadFromResource(final String path) {
		final URL url = DefaultFeatureService.class.getResource(path);
		if (url == null) return null;
		if ("file".equals(url.getProtocol())) return new ImagePlus(url.getPath());
		return new ImagePlus(url.toString());
	}

	public static void main(String... args) {
		final ImagePlus bridge = loadFromResource("/bridge.png");
		if (bridge == null)
		{
			System.err.println("bridge.png not found");
			System.exit(1);
		}

		final Context context = new Context( FeatureService.class );
		final FeatureService featureService = context.getService( FeatureService.class );

		System.err.println( "Found features in the FeatureService " + featureService.toString() );

		final Collection<Feature> allFeatures = featureService.getAvailableFeatures();
		final Collection<Feature> features = featureService.expandRange(allFeatures, "sigma", 2, 5, 17);
		for (final Feature feature : features) {
			System.err.println( "Feature: " + feature + " with " + feature.getParameterCount() + " parameters." );
			for (int i = 0; i < feature.getParameterCount(); i++) {
				System.err.println( "Parameter " + i + ": "
						+ feature.getParameterName(i) + " = " + feature.getParameter(i));
			}
		}
		final ImageStack featureStack = featureService.compute(bridge, features);

		new ImagePlus("feature stack", featureStack).show();
	}

}
