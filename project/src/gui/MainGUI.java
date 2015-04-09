package gui;

import javax.swing.*;

import client.Client;
import application.Application;
import application.ApplicationCallbacks;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;

@SuppressWarnings("serial")
public class MainGUI extends JFrame implements ActionListener, KeyListener, MouseListener, WindowListener, ApplicationCallbacks {
	private Container c;
	
	private static final Color BGCOLOR = Color.LIGHT_GRAY;
	
	public static final int LIST_MAX_SIZE = 300;
	
	private static final String[] colors = {"Red", "Blue", "Green", "Yellow", "Purple", "Orange", "Black"};
	private static final String[] fiftyShades = {"E0E0E0", "DEDEDE", "DBDBDB", "D9D9D9", "D6D6D6", "D4D4D4", "D1D1D1", "CFCFCF",
		"CCCCCC", "C9C9C9", "C7C7C7", "C4C4C4", "C2C2C2", "BFBFBF", "BDBDBD", "BABABA", "B8B8B8", "B5B5B5", "B3B3B3", "B0B0B0",
		"ADADAD", "ABABAB", "A9A9A9", "A8A8A8", "A6A6A6", "A3A3A3", "A1A1A1", "9E9E9E", "9C9C9C", "969696", "949494", "919191",
		"8F8F8F", "8C8C8C", "8A8A8A", "878787", "858585", "828282", "7F7F7F", "7D7D7D", "7A7A7A", "787878", "757575", "737373",
		"707070", "6E6E6E", "6B6B6B", "696969", "666666", "636363", "616161"};
	private static final String[] rainbow = {"F26C4F", "F68E55", "FBAF5C", "FFF467", "ACD372", "7CC576", "3BB878", "1ABBB4", "00BFF3",
		"438CCA", "5574B9", "605CA8", "855FA8", "A763A8", "F06EA9", "F26D7D"};
	
	private HashMap<String, String> colorMap = new HashMap<String, String>();
	private ColoringColors coloring = ColoringColors.NORMAL;
	
	private String[] history = new String[0];
	
	private volatile boolean rainbowMode = false;
	private boolean altRBMode = false;
	
	private HashMap<Integer, PrivateChat> chatMap = new HashMap<Integer, PrivateChat>();
	
	private JMenuBar menu;
	private JMenu optionMenu;
	private JMenuItem preferencesItem;
	private JMenuItem rainbowModeItem;
	private JMenu otherMenu;
	private JMenuItem fileItem;
	
	private JTabbedPane tabPane;
	private JTextField typeField;
	
	private DefaultListModel<String> peopleList;
	private JList<String> peopleArea;
	private JScrollPane peopleScrollPane;
	
	private DefaultListModel<String> list;
	private JList<String> receiveArea;
	private JScrollPane scrollPane;
	
	private JButton sendButton;
	
	private Client client;
	private Icon closeIcon;
	
	private Application app;
	private Alice alice;
	
	private Thread rbThread;
	private AnimationThread animation;
	
	private int currentHistory;
	
	/**
	 * Constuctor of the <code>class</code>.
	 * @param name
	 */
	public MainGUI(String name) {
		super("Chat");
		app = new Application(name, this);
		app.start();
		client = app.getLocalClient();
		init();
	}
	
	/**
	 * Inits the GUI and sets everything up. Creates the window and connects the User.
	 */
	public void init() {
		loadResources();
		
		animation = new AnimationThread();
		animation.setCont(true);
		animation.start();
		
		addWindowListener(this);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		c = getContentPane();
		c.setLayout(new BorderLayout());
		
		peopleList = new DefaultListModel<String>();
		list = new DefaultListModel<String>();
		
		tabPane = new JTabbedPane();
		
		typeField = new JTextField();
		typeField.addKeyListener(this);
		
		peopleArea = new JList<String>(peopleList);
		peopleArea.setForeground(Color.BLACK);
		peopleScrollPane = new JScrollPane(peopleArea);
		
		receiveArea = new JList<String>(list);
		receiveArea.setCellRenderer(new CustomCellRenderer(this, animation));
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
		
		gbc = new GridBagConstraints();
		sideBar.add(userLabel);
		
		JPanel mainChat = new JPanel();
		mainChat.setLayout(new BorderLayout());
		mainChat.add(scrollPane, BorderLayout.CENTER);
		mainChat.add(sendBar, BorderLayout.SOUTH);
		
		tabPane.addTab("Main Room", mainChat);

		c.add(tabPane, BorderLayout.CENTER);
		c.add(sideBar, BorderLayout.WEST);
		
		addUser(client.getName());
		addUser("Alice");
		alice = new Alice(this, "Alice");
		
		menu = new JMenuBar();
		
		optionMenu = new JMenu("Options");
		
		preferencesItem = new JMenuItem("Preferences");
		preferencesItem.addActionListener(this);
		
		rainbowModeItem = new JMenuItem("Rainbow Mode");
		rainbowModeItem.addActionListener(this);
		
		otherMenu = new JMenu("Other");
		
		fileItem = new JMenuItem("Open file...");
		fileItem.addActionListener(this);
		
		optionMenu.add(preferencesItem);
		optionMenu.add(rainbowModeItem);
		menu.add(optionMenu);
		otherMenu.add(fileItem);
		menu.add(otherMenu);
		
		setJMenuBar(menu);
		
		setBGColor(BGCOLOR);
		
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
	 * Loads the resources of the GUI.
	 */
	public void loadResources() {
		closeIcon = new ImageIcon("res/close.png");
	}
	
	/**
	 * Opens a new Private Chat to the specified User.
	 * @param name the name of the User to chat with.
	 */
	public void addPrivateChat(String name, boolean self) {
		if(tabPane.indexOfTab(name) != -1) {
			tabPane.setSelectedIndex(tabPane.indexOfTab(name));
			return;
		}
		if(name.equals(client.getName())) return;
		JPanel privChat = new JPanel();
		privChat.setLayout(new BorderLayout());
		PrivateChat pChat = new PrivateChat(client, name, this, app, alice, animation);
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
		
		if(self) tabPane.setSelectedIndex(i);
	}
	
	/**
	 * Adds a line of text to the text area.
	 * @param str the line of text to add.
	 */
	public void addToScreen(String str) {
		list.addElement(str);
		receiveArea.ensureIndexIsVisible(list.getSize() -1);
		if(list.getSize() > LIST_MAX_SIZE) {
			list.removeElement(list.firstElement());
		}
	}
	
	/**
	 * Called when the User wants to send a line of text.
	 */
	public void sendText() {
		String txt = typeField.getText();
		if(txt.toLowerCase().length() == 0 || txt.toLowerCase().matches("\\s*") || txt.toLowerCase().matches(".*<.*>.*") || txt.toLowerCase().matches(".*<script.*") || txt.length() > 3000) return;
		typeField.setText("");
		addToHistory(txt);
		receiveText(txt, client.getName(), false);
		if(txt.contains("Alice") || txt.contains("alice")) {
			receiveText(alice.getResponse(txt), "Alice", false);
			return;
		}
		app.onSendMessage(txt);
	}
	
	public void sendMultiple(String[] strs) {
		for(int i=0; i<strs.length; i++) {
			typeField.setText(strs[i]);
			sendText();
		}
	}
	
	/**
	 * Called when the user receives some text, private or public.
	 * @param str the received text.
	 * @param name the name of the Sender.
	 * @param priv true if it is part of the private chat between the two Users | false if it is part of the global chat.
	 */
	public void receiveText(String str, String name, boolean priv) {
		// Note: The Windows Eclipse client seems to struggle saving/displaying the file under the default settings.
		// This is due to the encoding used (Cp1252 on Windows, UTF-8 on Linux).
		// The GUI itself also doesn't display the characters correctly and they end up as garbage.
		// I fixed the editor issue by forcing Java source files to use UTF-8 on Windows (Preferences -> General -> Content Types
		// -> expand Text -> Select Java source file -> Set Default encoding to UTF-8).
		// Doing this also seems to run the program with UTF-8 encoding (for whatever reason), but if it doesn't Windows clients
		// will have to run the program with the '-Dfile.encoding=UTF-8' Java VM argument to possibly set the correct encoding.
		// This is a giant hack though as the Java VM doesn't have to use the specified encoding. Windows clients might just be SOL.
		str = str.replace(":)", "☺");
		str = str.replace(":(", "☹");
		str = str.replace("*check*", "✔");
		str = str.replace("*yinyang*", "☯");
		str = str.replace("*down*", "↓");
		str = str.replace("*left*", "←");
		str = str.replace("*right*", "→");
		str = str.replace("*up*", "↑");
		str = str.replace("*phone*", "☎");
		str = str.replace("*skull*", "☠");
		str = str.replace("*radio*", "☢");
		str = str.replace("*bio*", "☣");
		str = str.replace("*peace*", "☮");
		str = str.replace("*spade*", "♠");
		str = str.replace("*heart*", "♥");
		str = str.replace("*diamond*", "♦");
		str = str.replace("*club*", "♣");
		str = str.replace("*plane*", "✈");
		str = str.replace("*x*", "✖");
		str = str.replace("1/2", "½");
		str = str.replace("1/4", "¼");
		str = str.replace("*R*", "ℜ");
		str = str.replace("*N*", "ℵ");
		if(str.equals("*music*")) {
			if(name.equals(client.getName())) {
				String[] tmp = {"╔══╗ ♫", "║██║ ♪♪", "║██║♫♪", "║ ◎♫♪♫", "╚══╝"};
				sendMultiple(tmp);
			}
			return;
		}
		if(str.equals("*fatbunny*")) {
			if(name.equals(client.getName())) {
				String[] tmp = {"(\\____/)", "(='.'=)", "(\")__(\")"};
				sendMultiple(tmp);
			}
			return;
		}
		if(str.startsWith("*wavename*")) {
			if(name.equals(client.getName())) {
				String[] split = str.split("\\*");
				String[] tmp = {"¯¨'*·~-.¸¸,.-~*' " + split[2] + " ¯¨'*·~-.¸¸,.-~*'"};
				sendMultiple(tmp);
			}
			return;
		}
		// Note: Disabled fancy names as it would break a dirty hack I did.
		// Find a way to properly interact with the back-end through Client instances
		// directly, rather than trying to represent Client instances in a name string
		// which is directly displayed on the GUI/used in GUI logic.
		name = name.replace(":)", "☺");
		name = name.replace(":(", "☹");
		name = name.replace("*check*", "✔");
		name = name.replace("*yinyang*", "☯");
		name = name.replace("*down*", "↓");
		name = name.replace("*left*", "←");
		name = name.replace("*right*", "→");
		name = name.replace("*up*", "↑");
		name = name.replace("*phone*", "☎");
		name = name.replace("*skull*", "☠");
		name = name.replace("*radio*", "☢");
		name = name.replace("*bio*", "☣");
		name = name.replace("*peace*", "☮");
		name = name.replace("*spade*", "♠");
		name = name.replace("*heart*", "♥");
		name = name.replace("*diamond*", "♦");
		name = name.replace("*club*", "♣");
		name = name.replace("*plane*", "✈");
		name = name.replace("*x*", "✖");
		name = name.replace("1/2", "½");
		name = name.replace("1/4", "¼");
		name = name.replace("*R*", "ℜ");
		name = name.replace("*N*", "ℵ");
		if(priv) {
			System.out.println("DSDS");
			int index = tabPane.indexOfTab(name);
			if(index == -1) {
				addPrivateChat(name, false);
				index = tabPane.indexOfTab(name);
			}
			PrivateChat pChat = chatMap.get(index);
			pChat.receiveText(str, name);
		}
		else {
			addToScreen(name + ": " + str);
		}
	}
	
	private void addToHistory(String str) {
		String[] tmp = new String[history.length+1];
		tmp[0] = str;
		System.arraycopy(history, 0, tmp, 1, history.length);
		history = tmp;
		printArray(history);
	}
	
	private void printArray(String[] array) {
		System.out.print("Array: [");
		for(int i=0; i<array.length-1; i++) {
			System.out.print(array[i] + ", ");
		}
		System.out.print(array[array.length-1] + "]" + System.lineSeparator());
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
	
//=============================================================================
//========================== GETTERS AND SETTERS ==============================
//=============================================================================
	
	public void setAltRainBowMode(boolean enabled) {
		altRBMode = enabled;
	}
	
	public boolean getAltRBMode() {
		return altRBMode;
	}
	
	public void setRainBowMode(boolean enabled) {
		rainbowMode = enabled;
	}
	
	public boolean getRainBowMode() {
		return rainbowMode;
	}
	
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
	 * Gets the component which holds the tabbed pages.
	 * @return the component which holds the tabbed pages.
	 */
	public JTabbedPane getTabPane() {
		return tabPane;
	}
	
	/**
	 * Set the <code>ColoringColor</code> which decides which coloring scheme is used.
	 * @param coloring the coloring scheme to be used.
	 */
	public void setColoring(ColoringColors coloring) {
		this.coloring = coloring;
	}
	
	/**
	 * Gets the value of the <code>ColoringColor</code> which decides which coloring scheme is used.
	 * @return the value of the <code>ColoringColor</code> which decides which coloring scheme is used.
	 */
	public ColoringColors getColoring() {
		return coloring;
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
	
	public String getRainbow(int index) {
		return rainbow[index];
	}
	
	public void setBGColor(Color color) {
		c.setBackground(color);
		peopleArea.setBackground(color);
	}
	
	public String getLastSeen(String name) {
		return "NOPE!";
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
			PreferencesMenu menu = new PreferencesMenu(this);
			menu.pack();
			menu.setVisible(true);
		}
		
		if(e.getSource().equals(rainbowModeItem)) {
			if(rainbowMode) {
				rainbowMode = false;
			}
			else {
				rainbowMode = true;
				rbThread = new Thread(new RainBowMode());
				rbThread.start();
			}
		}
		
		if(e.getSource().equals(fileItem)) {
			JFileChooser chooser = new JFileChooser();
		    int returnVal = chooser.showOpenDialog(this);
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
		       addToScreen("You chose to open this file: " + chooser.getSelectedFile().getName());
		    }
		}
	}

	@Override
	/**
	 * Called when the User presses Enter when typing in the send field.
	 */
	public void keyPressed(KeyEvent arg0) {
		if(arg0.getSource().equals(typeField)) {
			if(arg0.getKeyCode() == KeyEvent.VK_ENTER) {
				sendText();
				currentHistory = 0;
			}
			if(arg0.getKeyCode() == KeyEvent.VK_UP) {
				System.out.println(currentHistory);
				typeField.setText(getFromHistory(currentHistory));
				currentHistory++;
			}
			if(arg0.getKeyCode() == KeyEvent.VK_DOWN) {
				System.out.println(currentHistory);
				currentHistory--;
				typeField.setText(getFromHistory(currentHistory));
			}
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
			
			UserMenu menu = new UserMenu(name, client, this);
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

	@Override
	/**
	 * Unused
	 */
	public void windowActivated(WindowEvent arg0) {}

	@Override
	/**
	 * Unused
	 */
	public void windowClosed(WindowEvent arg0) {}

	@Override
	public void windowClosing(WindowEvent arg0) {
		if (JOptionPane.showConfirmDialog(this, "Are you sure to close this window?", "Really Closing?", 
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
			app.stop();
			animation.setCont(false);
			rainbowMode = false;
	        this.dispose();
	    }
	}

	@Override
	/**
	 * Unused
	 */
	public void windowDeactivated(WindowEvent arg0) {}

	@Override
	/**
	 * Unused
	 */
	public void windowDeiconified(WindowEvent arg0) {}

	@Override
	/**
	 * Unused
	 */
	public void windowIconified(WindowEvent arg0) {}

	@Override
	/**
	 * Unused
	 */
	public void windowOpened(WindowEvent arg0) {}

	@Override
	public void onClientConnected(Client client) {
		addUser(client.getName());
	}

	@Override
	public void onClientDisconnected(Client client) {
		System.out.println("Disconnected!");
		removeUser(client.getName());
	}

	@Override
	public void onClientTimedOut(Client client) {
		System.out.println("Time Out!");
		removeUser(client.getName());
	}

	@Override
	public void onClientLostRoute(Client client) {
		System.out.println("Lost Route!");
		removeUser(client.getName());
	}

	@Override
	public void onChatMessageReceived(Client client, String message) {
		String user = client.getName();
		
		receiveText(message, user, false);
	}

	@Override
	public void onPrivateChatMessageReceived(Client client, String message) {
		String user = client.getName();
		
		System.out.println("RECV: " + user + " " + message);
		receiveText(message, user, true);
	}
	
	public class RainBowMode extends Thread {
		@Override
		public void run() {
			while(rainbowMode) {
				setBGColor(Color.getHSBColor(animation.getHue(), 1, 1));
				repaintAll();
				try {
					Thread.sleep(10);
				}	
				catch (InterruptedException e) { }
			}
		}
	}
}
