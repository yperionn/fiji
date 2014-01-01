package fiji.plugin.mamut.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.imglib2.ui.util.GuiUtil;
import bdv.img.cache.Cache;
import bdv.viewer.InputActionBindings;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerPanel.Options;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

/**
 * A {@link JFrame} containing a {@link ViewerPanel} and associated
 * {@link InputActionBindings}.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class MamutViewer extends JFrame implements TrackMateModelView
{
	private static final long serialVersionUID = 1L;

	protected final MamutViewerPanel viewerPanel;

	private final InputActionBindings keybindings;

	public MamutViewer(
			final int width, final int height,
			final List< SourceAndConverter< ? > > sources,
			final int numTimePoints,
			final Cache cache,
			final Model model,
			final SelectionModel selectionModel )
	{
		this( width, height, sources, numTimePoints, cache, model, selectionModel, ViewerPanel.options() );
	}

	/**
	 *
	 * @param width
	 *            width of the display window.
	 * @param height
	 *            height of the display window.
	 * @param sources
	 *            the {@link SourceAndConverter sources} to display.
	 * @param numTimePoints
	 *            number of available timepoints.
	 * @param cache
	 *            handle to cache. This is used to control io timing. Also, is
	 *            is used to subscribe / {@link #stop() unsubscribe} to the
	 *            cache as a consumer, so that eventually the io fetcher threads
	 *            can be shut down.
	 * @param optional
	 *            optional parameters. See {@link ViewerPanel#options()}.
	 */
	public MamutViewer(
			final int width, final int height,
			final List< SourceAndConverter< ? > > sources,
			final int numTimePoints,
			final Cache cache,
			final Model model,
			final SelectionModel selectionModel,
			final Options optional )
	{
		super("BigDataViewer", GuiUtil.getSuitableGraphicsConfiguration(GuiUtil.RGB_COLOR_MODEL));
		viewerPanel = new MamutViewerPanel(sources, numTimePoints, cache, optional.width(width).height(height));
		keybindings = new InputActionBindings();

		this.model = model;
		this.selectionModel = selectionModel;
		this.logger = new MamutViewerLogger();

		getRootPane().setDoubleBuffered(true);
		setPreferredSize(new Dimension(width, height));
		add(viewerPanel, BorderLayout.CENTER);
		pack();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				viewerPanel.stop();
			}
		});

		SwingUtilities.replaceUIActionMap(getRootPane(), keybindings.getConcatenatedActionMap());
		SwingUtilities.replaceUIInputMap(getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap());

		setVisible( true );
	}

	public void addHandler( final Object handler )
	{
		viewerPanel.getDisplay().addHandler( handler );
		if ( KeyListener.class.isInstance( handler ) )
			addKeyListener( ( KeyListener ) handler );
	}

	public MamutViewerPanel getViewerPanel()
	{
		return viewerPanel;
	}

	public InputActionBindings getKeybindings()
	{
		return keybindings;
	}











	private static final String INFO_TEXT = "A viewer based on Tobias Pietzsch SPIM Viewer";

	public static final String KEY = "MaMuT Viewer";

	/** The logger instance that echoes message on this view. */
	private final Logger logger;

	private final Model model;

	private final SelectionModel selectionModel;

	/** A map of String/Object that configures the look and feel of the display. */
	protected Map< String, Object > displaySettings = new HashMap< String, Object >();

	/** The mapping from spot to a color. */
	SpotColorGenerator spotColorProvider;

	TrackColorGenerator trackColorProvider;

	/**
	 * Returns the {@link Logger} object that will echo any message to this
	 * {@link MamutViewer} window.
	 *
	 * @return this {@link MamutViewer} logger.
	 */
	public Logger getLogger()
	{
		return logger;
	}

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}

	@Override
	public void render() {
		viewerPanel.overlay = new MamutOverlay( model, selectionModel, this );
	}

	@Override
	public void refresh() {
		viewerPanel.requestRepaint();
	}

	@Override
	public void clear() {
		viewerPanel.overlay = null;
	}

	@Override
	public void centerViewOn(final Spot spot) {
		viewerPanel.centerViewOn(spot);
	}

	@Override
	public Map<String, Object> getDisplaySettings() {
		return displaySettings;
	}

	@Override
	public void setDisplaySettings(final String key, final Object value) {
		if (key.equals(KEY_SPOT_COLORING)) {
			if (null != spotColorProvider) {
				spotColorProvider.terminate();
			}
			spotColorProvider = (SpotColorGenerator) value;
		} else if (key.equals(KEY_TRACK_COLORING)) {
			if (null != trackColorProvider) {
				trackColorProvider.terminate();
			}
			trackColorProvider = (TrackColorGenerator) value;
		}

		displaySettings.put(key, value);
		refresh();
	}

	@Override
	public Object getDisplaySettings(final String key) {
		return displaySettings.get( key );
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	/*
	 * INNER CLASSES
	 */

	private final class MamutViewerLogger extends Logger
	{

		@Override
		public void setStatus( final String status )
		{
			viewerPanel.showMessage(status);
		}

		@Override
		public void setProgress(final double val) {}

		@Override
		public void log( final String message, final Color color )
		{
			viewerPanel.showMessage(message);
		}

		@Override
		public void error( final String message )
		{
			viewerPanel.showMessage(message);
		}

	}
}
