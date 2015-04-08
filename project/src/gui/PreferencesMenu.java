package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;

public class PreferencesMenu extends JFrame implements ActionListener {
    private JComboBox<String> areaColor;
    private String[] areaStrings = {"Normal", "Fifty Shades of Gray", "Rainbow"};
    private String name;
    private MainGUI main;
    
    public PreferencesMenu(String name, MainGUI main){
    	this.main = main;
        this.name = name;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        areaColor = new JComboBox<String>(areaStrings);
        areaColor.addActionListener(this);
        add(areaColor);
    }
    
	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(arg0.getSource().equals(areaColor)) {
			JComboBox<String> jb = (JComboBox<String>)arg0.getSource();
			if(jb.getSelectedItem().equals("Normal")) main.setColoring(ColoringColors.NORMAL);
			else if(jb.getSelectedItem().equals("Fifty Shades of Gray")) main.setColoring(ColoringColors.FIFTY_SHADES);
			else if(jb.getSelectedItem().equals("Rainbow")) main.setColoring(ColoringColors.RAINBOW);
			main.repaintAll();
		}
	}
}
