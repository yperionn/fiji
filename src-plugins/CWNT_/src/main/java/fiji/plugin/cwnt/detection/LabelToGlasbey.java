package fiji.plugin.cwnt.detection;

import static fiji.plugin.cwnt.detection.LabelToRGB.GLASBEY_LUT;
import ij.ImagePlus;
import ij.measure.Calibration;

import java.util.Collection;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.BenchmarkAlgorithm;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorTable;
import net.imglib2.display.ColorTable8;
import net.imglib2.display.RealLUTConverter;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.display.imagej.ImageJVirtualStackARGB;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedIntType;

public class LabelToGlasbey extends BenchmarkAlgorithm {

	private final Labeling<Integer> labels;
	private ImagePlus imp;
	private double[] calibration;

	public LabelToGlasbey(Labeling<Integer> labels, double[] calibration) {
		super();
		this.labels = labels;
		this.calibration = calibration;
	}


	public ImagePlus getImp() {
		return imp;
	}


	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public boolean process() {
		long start = System.currentTimeMillis();

		long[] dimensions = new long[labels.numDimensions()];
		labels.dimensions(dimensions);
		ArrayImg<UnsignedIntType,IntArray> output = ArrayImgs.unsignedInts(dimensions);
		final int nColors = GLASBEY_LUT.size();

		Cursor<LabelingType<Integer>> cursor = labels.localizingCursor();
		RandomAccess<UnsignedIntType> target = output.randomAccess();
		while (cursor.hasNext()) {
			cursor.fwd();

			List<Integer> labeling = cursor.get().getLabeling();
			if (labeling.size() > 0) {
				int label = labeling .get(0);
				int colorIndex = label % nColors;
				target.setPosition(cursor);
				target.get().set(1+colorIndex); // leave label 0 to bg
			}
		}

		double min = 0;
		double max = 100;
//		Collection<Integer> locallabels = labels.getLabels();
//		for (Integer integer : locallabels) {
//			if (integer.doubleValue() > max) {
//				max = integer;
//			}
//		}
		
		ImageJFunctions.show(output);
		
		ColorTable lut = new ColorTable8(getGlasbeyLUT());
		Converter<UnsignedIntType, ARGBType> converter = new RealLUTConverter<UnsignedIntType>(min, max, lut);
		ImageJVirtualStackARGB<UnsignedIntType> stack = new ImageJVirtualStackARGB<UnsignedIntType>(output, converter);
		
		imp =  new ImagePlus("Labels", stack );
		Calibration cal = new Calibration();
		cal.pixelWidth = calibration[0];
		cal.pixelHeight = calibration[1];
		cal.pixelDepth = calibration[2];
		imp.setCalibration(cal);

		long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;	
	}




	public static final byte[][] getGlasbeyLUT() {

		int nColors = GLASBEY_LUT.size() + 1;

		byte[] r = new byte[nColors];
		byte[] g = new byte[nColors];
		byte[] b = new byte[nColors];

		r[0] = 0;
		g[0] = 0;
		b[0] = 0; // label = 0 -> black
		int[] col;
		for (int i = 1; i < nColors-1; i++) {
			col = GLASBEY_LUT.get(i);
			r[i] = (byte) col[0];
			g[i] = (byte) col[1];
			b[i] = (byte) col[2];
		}
//		LUT lut = new LUT(8, nColors, r, g, b);
		return new byte[][] { r, g, b };
	}

}

