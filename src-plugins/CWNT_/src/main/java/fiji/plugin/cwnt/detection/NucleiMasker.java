package fiji.plugin.cwnt.detection;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.algorithm.gauss.Gauss;
import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.multithreading.Chunk;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import fiji.plugin.trackmate.detection.util.MedianFilter3x3;

public class NucleiMasker <T extends RealType<T> & NativeType<T>> extends MultiThreadedBenchmarkAlgorithm implements OutputAlgorithm<Img<T>> {

	private static final boolean DEBUG = false;

	/** The target image for the pre-processing steps. */
	private Img<T> target;
	private Img<T> image;

	// Step 1
	private Img<T> filtered;
	private boolean doMedianFiltering = false;
	private double gaussFilterSigma;

	// Step 2
	private Img<T> anDiffImage;
	private Img<FloatType> scaled;
	private int nIterAnDiff;
	private double kappa;

	// Step 3
	private double gaussGradSigma;
	private Img<FloatType> Gx;
	private Img<FloatType> Gy;
	private Img<FloatType> Gnorm;
	private Img<FloatType> Gxx;
	private Img<FloatType> Gxy;
	private Img<FloatType> Gyx;
	private Img<FloatType> Gyy;
	private Img<FloatType> H;
	private Img<FloatType> L;

	// Step 4
	private Img<FloatType> M;
	private double gamma;
	private double alpha;
	private double beta;
	private double epsilon;
	private double delta;


	/*
	 * CONSTRUCTOR
	 */

	public NucleiMasker(Img<T> image) {
		super();
		this.image = image;
		this.target = image;
	}

	/*
	 * METHODS
	 */

	public Img<T> 		getGaussianFilteredImage() 		{ return filtered; }
	public Img<T> 		getAnisotropicDiffusionImage() 	{ return anDiffImage; }
	public Img<FloatType> getGradientNorm() 				{ return Gnorm; }
	public Img<FloatType> getLaplacianMagnitude() 		{ return L; }
	public Img<FloatType> getHessianDeterminant() 		{ return H; }
	public Img<FloatType> getMask()						{ return M; }
	@Override
	public Img<T> 		getResult() 					{ return target; }

	/** 
	 * Set the parameters used by this instance to compute the cell mask.
	 * In the argument list, the parameters must be ordered as follow:
	 * <ol start="1">
	 * 	<li> a boolean stating whether we do median filtering in step 1
	 * 	<li> the σ for the gaussian filtering in step 1
	 *  <li> the number of iteration for anisotropic filtering in step 2
	 *  <li> κ, the gradient threshold for anisotropic filtering in step 2
	 * 	<li> the σ for the gaussian derivatives in step 3
	 *  <li> γ, the <i>tanh</i> shift in step 4
	 *  <li> α, the gradient prefactor in step 4
	 *  <li> β, the laplacian positive magnitude prefactor in step 4
	 *  <li> ε, the hessian negative magnitude prefactor in step 4
	 *  <li> δ, the derivative sum scale in step 4
	 * </ol>
	 */
	public void setParameters(boolean doMedianFiltering, double gaussFilterSigma, int nIterAnDiff, double kappa, 
			double gaussGradSigma, double gamma, double alpha, double beta, double epsilon, double delta) {
		this.doMedianFiltering  = doMedianFiltering;
		this.gaussFilterSigma 	= gaussFilterSigma;
		this.nIterAnDiff 		= nIterAnDiff;
		this.kappa				= kappa;
		this.gaussGradSigma		= gaussGradSigma;
		this.gamma 				= gamma;
		this.alpha				= alpha;
		this.beta				= beta;
		this.epsilon			= epsilon;
		this.delta				= delta;
	}


	public boolean execStep1() {
		boolean check = true;
		target = image.copy();
		/*
		 * Step 1a: Median filter.
		 * So as remove speckle noise.
		 */
		if (doMedianFiltering) {
			long top = System.currentTimeMillis();
			if (DEBUG) {
				System.out.println("[NucleiMasker] Median filtering... ");
			}
			check = execMedianFiltering();
			if (!check) {
				return false;
			}
			long dt =  (System.currentTimeMillis()-top);
			processingTime += dt;
			if (DEBUG) {
				System.out.println("dt = "+dt/1e3+" s.");
			}

		}

		/*
		 * Step 1b: Low pass filter.
		 * So as to damper the noise. We simply do a gaussian filtering.
		 */
		if (gaussFilterSigma > 0) {
			long top = System.currentTimeMillis();
			if (DEBUG) {
				System.out.print(String.format("[NucleiMasker] Low pass filter, with σf = %.1f ... ", gaussFilterSigma));
			}
			check = execGaussianFiltering();
			if (!check) {
				return false;
			}
			long dt =  (System.currentTimeMillis()-top);
			processingTime += dt;
			if (DEBUG) {
				System.out.println("dt = "+dt/1e3+" s.");
			}
		}

		return check;
	}

	public boolean execStep2() {
		/*
		 * Step 2a: Anisotropic diffusion
		 * To have nuclei of approximative constant intensity.
		 */
		if (DEBUG) {
			System.out.print(String.format("[NucleiMasker] Anisotropic diffusion with n = %d and κ = %.1f ... ", nIterAnDiff, kappa));
		}
		long top = System.currentTimeMillis();
		boolean check = execAnisotropicDiffusion();
		if (!check) {
			return false;
		}
		long dt = (System.currentTimeMillis()-top);
		processingTime += dt;
		if (DEBUG) {
			System.out.println("dt = "+dt/1e3+" s.");
		}

		/*
		 * Step 2b: Intensity scaling
		 * Scale intensities in each plane to the range 0 - 1
		 */
		if (DEBUG) {
			System.out.print("[NucleiMasker] Intensity scaling... ");
		}
		top = System.currentTimeMillis();
		check = execIntensityScaling();
		if (!check) {
			return false;
		}
		dt = (System.currentTimeMillis()-top);
		processingTime += dt;
		if (DEBUG) {
			System.out.println("dt = "+dt/1e3+" s.");
		}

		return check;
	}

	public boolean execStep3() {
		/*
		 * Step 3a: Gaussian gradient
		 */
		if (DEBUG) {
			System.out.print(String.format("[NucleiMasker] Gaussian gradient with %.1f ... ", gaussGradSigma));
		}
		long top = System.currentTimeMillis();
		boolean check = execComputeGradient();
		if (!check) {
			return false;
		}
		long dt = (System.currentTimeMillis()-top);
		processingTime += dt;
		if (DEBUG) {
			System.out.println("dt = "+dt/1e3+" s.");
		}

		/*
		 * Step 3b: Laplacian
		 */
		if (DEBUG) {
			System.out.print("[NucleiMasker] Laplacian... ");
		}
		top = System.currentTimeMillis();
		check = execComputeLaplacian();
		if (!check) {
			return false;
		}
		dt = (System.currentTimeMillis()-top);
		processingTime += dt;
		if (DEBUG) {
			System.out.println("dt = "+dt/1e3+" s.");
		}

		/*
		 * Step 3c: Hessian
		 */
		if (DEBUG) {
			System.out.print("[NucleiMasker] Hessian... ");
		}
		top = System.currentTimeMillis();
		check = execComputeHessian();
		if (!check) {
			return false;
		}
		dt = (System.currentTimeMillis()-top);
		processingTime += dt;
		if (DEBUG) {
			System.out.println("dt = "+dt/1e3+" s.");
		}

		return check;
	}

	public boolean execStep4() {
		/*
		 * Step 4a: Create masking function
		 */
		if (DEBUG) {
			System.out.print(String.format("[NucleiMasker] Creating mask function with γ = %.1f, α = %.1f, β = %.1f, ε = %.1f, δ = %.1f ... ", gamma, alpha, beta, epsilon, delta));
		}
		long top = System.currentTimeMillis();
		boolean check = execCreateMask();
		if (!check) {
			return false;
		}
		long dt = (System.currentTimeMillis()-top);
		processingTime += dt;
		if (DEBUG) {
			System.out.println("dt = "+dt/1e3+" s.");
		}

		/*
		 * Step 4b: Do masking, with the gaussian filtered image
		 */
		if (DEBUG) {
			System.out.print("[NucleiMasker] Masking... ");
		}
		top = System.currentTimeMillis();
		check = execMasking();
		if (!check) {
			return false;
		}
		dt = (System.currentTimeMillis()-top);
		processingTime += dt;
		if (DEBUG) {
			System.out.println("dt = "+dt/1e3+" s.");
		}

		return check;
	}

	public boolean process() {
		
		boolean check;

		check = execStep1();
		if (!check) {
			return false;
		}

		check = execStep2();
		if (!check) {
			return false;
		}

		check = execStep3();
		if (!check) {
			return false;
		}

		check = execStep4();
		if (!check) {
			return false;
		}

		return true;

	}


	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}



	/*
	 * PRIVATE METHODS
	 */

	private boolean execMasking() {
		target = filtered.factory().create(filtered, filtered.firstElement().copy());
		final Vector<Chunk> chunks = SimpleMultiThreading.divideIntoChunks(target.size(), numThreads);
		final AtomicInteger ai = new AtomicInteger();

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("NucleiSegmenter - masking thread "+i) {
				public void run() {

					Chunk chunk = chunks.get(ai.getAndIncrement());
					Cursor<T> ct = target.cursor();
					Cursor<T> cs = filtered.cursor();
					Cursor<FloatType> cm = M.cursor();

					for (int j = 0; j < chunk.getStartPosition(); j++) {
						cm.fwd();
						ct.fwd();
						cs.fwd();
					}

					for (int j = 0; j < chunk.getLoopSize(); j++) {

						cm.fwd();
						ct.fwd();
						cs.fwd();
						ct.get().setReal( cs.get().getRealDouble() * cm.get().get());
					}
				}
			};
		}

		SimpleMultiThreading.startAndJoin(threads);
		return true;
	}

	private boolean execCreateMask() {

		M = Gnorm.factory().create(Gnorm, new FloatType());
		final Vector<Chunk> chunks = SimpleMultiThreading.divideIntoChunks(M.size(), numThreads);
		final AtomicInteger ai = new AtomicInteger();

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("NucleiSegmenter - create mask thread "+i) {
				public void run() {

					Chunk chunk = chunks.get(ai.getAndIncrement());

					Cursor<FloatType> cm = M.cursor();
					Cursor<FloatType> cg = Gnorm.cursor();
					Cursor<FloatType> cl = L.cursor();
					Cursor<FloatType> ch = H.cursor();

					for (int j = 0; j < chunk.getStartPosition(); j++) {
						cm.fwd();
						cg.fwd();
						cl.fwd();
						ch.fwd();
					}

					double m;
					for (int j = 0; j < chunk.getLoopSize(); j++) {

						cm.fwd();
						cg.fwd();
						cl.fwd();
						ch.fwd();

						m = 0.5 * ( Math.tanh(
								gamma 
								- (		alpha * cg.get().get()
										+ beta * cl.get().get()
										+ epsilon * ch.get().get()
										)  / delta

								)	+ 1		);

						cm.get().setReal(m);
					}
				};
			};
		}

		SimpleMultiThreading.startAndJoin(threads);
		return true;
	}

	private boolean execComputeHessian() {

		H = Gxx.factory().create(Gxx,  new FloatType());
		final Vector<Chunk> chunks = SimpleMultiThreading.divideIntoChunks(H.size(), numThreads);
		final AtomicInteger ai = new AtomicInteger();

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("NucleiSegmenter - compute hessian thread "+i) {
				public void run() {

					Cursor<FloatType> cxx = Gxx.cursor();
					Cursor<FloatType> cxy = Gxy.cursor();
					Cursor<FloatType> cyx = Gyx.cursor();
					Cursor<FloatType> cyy = Gyy.cursor();
					Cursor<FloatType> ch = H.cursor();

					Chunk chunk = chunks.get(ai.getAndIncrement());
					for (int j = 0; j < chunk.getStartPosition(); j++) {
						cxx.fwd();
						cxy.fwd();
						cyx.fwd();
						cyy.fwd();
						ch.fwd();
					}
					float h;

					for (int j = 0; j < chunk.getLoopSize(); j++) {

						ch.fwd();
						cxx.fwd();
						cxy.fwd();
						cyx.fwd();
						cyy.fwd();

						h = (cxx.get().get() * cyy.get().get()) - (cxy.get().get() * cyx.get().get());
						if ( h < 0) {
							ch.get().set(-h);
						}
					}
				}
			};
		}

		SimpleMultiThreading.startAndJoin(threads);
		return true;
	}

	private boolean execComputeLaplacian() {
		GaussianGradient2D<FloatType> gradX = new GaussianGradient2D<FloatType>(Gx, gaussGradSigma);
		gradX.setNumThreads(numThreads);
		boolean check = gradX.checkInput() && gradX.process();
		if (check) {
			List<Img<FloatType>> gcX = gradX.getGradientComponents();
			Gxx = gcX.get(0);
			Gxy = gcX.get(1);
		} else {
			errorMessage = gradX.getErrorMessage();
			return false;
		}

		GaussianGradient2D<FloatType> gradY = new GaussianGradient2D<FloatType>(Gy, gaussGradSigma);
		gradY.setNumThreads(numThreads);
		check = gradY.checkInput() && gradY.process();
		if (check) {
			List<Img<FloatType>> gcY = gradY.getGradientComponents();
			Gyx = gcY.get(0);
			Gyy = gcY.get(1);
		} else {
			errorMessage = gradY.getErrorMessage();
			return false;
		}

		// Enucluated laplacian magnitude
		L = Gxx.factory().create(Gxx, new FloatType());
		final Vector<Chunk> chunks = SimpleMultiThreading.divideIntoChunks(L.size(), numThreads);
		final AtomicInteger ai = new AtomicInteger();

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("NucleiSegmenter - compute laplacian thread "+i) {

				public void run() {

					Cursor<FloatType> cxx = Gxx.cursor();
					Cursor<FloatType> cyy = Gyy.cursor();
					Cursor<FloatType> cl = L.cursor();

					Chunk chunk = chunks.get(ai.getAndIncrement());
					for (int j = 0; j < chunk.getStartPosition(); j++) {
						cxx.fwd();
						cyy.fwd();
						cl.fwd();
					}

					float lap;
					for (int j = 0; j < chunk.getLoopSize(); j++) {
						cl.fwd();
						cxx.fwd();
						cyy.fwd();
						lap = cxx.get().get() + cyy.get().get();
						if (lap > 0) {
							cl.get().set(lap);
						}
					}
				}
			};
		}

		SimpleMultiThreading.startAndJoin(threads);
		return true;
	}



	private boolean execComputeGradient() {
		GaussianGradient2D<FloatType> grad = new GaussianGradient2D<FloatType>(scaled, gaussGradSigma);
		grad.setNumThreads(numThreads);
		boolean check = grad.checkInput() && grad.process();
		if (check) {
			List<Img<FloatType>> gc = grad.getGradientComponents();
			Gx = gc.get(0);
			Gy = gc.get(1);
			Gnorm = grad.getResult();
			return true;
		} else {
			errorMessage = grad.getErrorMessage();
			return false;
		}

	}

	private boolean execIntensityScaling() {
		ImgFactory<FloatType> factory = null;
		try {
			factory = anDiffImage.factory().imgFactory(new FloatType());
		} catch (IncompatibleTypeException e) {
			e.printStackTrace();
		}
		scaled = factory.create(filtered, new FloatType());

		final long width = scaled.dimension(0);
		final long height = scaled.dimension(1);
		final long nslices = scaled.dimension(2);

		final AtomicInteger aj = new AtomicInteger();

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);

		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("NucleiSegmenter intensity scaling thread "+i) {

				public void run() {

					RandomAccess<T> cs = anDiffImage.randomAccess();
					RandomAccess<FloatType> ct = scaled.randomAccess();

					float val;
					for (int z=aj.getAndIncrement(); z<nslices; z=aj.getAndIncrement()) {

						if (nslices > 1) { // If we get a 2D image
							cs.setPosition(z, 2);
							ct.setPosition(z, 2);
						}

						// Find min & max

						double val_min = anDiffImage.firstElement().createVariable().getMaxValue();
						double val_max = anDiffImage.firstElement().createVariable().getMinValue();
						T min = anDiffImage.firstElement().createVariable();
						T max = anDiffImage.firstElement().createVariable();
						min.setReal(val_min);
						max.setReal(val_max);

						for (int y = 0; y < height; y++) {
							cs.setPosition(y, 1);

							for (int x = 0; x < width; x++) {
								cs.setPosition(x, 0);

								if (cs.get().compareTo(min) < 0) {
									min.set(cs.get());
								}
								if (cs.get().compareTo(max) > 0) {
									max.set(cs.get());
								}

							}
						}

						// Scale
						for (int y = 0; y < height; y++) {
							cs.setPosition(y, 1);
							ct.setPosition(y, 1);

							for (int x = 0; x < width; x++) {
								cs.setPosition(x, 0);
								ct.setPosition(x, 0);

								val = (cs.get().getRealFloat() - min.getRealFloat()) / (max.getRealFloat() - min.getRealFloat());
								ct.get().set(val);


							}
						}

					}
				}

			};
		}

		SimpleMultiThreading.startAndJoin(threads);
		return true;
	}


	private boolean execAnisotropicDiffusion() {
		anDiffImage = filtered.copy();
		ArrayImgFactory<FloatType> factory = new ArrayImgFactory<FloatType>();
		for (long z = 0; z < anDiffImage.dimension(2); z++) {
			// only operate over a single slice
			RandomAccessibleInterval<T> slice;
			if (anDiffImage.numDimensions() > 2) {
				slice = Views.hyperSlice(anDiffImage, 2, z);
			} else {
				slice = anDiffImage;
			}
			for (int i = 0; i < nIterAnDiff; i++) {
				PeronaMalikAnisotropicDiffusion.inFloatInPlace(slice, factory, 0.15, kappa);
			}
		}
		return true;
	}


	private boolean execGaussianFiltering() {
		// 2D filtering
		for (long z = 0; z < target.dimension(2); z++) {
			Img<T> slice;
			if (target.numDimensions() >2) {
				slice = new HyperSliceImg<T>(target, 2, z);
			} else {
				slice = target;
			}
			Gauss.inFloatInPlace(gaussFilterSigma, slice);
		}
		filtered = target; // Store for last step.
		return true;
	}

	private boolean execMedianFiltering() {
		final MedianFilter3x3<T> medFilt = new MedianFilter3x3<T>(target); 
		if (!medFilt.process()) {
			errorMessage = "Failed in applying median filter";
			return false;
		}
		target = medFilt.getResult();
		filtered = target;
		return true; 
	}


	@Override
	public long getProcessingTime() {
		return processingTime;
	}

}
