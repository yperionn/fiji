package fiji.plugin.cwnt.detection;

import static fiji.plugin.cwnt.detection.CWNTKeys.KEY_ALPHA;
import static fiji.plugin.cwnt.detection.CWNTKeys.KEY_BETA;
import static fiji.plugin.cwnt.detection.CWNTKeys.KEY_DELTA;
import static fiji.plugin.cwnt.detection.CWNTKeys.KEY_DO_DISPLAY_LABELS;
import static fiji.plugin.cwnt.detection.CWNTKeys.KEY_EPSILON;
import static fiji.plugin.cwnt.detection.CWNTKeys.KEY_GAMMA;
import static fiji.plugin.cwnt.detection.CWNTKeys.KEY_KAPPA;
import static fiji.plugin.cwnt.detection.CWNTKeys.KEY_N_ANISOTROPIC_FILTERING;
import static fiji.plugin.cwnt.detection.CWNTKeys.KEY_SIGMA_FILTER;
import static fiji.plugin.cwnt.detection.CWNTKeys.KEY_SIGMA_GRADIENT;
import static fiji.plugin.cwnt.detection.CWNTKeys.KEY_THRESHOLD_FACTOR;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;
import ij.IJ;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.algorithm.labeling.AllConnectedComponents;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.SpotDetector;

public class CrownWearingSegmenter<T extends RealType<T> & NativeType<T>> extends MultiThreadedBenchmarkAlgorithm implements SpotDetector<T> {
	
	private Img<T> masked;
	private Img<T> source;
	private Img<BitType> thresholded;
	private Labeling<Integer> labeling;
	private List<Spot> spots;
	private final double[] calibration;
	private Map<String, Object> settings;
	private Iterator<Integer> labelGenerator;

	/*
	 * CONSTRUCTOR	
	 */

	public CrownWearingSegmenter(ImgPlus<T> img, Map<String, Object> settings) {
		super();
		this.source = img;
		this.calibration = new double[img.numDimensions()];
		img.calibration(calibration);
		this.settings = settings;
	}
	
	/*
	 * METHODS
	 */
	

	@Override
	public List<Spot> getResult() {
		return spots;
	}

	@Override
	public String toString() {
		return "Crown-Wearing Segmenter";
	}

	@Override
	public boolean checkInput() {
		StringBuilder errorHolder = new StringBuilder();
		boolean ok = checkInput(settings, errorHolder);
		if (!ok) {
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	@Override
	public boolean process() {
		long start = System.currentTimeMillis();
		boolean check;

		// Crown wearing mask
		NucleiMasker<T> masker = new NucleiMasker<T>(source);
		masker.setNumThreads(numThreads); // propagate MT choice 
		boolean doMedianFiltering 	= (Boolean) settings.get(KEY_DO_MEDIAN_FILTERING);
		boolean doDisplayLabels 	= (Boolean) settings.get(KEY_DO_DISPLAY_LABELS);
		double gaussFilterSigma 	= (Double) settings.get(KEY_SIGMA_FILTER);
		int nIterAnDiff 			= (Integer) settings.get(KEY_N_ANISOTROPIC_FILTERING);
		double kappa				= (Double) settings.get(KEY_KAPPA);
		double gaussGradSigma		= (Double) settings.get(KEY_SIGMA_GRADIENT);
		double gamma 				= (Double) settings.get(KEY_GAMMA);
		double alpha				= (Double) settings.get(KEY_ALPHA);
		double beta 				= (Double) settings.get(KEY_BETA);
		double epsilon				= (Double) settings.get(KEY_EPSILON);
		double delta 				= (Double) settings.get(KEY_DELTA);
		masker.setParameters(doMedianFiltering, gaussFilterSigma, nIterAnDiff, kappa, 
				gaussGradSigma, gamma, alpha, beta, epsilon, delta);
		check = masker.process();
		if (check) {
			masked = masker.getResult();
		} else {
			errorMessage = masker.getErrorMessage();
			return false;
		}
		
		// Thresholding
		double thresholdFactor = (Double) settings.get(KEY_THRESHOLD_FACTOR);
		OtsuThresholder2D<T> thresholder = new OtsuThresholder2D<T>(masked, thresholdFactor);
		check = thresholder.process();
		if (check) {
			thresholded = thresholder.getResult();
		} else {
			errorMessage = thresholder.getErrorMessage();
			return false;
		}
		
		// Labeling
		labelGenerator = AllConnectedComponents.getIntegerNames(0);
		long[] dims = new long[thresholded.numDimensions()];
		thresholded.dimensions(dims);
		labeling = new NativeImgLabeling<Integer, IntType>(new ArrayImgFactory< IntType >().create( dims , new IntType() ) );
		
		// 6-connected structuring element
		long[][] structuringElement = new long[][] { {-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1} };
		AllConnectedComponents.labelAllConnectedComponents(labeling, thresholded, labelGenerator, structuringElement);
		
		// Splitting and spot creation
		NucleiSplitter splitter = new NucleiSplitter(labeling, calibration, labelGenerator, doDisplayLabels);
		if (!(splitter.checkInput() && splitter.process())) {
			IJ.error("Problem with splitter: "+splitter.getErrorMessage());
			return false;
		}
		spots = splitter.getResult();
		processingTime = System.currentTimeMillis() - start;
		return true;
	}

	public Labeling<Integer> getLabeling() {
		return labeling;
	}
	
	/**
	 * @return the label generator that was used to assign labels to
	 * segmented blobs.
	 */
	public Iterator<Integer> getLabelGenerator() {
		return labelGenerator;
	}
	
	
	/*
	 * STATIC METHODS
	 */
	
	/**
	 * Check that the given settings map is suitable for CWNT detector.
	 * @param settings  the map to test.
	 * @param errorHolder  if not suitable, will contain an error message.
	 * @return  true if the settings map is valid.
	 */
	public static boolean checkInput(Map<String, Object> settings, StringBuilder errorHolder) {
		boolean ok = true;
		
		ok = ok & checkParameter(settings, KEY_TARGET_CHANNEL, Integer.class, errorHolder);
		ok = ok & checkParameter(settings, KEY_DO_MEDIAN_FILTERING, Boolean.class, errorHolder) ;
		ok = ok & checkParameter(settings, KEY_DO_DISPLAY_LABELS, Boolean.class, errorHolder) ;
		ok = ok & checkParameter(settings, KEY_N_ANISOTROPIC_FILTERING, Integer.class, errorHolder);
		ok = ok & checkParameter(settings, KEY_KAPPA, Double.class, errorHolder);
		ok = ok & checkParameter(settings, KEY_SIGMA_GRADIENT, Double.class, errorHolder);
		ok = ok & checkParameter(settings, KEY_SIGMA_FILTER, Double.class, errorHolder);
		ok = ok & checkParameter(settings, KEY_GAMMA, Double.class, errorHolder);
		ok = ok & checkParameter(settings, KEY_ALPHA, Double.class, errorHolder);
		ok = ok & checkParameter(settings, KEY_BETA, Double.class, errorHolder);
		ok = ok & checkParameter(settings, KEY_EPSILON, Double.class, errorHolder);
		ok = ok & checkParameter(settings, KEY_DELTA, Double.class, errorHolder);
		ok = ok & checkParameter(settings, KEY_THRESHOLD_FACTOR, Double.class, errorHolder);

		List<String> mandatoryKeys = new ArrayList<String>();
		mandatoryKeys.add(KEY_TARGET_CHANNEL);
		mandatoryKeys.add(KEY_DO_MEDIAN_FILTERING);
		mandatoryKeys.add(KEY_DO_DISPLAY_LABELS);
		mandatoryKeys.add(KEY_N_ANISOTROPIC_FILTERING);
		mandatoryKeys.add(KEY_KAPPA);
		mandatoryKeys.add(KEY_SIGMA_GRADIENT);
		mandatoryKeys.add(KEY_SIGMA_FILTER);
		mandatoryKeys.add(KEY_GAMMA);
		mandatoryKeys.add(KEY_ALPHA);
		mandatoryKeys.add(KEY_BETA);
		mandatoryKeys.add(KEY_EPSILON);
		mandatoryKeys.add(KEY_DELTA);
		mandatoryKeys.add(KEY_THRESHOLD_FACTOR);
																
		ok = ok & checkMapKeys(settings, mandatoryKeys, null, errorHolder);
		return ok;	
	}
	
	
	

	public static final String toString(Map<String, Object> settings) {
		String str = "";
		str += "  1. Pre-Filtering:\n";
		str += String.format("    - do median filtering: " + settings.get(KEY_DO_MEDIAN_FILTERING)+"\n");
		str += String.format("    - gaussian filter sigma: %.1f\n", settings.get(KEY_SIGMA_FILTER));
		str += "  2. Anisotropic diffusion:\n";
		str += String.format("    - number of iterations: %d\n", settings.get(KEY_N_ANISOTROPIC_FILTERING));
		str += String.format("    - gradient threshold kappa: %.1f\n", settings.get(KEY_KAPPA));
		str += "  3. Derivatives calculation:\n";
		str += String.format("    - gaussian gradient sigma: %.1f\n", settings.get(KEY_SIGMA_GRADIENT));
		str += "  4. Mask parameters:\n";
		str += String.format("    - ϒ tanh shift: %.1f\n", settings.get(KEY_GAMMA));
		str += String.format("    - α gradient contribution: %.1f\n", settings.get(KEY_ALPHA));
		str += String.format("    - β positive laplacian contribution: %.1f\n", settings.get(KEY_BETA));
		str += String.format("    - ε negative hessian contribution: %.1f\n", settings.get(KEY_EPSILON));
		str += String.format("    - δ derivatives sum scale: %.1f\n", settings.get(KEY_DELTA));
		str += "  5. Thresholding:\n";
		str += String.format("    - threshold pre-factor: %.1f\n", settings.get(KEY_THRESHOLD_FACTOR));
		str += "  Display:\n";
		str += "    - do display label image: "+settings.get(KEY_DO_DISPLAY_LABELS)+"\n";
		return str;
	}

	
}
