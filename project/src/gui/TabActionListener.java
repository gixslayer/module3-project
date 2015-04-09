package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TabActionListener implements ActionListener {
	private int index;
	private MainGUI main;
	
	public TabActionListener(int index, MainGUI main) {
		this.index = index;
		this.main = main;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		main.getTabPane().remove(index);
	}
}
