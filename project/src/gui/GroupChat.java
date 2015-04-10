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
public class GroupChat extends JPanel implements ActionListener, KeyListener, Chat {
	//private Container c;

	private JTextField typeField;
	
	private DefaultListModel<ChatLine> list;
	private JList<ChatLine> receiveArea;
	private JScrollPane scrollPane;
	
	private JButton sendButton;
	
	private Client localClient;
	private Group group;
	private MainGUI main;
	private Application app;
	private AnimationThread animation;
	
	public GroupChat(Client localClient, Group group, MainGUI main, Application app, AnimationThread animation) {
		this.localClient = localClient;
		this.group = group;
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
		
		app.onSendGroupMessage(group.getName(), txt);
	}
	
	public void receiveText(String str, Client client) {
		addToScreen(client, str);
	}
	
	public boolean isPrivate() {
		return false;
	}
	
	public Group getGroup() {
		return group;
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