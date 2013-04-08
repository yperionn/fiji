package fiji.plugin.cwnt.test;

import java.util.Map;

import javax.swing.JFrame;

import fiji.plugin.cwnt.CWNTDetectorProvider;
import fiji.plugin.cwnt.detection.CWNTPanel;

public class Panel_TestDrive {

	public static void main(String[] args) {
		
		CWNTPanel panel = new CWNTPanel(null);
		
		 Map<String, Object> cws = new CWNTDetectorProvider().getDefaultSettings();
		panel.setSettings(cws);
		
		JFrame frame = new JFrame("CWNT panel");
		frame.getContentPane().add(panel);
		frame.setSize(320, 500);
		frame.setVisible(true);
		
		
		
	}
	
}
