package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;

public class PreferencesMenu extends JFrame implements ActionListener {
    private JButton areaColor;
    private String name;
    private MainGUI main;
    
    public PreferencesMenu(String name, MainGUI main){
    	this.main = main;
        this.name = name;
        areaColor = new JButton("Change Area Color");
        areaColor.addActionListener(this);
        add(areaColor);
    }
    
	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(arg0.getSource().equals(areaColor)) {
			main.setFiftyEnabled(!main.getFiftyEnabled());
			main.repaintAll();
		}
	}
}
