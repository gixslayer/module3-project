package gui;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class PrivateChat extends JPanel implements ActionListener, KeyListener {
	private Container c;

	private JTextField typeField;
	
	private DefaultListModel<String> list;
	private JList<String> receiveArea;
	private JScrollPane scrollPane;
	
	private JButton sendButton;
	
	private String clientName;
	private MainGUI main;
	
	public PrivateChat(String name, MainGUI main) {
		clientName = name;
		this.main = main;
		init();
	}
	
	public void init() {
		setLayout(new BorderLayout());
		
		list = new DefaultListModel<String>();
		
		typeField = new JTextField();
		typeField.addKeyListener(this);
		
		receiveArea = new JList<String>(list);
		receiveArea.setCellRenderer(new MyCellRenderer());
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
	
	public class MyCellRenderer extends JLabel implements ListCellRenderer<Object> {
	     public MyCellRenderer() {
	         setOpaque(true);
	     }

	     public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
	         setText(value.toString());
	         Color background = Color.WHITE;
	         Color foreground = Color.BLACK;
	         if(main.getFiftyEnabled()) {
	        	background = Color.decode("0x" + main.fiftyShades[index % 50]);
	         }
	         else {
	        	 if(index % 2 == 0)
	        		 background = Color.decode("0x" + main.fiftyShades[10]);
	        	 else
	        		 background = Color.decode("0x" + main.fiftyShades[20]);
	         }
	         
	         if(value.toString().startsWith("[JOIN]:")) 
	        	 setText("<html><font color=blue>[JOIN]:</font>" + value.toString().split(":")[1] + "</html>");
	         
	         else if(value.toString().startsWith("[LEAVE]:")) 
	        	 setText("<html><font color=red>" + value.toString().split(":")[0] + "</font>:" + value.toString().split(":")[1] + "</html>");
	         else {
	        	 setText("<html><font color="+ main.getUserColor(value.toString().split(":")[0]) +">" + value.toString().split(":")[0] + "</font>:" + value.toString().split(":")[1] + "</html>");
	         }
	        	 
	         setBackground(background);
	         setForeground(foreground);

	         return this;
	     }
	 }
}
