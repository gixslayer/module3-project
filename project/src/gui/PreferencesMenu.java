package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PreferencesMenu extends JFrame implements ActionListener {
	private JLabel[] labels = new JLabel[2];
	private JButton doneButton;
	
    private JComboBox<String> areaColor;
    private String[] areaStrings = {"Normal", "Fifty Shades of Gray", "Rainbow"};
    
    private JComboBox<String> bgColor;
    private String[] bgStrings = {"Gray", "Red", "White", "Blue"};
    
    private String name;
    private MainGUI main;
    
    public PreferencesMenu(String name, MainGUI main){
    	this.main = main;
        this.name = name;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2,2));
        
        labels[0] = new JLabel("Text Area Coloring: ");
        panel.add(labels[0]);
        
        areaColor = new JComboBox<String>(areaStrings);
        areaColor.addActionListener(this);
        panel.add(areaColor);
        
        labels[1] = new JLabel("Background Color: ");
        panel.add(labels[1]);
        
        bgColor = new JComboBox<String>(bgStrings);
        bgColor.addActionListener(this);
        panel.add(bgColor, BorderLayout.CENTER);
        
        add(panel, BorderLayout.CENTER);
        
        doneButton = new JButton("Done");
        doneButton.addActionListener(this);
        add(doneButton, BorderLayout.SOUTH);
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
		
		if(arg0.getSource().equals(bgColor)) {
			JComboBox<String> jb = (JComboBox<String>)arg0.getSource();
			if(jb.getSelectedItem().equals("Gray")) main.setBGColor(Color.GRAY);
			else if(jb.getSelectedItem().equals("Red")) main.setBGColor(Color.RED);
			else if(jb.getSelectedItem().equals("White")) main.setBGColor(Color.WHITE);
			else if(jb.getSelectedItem().equals("Blue")) main.setBGColor(Color.BLUE);
			main.repaintAll();
		}
		
		if(arg0.getSource().equals(doneButton)) {
			this.dispose();
		}
	}
}
