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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("serial")
public class MainGUI extends JFrame implements ActionListener, KeyListener, MouseListener, WindowListener, ApplicationCallbacks {
	private Container c;
	
	private static final Color BGCOLOR = Color.LIGHT_GRAY;
	
	public static final int LIST_MAX_SIZE = 300;
	private static final int HISTORY_MAX_SIZE = 20;
	
	private static final String[] colors = {"Red", "Blue", "Green", "Yellow", "Purple", "Orange", "Black"};
	
	private HashMap<Client, String> colorMap = new HashMap<Client, String>();
	private ColoringColors coloring = ColoringColors.NORMAL;
	
	private String[] history = new String[0];
	private int currentHistory;
	
	private volatile boolean rainbowMode = false;
	private boolean altRBMode = false;
	
	private ArrayList<String> checkTextStrings = new ArrayList<String>();
	private Color chatFG;
	
	private HashMap<Integer, Chat> chatMap = new HashMap<Integer, Chat>();
	private HashMap<Client, Integer> tabMap = new HashMap<Client, Integer>();
	private HashMap<Group, Integer> groupMap = new HashMap<Group, Integer>();
	private ArrayList<Group> groupList = new ArrayList<Group>();
	
	private JMenuBar menu;
	private JMenu optionMenu;
	private JMenuItem preferencesItem;
	private JMenuItem rainbowModeItem;
	private JMenuItem nameChangeItem;
	private JMenuItem createGroupItem;
	private JMenu otherMenu;
	private JMenuItem fileItem;
	
	private JTabbedPane tabPane;
	private JTextField typeField;
	
	private DefaultListModel<Client> peopleList;
	private JList<Client> peopleArea;
	private JScrollPane peopleScrollPane;
	
	private JFrame prefMenu;
	
	private DefaultListModel<ChatLine> list;
	private JList<ChatLine> receiveArea;
	private JScrollPane scrollPane;
	private Client botClient;
	
	private JButton sendButton;
	
	private Client localClient;
	private Icon closeIcon;
	
	private Application app;
	
	private Thread rbThread;
	private AnimationThread animation;
	
	/**
	 * Constuctor of the <code>class</code>.
	 * @param name
	 */
	public MainGUI(String name) {
		super("Chat");
		app = new Application(name);
		app.subscribe(this);
		localClient = app.getLocalClient();
		init();
		app.start();
	}
	
	/**
	 * Inits the GUI and sets everything up. Creates the window and connects the User.
	 */
	public void init() {
		peopleList = new DefaultListModel<Client>();
		loadResources();
		
		animation = new AnimationThread();
		animation.setCont(true);
		animation.start();
		
		addWindowListener(this);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		c = getContentPane();
		c.setLayout(new BorderLayout());
		
		list = new DefaultListModel<ChatLine>();
		
		tabPane = new JTabbedPane();
		
		typeField = new JTextField();
		typeField.addKeyListener(this);
		
		peopleArea = new JList<Client>(peopleList);
		peopleArea.setForeground(Color.BLACK);
		peopleScrollPane = new JScrollPane(peopleArea);
		
		receiveArea = new JList<ChatLine>(list);
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
		chatMap.put(0, null);
		groupMap.put(new Group("t"), 0);
		tabMap.put(botClient, 0);
		tabPane.addMouseListener(this);

		c.add(tabPane, BorderLayout.CENTER);
		c.add(sideBar, BorderLayout.WEST);
		
		botClient = new Client("[BOT]", localClient.getAddress());
		setUserColor(botClient, "Red");
		
		addUser(localClient);
		
		menu = new JMenuBar();
		
		optionMenu = new JMenu("Options");
		
		preferencesItem = new JMenuItem("Preferences");
		preferencesItem.addActionListener(this);
		
		rainbowModeItem = new JMenuItem("Rainbow Mode");
		rainbowModeItem.addActionListener(this);
		
		nameChangeItem = new JMenuItem("Change Name");
		nameChangeItem.addActionListener(this);
		
		createGroupItem = new JMenuItem("Create/Join Group");
		createGroupItem.addActionListener(this);
		
		otherMenu = new JMenu("Other");
		
		fileItem = new JMenuItem("Open file...");
		fileItem.addActionListener(this);
		
		optionMenu.add(preferencesItem);
		optionMenu.add(rainbowModeItem);
		optionMenu.add(nameChangeItem);
		optionMenu.add(createGroupItem);
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
	public void removeUser(Client client) {
		peopleList.removeElement(client);
		addToScreen(botClient, "User " + client.getName() + " has left the chat room.");
	}
	
	/**
	 * Adds a User to the Chat Room.
	 * @param name the name of the User.
	 */
	public void addUser(Client client) {
		peopleList.addElement(client);
		peopleArea.addMouseListener(this);
		peopleArea.ensureIndexIsVisible(peopleList.getSize()-1);
		setUserColor(client, colors[(int)(Math.random()*7)]);
		addToScreen(botClient, "User <font color=" + getUserColor(client) + ">" + client.getName() + "</font> entered the chat room.");
	}
	
	/**
	 * Loads the resources of the GUI.
	 */
	public void loadResources() {
		closeIcon = new ImageIcon("res/close.png");
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File("res/smileys.txt")), "UTF-8"));
			String check = "";
			while((check = in.readLine()) != null) {
				checkTextStrings.add(check);
			}
			in.close();
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	public void addGroupChat(Group group) {
		if(groupMap.containsKey(group)) {
			tabPane.setSelectedIndex(groupMap.get(group));
			return;
		}
		JPanel groupChat = new JPanel();
		group.joinGroup(localClient);
		groupChat.setLayout(new BorderLayout());
		GroupChat gChat = new GroupChat(localClient, group, this, app, animation);
		groupChat.add(gChat, BorderLayout.CENTER);
		
		tabPane.addTab("t", groupChat);
		int i = tabPane.indexOfTab("t");
		
		groupMap.put(group, i);
		chatMap.put(i, gChat);
		
		addTabName(i, group.getName());
		tabPane.setSelectedIndex(i);
	}
	
	/**
	 * Opens a new Private Chat to the specified User.
	 * @param name the name of the User to chat with.
	 */
	public void addPrivateChat(Client client, boolean self) {
		if(tabMap.containsKey(client)) {
			tabPane.setSelectedIndex(tabMap.get(client));
			return;
		}
		if(client.equals(localClient)) return;
		
		JPanel privChat = new JPanel();
		privChat.setLayout(new BorderLayout());
		PrivateChat pChat = new PrivateChat(localClient, client, this, app, animation);
		privChat.add(pChat, BorderLayout.CENTER);
		
		tabPane.addTab("t", privChat);
		int i = tabPane.indexOfTab("t");
		
		tabMap.put(client, i);		
		chatMap.put(i, pChat);
		
		addTabName(i, client.getName());
		
		if(self) tabPane.setSelectedIndex(i);
	}
	
	private void addTabName(int i, String str) {
		JPanel tabPanel = new JPanel(new GridBagLayout());
		tabPanel.setOpaque(false);
		JLabel tabTitle = new JLabel(str + " ");
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
		tabClose.addActionListener(new TabActionListener(i, this));
	}
	
	public void removeTab(int index) {
		if(!chatMap.get(index).isPrivate()) {
			Group group = ((GroupChat)chatMap.get(index)).getGroup();
			group.leaveGroup(localClient);
			groupList.remove(group);
			groupMap.remove(group);
		}
		else {
			PrivateChat priv = ((PrivateChat)chatMap.get(index));
			tabMap.remove(priv);
		}
		chatMap.remove(index);
	}
	
	/**
	 * Adds a line of text to the text area.
	 * @param str the line of text to add.
	 */
	public void addToScreen(Client client, String str) {
		list.addElement(new ChatLine(client, str));
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
		if(IllegalCharCheck(txt)) return;
		typeField.setText("");
		addToHistory(txt);
		receiveText(txt, localClient, false, false, null);
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
	public void receiveText(String str, Client client, boolean priv, boolean g, Group group) {
		str = changeText(str);
		if(checkMultiple(str, client)) return;
		
		if(priv) {
			int index = 0;
			if(!tabMap.containsKey(client)) 
				addPrivateChat(client, false);
			index = tabMap.get(client);
			Chat chat = chatMap.get(index);
			if(!chat.isPrivate()) return;
			PrivateChat pChat = (PrivateChat)chat;
			pChat.receiveText(str, client);
			if(tabPane.getSelectedIndex() != index) {
				tabPane.remove(index);
				tabPane.add(pChat, index);
				tabPane.setBackgroundAt(index, Color.YELLOW);
				addTabName(index, client.getName() + " [!]");
			}
		}
		else if(g) {
			int index = 0;
			index = groupMap.get(group);
			Chat chat = chatMap.get(index);
			if(chat.isPrivate()) return;
			GroupChat gChat = (GroupChat)chat;
			gChat.receiveText(str, client);
			if(tabPane.getSelectedIndex() != index) {
				tabPane.remove(index);
				tabPane.add(gChat, index);
				tabPane.setBackgroundAt(index, Color.YELLOW);
				addTabName(index, group.getName() + " [!]");
			}
		}
		else {
			addToScreen(client, str);
			if(tabPane.getSelectedIndex() != 0) {
				JPanel mainChat = (JPanel)tabPane.getComponentAt(0);
				tabPane.remove(0);
				tabPane.add(mainChat, "Main Room [!]", 0);
				tabPane.setBackgroundAt(0, Color.YELLOW);
			}
		}
	}
	
	private void addToHistory(String str) {
		String[] tmp = new String[0];
		if(history.length+1 <= HISTORY_MAX_SIZE) {
			tmp = new String[history.length+1];
		}
		else {
			tmp = new String[history.length];
		}
		tmp[0] = str;
		if(history.length+1 <= HISTORY_MAX_SIZE) System.arraycopy(history, 0, tmp, 1, history.length);
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
	
	public String changeText(String txt) {
		for(String check : checkTextStrings) {
			String[] split = check.split(",");
			txt = txt.replace(split[0], split[1]);
		}
		return txt;
	}
	
	private boolean checkMultiple(String str, Client client) {
		if(str.equals("*music*")) {
			if(client.equals(localClient)) {
				String[] tmp = {"╔══╗ ♫", "║██║ ♪♪", "║██║♫♪", "║ ◎♫♪♫", "╚══╝"};
				sendMultiple(tmp);
			}
			return true;
		}
		if(str.equals("*fatbunny*")) {
			if(client.equals(localClient)) {
				String[] tmp = {"(\\____/)", "(='.'=)", "(\")__(\")"};
				sendMultiple(tmp);
			}
			return true;
		}
		if(str.startsWith("*wavename*")) {
			if(client.equals(localClient)) {
				String[] split = str.split("\\*");
				String[] tmp = {"¯¨'*·~-.¸¸,.-~*' " + split[2] + " ¯¨'*·~-.¸¸,.-~*'"};
				sendMultiple(tmp);
			}
			return true;
		}
		if(str.startsWith("*owls*")) {
			if(client.equals(localClient)) {
				String[] tmp = {"☆.´ `. ☽¸.☆","(͡๏̯͡๏)(͡๏̯͡๏)","( , ,)( , ,).","¯**´¯**´¯`\""};
				sendMultiple(tmp);
			}
			return true;
		}
		return false;
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
	public String getUserColor(Client client) {
		if(colorMap.containsKey(client)) {
			return colorMap.get(client);
		}
		return "Black";
	}
	
	/**
	 * Sets the specified User's color.
	 * @param name the name of the User.
	 * @param color The color to set the User to.
	 */
	public void setUserColor(Client client, String color) {
		colorMap.put(client, color);
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
	
	public String getColorFromArray(ColoringColors color, int index) {
		switch(color) {
			case FIFTY_SHADES: return ColorArrays.fiftyShades[index];
			case RAINBOW: return ColorArrays.rainbow[index];
			case G2G: return ColorArrays.gray2green[index];
			case G2R: return ColorArrays.gray2red[index];
			case G2B: return ColorArrays.gray2blue[index];
			case R2B: return ColorArrays.red2blue[index];
			case LB2G: return ColorArrays.lblue2green[index];
			case G2BL: return ColorArrays.gray2black[index];
			case BLACK: return ColorArrays.black[index];
			default: return "000000";
		}
	}
	
	public int getLenghtOfColoringArray(ColoringColors color) {
		switch(color) {
			case FIFTY_SHADES: return ColorArrays.fiftyShades.length;
			case RAINBOW: return ColorArrays.rainbow.length;
			case G2G: return ColorArrays.gray2green.length;
			case G2R: return ColorArrays.gray2red.length;
			case G2B: return ColorArrays.gray2blue.length;
			case R2B: return ColorArrays.red2blue.length;
			case LB2G: return ColorArrays.lblue2green.length;
			case G2BL: return ColorArrays.gray2black.length;
			case BLACK: return ColorArrays.black.length;
			default: return 0;
		}
	}
	
	public int getNumberofColoringArrays() {
		return ColorArrays.COLORING_ARRAYS;
	}
	
	public void setChatFGColor(Color color) {
		chatFG = color;
	}
	
	public Color getChatFGColor() {
		return chatFG;
	}
	
	public void setBGColor(Color color) {
		c.setBackground(color);
		peopleArea.setBackground(color);
	}
	
	public String getLastSeen(Client client) {
		return "NOPE!";
	}
	
	public Client getBot() {
		return botClient;
	}
	
	public Group getGroup(String name) {
		for(Group group : groupList) {
			if(group.getName().equals(name))
				return group;
		}
		return null;
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
	
	public boolean IllegalCharCheck(String txt) {
		if(txt.toLowerCase().length() == 0 || txt.toLowerCase().matches("\\s*") || txt.toLowerCase().matches(".*<.*>.*") || 
			txt.toLowerCase().matches(".*<script.*") || txt.length() > 3000) {
			return true;
		}
		return false;
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
			prefMenu = new PreferencesMenu(this);
			prefMenu.pack();
			prefMenu.setVisible(true);
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
		
		if(e.getSource().equals(nameChangeItem)) {
			String name = JOptionPane.showInputDialog("Please input your new name.", localClient.getName());
			if(name != null && !IllegalCharCheck(name)) {
				addToScreen(botClient, "HAH! Like hell you'll be named: " + name + "!");
				localClient.setName(name);
			}
		}
		
		if(e.getSource().equals(createGroupItem)) {
			String name = JOptionPane.showInputDialog("Please input the group's name.", "Group Name");
			if(name != null && !IllegalCharCheck(name)) {
				Group g = new Group(name);
				groupList.add(g);
				addGroupChat(g);
			}
		}
		
		if(e.getSource().equals(fileItem)) {
			JFileChooser chooser = new JFileChooser();
		    int returnVal = chooser.showOpenDialog(this);
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
		       addToScreen(botClient, "You chose to open this file: " + chooser.getSelectedFile().getName());
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
			Client client = peopleArea.getSelectedValue();
			if(client == null) return;
			
			UserMenu menu = new UserMenu(client, localClient, this);
		    menu.show(arg0.getComponent(), arg0.getX(), arg0.getY());
		}
		
		if(arg0.getSource().equals(tabPane) && arg0.getButton() == MouseEvent.BUTTON1) {
			int index = tabPane.getSelectedIndex();
			if(chatMap.get(index) == null) {
				JPanel mainChat = (JPanel)tabPane.getComponentAt(index);
				tabPane.remove(index);
				tabPane.add(mainChat, "Main Room", index);
				tabPane.setSelectedIndex(index);
			}
			else {
				Client otherClient = null;
				PrivateChat pChat = null;
				Group group = null;
				GroupChat gChat = null;
				if(chatMap.get(index).isPrivate()) {
					pChat = (PrivateChat)chatMap.get(index);
					otherClient = pChat.getOtherClient();
					tabPane.remove(index);
					tabPane.add(pChat, index);
					addTabName(index, otherClient.getName());
				}
				else {
					gChat = (GroupChat)chatMap.get(index);
					group = gChat.getGroup();
					tabPane.remove(index);
					tabPane.add(gChat, index);
					addTabName(index, group.getName());
				}
				
				tabPane.setSelectedIndex(index);
			}
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
			if(prefMenu != null) prefMenu.dispose();
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
		addUser(client);
	}

	@Override
	public void onClientDisconnected(Client client) {
		System.out.println("Disconnected!");
		removeUser(client);
	}

	@Override
	public void onClientTimedOut(Client client) {
		System.out.println("Time Out!");
		removeUser(client);
	}

	@Override
	public void onClientLostRoute(Client client) {
		System.out.println("Lost Route!");
		removeUser(client);
	}

	@Override
	public void onChatMessageReceived(Client client, String message) {
		receiveText(message, client, false, false, null);
	}

	@Override
	public void onPrivateChatMessageReceived(Client client, String message) {
		receiveText(message, client, true, false, null);
	}
	
	@Override
	public void onGroupChatMessageReceived(Client client, String groupName, String message) {
		Group group = getGroup(groupName);
		if(group != null) {
			receiveText(message, client, false, true, group);
		}
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
