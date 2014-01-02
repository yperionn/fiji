package fiji.plugin.mamut.gui;

import java.awt.event.ActionEvent;

import javax.swing.ActionMap;

import bdv.util.AbstractNamedAction;
import bdv.util.AbstractNamedAction.NamedActionAdder;
import fiji.plugin.mamut.MaMuT;
import fiji.plugin.mamut.viewer.MamutViewer;
import fiji.plugin.trackmate.Logger;

public class MamutActions {

	private MamutActions() {}

	public static final String ADD_SPOT = "add spot";
	public static final String DELETE_SPOT = "delete spot";
	public static final String SEMI_AUTO_TRACKING = "semi-auto tracking";
	public static final String TOGGLE_LINKING = "toggle linking mode";
	public static final String INCREASE_SPOT_RADIUS = "increase spot radius";
	public static final String DECREASE_SPOT_RADIUS = "decrease spot radius";
	public static final String INCREASE_SPOT_RADIUS_A_LOT = "increase spot radius a lot";
	public static final String DECREASE_SPOT_RADIUS_A_LOT = "decrease spot radius a lot";
	public static final String INCREASE_SPOT_RADIUS_A_BIT = "increase spot radius a bit";
	public static final String DECREASE_SPOT_RADIUS_A_BIT = "decrease spot radius a bit";
	public static final String BRIGHTNESS_SETTINGS = "toggle brightness dialog";
	public static final String SHOW_HELP = "show help";

	public static ActionMap createActionMap(final MaMuT mamut, final MamutViewer viewer) {
		final ActionMap actionMap = new ActionMap();
		final NamedActionAdder map = new NamedActionAdder(actionMap);

		map.put(new AddSpotAction(ADD_SPOT, mamut, viewer));
		map.put(new DeleteSpotAction(DELETE_SPOT, mamut, viewer));
		map.put(new SemiAutoTrackingAction(SEMI_AUTO_TRACKING, mamut));
		map.put(new ToggleLinkingModeAction(TOGGLE_LINKING, mamut, viewer.getLogger()));
		map.put(new IncreaseRadiusAction(INCREASE_SPOT_RADIUS, mamut, viewer, 1d));
		map.put(new IncreaseRadiusAction(INCREASE_SPOT_RADIUS_A_LOT, mamut, viewer, 10d));
		map.put(new IncreaseRadiusAction(INCREASE_SPOT_RADIUS_A_BIT, mamut, viewer, 0.1d));
		map.put(new IncreaseRadiusAction(DECREASE_SPOT_RADIUS, mamut, viewer, -1d));
		map.put(new IncreaseRadiusAction(DECREASE_SPOT_RADIUS_A_LOT, mamut, viewer, -5d));
		map.put(new IncreaseRadiusAction(DECREASE_SPOT_RADIUS_A_BIT, mamut, viewer, -0.1d));
		map.put(new ToggleBrightnessDialogAction(BRIGHTNESS_SETTINGS, mamut));
		map.put(new ShowHelpAction(SHOW_HELP, mamut));

		return actionMap;
	}

	private static abstract class MamutAction extends AbstractNamedAction {

		private static final long serialVersionUID = 1L;
		protected final MaMuT mamut;

		public MamutAction(final String name, final MaMuT mamut) {
			super(name);
			this.mamut = mamut;
		}
	}

	private static abstract class MamutViewerAction extends MamutAction {

		private static final long serialVersionUID = 1L;
		protected final MamutViewer viewer;

		public MamutViewerAction(final String name, final MaMuT mamut, final MamutViewer viewer) {
			super(name, mamut);
			this.viewer = viewer;
		}
	}

	private static final class ToggleLinkingModeAction extends MamutAction {

		private static final long serialVersionUID = 1L;
		private final Logger logger;

		public ToggleLinkingModeAction(final String name, final MaMuT mamut, final Logger logger) {
			super(name, mamut);
			this.logger = logger;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			mamut.toggleLinkingMode(logger);
		}
	}

	private static final class ToggleBrightnessDialogAction extends MamutAction {

		private static final long serialVersionUID = 1L;

		public ToggleBrightnessDialogAction(final String name, final MaMuT mamut) {
			super(name, mamut);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			mamut.toggleBrightnessDialog();
		}

	}

	private static final class ShowHelpAction extends MamutAction {

		private static final long serialVersionUID = 1L;

		public ShowHelpAction(final String name, final MaMuT mamut) {
			super(name, mamut);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			mamut.toggleHelpDialog();
		}

	}

	private static final class AddSpotAction extends MamutViewerAction {

		private static final long serialVersionUID = 1L;

		public AddSpotAction(final String name, final MaMuT mamut, final MamutViewer viewer) {
			super(name, mamut, viewer);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			mamut.addSpot(viewer);
		}

	}

	private static final class DeleteSpotAction extends MamutViewerAction {

		private static final long serialVersionUID = 1L;

		public DeleteSpotAction(final String name, final MaMuT mamut, final MamutViewer viewer) {
			super(name, mamut, viewer);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			mamut.deleteSpot(viewer);
		}

	}

	private static final class SemiAutoTrackingAction extends MamutAction {

		private static final long serialVersionUID = 1L;

		public SemiAutoTrackingAction(final String name, final MaMuT mamut) {
			super(name, mamut);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			mamut.semiAutoDetectSpot();
		}
	}

	private static final class IncreaseRadiusAction extends MamutViewerAction {

		private static final long serialVersionUID = 1L;
		private final double factor;

		public IncreaseRadiusAction(final String name, final MaMuT mamut, final MamutViewer viewer, final double factor) {
			super(name, mamut, viewer);
			this.factor = factor;
		}

		@Override
		public void actionPerformed(final ActionEvent arg0) {
			mamut.increaseSpotRadius(viewer, factor);
		}
	}
}
