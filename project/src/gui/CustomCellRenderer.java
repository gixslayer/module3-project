package gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import client.Client;

@SuppressWarnings("serial")
public class CustomCellRenderer extends JLabel implements ListCellRenderer<Object> {
	private MainGUI main;
	private AnimationThread animation;
	private boolean reverse = false;
	
    public CustomCellRenderer(MainGUI main, AnimationThread animation) {
    	this.main = main;
    	this.animation = animation;
        setOpaque(true);
    }

    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        ChatLine cLine = (ChatLine)value;
        Client client = cLine.getClient();
        String line = cLine.getLine();
        String time = "";
        if(main.isTimestampEnabled()) {
        	time = cLine.getTime();
        }
        
        Color background = Color.WHITE;
        Color foreground = main.getChatFGColor();
        
        if(main.getColoring() == ColoringColors.RAINBOW) {
        	background = Color.getHSBColor((float)(index*0.01), 1, 1);	
        }
        else if(main.getColoring() == ColoringColors.ANIMATED_RAINBOW) {
        	if(!main.getAltRBMode()) 
        		background = Color.getHSBColor(animation.getHue(), 1, 1);
        	else 
        		background = Color.getHSBColor((float)((index+(animation.getHue()*100))*0.01), 1, 1);
        }
        else if(main.getColoring() != ColoringColors.NORMAL) {
        	int length = main.getLenghtOfColoringArray(main.getColoring());
        	if(index  % length == length-1) {
        		reverse = true;
        	}
        	if(reverse) {
        		if(length - (index % length) == 0)
        			reverse = false;
        		if(index % (2*length) >= length) {
        			if(main.getColoring() == ColoringColors.CUSTOM) {
        				background = main.getTotalGradient((length-1) - (index % length));
        			}
        			else background = Color.decode("0x" + main.getColorFromArray(main.getColoring(), (length-1) - (index % length)));
        		} else {
        			if(main.getColoring() == ColoringColors.CUSTOM) {
        				background = main.getTotalGradient(index % length);
        			}
        			else background = Color.decode("0x" + main.getColorFromArray(main.getColoring(), index % length));
        		}
        	}
        	else {
        		if(main.getColoring() == ColoringColors.CUSTOM) {
    				background = main.getTotalGradient(index % length);
    			}
        		else background = Color.decode("0x" + main.getColorFromArray(main.getColoring(), index % length));
        	}
        }
        else {
        	if(index % 2 == 0)
        		background = Color.decode("0xC7C7C7");
       	 	else
       	 		background = Color.decode("0xADADAD");
        }
       
        if(main.isTimestampEnabled()) setText("<html>[" + time + "] <font color=" + main.getUserColor(client) + ">"+ client.getName() + "</font>: " + line);
        else setText("<html><font color=" + main.getUserColor(client) + ">"+ client.getName() + "</font>: " + line);
       	 
        setBackground(background);
        setForeground(foreground);
        main.repaintAll();

        return this;
    }
}
