package trainableSegmentation;

import java.util.Collection;

import org.scijava.service.Service;

import ij.ImagePlus;
import ij.ImageStack;

// TODO: use ImgLib interfaces instead, to allow for more efficient and on-the-fly computation

/**
 * The service to calculate the features for the Advanced Weka Segmentation.
 * 
 * @author Johannes Schindelin
 */
public interface FeatureService extends Service {
	Collection<Feature> getAvailableFeatures();

	Feature cloneFeature(Feature feature);

	//TODO: ImageStack is limited to 3 dimensions. Let's use ImgLib2 instead
	ImageStack computeFromDescriptors(ImagePlus image, Iterable<String> featureDescriptors);

	/**
	 * Compute a feature stack.
	 * 
	 * @param image
	 *            the image to compute the feature stack for
	 * @param features
	 * 
	 * @return
	 */
	ImageStack compute(ImagePlus image, Iterable<Feature> features);

	Collection<Feature> expandRange(Iterable<Feature> features,
			String parameter, double... values);
}
