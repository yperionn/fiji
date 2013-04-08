package fiji.plugin.cwnt.detection;

import java.util.Map;

import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.labeling.Labeling;
import net.imglib2.view.HyperSliceImgPlus;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.util.CropImgView;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;

public class CWNTFrameSegmenter extends MultiThreadedBenchmarkAlgorithm {

	private static final boolean DEBUG = true;
	private final CWNTPanel source;
	private final Map<String, Object> sm;
	private final Settings tmSettings;

	/*
	 * CONSTRUCTOR
	 */
	
	public CWNTFrameSegmenter(CWNTPanel panel) {
		super();
		this.source = panel;
		this.tmSettings = panel.getTMSettings();
		this.sm = panel.getSettings();
	}
	
	
	/*
	 * METHODS
	 */
	
	@Override
	public boolean checkInput() {
		StringBuilder errorHolder = new StringBuilder();
		boolean ok = CrownWearingSegmenter.checkInput(sm, errorHolder);
		if (!ok) {
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean process() {
		final long start = System.currentTimeMillis();
		
		final ImagePlus imp = tmSettings.imp;
		final ImgPlus rawImg = TMUtils.rawWraps(tmSettings.imp);
		
		ImgPlus img;
		if (tmSettings.xstart != 0 
				|| tmSettings.ystart != 0
				|| tmSettings.zstart != 0
				|| tmSettings.xend != tmSettings.imp.getWidth()-1
				|| tmSettings.yend != tmSettings.imp.getHeight()-1
				|| tmSettings.zend != tmSettings.imp.getNSlices()-1) {
			// Yes, we want to crop

			long[] max = new long[rawImg.numDimensions()];
			long[] min = new long[rawImg.numDimensions()];
			// X, we must have it
			int xindex = TMUtils.findXAxisIndex(rawImg);
			if (xindex < 0) {
				errorMessage = "Source image has no X axis.\n";
				return false;
			}
			min[xindex] = tmSettings.xstart;
			max[xindex] = tmSettings.xend;
			// Y, we must have it
			int yindex = TMUtils.findYAxisIndex(rawImg);
			if (yindex < 0) {
				errorMessage  = "Source image has no Y axis.\n";
				return false;
			}
			min[yindex] = tmSettings.ystart;
			max[yindex] = tmSettings.yend;
			// Z, we MIGHT have it
			int zindex = TMUtils.findZAxisIndex(rawImg);
			if (zindex >= 0) {
				min[zindex] = tmSettings.zstart;
				max[zindex] = tmSettings.zend;
			}
			// CHANNEL, we might have it 
			int cindex = TMUtils.findCAxisIndex(rawImg);
			if (cindex >= 0) {
				min[cindex] = 0;
				max[cindex] = tmSettings.imp.getNChannels();
			}
			// crop: we now have a cropped view of the source image
			CropImgView cropView = new CropImgView(rawImg, min, max);
			// Put back metadata in a new ImgPlus 
			img = new ImgPlus(cropView, rawImg);
			
		} else {
			img = rawImg;
		}
		
		final int frame = imp.getFrame() - 1;
		final int targetChannel = imp.getChannel() - 1;
		final ImgPlus imgC = HyperSliceImgPlus.fixChannelAxis(img, targetChannel);
		final ImgPlus imgT = HyperSliceImgPlus.fixTimeAxis(imgC, frame);
		
		final CrownWearingSegmenter cws = new CrownWearingSegmenter(imgT, sm);
		cws.setNumThreads(numThreads); // since it is only for one frame, we can propagate and use MT version
		
		if (!cws.checkInput() || !cws.process()) {
			errorMessage = cws.getErrorMessage();
			return false;
		}
		
		final long end = System.currentTimeMillis();
		processingTime = end - start;
		
		if (DEBUG) {
			System.out.println("["+this.getClass().getSimpleName()+"] #process: coloring label started");
		}
		
		Labeling labels = cws.getLabeling();
		double[] calibration = new double[rawImg.numDimensions()]; 
		rawImg.calibration(calibration);
		
		LabelToGlasbey converter = new LabelToGlasbey(labels, calibration);
		converter.process();
		ImagePlus result = converter.getImp();
		result.show();

		if (DEBUG) {
			System.out.println("["+this.getClass().getSimpleName()+"] #process: coloring label done");
		}
		
		int tmin = (int) Math.ceil(processingTime / 1e3 / 60); //min 
		source.labelDurationEstimate.setText("Total duration rough estimate: "+tmin+" min.");
		
		return true;
	}

}
