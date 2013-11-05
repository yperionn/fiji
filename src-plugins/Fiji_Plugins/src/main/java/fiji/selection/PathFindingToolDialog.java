package fiji.selection;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.selection.PathFinder.PathType;

final class PathFindingToolDialog extends JFrame
{
	private static final long serialVersionUID = 1L;

	private static final Font FONT = new Font( "Arial", Font.PLAIN, 10 );

	private static final ImageIcon BUTTON_ICON = new ImageIcon( PathFindingToolDialog.class.getResource( "page_white_text.png" ) );

	final ActionEvent SETTINGS_CHANGED = new ActionEvent( this, 0, "SettingsChanged" );

	final ActionEvent RESULT_BUTTON_PUSHED = new ActionEvent( this, 1, "ResultButtonPushed" );

	private final JComboBox comboBox;

	private final JSlider slider;

	private final Set< ActionListener > actionListener = new HashSet< ActionListener >();

	public PathFindingToolDialog( final double heuristicStrength, final PathType pathType )
	{
		setSize( new Dimension( 300, 300 ) );
		setResizable( false );

		final JPanel panel = new JPanel();
		getContentPane().add( panel, BorderLayout.CENTER );
		panel.setLayout( null );

		final JLabel lblInfo = new JLabel();
		lblInfo.setVerticalAlignment( SwingConstants.TOP );
		lblInfo.setFont( FONT );
		lblInfo.setText( "<html><h2>Pathfinding profile</h2>CLick & drag the mouse on the target image to specify the start and end points." + "</html>" );
		lblInfo.setBounds( 6, 6, 287, 79 );
		panel.add( lblInfo );

		slider = new JSlider( 0, 200, ( int ) ( 100 * heuristicStrength ) );
		slider.setBounds( 138, 202, 155, 29 );
		slider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent event )
			{
				fireStateChanged( SETTINGS_CHANGED );
			}
		} );
		panel.add( slider );

		comboBox = new JComboBox( PathFinder.PathType.values() );
		comboBox.setBounds( 138, 163, 155, 27 );
		comboBox.setFont( FONT );
		comboBox.addActionListener( new ActionListener()
		{

			@Override
			public void actionPerformed( final ActionEvent event )
			{
				fireStateChanged( SETTINGS_CHANGED );
			}
		} );
		panel.add( comboBox );

		final JLabel lblPathType = new JLabel( "Find path on:" );
		lblPathType.setFont( FONT );
		lblPathType.setBounds( 6, 167, 120, 16 );
		panel.add( lblPathType );

		final JLabel lblHeuristicStrength = new JLabel( "Long paths penalty" );
		lblHeuristicStrength.setFont( FONT );
		lblHeuristicStrength.setBounds( 6, 202, 130, 16 );
		panel.add( lblHeuristicStrength );

		// final JLabel lblStatus = new JLabel( targetName );
		// lblStatus.setHorizontalAlignment( SwingConstants.CENTER );
		// lblStatus.setFont( FONT.deriveFont( Font.ITALIC ).deriveFont( 12f )
		// );
		// lblStatus.setBorder( new LineBorder( Color.ORANGE, 1, true ) );
		// lblStatus.setBounds( 6, 89, 287, 62 );
		// panel.add( lblStatus );

		final JButton btnGenerateTable = new JButton( "Results table" );
		btnGenerateTable.setFont( FONT );
		btnGenerateTable.setBounds( 6, 243, 117, 29 );
		btnGenerateTable.setIcon( BUTTON_ICON );
		btnGenerateTable.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				fireStateChanged( RESULT_BUTTON_PUSHED );
			}
		} );
		panel.add( btnGenerateTable );

	}

	private void fireStateChanged( final ActionEvent event )
	{
		for ( final ActionListener listener : actionListener )
		{
			listener.actionPerformed( event );
		}
	}

	public boolean addActionListener( final ActionListener listener )
	{
		return actionListener.add( listener );
	}

	public boolean removeActionListener( final ActionListener listener )
	{
		return actionListener.remove( listener );
	}


	PathFinder.PathType getPathType()
	{
		return ( PathFinder.PathType ) comboBox.getSelectedItem();
	}

	double getSliderValue()
	{
		return ( slider.getValue() / 100d );
	}

	@Override
	public String toString()
	{
		return "PathType = " + getPathType() + ", slider value = " + getSliderValue();
	}
}