package fiji.plugin.cwnt.detection;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.Cursor;
import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.gauss3.SeparableSymmetricConvolution;
import net.imglib2.algorithm.gradient.PartialDerivative;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.multithreading.Chunk;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.apache.commons.math.util.FastMath;

public class GaussianGradient2D <T extends NumericType<T> & NativeType<T>> extends MultiThreadedBenchmarkAlgorithm implements OutputAlgorithm<Img<FloatType>> {

	private static final String BASE_ERROR_MESSAGE = "[GaussianGradient2D] ";
	private Img<T> source;
	private double sigma;
	private Img<FloatType> Dx;
	private Img<FloatType> Dy;
	private List<Img<FloatType>> components = new ArrayList<Img<FloatType>>(2);


	/*
	 * CONSTRUCTOR
	 */


	public GaussianGradient2D(Img<T> source, double sigma) {
		super();
		this.source = source;
		this.sigma = sigma;
	}


	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput() {
		if (source.numDimensions() < 2 || source.numDimensions() > 3) {
			errorMessage = BASE_ERROR_MESSAGE +"Can only operate on 2D  or 3D images, got " + source.numDimensions() + "D.";
			return false;
		}
		return true;
	}

	@Override
	public boolean process() {
		long start = System.currentTimeMillis();
		double[] sigmas = new double[] { sigma, sigma } ;
		final double[][] halfkernels = Gauss3.halfkernels( sigmas );

		try {
			Dx = source.factory().imgFactory(new FloatType()).create(source, new FloatType());
			Dy = source.factory().imgFactory(new FloatType()).create(source, new FloatType());
		} catch (IncompatibleTypeException e) {
			errorMessage = BASE_ERROR_MESSAGE + "Could not instantiate target image: " 
					+ e.getMessage();
			return false;
		}
		
		long nzs = source.dimension(2);
		for (int z = 0; z < nzs; z++) {
			
			RandomAccessibleInterval<T> slice;
			RandomAccessibleInterval<FloatType> targetSliceDx;
			RandomAccessibleInterval<FloatType> targetSliceDy;
			if (source.numDimensions() > 2) {
				slice = Views.hyperSlice(source, 2, z);
				targetSliceDx = Views.hyperSlice(Dx, 2, z);
				targetSliceDy = Views.hyperSlice(Dy, 2, z);
			} else {
				slice = source;
				targetSliceDx = Dx;
				targetSliceDy = Dy;
			}

			Img<FloatType> target;
			try {
				target = source.factory().imgFactory(new FloatType()).create(slice, new FloatType());
			} catch (IncompatibleTypeException e) {
				errorMessage = BASE_ERROR_MESSAGE + "Could not instantiate target image: " 
						+ e.getMessage();
				return false;
			}

			try {
				ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval<T>> exslice = Views.extendBorder(slice);
				SeparableSymmetricConvolution.convolve( halfkernels, exslice, target, numThreads );
			} catch (IncompatibleTypeException e) {
				errorMessage = BASE_ERROR_MESSAGE + "Could not compute gaussian filtered image: " 
						+ e.getMessage();
				return false;
			}
			

			ExtendedRandomAccessibleInterval<FloatType, Img<FloatType>> extarget = Views.extendBorder(target);
			// In X
			PartialDerivative.gradientCentralDifference(extarget, targetSliceDx, 0);
			// In Y
			PartialDerivative.gradientCentralDifference(extarget, targetSliceDy, 1);
		}
		
		components.clear();
		components.add(Dx);
		components.add(Dy);

		long end = System.currentTimeMillis();
		processingTime = end-start;
		return true;
	}


	public List<Img<FloatType>> getGradientComponents() {
		return components;
	}


	/**
	 * Returns the gradient norm
	 */
	@Override
	public Img<FloatType> getResult() {
		final Img<FloatType> norm = Dx.factory().create(Dx, new FloatType());

		final Vector<Chunk> chunks = SimpleMultiThreading.divideIntoChunks(norm.size(), numThreads);
		final AtomicInteger ai = new AtomicInteger();

		Thread[] threads = new Thread[chunks.size()];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("Gradient norm thread "+i) {
				public void run() {

					Chunk chunk = chunks.get(ai.getAndIncrement());

					Cursor<FloatType> cx = Dx.cursor();
					Cursor<FloatType> cy = Dy.cursor();
					Cursor<FloatType> cn = norm.cursor();

					double x, y;
					cn.jumpFwd(chunk.getStartPosition());
					cx.jumpFwd(chunk.getStartPosition());
					cy.jumpFwd(chunk.getStartPosition()); 
					for (long j = 0; j < chunk.getLoopSize(); j++) {
						cn.fwd();
						cx.fwd();
						cy.fwd(); // Ok because we have identical containers
						x = cx.get().get();
						y = cy.get().get();
						cn.get().setReal(FastMath.sqrt(x*x+y*y));
					}
				}

			};
		}

		SimpleMultiThreading.startAndJoin(threads);
		return norm;
	}

}
