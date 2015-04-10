package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class PreferencesMenu extends JFrame implements ActionListener, ChangeListener {
	private JLabel[] labels = new JLabel[3];
	private JButton doneButton;
	
    private JComboBox<String> areaColor;
    private String[] areaStrings;
    		
    private JCheckBox rainbowMode;
    
    private JColorChooser bgColor;
  
    private MainGUI main;
    
    public PreferencesMenu(MainGUI main){
    	this.main = main;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2,3));
        
        labels[0] = new JLabel("Text Area Coloring: ");
        panel.add(labels[0]);
        
        areaStrings = new String[main.getNumberofColoringArrays()];
        for(int i=0; i<main.getNumberofColoringArrays(); i++) {
        	areaStrings[i] = ColoringColors.getColor(i).toString();
        }
        
        areaColor = new JComboBox<String>(areaStrings);
        areaColor.addActionListener(this);
        panel.add(areaColor);
        
        labels[2] = new JLabel("Alternative Rainbow Mode:");
        panel.add(labels[2]);
        
        labels[1] = new JLabel("Background Color: ");
        panel.add(labels[1]);
        
        bgColor = new JColorChooser(Color.GRAY);
        bgColor.getSelectionModel().addChangeListener(this);
        AbstractColorChooserPanel[] panels = { new MyChooserPanel() }; 
        bgColor.setChooserPanels(panels);
        bgColor.setPreviewPanel(new JPanel());
        panel.add(bgColor, BorderLayout.CENTER);
        
        rainbowMode = new JCheckBox();
        rainbowMode.addChangeListener(this);
        rainbowMode.setSelected(main.getAltRBMode());
        panel.add(rainbowMode);
        
        add(panel, BorderLayout.CENTER);
        
        doneButton = new JButton("Done");
        doneButton.addActionListener(this);
        add(doneButton, BorderLayout.SOUTH);
    }
    
	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(arg0.getSource().equals(areaColor)) {
			@SuppressWarnings("unchecked") // TODO: Verify this cast cannot blow up on us.
			JComboBox<String> jb = (JComboBox<String>)arg0.getSource();
			for(int i=0; i<main.getNumberofColoringArrays(); i++) {
				if(jb.getSelectedItem().equals(ColoringColors.getColor(i).toString())) main.setColoring(ColoringColors.getColor(i));
			}
			main.repaintAll();
		}
		
		if(arg0.getSource().equals(doneButton)) {
			this.dispose();
		}
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		if(arg0.getSource().equals(rainbowMode)) {
			main.setAltRainBowMode(rainbowMode.isSelected());
		}
		main.setBGColor(bgColor.getColor());
	}
}

@SuppressWarnings("serial")
class MyChooserPanel extends AbstractColorChooserPanel {
	public void buildChooser() {
		setLayout(new GridLayout(2, 3));
	    makeAddButton("Light Gray", Color.LIGHT_GRAY);
	    makeAddButton("Light Green", Color.getColor("0x69CE50"));
	    makeAddButton("Light Blue", Color.getHSBColor(168, 56, 56));
	    makeAddButton("Light Red", Color.getHSBColor(0, 56, 56));
	    makeAddButton("Light Yellow", Color.getHSBColor(51, 56, 56));
	    makeAddButton("Light Purple", Color.getHSBColor(296, 56, 56));
	}

	public void updateChooser() { }

	public String getDisplayName() {
		return "MyChooserPanel";
	}

	public Icon getSmallDisplayIcon() {
		return null;
	}
	
	public Icon getLargeDisplayIcon() {
		return null;
	}
	
	private void makeAddButton(String name, Color color) {
	    JButton button = new JButton(name);
	    button.setBackground(color);
	    button.setAction(setColorAction);
	    add(button);
	}
	
	Action setColorAction = new AbstractAction() {
	    public void actionPerformed(ActionEvent evt) {
	    	JButton button = (JButton) evt.getSource();
	    	getColorSelectionModel().setSelectedColor(button.getBackground());
	    }
	};
}
