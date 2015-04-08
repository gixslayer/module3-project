package gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class CustomCellRenderer extends JLabel implements ListCellRenderer<Object> {
	private MainGUI main;
	
    public CustomCellRenderer(MainGUI main) {
    	this.main = main;
        setOpaque(true);
    }

    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        setText(value.toString());
        Color background = Color.WHITE;
        Color foreground = Color.BLACK;
        if(main.getColoring() == ColoringColors.FIFTY_SHADES) {
        	background = Color.decode("0x" + main.getFiftyShade(index % 50));
        }
        else if(main.getColoring() == ColoringColors.RAINBOW) {
        	background = Color.decode("0x" + main.getRainbow(index % 15));	
        }
        else {
        	if(index % 2 == 0)
        		background = Color.decode("0x" + main.getFiftyShade(10));
       	 	else
       	 		background = Color.decode("0x" + main.getFiftyShade(20));
        }
        
        if(value.toString().startsWith("[JOIN]:")) 
       	 	setText("<html><font color=blue>[JOIN]:</font>" + value.toString().split(":")[1] + "</html>");
        
        else if(value.toString().startsWith("[LEAVE]:")) 
       	 	setText("<html><font color=red>" + value.toString().split(":")[0] + "</font>:" + value.toString().split(":")[1] + "</html>");
        else {
       	 	setText("<html><font color="+ main.getUserColor(value.toString().split(":")[0]) +">" + value.toString().split(":")[0] + "</font>:" + value.toString().split(":")[1] + "</html>");
        }
       	 
        setBackground(background);
        setForeground(foreground);

        return this;
    }
}
