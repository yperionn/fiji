package fiji.plugin.cwnt.detection;

import net.imglib2.Cursor;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.algorithm.stats.Histogram;
import net.imglib2.algorithm.stats.RealBinMapper;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class OtsuThresholder2D <T extends RealType<T>> extends MultiThreadedBenchmarkAlgorithm implements OutputAlgorithm<Img<BitType>>{

	private static final String BASE_ERROR_MESSAGE = "[OtsuThresholder2D] ";
	private Img<T> source;
	private Img<BitType> target;
	private double levelFactor;

	/*
	 * CONSTRUCTOR
	 */

	public OtsuThresholder2D(Img<T> source, double thresholdFactor) {
		super();
		this.source = source;
		this.levelFactor = thresholdFactor;
	}


	@Override
	public boolean checkInput() {
		if (source.numDimensions() > 3 || source.numDimensions() < 2) {
			errorMessage = BASE_ERROR_MESSAGE + "Source must of dimension 2 or 3.";
			return false;
		}
		return true;
	}

	@Override
	public boolean process() {
		
		long start = System.currentTimeMillis();
		
		// Create destination image
		try {
			target  = source.factory().imgFactory(new BitType()).create(source, new BitType());
		} catch (IncompatibleTypeException e) {
			errorMessage = BASE_ERROR_MESSAGE + "Could not create target image: " + e.getMessage();
			return false;
		}
		
		// Create cursor over source image
		long nslices = source.dimension(2);

		ComputeMinMax<T> cmm = new ComputeMinMax<T>(source);
		cmm.setNumThreads(numThreads);
		if (!cmm.checkInput() || !cmm.process()) {
			errorMessage = BASE_ERROR_MESSAGE 
					+ "Could not compute min & max :" + cmm.getErrorMessage();
			return false;
		}
		T minBin = cmm.getMin();
		T maxBin = cmm.getMax();
		int nBins = 256;
		for (int z = 0; z < nslices; z++) {
			
			Cursor<T> cursor;
			Cursor<BitType> targetCursor;
			if (source.numDimensions() > 2) {
				cursor = Views.iterable( Views.hyperSlice(source, 2, z) ).localizingCursor();
				targetCursor = Views.iterable( Views.hyperSlice(target, 2, z) ).localizingCursor();
			} else {
				cursor = source.localizingCursor();
				targetCursor = target.localizingCursor();
			}
			
			// Build histogram in given plane
			RealBinMapper<T> binMapper = new RealBinMapper<T>(minBin, maxBin, nBins);
			Histogram<T> histoalgo = new Histogram<T>(binMapper, cursor);
			boolean check = histoalgo.checkInput() && histoalgo.process();
			if (!check) {
				errorMessage = BASE_ERROR_MESSAGE + "Error computing histogram of slice " + z 
						+ ": " + histoalgo.getErrorMessage();
				return false;
			}
			int[] histarray = histoalgo.getHistogram();

			// Get Otsu threshold
			int threshold = otsuThresholdIndex(histarray, source.dimension(0) * source.dimension(1));
			threshold = (int) (threshold * levelFactor) ;
			
			
			// Iterate over target image in the plane

			cursor.reset();
			while(targetCursor.hasNext()) {
				targetCursor.fwd();
				cursor.fwd();
				targetCursor.get().set( cursor.get().compareTo( binMapper.invMap(threshold) ) > 0);
			}

		}

		long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	@Override
	public Img<BitType> getResult() {
		return target;
	}

	/**
	 * Given a histogram array <code>hist</code>, built with an initial amount of <code>nPoints</code>
	 * data item, this method return the bin index that thresholds the histogram in 2 classes.
	 * The threshold is performed using the Otsu Threshold Method,
	 * {@link http://www.labbookpages.co.uk/software/imgProc/otsuThreshold.html}.
	 * @param hist  the histogram array
	 * @param nPoints  the number of data items this histogram was built on
	 * @return the bin index of the histogram that thresholds it
	 */
	public static final int otsuThresholdIndex(final int[] hist, final long nPoints)     {
		long total = nPoints;

		double sum = 0;
		for (int t = 0 ; t < hist.length ; t++)
			sum += t * hist[t];

		double sumB = 0;
		int wB = 0;
		long wF = 0;

		double varMax = 0;
		int threshold = 0;

		for (int t = 0 ; t < hist.length ; t++) {
			wB += hist[t];               // Weight Background
			if (wB == 0) continue;

			wF = total - wB;                 // Weight Foreground
			if (wF == 0) break;

			sumB += (float) (t * hist[t]);

			double mB = sumB / wB;            // Mean Background
			double mF = (sum - sumB) / wF;    // Mean Foreground

			// Calculate Between Class Variance
			double varBetween = (float)wB * (float)wF * (mB - mF) * (mB - mF);

			// Check if new maximum found
			if (varBetween > varMax) {
				varMax = varBetween;
				threshold = t;
			}
		}
		return threshold;
	}


}
