package gui;

import javax.swing.*;

import project.Main;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class StartGUI extends JFrame {
	private Container c;
	public JTextField name;
	private JButton start;
	private JLabel text;
	
	public StartGUI() {
		super("Login");
		init();
	}
	
	private void init() {
		c = getContentPane();
		c.setLayout(new BorderLayout());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		text = new JLabel("Please enter your username.");
		c.add(text, BorderLayout.NORTH);
		
		name = new JTextField();
		c.add(name, BorderLayout.CENTER);
		
		start = new JButton("START");
		start.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					new Main(name.getText());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		c.add(start, BorderLayout.EAST);
		
		pack();
		setVisible(true);
	}
	
	public static void main(String[] args) {
		new StartGUI();
	}
}
