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
	private static final Color JOIN_COLOR = Color.BLUE;
	private static final Color LEAVE_COLOR = Color.RED;
	
	private static final String[] colors = {"Red", "Blue", "Green", "Yellow", "Purple", "Orange", "Black"};
	public static final String[] fiftyShades = {"E0E0E0", "DEDEDE", "DBDBDB", "D9D9D9", "D6D6D6", "D4D4D4", "D1D1D1", "CFCFCF",
		"CCCCCC", "C9C9C9", "C7C7C7", "C4C4C4", "C2C2C2", "BFBFBF", "BDBDBD", "BABABA", "B8B8B8", "B5B5B5", "B3B3B3", "B0B0B0",
		"ADADAD", "ABABAB", "A9A9A9", "A8A8A8", "A6A6A6", "A3A3A3", "A1A1A1", "9E9E9E", "9C9C9C", "969696", "949494", "919191",
		"8F8F8F", "8C8C8C", "8A8A8A", "878787", "858585", "828282", "7F7F7F", "7D7D7D", "7A7A7A", "787878", "757575", "737373",
		"707070", "6E6E6E", "6B6B6B", "696969", "666666", "636363", "616161"};
	
	private HashMap<String, String> colorMap;
	public boolean fiftyEnabled = false;
	
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
	
	public MainGUI(String name) {
		super("Chat");
		clientName = name;
		init();
	}
	
	public void init() {
		colorMap = new HashMap<String, String>();
		
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
		receiveArea.setCellRenderer(new MyCellRenderer());
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
	
	public boolean getFiftyEnabled() {
		return fiftyEnabled;
	}
	
	public void removeUser(String name) {
		peopleList.removeElement(name);
		addToScreen("[LEAVE]: User " + name + " has left the chat room.");
	}
	
	public void addUser(String name) {
		peopleList.addElement(name);
		peopleArea.addMouseListener(this);
		peopleArea.ensureIndexIsVisible(peopleList.getSize()-1);
		setUserColor(name, colors[(int)(Math.random()*7)]);
		addToScreen("[JOIN]: User <font color=" + getUserColor(name) + ">" + name + "</font> entered the chat room.");
	}
	
	public String getUserColor(String name) {
		if(colorMap.containsKey(name)) {
			return colorMap.get(name);
		}
		return "Black";
	}
	
	public void setUserColor(String name, String color) {
		colorMap.put(name, color);
		receiveArea.repaint();
	}
	
	public void loadIcons() {
		closeIcon = new ImageIcon("images/close.png");
	}
	
	public void addPrivateChat(String name) {
		if(tabPane.indexOfTab(name) != -1) {
			tabPane.setSelectedIndex(tabPane.indexOfTab(name));
			return;
		}
		if(name.equals(clientName)) return;
		JPanel privChat = new JPanel();
		privChat.setLayout(new BorderLayout());
		privChat.add(new PrivateChat(clientName, this), BorderLayout.CENTER);
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
		
		tabPane.setSelectedIndex(i);
	}
	
	public void addToScreen(String str) {
		list.addElement(str);
		receiveArea.ensureIndexIsVisible(list.getSize() -1);
	}
	
	public void sendText() {
		String txt = typeField.getText();
		if(txt.length() == 0) return;
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
		
		if(e.getSource().equals(preferencesItem)) {
			PreferencesMenu menu = new PreferencesMenu(clientName);
			menu.pack();
			menu.setVisible(true);
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
	public void keyReleased(KeyEvent arg0) { }

	@Override
	public void keyTyped(KeyEvent arg0) { }
	
	@Override
	public void mouseClicked(MouseEvent arg0) { 
		if(arg0.getSource().equals(peopleArea) && arg0.getButton() == MouseEvent.BUTTON3) {
			String name = peopleArea.getSelectedValue();
			if(name == null) return;
			
			UserMenu menu = new UserMenu(name);
		    menu.show(arg0.getComponent(), arg0.getX(), arg0.getY());
		}
	}

	@Override
	public void mouseEntered(MouseEvent arg0) { }

	@Override
	public void mouseExited(MouseEvent arg0) { }

	@Override
	public void mousePressed(MouseEvent arg0) { }

	@Override
	public void mouseReleased(MouseEvent arg0) { }

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
	
	public class MyCellRenderer extends JLabel implements ListCellRenderer<Object> {
	     public MyCellRenderer() {
	         setOpaque(true);
	     }

	     public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
	         setText(value.toString());
	         Color background = Color.WHITE;
	         Color foreground = Color.BLACK;
	         if(fiftyEnabled) {
	        	background = Color.decode("0x" + fiftyShades[index % 50]);
	         }
	         else {
	        	 if(index % 2 == 0)
	        		 background = Color.decode("0x" + fiftyShades[10]);
	        	 else
	        		 background = Color.decode("0x" + fiftyShades[20]);
	         }
	         
	         if(value.toString().startsWith("[JOIN]:")) 
	        	 setText("<html><font color=blue>[JOIN]:</font>" + value.toString().split(":")[1] + "</html>");
	         
	         else if(value.toString().startsWith("[LEAVE]:")) 
	        	 setText("<html><font color=red>" + value.toString().split(":")[0] + "</font>:" + value.toString().split(":")[1] + "</html>");
	         else {
	        	 setText("<html><font color="+ getUserColor(value.toString().split(":")[0]) +">" + value.toString().split(":")[0] + "</font>:" + value.toString().split(":")[1] + "</html>");
	         }
	        	 
	         setBackground(background);
	         setForeground(foreground);

	         return this;
	     }
	 }
	
	public class UserMenu extends JPopupMenu implements ActionListener {
	    JMenuItem privChatItem;
	    JMenuItem pokeItem;
	    JMenu chooseColor;
	    JMenuItem[] color = new JMenuItem[7];
	    String name;
	    
	    public UserMenu(String name){
	        this.name = name;
	        if(name.equals(clientName)) {
	        	chooseColor = new JMenu("Choose Color");
	        	for(int i=0; i<color.length; i++) {
	        		color[i] = new JMenuItem(colors[i]);
	        		color[i].addActionListener(this);
	        		chooseColor.add(color[i]);
	        	}
	        	add(chooseColor);
	        }
	        else {
	        	privChatItem = new JMenuItem("Private Chat");
		        privChatItem.addActionListener(this);
		        add(privChatItem);
		        
		        pokeItem = new JMenuItem("Poke");
		        pokeItem.addActionListener(this);
		        add(pokeItem);
	        }
	    }
	    
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if(arg0.getSource().equals(privChatItem)) {
				addPrivateChat(name);
			}
			if(arg0.getSource().equals(pokeItem)) {
				removeUser(name);
			}
			for(int i=0; i<color.length; i++) {
				if(arg0.getSource().equals(color[i])) {
					setUserColor(name, colors[i]);
				}
			}
		}
	}
	
	public class PreferencesMenu extends JFrame implements ActionListener {
	    private JButton areaColor;
	    private String name;
	    
	    public PreferencesMenu(String name){
	        this.name = name;
	        areaColor = new JButton("Change Area Color");
	        areaColor.addActionListener(this);
	        add(areaColor);
	    }
	    
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if(arg0.getSource().equals(areaColor)) {
				fiftyEnabled = !fiftyEnabled;
				receiveArea.repaint();
			}
		}
	}
	
	public static void main(String[] args) {
		new MainGUI("Test");
	}
}
