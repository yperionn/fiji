package trainableSegmentation;

import org.scijava.plugin.SciJavaPlugin;

import ij.process.ImageProcessor;

/**
 * Base interface for all the features supported by Advanced Weka Segmentation.
 * 
 * @author Johannes Schindelin
 */
public interface Feature extends SciJavaPlugin {
	String getDescriptor();

	void setParametersFromDescriptor(String descriptor);


	int getParameterCount();

	String getParameterName(int index);

	int findParameterIndex(String key);

	double getParameter(int index);

	void setParameter(int index, double value);


	ImageProcessor compute(ImageProcessor input);
}
