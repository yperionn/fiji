package fiji.plugin.cwnt.detection;


import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.util.Map;

import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.HyperSliceImgPlus;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;

public class CWNTFactory<T extends RealType<T> & NativeType<T>> implements SpotDetectorFactory<T> {

	
	/*
	 * CONSTANTS
	 */

	/** A string key identifying this factory. */ 
	public static final String DETECTOR_KEY = "CWNT_DETECTOR";
	/** The pretty name of the target detector. */
	public static final String NAME = "Crown-Wearing Nuclei Tracker";
	/** An html information text. */
	public static final String INFO_TEXT = "<html>" +
			"<div align=\"justify\">" +
			"This plugin allows the segmentation and tracking of bright blobs objects, " +
			"typically nuclei imaged in 3D over time. " +
			"<p> " +
			"It is specially designed to deal with cases where nuclei are very-densily packed, " +
			"and observed at intermediate resolution,  such as developing zebra-fish embryogenesis. " +
			"To do so, this plugin operates in 2 steps:" +
			"<p>" +
			" - The image is first pre-processed, by computing a special mask that stresses " +
			"the nuclei boundaries. A crown-like mak is computed from the 2D spatial derivatives " +
			"of the image, and a masked image where the nuclei are better separated is generated. " +
			"<br/>" +
			" - Then the nuclei are thresholded from the background of the masked image, " +
			"labeled in 3D and tracked over time. " +
			"<p>" +
			"Because the crown-like mask needs about 10 parameters to be specified, this plugin offers " +
			"to live-test their effect while tuning their values in the 2nd and 3rd tab of this GUI. " +
			"The resulting masked image and intermediate images will be computed over a limited area " +
			"of the source image, specified by the ROI. " +
			"<p> " +
			"</div>" +
			"<div align=\"right\"> " +
			"<tt>" +
			"Bhavna Rajasekaran <br/>" +
			"Jean-Yves Tinevez <br/>" +
			"Andrew Oates lab - MPI-CBG, Dresden, 2011-2013 " +
			"</tt>" +
			"</div>" +
			"</html>";
	
	/*
	 * FIELDS
	 */
	
	/** The image to operate on. Multiple frames, single channel.	 */
	protected ImgPlus<T> img;
	protected Map<String, Object> settings;
	protected String errorMessage;
	
	/*
	 * METHODS
	 */
	
	@Override
	public SpotDetector<T> getDetector(int frame) {
		final int targetChannel = (Integer) settings.get(KEY_TARGET_CHANNEL);
		final ImgPlus<T> imgC = HyperSliceImgPlus.fixChannelAxis(img, targetChannel);
		final ImgPlus<T> imgT = HyperSliceImgPlus.fixTimeAxis(imgC, frame);
		final CrownWearingSegmenter<T> cws = new CrownWearingSegmenter<T>(imgT, settings);
		return cws;
	}

	@Override
	public void setTarget(ImgPlus<T> img, Map<String, Object> settings) {
		this.img = img;
		this.settings = settings;
	}

	@Override
	public String getKey() {
		return DETECTOR_KEY;
	}

	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}
	
	@Override
	public String toString() {
		return NAME;
	}
}
