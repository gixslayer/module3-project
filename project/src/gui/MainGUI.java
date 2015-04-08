package gui;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;

public class MainGUI extends JFrame implements ActionListener, KeyListener, MouseListener {
	private Container c;
	
	private static final Color BGCOLOR = Color.GRAY;
	
	private static final String[] colors = {"Red", "Blue", "Green", "Yellow", "Purple", "Orange", "Black"};
	public static final String[] fiftyShades = {"E0E0E0", "DEDEDE", "DBDBDB", "D9D9D9", "D6D6D6", "D4D4D4", "D1D1D1", "CFCFCF",
		"CCCCCC", "C9C9C9", "C7C7C7", "C4C4C4", "C2C2C2", "BFBFBF", "BDBDBD", "BABABA", "B8B8B8", "B5B5B5", "B3B3B3", "B0B0B0",
		"ADADAD", "ABABAB", "A9A9A9", "A8A8A8", "A6A6A6", "A3A3A3", "A1A1A1", "9E9E9E", "9C9C9C", "969696", "949494", "919191",
		"8F8F8F", "8C8C8C", "8A8A8A", "878787", "858585", "828282", "7F7F7F", "7D7D7D", "7A7A7A", "787878", "757575", "737373",
		"707070", "6E6E6E", "6B6B6B", "696969", "666666", "636363", "616161"};
	
	private HashMap<String, String> colorMap = new HashMap<String, String>();
	public boolean fiftyEnabled = false;
	
	private HashMap<Integer, PrivateChat> chatMap = new HashMap<Integer, PrivateChat>();
	
	private JMenuBar menu;
	private JMenu optionMenu;
	private JMenuItem preferencesItem;
	
	private JTabbedPane tabPane;
	private JTextField typeField;
	
	private DefaultListModel<String> peopleList;
	private JList<String> peopleArea;
	private JScrollPane peopleScrollPane;
	
	private DefaultListModel<String> list;
	private JList<String> receiveArea;
	private JScrollPane scrollPane;
	
	private JButton sendButton;
	private String clientName;
	private Icon closeIcon;
	
	/**
	 * Constuctor of the <code>class</code>.
	 * @param name
	 */
	public MainGUI(String name) {
		super("Chat");
		clientName = name;
		init();
	}
	
	/**
	 * Inits the GUI and sets everything up. Creates the window and connects the User.
	 */
	public void init() {
		loadIcons();
		
		c = getContentPane();
		c.setLayout(new BorderLayout());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		peopleList = new DefaultListModel<String>();
		list = new DefaultListModel<String>();
		
		tabPane = new JTabbedPane();
		c.setBackground(BGCOLOR);
		
		typeField = new JTextField();
		typeField.addKeyListener(this);
		
		peopleArea = new JList<String>(peopleList);
		peopleArea.setForeground(Color.WHITE);
		peopleScrollPane = new JScrollPane(peopleArea);
		
		receiveArea = new JList<String>(list);
		receiveArea.setCellRenderer(new CustomCellRenderer(this));
		scrollPane = new JScrollPane(receiveArea);
	
		sendButton = new JButton("Send");
		sendButton.addActionListener(this);
		
		JPanel sendBar = new JPanel();
		sendBar.setLayout(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.BOTH;
		
		sendBar.add(typeField, gbc);
		sendBar.add(sendButton);
		
		JLabel userLabel = new JLabel("Users");
		
		JPanel sideBar = new JPanel();
		sideBar.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipadx = 30;
		
		sideBar.add(peopleScrollPane, gbc);
		peopleArea.setBackground(BGCOLOR);
		
		gbc = new GridBagConstraints();
		sideBar.add(userLabel);
		
		JPanel mainChat = new JPanel();
		mainChat.setLayout(new BorderLayout());
		mainChat.add(scrollPane, BorderLayout.CENTER);
		mainChat.add(sendBar, BorderLayout.SOUTH);
		
		tabPane.addTab("Main Room", mainChat);

		c.add(tabPane, BorderLayout.CENTER);
		c.add(sideBar, BorderLayout.WEST);
		
		addUser(clientName);
		addUser("Alice");
		addUser("Bob");
		
		menu = new JMenuBar();
		
		optionMenu = new JMenu("Options");
		
		preferencesItem = new JMenuItem("Preferences");
		preferencesItem.addActionListener(this);
		
		optionMenu.add(preferencesItem);
		menu.add(optionMenu);
		
		setJMenuBar(menu);
		
		setSize(800,800);
		setVisible(true);
	}
	
//=============================================================================
//=============================== SHORTCUTS ===================================
//=============================================================================
	
	/**
	 * Removes a User from the Chat Room.
	 * @param name the name of the User.
	 */
	public void removeUser(String name) {
		peopleList.removeElement(name);
		addToScreen("[LEAVE]: User " + name + " has left the chat room.");
	}
	
	/**
	 * Adds a User to the Chat Room.
	 * @param name the name of the User.
	 */
	public void addUser(String name) {
		peopleList.addElement(name);
		peopleArea.addMouseListener(this);
		peopleArea.ensureIndexIsVisible(peopleList.getSize()-1);
		setUserColor(name, colors[(int)(Math.random()*7)]);
		addToScreen("[JOIN]: User <font color=" + getUserColor(name) + ">" + name + "</font> entered the chat room.");
	}
	
	/**
	 * Loads the icons of the GUI.
	 */
	public void loadIcons() {
		closeIcon = new ImageIcon("images/close.png");
	}
	
	/**
	 * Opens a new Private Chat to the specified User.
	 * @param name the name of the User to chat with.
	 */
	public void addPrivateChat(String name) {
		if(tabPane.indexOfTab(name) != -1) {
			tabPane.setSelectedIndex(tabPane.indexOfTab(name));
			return;
		}
		if(name.equals(clientName)) return;
		JPanel privChat = new JPanel();
		privChat.setLayout(new BorderLayout());
		PrivateChat pChat = new PrivateChat(clientName, this);
		privChat.add(pChat, BorderLayout.CENTER);
		
		tabPane.addTab(name, privChat);
		int i = tabPane.indexOfTab(name);
		
		chatMap.put(i, pChat);
		
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
		tabClose.addActionListener(new TabActionListener(name, this));
		
		tabPane.setSelectedIndex(i);
	}
	
	/**
	 * Adds a line of text to the text area.
	 * @param str the line of text to add.
	 */
	public void addToScreen(String str) {
		str = str.replace(":)", "☺");
		str = str.replace(":(", "☹");
		str = str.replace("*check*", "✔");
		str = str.replace("*yinyang*", "☯");
		list.addElement(str);
		receiveArea.ensureIndexIsVisible(list.getSize() -1);
	}
	
	/**
	 * Called when the User wants to send a line of text.
	 */
	public void sendText() {
		String txt = typeField.getText();
		if(txt.length() == 0) return;
		addToScreen(clientName + ": " + txt);
		typeField.setText("");
	}
	
	/**
	 * Called when the user receives some text, private or public.
	 * @param str the received text.
	 * @param name the name of the Sender.
	 * @param priv true if it is part of the private chat between the two Users | false if it is part of the global chat.
	 */
	public void receiveText(String str, String name, boolean priv) {
		if(priv) {
			int index = tabPane.indexOfTab(name);
			PrivateChat pChat = chatMap.get(index);
			pChat.receiveText(str, name);
		}
		else {
			addToScreen(name + ": " + str);
		}
	}
	
//=============================================================================
//========================== GETTERS AND SETTERS ==============================
//=============================================================================
	
	/**
	 * Gets the specified User's color.
	 * @param name the name of the User.
	 * @return the color of the specified User.
	 */
	public String getUserColor(String name) {
		if(colorMap.containsKey(name)) {
			return colorMap.get(name);
		}
		return "Black";
	}
	
	/**
	 * Sets the specified User's color.
	 * @param name the name of the User.
	 * @param color The color to set the User to.
	 */
	public void setUserColor(String name, String color) {
		colorMap.put(name, color);
		receiveArea.repaint();
	}
	
	/**
	 * Set the <code>boolean</code> which decides which coloring scheme is used.
	 * @param enabled true if alternative coloring needs to be used | false if the standard coloring needs to be used.
	 */
	public void setFiftyEnabled(boolean enabled) {
		this.fiftyEnabled = enabled;
	}
	
	/**
	 * Gets the component which holds the tabbed pages.
	 * @return the component which holds the tabbed pages.
	 */
	public JTabbedPane getTabPane() {
		return tabPane;
	}
	
	/**
	 * Gets the value of the <code>boolean</code> which decides which coloring scheme is used.
	 * @return the value of the <code>boolean</code> which decides which coloring scheme is used.
	 */
	public boolean getFiftyEnabled() {
		return fiftyEnabled;
	}
	
	/**
	 * Gets a specified Color from the pool of colors Users can be.
	 * @param index the index of the Color.
	 * @return the color mapped to the specified index.
	 */
	public String getColor(int index) {
		return colors[index];
	}
	
	/**
	 * Get the specified Color from the pool of colors the background can be.
	 * @param index the index of the Color.
	 * @return the color mapped to the specified index.
	 */
	public String getFiftyShade(int index) {
		return fiftyShades[index];
	}
	
	/**
	 * Repaints all text areas.
	 */
	public void repaintAll() {
		receiveArea.repaint();
		for(int i=0; i<tabPane.getTabCount(); i++) {
			tabPane.repaint();
		}
	}
	
//=============================================================================
//============================= EVENT HANDLERS ================================
//=============================================================================

	@Override
	/**
	 * Called when the User clicks either the Send Button or the Preferences Menu Item.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource().equals(sendButton)) {
			sendText();
		}
		
		if(e.getSource().equals(preferencesItem)) {
			PreferencesMenu menu = new PreferencesMenu(clientName, this);
			menu.pack();
			menu.setVisible(true);
		}
	}

	@Override
	/**
	 * Called when the User presses Enter when typing in the send field.
	 */
	public void keyPressed(KeyEvent arg0) {
		if(arg0.getSource().equals(typeField)) {
			if(arg0.getKeyCode() == KeyEvent.VK_ENTER) 
				sendText();
		}
	}

	@Override
	/**
	 * Unused
	 */
	public void keyReleased(KeyEvent arg0) { }

	@Override
	/**
	 * Unused
	 */
	public void keyTyped(KeyEvent arg0) { }
	
	@Override
	/**
	 * Gets called when the User clicks on the Online User List.
	 */
	public void mouseClicked(MouseEvent arg0) { 
		if(arg0.getSource().equals(peopleArea) && arg0.getButton() == MouseEvent.BUTTON3) {
			String name = peopleArea.getSelectedValue();
			if(name == null) return;
			
			UserMenu menu = new UserMenu(name, clientName, this);
		    menu.show(arg0.getComponent(), arg0.getX(), arg0.getY());
		}
	}

	@Override
	/**
	 * Unused
	 */
	public void mouseEntered(MouseEvent arg0) { }

	@Override
	/**
	 * Unused
	 */
	public void mouseExited(MouseEvent arg0) { }

	@Override
	/**
	 * Unused
	 */
	public void mousePressed(MouseEvent arg0) { }

	@Override
	/**
	 * Unused
	 */
	public void mouseReleased(MouseEvent arg0) { }
	
	/**
	 * Starts a test instance of the <code>class</code>.
	 * @param args unused
	 */
	public static void main(String[] args) {
		new MainGUI("Test");
	}
}
