package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class UserMenu extends JPopupMenu implements ActionListener {
	private JMenuItem privChatItem;
    private JMenuItem pokeItem;
    private JMenu chooseColor;
    private JMenuItem[] color = new JMenuItem[7];
    private String name;
    private MainGUI main;
    
    public UserMenu(String name, String clientName, MainGUI main){
        this.name = name;
        this.main = main;
        if(name.equals(clientName)) {
        	chooseColor = new JMenu("Choose Color");
        	for(int i=0; i<color.length; i++) {
        		color[i] = new JMenuItem(main.getColor(i));
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
			main.addPrivateChat(name);
		}
		if(arg0.getSource().equals(pokeItem)) {
			main.removeUser(name);
		}
		for(int i=0; i<color.length; i++) {
			if(arg0.getSource().equals(color[i])) {
				main.setUserColor(name, main.getColor(i));
			}
		}
	}
}