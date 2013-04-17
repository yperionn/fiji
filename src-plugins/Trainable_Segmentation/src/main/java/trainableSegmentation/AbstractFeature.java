package trainableSegmentation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ij.process.ImageProcessor;

public abstract class AbstractFeature implements Feature {

	private final String[] parameterNames;
	private final double[] parameterValues;

	protected final static Pattern descriptorPattern = Pattern.compile(".*\\((.*)\\)");

	public AbstractFeature(final String[] parameterNames) {
		this.parameterNames = parameterNames;
		this.parameterValues = new double[parameterNames.length];
	}

	@Override
	public String getDescriptor() {
		final StringBuilder builder = new StringBuilder();
		builder.append(getClass().getName()).append('(');
		for (int i = 0; i < parameterNames.length; i++) {
			if (i > 0) builder.append(',');
			builder.append(parameterNames[i]).append("=").append(parameterValues[i]);
		}
		builder.append(')');
		return builder.toString();
	}

	// TODO: should we use the LogService here to report errors?

	@Override
	public void setParametersFromDescriptor(String descriptor) {
		final Matcher matcher = descriptorPattern.matcher( descriptor );
		if (matcher.matches()) {
			final String[] list = matcher.group(1).split(",");
			for (int i = 0; i < list.length; i++) {
				int equal = list[i].indexOf('=');
				if (equal < 0) continue;
				final String key = list[i].substring(0, equal);
				final double value = Double.parseDouble(list[i].substring(equal + 1));
				int index = key.equals(parameterNames[i]) ? i : findParameterIndex(key);
				if (index < 0) continue;
				parameterValues[index] = value;
			}
		}
	}

	@Override
	public int getParameterCount() {
		return parameterNames.length;
	}

	@Override
	public String getParameterName(int index) {
		return parameterNames[index];
	}

	@Override
	public void setParameter(int index, double value) {
		parameterValues[index] = value;
	}

	@Override
	public double getParameter( int index ) {
		return parameterValues[index];
	}

	@Override
	public int findParameterIndex(String key) {
		if (key == null) return -1;
		for (int i = 0; i < parameterNames.length; i++) {
			if (key.equals(parameterNames[i])) return i;
		}
		return -1;
	}
	
	@Override
	public abstract ImageProcessor compute(ImageProcessor input);

}
