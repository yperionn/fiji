package trainableSegmentation.features;

import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;

import org.scijava.plugin.Plugin;

import trainableSegmentation.AbstractFeature;
import trainableSegmentation.Feature;

@Plugin(type = Feature.class)
public class Gaussian extends AbstractFeature {

	public Gaussian() {
		super(new String[] { "sigma" });
	}

	@Override
	public ImageProcessor compute(ImageProcessor input) {
		final double sigma = getParameter(0);
		ImageProcessor ip = input.duplicate();
		GaussianBlur gs = new GaussianBlur();
		gs.blurGaussian(ip, 0.4 * sigma, 0.4 * sigma,  0.0002);
		return ip;		
	}

}
