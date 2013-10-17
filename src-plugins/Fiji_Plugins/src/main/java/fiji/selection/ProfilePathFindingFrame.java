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

final class ProfilePathFindingFrame extends JFrame
{
	private static final long serialVersionUID = 1L;

	private static final Font FONT = new Font( "Arial", Font.PLAIN, 10 );

	private static final ImageIcon BUTTON_ICON = new ImageIcon( ProfilePathFindingFrame.class.getResource( "page_white_text.png" ) );

	final ActionEvent SETTINGS_CHANGED = new ActionEvent( this, 0, "SettingsChanged" );

	final ActionEvent RESULT_BUTTON_PUSHED = new ActionEvent( this, 1, "ResultButtonPushed" );

	private final JComboBox comboBox;

	private final JSlider slider;

	private final Set< ActionListener > actionListener = new HashSet< ActionListener >();

	public ProfilePathFindingFrame( final int min, final int max )
	{
		setSize( new Dimension( 300, 300 ) );
		setResizable( false );

		final JPanel panel = new JPanel();
		getContentPane().add( panel, BorderLayout.CENTER );
		panel.setLayout( null );

		final JLabel lblInfo = new JLabel();
		lblInfo.setVerticalAlignment( SwingConstants.TOP );
		lblInfo.setFont( FONT );
		lblInfo.setText( "<html><h2>Pathfinding profile</h2>Using the ImageJ point tool, create two points  on the target image to specify the start and end points." + "</html>" );
		lblInfo.setBounds( 6, 6, 287, 79 );
		panel.add( lblInfo );

		slider = new JSlider( min, max );
		slider.setBounds( 138, 154, 155, 29 );
		slider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent event )
			{
				fireStateChanged( SETTINGS_CHANGED );
			}
		} );
		panel.add( slider );

		comboBox = new JComboBox( ProfilePathFinding.PathType.values() );
		comboBox.setBounds( 138, 115, 155, 27 );
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
		lblPathType.setBounds( 6, 119, 120, 16 );
		panel.add( lblPathType );

		final JLabel lblHeuristicStrength = new JLabel( "Long paths penalty" );
		lblHeuristicStrength.setFont( FONT );
		lblHeuristicStrength.setBounds( 6, 154, 130, 16 );
		panel.add( lblHeuristicStrength );

		final JLabel lblStatus = new JLabel();
		lblStatus.setBounds( 6, 182, 287, 64 );
		panel.add( lblStatus );

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


	ProfilePathFinding.PathType getPathType()
	{
		return ( ProfilePathFinding.PathType ) comboBox.getSelectedItem();
	}

	int getSliderValue() {
		return slider.getValue();
	}

	@Override
	public String toString()
	{
		return "PathType = " + getPathType() + ", slider value = " + getSliderValue();
	}
}