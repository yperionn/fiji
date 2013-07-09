package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;

public abstract class AbstractTMAction implements TrackMateAction {

	protected Logger logger = Logger.VOID_LOGGER;
	protected ImageIcon icon = null;
	protected final TrackMate trackmate;
	protected final TrackMateGUIController controller;

	public AbstractTMAction(final TrackMate trackmate, final TrackMateGUIController controller) {
		this.trackmate = trackmate;
		this.controller = controller;
	}

	@Override
	public void setLogger(final Logger logger) {
		this.logger = logger;
	}

	@Override
	public ImageIcon getIcon() {
		return icon;
	}

}
