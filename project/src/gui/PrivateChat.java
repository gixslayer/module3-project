package gui;

import javax.swing.*;

import client.Client;
import application.Application;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

@SuppressWarnings("serial")
public class PrivateChat extends JPanel implements ActionListener, KeyListener {
	//private Container c;

	private JTextField typeField;
	
	private DefaultListModel<ChatLine> list;
	private JList<ChatLine> receiveArea;
	private JScrollPane scrollPane;
	
	private JButton sendButton;
	
	private Client localClient;
	private Client otherClient;
	private MainGUI main;
	private Application app;
	private AnimationThread animation;
	
	public PrivateChat(Client localClient, Client otherClient, MainGUI main, Application app, AnimationThread animation) {
		this.localClient = localClient;
		this.otherClient = otherClient;
		this.main = main;
		this.app = app;
		this.animation = animation;
		init();
	}
	
	public void init() {
		setLayout(new BorderLayout());
		
		list = new DefaultListModel<ChatLine>();
		
		typeField = new JTextField();
		typeField.addKeyListener(this);
		
		receiveArea = new JList<ChatLine>(list);
		receiveArea.setCellRenderer(new CustomCellRenderer(main, animation));
		scrollPane = new JScrollPane(receiveArea);
	
		sendButton = new JButton("Send");
		sendButton.addActionListener(this);
		
		JPanel sendBar = new JPanel();
		sendBar.setLayout(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.BOTH;
		
		sendBar.add(typeField, gbc);
		sendBar.add(sendButton);
		
		add(scrollPane, BorderLayout.CENTER);
		add(sendBar, BorderLayout.SOUTH);
	}
	
	public void addToScreen(Client client, String str) {
		list.addElement(new ChatLine(client, str));
		receiveArea.ensureIndexIsVisible(list.getSize() -1);
		if(list.getSize() > MainGUI.LIST_MAX_SIZE) {
			list.removeElement(list.firstElement());
		}
	}
	
	public void sendText() {
		String txt = typeField.getText();
		if(txt.length() == 0 || txt.matches("\\s*") || txt.length() > 3000) return;
		addToScreen(localClient, txt);
		typeField.setText("");
		
		app.onSendPrivateMessage(otherClient, txt);
	}
	
	public void receiveText(String str, Client client) {
		addToScreen(client, str);
	}
	
	public Client getOtherClient() {
		return otherClient;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource().equals(sendButton)) {
			sendText();
		}
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		if(arg0.getSource().equals(typeField)) {
			if(arg0.getKeyCode() == KeyEvent.VK_ENTER) 
				sendText();
		}
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
	}
}
