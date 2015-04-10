package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
	private JLabel[] labels = new JLabel[4];
	private JButton doneButton;
	
    private JComboBox<String> areaColor;
    private String[] areaStrings;
    
    private JColorChooser textColor;
    
    private JCheckBox rainbowMode;
    
    private JColorChooser bgColor;
  
    private MainGUI main;
    
    public PreferencesMenu(MainGUI main){
    	super("Preferences");
    	this.main = main;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        
        JPanel panel = new JPanel();
        GridLayout lo = new GridLayout(3,3);
        lo.setVgap(10);
        lo.setHgap(10);
        panel.setLayout(lo);
        
        labels[0] = new JLabel("Text Area Coloring:");
        panel.add(labels[0]);
        
        areaStrings = new String[main.getNumberofColoringArrays()];
        for(int i=0; i<main.getNumberofColoringArrays(); i++) {
        	areaStrings[i] = ColoringColors.getColor(i).toString();
        }
        
        areaColor = new JComboBox<String>(areaStrings);
        areaColor.addActionListener(this);
        panel.add(areaColor);
        
        panel.add(new JPanel());
        
        labels[3] = new JLabel("Text Coloring: ");
        panel.add(labels[3]);
        
        textColor = new JColorChooser(Color.GRAY);
        textColor.getSelectionModel().addChangeListener(this);
        AbstractColorChooserPanel[] panels = { new MyChooserPanel(2) }; 
        textColor.setChooserPanels(panels);
        textColor.setPreviewPanel(new JPanel());
        panel.add(textColor, BorderLayout.CENTER);
        
        JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayout(2,1));
        
        labels[2] = new JLabel("Alternative Rainbow Mode: ");
        panel2.add(labels[2]);
        
        rainbowMode = new JCheckBox();
        rainbowMode.addChangeListener(this);
        rainbowMode.setSelected(main.getAltRBMode());
        panel2.add(rainbowMode);
        panel.add(panel2);
        
        labels[1] = new JLabel("Background Color:");
        panel.add(labels[1]);
        
        bgColor = new JColorChooser(Color.GRAY);
        bgColor.getSelectionModel().addChangeListener(this);
        AbstractColorChooserPanel[] panels2 = { new MyChooserPanel(1) }; 
        bgColor.setChooserPanels(panels2);
        bgColor.setPreviewPanel(new JPanel());
        panel.add(bgColor, BorderLayout.CENTER);
        
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
		if(arg0.getSource().equals(bgColor.getSelectionModel())) {
			main.setBGColor(bgColor.getColor());
		}
		if(arg0.getSource().equals(textColor.getSelectionModel())) {
			main.setChatFGColor(textColor.getColor());
		}
	}
}

@SuppressWarnings("serial")
class MyChooserPanel extends AbstractColorChooserPanel {
	private int panel;
	
	public MyChooserPanel(int panel) {
		this.panel = panel;
	}
	
	public void buildChooser() {
		if(panel == 1) {
			setLayout(new GridLayout(2, 3));
			makeAddButton("Light Gray", Color.LIGHT_GRAY);
			makeAddButton("Light Green", Color.getHSBColor(0.108f, 0.56f, 0.56f));
			makeAddButton("Light Blue", Color.getHSBColor(0.168f, 0.56f, 0.56f));
			makeAddButton("Light Red", Color.getHSBColor(0f, 0.56f, 0.56f));
			makeAddButton("Light Yellow", Color.getHSBColor(0.51f, 0.56f, 0.56f));
			makeAddButton("Light Purple", Color.getHSBColor(0.296f, 0.56f, 0.56f));
		}
		if(panel == 2) {
			setLayout(new GridLayout(2, 3));
			makeAddButton("Black", Color.BLACK);
			makeAddButton("White", Color.WHITE);
			makeAddButton("Red", Color.RED);
			makeAddButton("Green", Color.GREEN);
			makeAddButton("Blue", Color.BLUE);
		}
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
	    button.setPreferredSize(new Dimension(20, 20));
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
