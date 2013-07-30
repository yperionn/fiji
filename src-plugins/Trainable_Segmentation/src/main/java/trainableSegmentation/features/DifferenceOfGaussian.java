package trainableSegmentation.features;

import org.scijava.plugin.Plugin;

import ij.plugin.filter.GaussianBlur;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import trainableSegmentation.AbstractFeature;
import trainableSegmentation.Feature;

@Plugin(type = Feature.class)
public class DifferenceOfGaussian extends AbstractFeature
{
		
	public DifferenceOfGaussian() 
	{
		super(new String[]{ "sigma1", "sigma2" });
	}

	@Override
	public ImageProcessor compute(ImageProcessor input) 
	{
		final double sigma1 = getParameter( 0 );
		final double sigma2 = getParameter( 1 );
						
		GaussianBlur gs = new GaussianBlur();
		
		ImageProcessor ip1 = input.duplicate();
		
		gs.blurGaussian(ip1, 0.4 * sigma1, 0.4 * sigma1,  0.0002);
		
		ImageProcessor ip2 = input.duplicate();			

		gs.blurGaussian(ip2, 0.4 * sigma2, 0.4 * sigma2,  0.0002);

		int width = input.getWidth();
		int height = input.getHeight();
		
		ImageProcessor ip = (input instanceof ColorProcessor) ? 
				new ColorProcessor(width, height) : new FloatProcessor(width, height);

		for (int x=0; x<width; x++)
		{
			for (int y=0; y<height; y++)
			{
				float v1 = ip1.getf(x,y);
				float v2 = ip2.getf(x,y);
				ip.setf(x,y, v2-v1);
			}
		}
		
		return ip;

	}

}
