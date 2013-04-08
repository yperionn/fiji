package fiji.plugin.cwnt.detection;

import static fiji.plugin.cwnt.detection.CWNTKeys.*;
import static fiji.plugin.trackmate.detection.DetectorKeys.*;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Duplicator;
import ij.process.FloatProcessor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import net.imglib2.display.RealFloatConverter;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJVirtualStackFloat;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class CWNTLivePreviewer extends MouseAdapter implements ActionListener {

	private CWNTPanel source;
	private int stepUpdateToPerform = Integer.MAX_VALUE;
	private DisplayUpdater updater;
	private NucleiMasker<?> nucleiMasker;
	private ImagePlus comp2;
	private ImagePlus comp1;
	private ImagePlus imp;

	/*
	 * CONSTRUCTOR
	 */

	public CWNTLivePreviewer(CWNTPanel panel) {
		this.source = panel;
		this.imp = panel.getTMSettings().imp;
		this.updater = new DisplayUpdater();

		source.addActionListener(this);
		imp.getCanvas().addMouseListener(this);

		recomputeSampleWindows(imp);
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void mouseReleased(MouseEvent e) {
		new Thread("CWNT tuning thread") {
			public void run() {
				recomputeSampleWindows(imp);
			};
		}.start();
	}

	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e == source.STEP1_PARAMETER_CHANGED) {
			stepUpdateToPerform = Math.min(1, stepUpdateToPerform);
			updater.doUpdate();

		} else if (e == source.STEP2_PARAMETER_CHANGED) {
			stepUpdateToPerform = Math.min(2, stepUpdateToPerform);
			updater.doUpdate();

		} else if (e == source.STEP3_PARAMETER_CHANGED) {
			stepUpdateToPerform = Math.min(3, stepUpdateToPerform);
			updater.doUpdate();

		} else if (e == source.STEP4_PARAMETER_CHANGED) {
			stepUpdateToPerform = Math.min(4, stepUpdateToPerform);
			updater.doUpdate();

		} else if (e == source.STEP5_PARAMETER_CHANGED) {
			stepUpdateToPerform = Math.min(5, stepUpdateToPerform);
			updater.doUpdate();

		} else {
			System.err.println("Unknwon event caught: "+e);
		}
	}
	
	void quit() {
		updater.quit();
		source.removeActionListener(this);
		imp.getCanvas().removeMouseListener(this);
		comp1.changes = false;
		comp1.close();
		comp2.changes = false;
		comp2.close();
	}

	/*
	 * PRIVATE METHODS
	 */

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void recomputeSampleWindows(ImagePlus imp) {

		ImagePlus snip = new Duplicator().run(imp, imp.getSlice(), imp.getSlice());

		// Copy to Imglib
		Img<? extends IntegerType<?>> img = null;
		switch( imp.getType() )	{		
		case ImagePlus.GRAY8 : 
			img =  ImagePlusAdapter.wrapByte( snip );
			break;
		case ImagePlus.GRAY16 : 
			img = ImagePlusAdapter.wrapShort( snip );
			break;
		default:
			System.err.println("Img type not handled: "+imp.getType());
			return;
		}

		// Prepare algo
		nucleiMasker = new NucleiMasker(img);
		readMaskingParameters();
		boolean check = nucleiMasker.checkInput() && nucleiMasker.process();
		if (!check) {
			System.err.println("Problem with the segmenter: "+nucleiMasker.getErrorMessage());
			return;
		}

		double snipPixels = snip.getWidth() * snip.getHeight() * snip.getNSlices() * snip.getNFrames();
		double allPixels = imp.getWidth() * imp.getHeight() * imp.getNSlices() * imp.getNFrames();
		double dt = nucleiMasker.getProcessingTime();
		int tmin = (int) Math.ceil(dt * allPixels / snipPixels / 1e3 / 60); //min 
		source.labelDurationEstimate.setText("Total duration rough estimate: "+tmin+" min.");

		// Prepare results holder;
		Img 			F 	= nucleiMasker.getGaussianFilteredImage();
		Img 			AD	= nucleiMasker.getAnisotropicDiffusionImage();
		Img<FloatType> 	G 	= nucleiMasker.getGradientNorm();
		Img<FloatType> 	L 	= nucleiMasker.getLaplacianMagnitude();
		Img<FloatType> 	H 	= nucleiMasker.getHessianDeterminant();
		Img<FloatType> 	M 	= nucleiMasker.getMask();
		Img 			R 	= nucleiMasker.getResult();

		double thresholdFactor  = (Double) source.getSettings().get(KEY_THRESHOLD_FACTOR);
		OtsuThresholder2D thresholder = new OtsuThresholder2D(R, thresholdFactor);
		thresholder.process();
		Img 				B = thresholder.getResult();
		
		long width = F.dimension(0);
		long height = F.dimension(1);

		ImageStack floatStack = new ImageStack((int) width, (int) height); // The stack of ips that scales roughly from 0 to 1
		floatStack.addSlice("Gradient norm", toFloatProcessor(G));
		floatStack.addSlice("Laplacian mangitude", toFloatProcessor(L));
		floatStack.addSlice("Hessian determintant", toFloatProcessor(H));
		floatStack.addSlice("Mask", toFloatProcessor(M));
		floatStack.addSlice("Thresholded", toFloatProcessor(B));
		if (comp2 == null) {
			comp2 = new ImagePlus("Scaled derivatives", floatStack);
		} else {
			comp2.setStack(floatStack); 
		}
		comp2.show();
		comp2.getProcessor().resetMinAndMax();

		ImageStack tStack = new ImageStack((int) width, (int) height); // The stack of ips that scales roughly like source image
		tStack.addSlice("Gaussian filtered", toFloatProcessor(F));
		tStack.addSlice("Anisotropic diffusion", toFloatProcessor(AD));
		tStack.addSlice("Masked image", toFloatProcessor(R));
		if (comp1 == null) {
			comp1 = new ImagePlus("Components", tStack);
		} else {
			comp1.setStack(tStack);
		}
		comp1.show();

		positionComponentRelativeTo(comp1.getWindow(), imp.getWindow(), 1);
		positionComponentRelativeTo(comp2.getWindow(), comp1.getWindow(), 2);
	}

	private void refresh() {
		switch (stepUpdateToPerform) {
		case 1: 
			paramStep1Changed();
			break;
		case 2:
			paramStep2Changed();
			break;
		case 3:
			paramStep3Changed();
			break;
		case 4:
			paramStep4Changed();
			break;
		case 5:
			paramStep5Changed();
			break;
		}
		stepUpdateToPerform = Integer.MAX_VALUE;
	}

	private void paramStep1Changed() {
		readMaskingParameters();
		
		// We have to redo all.
		nucleiMasker.execStep1(); 
		nucleiMasker.execStep2(); 
		nucleiMasker.execStep3(); 
		nucleiMasker.execStep4();
		paramStep5Changed();

		int slice1 = comp1.getSlice();
		comp1.setSlice(1);
		comp1.setProcessor(toFloatProcessor(nucleiMasker.getGaussianFilteredImage()));
		comp1.setSlice(2);
		comp1.setProcessor(toFloatProcessor(nucleiMasker.getAnisotropicDiffusionImage()));
		comp1.setSlice(3);
		comp1.setProcessor(toFloatProcessor(nucleiMasker.getResult()));
		comp1.setSlice(slice1);

		int slice2 = comp2.getSlice();
		comp2.setSlice(1);
		comp2.setProcessor(toFloatProcessor(nucleiMasker.getGradientNorm()));
		comp2.setSlice(2);
		comp2.setProcessor(toFloatProcessor(nucleiMasker.getLaplacianMagnitude()));
		comp2.setSlice(3);
		comp2.setProcessor(toFloatProcessor(nucleiMasker.getHessianDeterminant()));
		comp2.setSlice(4);
		comp2.setProcessor(toFloatProcessor(nucleiMasker.getMask()));
		comp2.setSlice(slice2);
		comp2.getProcessor().setMinAndMax(0, 2);
	}

	private void paramStep2Changed() {
		readMaskingParameters();

		nucleiMasker.execStep2(); 
		nucleiMasker.execStep3(); 
		nucleiMasker.execStep4(); 

		int slice1 = comp1.getSlice();
		comp1.setSlice(2);
		comp1.setProcessor(toFloatProcessor(nucleiMasker.getAnisotropicDiffusionImage()));
		comp1.setSlice(3);
		comp1.setProcessor(toFloatProcessor(nucleiMasker.getResult()));
		comp1.setSlice(slice1);
		paramStep5Changed();

		int slice2 = comp2.getSlice();
		comp2.setSlice(1);
		comp2.setProcessor(toFloatProcessor(nucleiMasker.getGradientNorm()));
		comp2.setSlice(2);
		comp2.setProcessor(toFloatProcessor(nucleiMasker.getLaplacianMagnitude()));
		comp2.setSlice(3);
		comp2.setProcessor(toFloatProcessor(nucleiMasker.getHessianDeterminant()));
		comp2.setSlice(4);
		comp2.setProcessor(toFloatProcessor(nucleiMasker.getMask()));
		comp2.setSlice(slice2);
		comp2.getProcessor().setMinAndMax(0, 2);
	}

	private void paramStep3Changed() {
		readMaskingParameters();
		
		nucleiMasker.execStep3(); 
		nucleiMasker.execStep4(); 
		paramStep5Changed();

		int slice1 = comp1.getSlice();
		comp1.setSlice(3);
		comp1.setProcessor(toFloatProcessor(nucleiMasker.getResult()));
		comp1.setSlice(slice1);

		int slice2 = comp2.getSlice();
		comp2.setSlice(1);
		comp2.setProcessor(toFloatProcessor(nucleiMasker.getGradientNorm()));
		comp2.setSlice(2);
		comp2.setProcessor(toFloatProcessor(nucleiMasker.getLaplacianMagnitude()));
		comp2.setSlice(3);
		comp2.setProcessor(toFloatProcessor(nucleiMasker.getHessianDeterminant()));
		comp2.setSlice(4);
		comp2.setProcessor(toFloatProcessor(nucleiMasker.getMask()));
		comp2.setSlice(slice2);
		comp2.getProcessor().setMinAndMax(0, 2);
	}

	private void paramStep4Changed() {
		readMaskingParameters();
		
		nucleiMasker.execStep4();
		paramStep5Changed();

		int slice1 = comp1.getSlice();
		comp1.setSlice(3);
		comp1.setProcessor(toFloatProcessor(nucleiMasker.getResult()));
		comp1.setSlice(slice1);

		int slice2 = comp2.getSlice();
		comp2.setSlice(4);
		comp2.setProcessor(toFloatProcessor(nucleiMasker.getMask()));
		comp2.setSlice(slice2);
		comp2.getProcessor().setMinAndMax(0, 2);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void paramStep5Changed() {
		double threshFact = (Double) source.getSettings().get(KEY_THRESHOLD_FACTOR);
		Img<?> img = nucleiMasker.getResult();
		OtsuThresholder2D<?> thresholder = new OtsuThresholder2D(img, threshFact);
		thresholder.process();
		Img<BitType> bit = thresholder.getResult();
		
		int slice2 = comp2.getSlice();
		comp2.setSlice(5);
		comp2.setProcessor(toFloatProcessor(bit));
		comp2.setSlice(slice2);
		comp2.getProcessor().setMinAndMax(0, 2);
	}
	
	

	private void readMaskingParameters() {
		Map<String, Object> settings = source.getSettings();
		boolean doMedianFiltering 	= (Boolean) settings.get(KEY_DO_MEDIAN_FILTERING);
		double gaussFilterSigma 	= (Double) settings.get(KEY_SIGMA_FILTER);
		int nIterAnDiff 			= (Integer) settings.get(KEY_N_ANISOTROPIC_FILTERING);
		double kappa				= (Double) settings.get(KEY_KAPPA);
		double gaussGradSigma		= (Double) settings.get(KEY_SIGMA_GRADIENT);
		double gamma 				= (Double) settings.get(KEY_GAMMA);
		double alpha				= (Double) settings.get(KEY_ALPHA);
		double beta 				= (Double) settings.get(KEY_BETA);
		double epsilon				= (Double) settings.get(KEY_EPSILON);
		double delta 				= (Double) settings.get(KEY_DELTA);
		nucleiMasker.setParameters(doMedianFiltering, gaussFilterSigma, nIterAnDiff, kappa, 
				gaussGradSigma, gamma, alpha, beta, epsilon, delta);
	}

	/*
	 * STATIC METHODS
	 */


	public static void positionComponentRelativeTo(final Component target, final Component anchor, final int direction) {

		int x, y;
		switch (direction) {
		case 0:
			x = anchor.getX();
			y = anchor.getY() - target.getHeight();
			break;
		case 1:
		default:
			x = anchor.getX() + anchor.getWidth();
			y = anchor.getY();
			break;
		case 2:
			x = anchor.getX();
			y = anchor.getY() + anchor.getHeight();
			break;
		case 3:
			x = anchor.getX() - target.getWidth();
			y = anchor.getY();
			break;
		}

		// make sure the dialog fits completely on the screen...
		final Dimension sd = Toolkit.getDefaultToolkit().getScreenSize();
		final Rectangle s =  new Rectangle (0, 0, sd.width, sd.height);
		x = Math.min(x, (s.width - target.getWidth()));
		x = Math.max(x, 0);
		y = Math.min(y, (s.height - target.getHeight()));
		y = Math.max(y, 0);

		target.setBounds(x + s.x, y + s.y, target.getWidth(), target.getHeight());

	}

	/*
	 * PRIVATE CLASS
	 */

	/**
	 * This is a helper class modified after a class by Albert Cardona
	 */
	private class DisplayUpdater extends Thread {
		long request = 0;

		// Constructor autostarts thread
		DisplayUpdater() {
			super("CWNT updater thread");
			setPriority(Thread.NORM_PRIORITY);
			start();
		}

		void doUpdate() {
			if (isInterrupted())
				return;
			synchronized (this) {
				request++;
				notify();
			}
		}

		void quit() {
			interrupt();
			synchronized (this) {
				notify();
			}
		}

		public void run() {
			while (!isInterrupted()) {
				try {
					final long r;
					synchronized (this) {
						r = request;
					}
					// Call displayer update from this thread
					if (r > 0)
						refresh();
					synchronized (this) {
						if (r == request) {
							request = 0; // reset
							wait();
						}
						// else loop through to update again
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}
	
	private static final <T extends NativeType<T> & RealType<T>> FloatProcessor toFloatProcessor(Img<T> img) {
		ImageJVirtualStackFloat<T> v = new ImageJVirtualStackFloat<T>(img, new RealFloatConverter<T>());
		return (FloatProcessor) v.getProcessor(1);
	}
}