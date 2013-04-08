package fiji.plugin.cwnt.test;

import fiji.plugin.cwnt.detection.GaussianGradient2D;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;
import java.util.List;

import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;


public class GaussianGradient_TestDrive {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String[] args) {
		
//		File testImage = new File("/Users/tinevez/Projects/BRajaseka/Data/point.tif");
		File testImage = new File("/Users/tinevez/Projects/BRajasekaran/Data/Meta-nov7mdb18ssplus-embryo2-1.tif");
		
		
		ImageJ.main(args);
		ImagePlus imp = IJ.openImage(testImage.getAbsolutePath());
		imp.show();

		Img<? extends RealType> source = ImageJFunctions.wrap(imp);

		
		System.out.print("Gaussian gradient 2D ... ");
		GaussianGradient2D grad = new GaussianGradient2D(source, 1);
//		GaussianGradient2D grad = new GaussianGradient2D(floatImage, 1);
		grad.setNumThreads();
		if (!grad.checkInput() || !	grad.process() ) {
			System.out.println(grad.getErrorMessage());
			return;
		}
		
		Img norm = grad.getResult();
		List<Img> list = grad.getGradientComponents();
		System.out.println("dt = "+grad.getProcessingTime()/1e3+" s.");
		
		
		ImageJFunctions.show(norm);
		ImageJFunctions.show(list.get(0));
		ImageJFunctions.show(list.get(1));
		

		
	}
	
}
