package fiji.plugin.cwnt;

import static fiji.plugin.cwnt.detection.CWNTKeys.KEY_DO_DISPLAY_LABELS;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.Concatenator;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.imglib2.labeling.Labeling;
import fiji.plugin.cwnt.detection.CWNTFactory;
import fiji.plugin.cwnt.detection.LabelToGlasbey;
import fiji.plugin.trackmate.DetectorProvider;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotAnalyzerProvider;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.ActionChooserPanel;
import fiji.plugin.trackmate.gui.DetectorChoiceDescriptor;
import fiji.plugin.trackmate.gui.DetectorDescriptor;
import fiji.plugin.trackmate.gui.DisplayerPanel;
import fiji.plugin.trackmate.gui.GuiReader;
import fiji.plugin.trackmate.gui.InitFilterDescriptor;
import fiji.plugin.trackmate.gui.LoadDescriptor;
import fiji.plugin.trackmate.gui.LogPanelDescriptor;
import fiji.plugin.trackmate.gui.SaveDescriptor;
import fiji.plugin.trackmate.gui.SpotFilterDescriptor;
import fiji.plugin.trackmate.gui.StartDialogPanel;
import fiji.plugin.trackmate.gui.TrackFilterDescriptor;
import fiji.plugin.trackmate.gui.TrackerChoiceDescriptor;
import fiji.plugin.trackmate.gui.TrackingDescriptor;
import fiji.plugin.trackmate.gui.WizardController;
import fiji.plugin.trackmate.gui.WizardPanelDescriptor;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.hyperstack.SpotOverlay;
import fiji.plugin.trackmate.visualization.hyperstack.TrackOverlay;

/**
 * An entry for CWNT that uses {@link TrackMate_} as a GUI.
 * @author Jean-Yves Tinevez 2011-2012
 *
 */
public class TrackMate_CWNT extends TrackMate_ {
	
	/**
	 * The ImagePlus that will display labels resulting from segmentation.
	 */
	protected ImagePlus labelImp;

	protected TreeMap<Integer, Labeling<Integer>> labels = new TreeMap<Integer, Labeling<Integer>>();

	/*
	 * CONSTRUCTOR
	 */
	
	public TrackMate_CWNT() {
		super();
		model.getSettings().initialSpotFilterValue = 0d; // Set it to 0 and forget about it.
	}
	
	/*
	 * SPECIALIZED METHODS
	 */
	
	
	
	@Override
	protected DetectorProvider createDetectorProvider() {
		return new CWNTDetectorProvider();
	}

	@Override
	protected SpotAnalyzerProvider createSpotAnalyzerProvider() {
		// Return no feature analyzer
		return new SpotAnalyzerProvider(model) {
			@Override
			protected void registerSpotFeatureAnalyzers() {
				analyzerNames = new ArrayList<String>();
				// features
				features = new HashMap<String, List<String>>();
				// features names
				featureNames = new HashMap<String, String>();
				// features short names
				featureShortNames = new HashMap<String, String>();
				// feature dimensions
				featureDimensions = new HashMap<String, Dimension>();
			}
		};
	}

	@Override
	protected void launchGUI() {
		WizardController controller = new WizardController(this) {

			@Override
			protected List<WizardPanelDescriptor> createWizardPanelDescriptorList() {
				List<WizardPanelDescriptor> descriptors = new ArrayList<WizardPanelDescriptor>(14);
				descriptors.add(new StartDialogPanel());
				descriptors.add(new DetectorChoiceDescriptor());
				descriptors.add(new CWNTSegmentationDescriptor());
				descriptors.add(new CWMNTSpotFilterDescriptor());
				descriptors.add(new TrackerChoiceDescriptor());
				descriptors.add(new TrackingDescriptor());
				descriptors.add(new TrackFilterDescriptor());
				descriptors.add(new DisplayerPanel());
				descriptors.add(ActionChooserPanel.instantiateForPlugin(plugin));
				descriptors.add(new InitFilterDescriptor()); // We put it, even if we skip it, so that we avoid NPE when loading
				// Override to pass the specific display made for CWNT when loading data
				descriptors.add(new LoadDescriptor() {
					@Override
					public void displayingPanel() {
						try {
							// New model to feed
							TrackMateModel newModel = new TrackMateModel();
							newModel.setLogger(logger);

							if (null == file) {
								File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
								try {
									file = new File(folder.getPath() + File.separator + plugin.getModel().getSettings().imp.getShortTitle() +".xml");
								} catch (NullPointerException npe) {
									file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
								}
							}

							GuiReader reader = new GuiReader(wizard);
							reader.setDisplayer(createLocalSliceDisplayer());
							File tmpFile = reader.askForFile(file);
							if (null == tmpFile) {
								wizard.setNextButtonEnabled(true);
								return;
							}
							file = tmpFile;
							reader.loadFile(file);
							setTargetNextID(reader.getTargetDescriptor());

						} finally {
							wizard.setNextButtonEnabled(true);
						}					
					}
				});
				descriptors.add(new SaveDescriptor());
				
				WizardPanelDescriptor logPanelDescriptor = new LogPanelDescriptor();
				logPanelDescriptor.setWizard(wizard);
				descriptors.add(logPanelDescriptor);

				
				return descriptors;
			}

		};

		// Show version number in the frame title bar
		controller.getWizard().setTitle("CWNT - β4");
	}



	/*
	 * INNER CLASSES
	 */




	private class  CWMNTSpotFilterDescriptor extends SpotFilterDescriptor {
		@Override
		public String getPreviousDescriptorID() {
			// So as to skip the initfilter step in the GUI
			return CWNTSegmentationDescriptor.DESCRIPTOR;
		}

	}

	private class  CWNTSegmentationDescriptor extends DetectorDescriptor {

		@Override
		public String getNextDescriptorID() {
			// So as to skip the initfilter step in the GUI
			return CWMNTSpotFilterDescriptor.DESCRIPTOR;
		}

		/**
		 * Override to display all labels as an imp
		 */
		@Override
		public void aboutToHidePanel() {

			Map<String, Object> detectorSettings = model.getSettings().detectorSettings;
			SpotDetectorFactory<?> detectorFactory = model.getSettings().detectorFactory;
			
			if ( detectorFactory.getKey().equals(CWNTFactory.DETECTOR_KEY)) {
				boolean doDisplayLabels = (Boolean) detectorSettings.get(KEY_DO_DISPLAY_LABELS);
				if (doDisplayLabels) {

					// Do that in another thread
					new Thread("Crown-Wearing segmenter label renderer thread") {

						public void run() {

							model.getLogger().log("Rendering labels started...\n");

							// Get all segmented frames
							int nFrames = labels.size();
							ImagePlus[] coloredFrames = new ImagePlus[nFrames];

							// Check if we have a label for each frame
							int index = 0;
							for (Labeling<Integer> labelThisFrame : labels.values()) {

								ImagePlus imp = null;

								if (null == labelThisFrame) {
									// No labels there. This frame was skipped. We prepare a blank imp to feed the final imp.
									imp = createBlankImagePlus();

								} else {
									// Get the label, make it an 8-bit colored imp
									double[] calibration = TMUtils.getSpatialCalibration(model.getSettings().imp);
									LabelToGlasbey colorer = new LabelToGlasbey(labelThisFrame, calibration);
									if (colorer.checkInput() && colorer.process()) {
										imp = colorer.getImp();
									} else {
										// Blank image
										imp = createBlankImagePlus();
									}
								}
								coloredFrames[index] = imp;
								index++;
							}

							Concatenator cat = new Concatenator();
							labelImp = cat.concatenate(coloredFrames, false);
							int nSlices = model.getSettings().zend - model.getSettings().zstart + 1;
							labelImp.setCalibration(model.getSettings().imp.getCalibration());
							labelImp.setTitle(model.getSettings().imp.getShortTitle()+"_segmented");
							labelImp.setDimensions(1, nSlices, nFrames);
							labelImp.setOpenAsHyperStack(true);
							labelImp.show();

							model.getLogger().log("Rendering labels done.\n");
						};

					}.start();
				}
			}

			launchDisplayerAndComputeFeatures();
		}

		private ImagePlus createBlankImagePlus() {
			Settings settings = model.getSettings();
			int slices = settings.zend - settings.zstart;
			int height = settings.yend - settings.ystart;
			int width = settings.xend - settings.xstart;
			return NewImage.createByteImage("", width, height, slices, NewImage.FILL_BLACK);
		}

		private void launchDisplayerAndComputeFeatures() {
			// Launch renderer
			logger .log("Rendering results...\n",Logger.BLUE_COLOR);
			wizard.setNextButtonEnabled(false);
			final TrackMateModelView displayer = createLocalSliceDisplayer();
			wizard.setDisplayer(displayer);

			try {
				displayer.render();
			} finally {
				// Re-enable the GUI
				logger.log("Rendering done.\n", Logger.BLUE_COLOR);
				wizard.setNextButtonEnabled(true);
			}
		}
	}


	/**
	 * Return a displayer where the tracks and the spots are only displayed in the current or nearing Z
	 * slices, to accommodate the large data spread in Z that is typically met by CWNT.
	 * @return
	 */
	private  HyperStackDisplayer createLocalSliceDisplayer() {

		return new HyperStackDisplayer(model) {

			@Override
			protected SpotOverlay createSpotOverlay() {

				return new SpotOverlay(model, model.getSettings().imp, displaySettings) {

					@Override
					public void drawSpot(final Graphics2D g2d, final Spot spot, final double zslice, 
							final int xcorner, final int ycorner, final double magnification) {

						final double x = spot.getFeature(Spot.POSITION_X);
						final double y = spot.getFeature(Spot.POSITION_Y);
						final double z = spot.getFeature(Spot.POSITION_Z);
						final double dz2 = (z - zslice) * (z - zslice);
						float radiusRatio = (Float) displaySettings.get(TrackMateModelView.KEY_SPOT_RADIUS_RATIO);
						final double radius = spot.getFeature(Spot.RADIUS) * radiusRatio;
						if (dz2 >= radius * radius)
							return;

						// In pixel units
						final double xp = x / calibration[0];
						final double yp = y / calibration[1];
						// Scale to image zoom
						final double xs = (xp - xcorner) * magnification ;
						final double ys = (yp - ycorner) * magnification ;

						// Spots are painted as purple circled of white
						final double apparentRadius =  3 *radiusRatio; 
						g2d.fillOval( 
								(int) Math.round(xs - apparentRadius), 
								(int) Math.round(ys - apparentRadius), 
								(int) Math.round(2 * apparentRadius), 
								(int) Math.round(2 * apparentRadius));		
						g2d.setColor(Color.CYAN.darker()); // Carefully set to reflect Aliette Leroy's opinion
						g2d.drawOval(
								(int) Math.round(xs - apparentRadius), 
								(int) Math.round(ys - apparentRadius), 
								(int) Math.round(2 * apparentRadius), 
								(int) Math.round(2 * apparentRadius));		

						boolean spotNameVisible = (Boolean) displaySettings.get(TrackMateModelView.KEY_DISPLAY_SPOT_NAMES);
						if (spotNameVisible ) {
							String str = spot.toString();
							int xindent = fm.stringWidth(str) / 2;
							int yindent = fm.getAscent() / 2;
							g2d.drawString(
									spot.toString(), 
									(int) xs-xindent, 
									(int) ys+yindent);
						}
					}

				};
			}



			@Override
			protected TrackOverlay createTrackOverlay() {

				return new TrackOverlay(model, model.getSettings().imp, displaySettings) {

					@Override
					protected void drawEdge(Graphics2D g2d, Spot source, Spot target, int xcorner, int ycorner,	double magnification) {
						// Find x & y in physical coordinates
						final double x0i = source.getFeature(Spot.POSITION_X);
						final double y0i = source.getFeature(Spot.POSITION_Y);
						final double z0i = source.getFeature(Spot.POSITION_Z);
						final double x1i = target.getFeature(Spot.POSITION_X);
						final double y1i = target.getFeature(Spot.POSITION_Y);
						final double z1i = target.getFeature(Spot.POSITION_Z);
						// In pixel units
						final double x0p = x0i / calibration[0];
						final double y0p = y0i / calibration[1];
						final double x1p = x1i / calibration[0];
						final double y1p = y1i / calibration[1];

						// Check if we are nearing their plane, if not, do not draw
						final double zslice = (imp.getSlice()-1) * calibration[2];
						double radiusRatio = (Float) displaySettings.get(TrackMateModelView.KEY_SPOT_RADIUS_RATIO);
						final double tRadius = target.getFeature(Spot.RADIUS) * radiusRatio;
						final double dz1 = (z1i - zslice) * (z1i - zslice);
						final double sRadius = source.getFeature(Spot.RADIUS) * radiusRatio;
						final double dz0 = (z0i - zslice) * (z0i - zslice);

						if (dz1 >= tRadius * tRadius && dz0 >= sRadius * sRadius)
							return;

						// Scale to image zoom
						final double x0s = (x0p - xcorner) * magnification ;
						final double y0s = (y0p - ycorner) * magnification ;
						final double x1s = (x1p - xcorner) * magnification ;
						final double y1s = (y1p - ycorner) * magnification ;
						// Round
						final long x0 = Math.round(x0s);
						final long y0 = Math.round(y0s);
						final long x1 = Math.round(x1s);
						final long y1 = Math.round(y1s);

						g2d.drawLine(
								(int) x0, 
								(int) y0, 
								(int) x1, 
								(int) y1);
					}


				};

			}

		};

	}

}
