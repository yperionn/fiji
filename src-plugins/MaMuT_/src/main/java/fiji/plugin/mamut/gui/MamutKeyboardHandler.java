package fiji.plugin.mamut.gui;

import static bdv.viewer.NavigationActions.ALIGN_PLANE;
import static bdv.viewer.NavigationActions.NEXT_TIMEPOINT;
import static bdv.viewer.NavigationActions.PREVIOUS_TIMEPOINT;
import static bdv.viewer.NavigationActions.SET_CURRENT_SOURCE;
import static bdv.viewer.NavigationActions.TOGGLE_FUSED_MODE;
import static bdv.viewer.NavigationActions.TOGGLE_GROUPING;
import static bdv.viewer.NavigationActions.TOGGLE_INTERPOLATION;
import static bdv.viewer.NavigationActions.TOGGLE_SOURCE_VISIBILITY;
import static fiji.plugin.mamut.gui.MamutActions.ADD_SPOT;
import static fiji.plugin.mamut.gui.MamutActions.BRIGHTNESS_SETTINGS;
import static fiji.plugin.mamut.gui.MamutActions.DECREASE_SPOT_RADIUS;
import static fiji.plugin.mamut.gui.MamutActions.DECREASE_SPOT_RADIUS_A_BIT;
import static fiji.plugin.mamut.gui.MamutActions.DECREASE_SPOT_RADIUS_A_LOT;
import static fiji.plugin.mamut.gui.MamutActions.DELETE_SPOT;
import static fiji.plugin.mamut.gui.MamutActions.INCREASE_SPOT_RADIUS;
import static fiji.plugin.mamut.gui.MamutActions.INCREASE_SPOT_RADIUS_A_BIT;
import static fiji.plugin.mamut.gui.MamutActions.INCREASE_SPOT_RADIUS_A_LOT;
import static fiji.plugin.mamut.gui.MamutActions.SEMI_AUTO_TRACKING;
import static fiji.plugin.mamut.gui.MamutActions.SHOW_HELP;
import static fiji.plugin.mamut.gui.MamutActions.TOGGLE_LINKING;

import java.io.File;

import javax.swing.ActionMap;
import javax.swing.InputMap;

import bdv.util.KeyProperties;
import bdv.util.KeyProperties.KeyStrokeAdder;
import bdv.viewer.InputActionBindings;
import bdv.viewer.NavigationActions;
import bdv.viewer.ViewerPanel.AlignPlane;
import fiji.FijiTools;
import fiji.plugin.mamut.MaMuT;
import fiji.plugin.mamut.viewer.MamutViewer;

public class MamutKeyboardHandler {

	private final MamutViewer viewer;
	private final MaMuT mamut;

	public MamutKeyboardHandler(final MaMuT mamut, final MamutViewer viewer) {
		this.mamut = mamut;
		this.viewer = viewer;

		installKeyboardActions();
	}

	protected void installKeyboardActions() {
		final String fijiDir = FijiTools.getFijiDir();
		final KeyProperties keyProperties = KeyProperties.readPropertyFile(new File(fijiDir, "mamut.properties"));
		installActionBindings(viewer.getKeybindings(), mamut, viewer, keyProperties);
	}

	/**
	 * Create actions and install them in the specified
	 * {@link InputActionBindings}.
	 *
	 * @param inputActionBindings
	 *            {@link InputMap} and {@link ActionMap} are installed here.
	 * @param mamut
	 *            Actions are targeted at this {@link MaMuT} instance.
	 * @param viewer
	 *            Actions are targeted at this {@link MamutViewer} window.
	 * @param keyProperties
	 *            user-defined key-bindings.
	 */
	public static void installActionBindings(
			final InputActionBindings inputActionBindings,
			final MaMuT mamut,
			final MamutViewer viewer,
			final KeyProperties keyProperties )
	{
		inputActionBindings.addActionMap("mamut", MamutActions.createActionMap(mamut, viewer));
		inputActionBindings.addActionMap("navigation", NavigationActions.createActionMap(viewer.getViewerPanel()));
		inputActionBindings.addInputMap("all", createInputMap(keyProperties));
	}

	public static InputMap createInputMap( final KeyProperties keyProperties )
	{
		final InputMap inputMap = new InputMap();
		final KeyStrokeAdder map = keyProperties.adder( inputMap );

		map.put(ADD_SPOT,                   "A", "ENTER");
		map.put(DELETE_SPOT,                "D");
		map.put(SEMI_AUTO_TRACKING,         "shift A");
		map.put(TOGGLE_LINKING,             "shift L");
		map.put(INCREASE_SPOT_RADIUS,       "E");
		map.put(DECREASE_SPOT_RADIUS,       "Q");
		map.put(INCREASE_SPOT_RADIUS_A_LOT, "shift E");
		map.put(DECREASE_SPOT_RADIUS_A_LOT, "shift Q");
		map.put(INCREASE_SPOT_RADIUS_A_BIT, "control E");
		map.put(DECREASE_SPOT_RADIUS_A_BIT, "control Q");
		map.put(SHOW_HELP,                  "F1", "H");
		map.put(BRIGHTNESS_SETTINGS,        "S");

		map.put( TOGGLE_INTERPOLATION, "I" );
		map.put( TOGGLE_FUSED_MODE, "F" );
		map.put( TOGGLE_GROUPING, "G" );
		map.put( NEXT_TIMEPOINT, "CLOSE_BRACKET", "M" );
		map.put( PREVIOUS_TIMEPOINT, "OPEN_BRACKET", "N" );

		final String[] numkeys = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };
		for ( int i = 0; i < numkeys.length; ++i )
		{
			map.put( String.format( SET_CURRENT_SOURCE, i ), numkeys[ i ] );
			map.put( String.format( TOGGLE_SOURCE_VISIBILITY, i ), "shift " + numkeys[ i ] );
		}

		map.put( String.format( ALIGN_PLANE, AlignPlane.XY ), "shift Z" );
		map.put( String.format( ALIGN_PLANE, AlignPlane.ZY ), "shift X" );
		map.put( String.format( ALIGN_PLANE, AlignPlane.XZ ), "shift Y", "shift C" );

		return inputMap;
	}
}
