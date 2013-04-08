package fiji.plugin.cwnt;

import static fiji.plugin.cwnt.detection.CWNTKeys.DEFAULT_ALPHA;
import static fiji.plugin.cwnt.detection.CWNTKeys.DEFAULT_BETA;
import static fiji.plugin.cwnt.detection.CWNTKeys.DEFAULT_DELTA;
import static fiji.plugin.cwnt.detection.CWNTKeys.DEFAULT_DO_DISPLAY_LABELS;
import static fiji.plugin.cwnt.detection.CWNTKeys.DEFAULT_EPSILON;
import static fiji.plugin.cwnt.detection.CWNTKeys.DEFAULT_GAMMA;
import static fiji.plugin.cwnt.detection.CWNTKeys.DEFAULT_KAPPA;
import static fiji.plugin.cwnt.detection.CWNTKeys.DEFAULT_N_ANISOTROPIC_FILTERING;
import static fiji.plugin.cwnt.detection.CWNTKeys.DEFAULT_SIGMA_FILTER;
import static fiji.plugin.cwnt.detection.CWNTKeys.DEFAULT_SIGMA_GRADIENT;
import static fiji.plugin.cwnt.detection.CWNTKeys.DEFAULT_THRESHOLD_FACTOR;
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
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.XML_ATTRIBUTE_DETECTOR_NAME;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Element;

import fiji.plugin.cwnt.detection.CWNTFactory;
import fiji.plugin.cwnt.detection.CWNTPanel;
import fiji.plugin.cwnt.detection.CrownWearingSegmenter;
import fiji.plugin.trackmate.DetectorProvider;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.WizardController;

public class CWNTDetectorProvider extends DetectorProvider {

	public CWNTDetectorProvider() {
		registerDetectors();
		currentKey = CWNTFactory.DETECTOR_KEY;
	}


	/*
	 * METHODS
	 */

	/**
	 * Only registers CWNT detector factory.
	 */
	protected void registerDetectors() {
		// keys
		keys = new ArrayList<String>(1);
		keys.add(CWNTFactory.DETECTOR_KEY);
		// names
		names = new ArrayList<String>(1);
		names.add(CWNTFactory.NAME);
		// infoTexts
		infoTexts = new ArrayList<String>(1);
		infoTexts.add(CWNTFactory.INFO_TEXT);
	}
	
	@Override
	public boolean checkSettingsValidity(Map<String, Object> settings) {
		if (null == settings) {
			errorMessage = "Settings map is null.\n";
			return false;
		}

		StringBuilder errorHolder = new StringBuilder();
		if (currentKey.equals(CWNTFactory.DETECTOR_KEY)) {

			boolean ok = CrownWearingSegmenter.checkInput(settings, errorHolder);
			if (!ok) {
				errorMessage = errorHolder.toString();
			}
			return ok;

		} else {
			return super.checkSettingsValidity(settings);
		}
	}

	@Override
	public boolean marshall(Map<String, Object> settings, Element element) {
		// Is it one of the other detector?
		boolean ok = super.marshall(settings, element);
		if (ok) {
			return true;
		} else if (currentKey.equals(CWNTFactory.DETECTOR_KEY)) {
			// The it must be ours
			return writeDoMedian(settings, element) &&
					writeAttribute(settings, element, KEY_SIGMA_FILTER, Double.class) &&
					writeAttribute(settings, element, KEY_SIGMA_GRADIENT, Double.class) &&
					writeAttribute(settings, element, KEY_KAPPA, Double.class) &&
					writeAttribute(settings, element, KEY_ALPHA, Double.class) &&
					writeAttribute(settings, element, KEY_BETA, Double.class) &&
					writeAttribute(settings, element, KEY_DELTA, Double.class) &&
					writeAttribute(settings, element, KEY_GAMMA, Double.class) &&
					writeAttribute(settings, element, KEY_EPSILON, Double.class) &&
					writeAttribute(settings, element, KEY_N_ANISOTROPIC_FILTERING, Integer.class) &&
					writeAttribute(settings, element, KEY_THRESHOLD_FACTOR, Double.class);
		} else {
			errorMessage = "Unknow detector factory key: "+currentKey+".\n";
			return false;
		}
	}

	@Override
	public boolean unmarshall(Element element, Map<String, Object> settings) {

		settings.clear();

		String detectorKey = element.getAttributeValue(XML_ATTRIBUTE_DETECTOR_NAME);
		// Try to set the state of this provider from the key read in xml.
		boolean ok = select(detectorKey);
		if (!ok) {
			errorMessage = "Detector key found in XML ("+detectorKey+") is unknown to this provider.\n";
			return false;
		}

		StringBuilder errorHolder = new StringBuilder();

		if (currentKey.equals(CWNTFactory.DETECTOR_KEY)) {

			ok = true;
			ok = ok & readBooleanAttribute(element, settings, KEY_DO_MEDIAN_FILTERING, errorHolder);
			ok = ok & readBooleanAttribute(element, settings, KEY_ALPHA, errorHolder);
			ok = ok & readBooleanAttribute(element, settings, KEY_BETA, errorHolder);
			ok = ok & readBooleanAttribute(element, settings, KEY_GAMMA, errorHolder);
			ok = ok & readBooleanAttribute(element, settings, KEY_DELTA, errorHolder);
			ok = ok & readBooleanAttribute(element, settings, KEY_EPSILON, errorHolder);
			ok = ok & readBooleanAttribute(element, settings, KEY_KAPPA, errorHolder);
			ok = ok & readBooleanAttribute(element, settings, KEY_SIGMA_FILTER, errorHolder);
			ok = ok & readBooleanAttribute(element, settings, KEY_SIGMA_GRADIENT, errorHolder);
			ok = ok & readBooleanAttribute(element, settings, KEY_N_ANISOTROPIC_FILTERING, errorHolder);
			ok = ok & readBooleanAttribute(element, settings, KEY_THRESHOLD_FACTOR, errorHolder);

			if (!ok) {
				errorMessage = errorHolder.toString();
			}
			return ok;

		} else {
			return super.unmarshall(element, settings);
		}
	}

	@SuppressWarnings("rawtypes")
	public SpotDetectorFactory getDetectorFactory() {

		if (currentKey.equals(CWNTFactory.DETECTOR_KEY)) {
			return new CWNTFactory(); 

		} else {
			return super.getDetectorFactory();

		}
	}

	@Override
	public String getInfoText() {
		if (currentKey.equals(CWNTFactory.DETECTOR_KEY)) {
			return CWNTFactory.INFO_TEXT;
		} else {
			return super.getInfoText();
		}
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel(WizardController controller) {

		if (currentKey.equals(CWNTFactory.DETECTOR_KEY)) {

			Settings settings = controller.getPlugin().getModel().getSettings();
			return new CWNTPanel(settings);

		} else  {
			return super.getDetectorConfigurationPanel(controller);
		}
	}

	@Override
	public Map<String, Object> getDefaultSettings() {

		if (currentKey.equals(CWNTFactory.DETECTOR_KEY) ) {
			Map<String, Object> settings = new HashMap<String, Object>();
			settings.put(KEY_DO_MEDIAN_FILTERING, 		DEFAULT_DO_MEDIAN_FILTERING);
			settings.put(KEY_SIGMA_FILTER, 				DEFAULT_SIGMA_FILTER);
			settings.put(KEY_N_ANISOTROPIC_FILTERING, 	DEFAULT_N_ANISOTROPIC_FILTERING);
			settings.put(KEY_KAPPA, 					DEFAULT_KAPPA);
			settings.put(KEY_SIGMA_GRADIENT, 			DEFAULT_SIGMA_GRADIENT);
			settings.put(KEY_GAMMA, 					DEFAULT_GAMMA);
			settings.put(KEY_ALPHA, 					DEFAULT_ALPHA);
			settings.put(KEY_BETA, 						DEFAULT_BETA);
			settings.put(KEY_EPSILON, 					DEFAULT_EPSILON);
			settings.put(KEY_DELTA, 					DEFAULT_DELTA);
			settings.put(KEY_THRESHOLD_FACTOR, 			DEFAULT_THRESHOLD_FACTOR);
			settings.put(KEY_DO_DISPLAY_LABELS, 		DEFAULT_DO_DISPLAY_LABELS);
			settings.put(KEY_TARGET_CHANNEL, 			1);
			return settings;

		} else {
			return super.getDefaultSettings();
		}
	}
}
