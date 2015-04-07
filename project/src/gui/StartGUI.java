package gui;

import javax.swing.*;

import project.Main;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

public class StartGUI extends JFrame implements ActionListener, KeyListener {
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
		
		name = new JTextField();
		name.addKeyListener(this);
		
		start = new JButton("START");
		start.addActionListener(this);

		c.add(text, BorderLayout.NORTH);
		c.add(name, BorderLayout.CENTER);
		c.add(start, BorderLayout.EAST);
		
		pack();
		setVisible(true);
	}
	
	public static void main(String[] args) {
		new StartGUI();
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		if(arg0.getSource().equals(name)) {
			if(arg0.getKeyCode() == KeyEvent.VK_ENTER) {
				new MainGUI(name.getText());
				this.dispose();
			}
		}	
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(arg0.getSource().equals(start)) {
			new MainGUI(name.getText());
			this.dispose();
		}
	}
}
