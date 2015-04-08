package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TabActionListener implements ActionListener {
	private String name;
	private MainGUI main;
	
	public TabActionListener(String name, MainGUI main) {
		this.name = name;
		this.main = main;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		main.getTabPane().remove(main.getTabPane().indexOfTab(name));
	}
}
