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
	
	private static final String[] colors = {"Red", "Blue", "Green", "Yellow", "Purple", "Black", "Orange", "Pink", "Seagreen", "Chocolate"};
	
	private HashMap<String, String> colorMap;
	
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
		
		setSize(800,800);
		setVisible(true);
	}
	
	public void removeUser(String name) {
		peopleList.removeElement(name);
		addToScreen("[LEAVE]: User " + name + " has left the chat room.");
	}
	
	public void addUser(String name) {
		peopleList.addElement(name);
		peopleArea.addMouseListener(this);
		peopleArea.ensureIndexIsVisible(peopleList.getSize()-1);
		setUserColor(name, colors[(int)(Math.random()*10)]);
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
	         
	         /*if(index % 2 == 0) {
	        	 background = Color.LIGHT_GRAY;
	         }
	         else background = Color.WHITE;
	         */
	         
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
	    JMenuItem[] color = new JMenuItem[10];
	    String name;
	    
	    public UserMenu(String name){
	        this.name = name;
	        if(name.equals(clientName)) {
	        	chooseColor = new JMenu("Choose Color");
	        	for(int i=0; i<color.length; i++) {
	        		color[i] = new JMenuItem(colors[i]);
	        		color[i].addActionListener(this);
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
	
	public static void main(String[] args) {
		new MainGUI("Test");
	}
}
