package gui;

import javax.swing.*;

import backend.Backend;
import client.Client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

@SuppressWarnings("serial")
public class GroupChat extends JPanel implements ActionListener, KeyListener, Chat {
	private JTextField typeField;
	
	private DefaultListModel<ChatLine> list;
	private JList<ChatLine> receiveArea;
	private JScrollPane scrollPane;
	
	private String[] history = new String[0];
	private int currentHistory;
	
	private JButton sendButton;
	
	private Client localClient;
	private Group group;
	private MainGUI main;
	private Backend app;
	private AnimationThread animation;
	
	public GroupChat(Client localClient, Group group, MainGUI main, Backend app, AnimationThread animation) {
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
		list.addElement(new TextLine(client, str));
		receiveArea.ensureIndexIsVisible(list.getSize() -1);
		if(list.getSize() > MainGUI.LIST_MAX_SIZE) {
			list.removeElement(list.firstElement());
		}
	}
	
	public void sendText() {
		String txt = typeField.getText();
		if(main.IllegalCharCheck(txt)) return;
		addToScreen(localClient, txt);
		typeField.setText("");
		addToHistory(txt);
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
	
	private void addToHistory(String str) {
		String[] tmp = new String[0];
		if(history.length+1 <= main.HISTORY_MAX_SIZE) {
			tmp = new String[history.length+1];
		}
		else {
			tmp = new String[history.length];
		}
		tmp[0] = str;
		if(history.length+1 <= main.HISTORY_MAX_SIZE) System.arraycopy(history, 0, tmp, 1, history.length);
		else System.arraycopy(history, 0, tmp, 1, history.length-1);
		history = tmp;
	}
	
	private String getFromHistory(int index) {
		if(index >= history.length) {
			currentHistory = history.length-1;
			return history[history.length-1];
		}
		if(index < 0) {
			currentHistory = 0;
			return history[0];
		}
		else return history[index];
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
			if(arg0.getKeyCode() == KeyEvent.VK_ENTER) {
				sendText();
				currentHistory = 0;
			}
			if(arg0.getKeyCode() == KeyEvent.VK_UP) {
				typeField.setText(getFromHistory(currentHistory));
				currentHistory++;
			}
			if(arg0.getKeyCode() == KeyEvent.VK_DOWN) {
				currentHistory--;
				typeField.setText(getFromHistory(currentHistory));
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
	}
}
