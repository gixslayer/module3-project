package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class MainGUI extends JFrame implements ActionListener, KeyListener {
	private Container c;
	
	private JTabbedPane tabPane;
	
	private JTextField typeField;
	
	private DefaultListModel<String> list;
	private JList<String> receiveArea;
	private JScrollPane scrollPane;
	
	private JButton sendButton;
	
	private String clientName;
	
	private Icon closeIcon;
	
	public MainGUI(String name) {
		super("Chat");
		clientName = name;
		init();
	}
	
	public void init() {
		loadIcons();
		
		c = getContentPane();
		c.setLayout(new BorderLayout());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		list = new DefaultListModel<String>();
		
		tabPane = new JTabbedPane();
		
		typeField = new JTextField();
		typeField.addKeyListener(this);
		
		receiveArea = new JList<String>(list);
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
		
		JPanel mainChat = new JPanel();
		mainChat.setLayout(new BorderLayout());
		mainChat.add(scrollPane, BorderLayout.CENTER);
		mainChat.add(sendBar, BorderLayout.SOUTH);
		
		tabPane.addTab("Main Room", mainChat);

		c.add(tabPane, BorderLayout.CENTER);
		
		addPrivateChat("Alice");
		addPrivateChat("Bob");
		addPrivateChat("Charles");
		
		setSize(800,800);
		setVisible(true);
	}
	
	public void loadIcons() {
		closeIcon = new ImageIcon("images/close.png");
	}
	
	public void addPrivateChat(String name) {
		JPanel privChat = new JPanel();
		privChat.setLayout(new BorderLayout());
		privChat.add(new PrivateChat(clientName), BorderLayout.CENTER);
		tabPane.addTab(name, privChat);
		int i = tabPane.indexOfTab(name);
		
		JPanel tabPanel = new JPanel(new GridBagLayout());
		tabPanel.setOpaque(false);
		JLabel tabTitle = new JLabel(name + " ");
		JButton tabClose = new JButton(closeIcon);
		tabClose.setBorderPainted(false);
		tabClose.setBorder(BorderFactory.createEmptyBorder());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;

		tabPanel.add(tabTitle, gbc);

		gbc.gridx++;
		gbc.weightx = 0;
		tabPanel.add(tabClose, gbc);

		tabPane.setTabComponentAt(i, tabPanel);
		tabClose.addActionListener(new TabActionListener(name));
	}
	
	public void addToScreen(String str) {
		list.addElement(str);
		receiveArea.ensureIndexIsVisible(list.getSize() -1);
	}
	
	public void sendText() {
		String txt = typeField.getText();
		txt = txt.replace(":)", "☺");
		txt = txt.replace(":(", "☹");
		txt = txt.replace("*check*", "✔");
		txt = txt.replace("*yinyang*", "☯");
		addToScreen(clientName + ": " + txt);
		typeField.setText("");
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
	
	public static void main(String[] args) {
		new MainGUI("Test");
	}

	public class TabActionListener implements ActionListener {
		private String name;
		
		public TabActionListener(String name) {
			this.name = name;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			tabPane.remove(tabPane.indexOfTab(name));
		}
	}
}
