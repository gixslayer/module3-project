package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import client.Client;

@SuppressWarnings("serial")
public class UserMenu extends JPopupMenu implements ActionListener {
	private JMenuItem privChatItem;
    private JMenuItem pokeItem;
    private JMenuItem lastSeen;
    private JMenu chooseColor;
    private JMenuItem[] color = new JMenuItem[7];
    private Client otherClient;
    private MainGUI main;
    
    public UserMenu(Client otherClient, Client localClient, MainGUI main){
        this.otherClient = otherClient;
        this.main = main;
        chooseColor = new JMenu("Choose Color");
        for(int i=0; i<color.length; i++) {
        	color[i] = new JMenuItem(main.getColor(i));
        	color[i].addActionListener(this);
        	chooseColor.add(color[i]);
        }
        add(chooseColor);
        if(!otherClient.equals(localClient)) {
        	privChatItem = new JMenuItem("Private Chat");
	        privChatItem.addActionListener(this);
	        add(privChatItem);
	        
	        pokeItem = new JMenuItem("Poke");
	        pokeItem.addActionListener(this);
	        add(pokeItem);
	        
	        lastSeen = new JMenuItem("Last Seen");
	        lastSeen.addActionListener(this);
	        add(lastSeen);
        }
    }
    
	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(arg0.getSource().equals(privChatItem)) {
			main.addPrivateChat(otherClient, true);
		}
		else if(arg0.getSource().equals(pokeItem)) {
			main.receiveText("Hey! Don't poke me! :(", otherClient, true);
		}
		else if(arg0.getSource().equals(lastSeen)) {
			main.addToScreen(main.getBot(), otherClient + " was last seen at: " + main.getLastSeen(otherClient));
		}
		else {
			for(int i=0; i<color.length; i++) {
				if(arg0.getSource().equals(color[i])) {
					main.setUserColor(otherClient, main.getColor(i));
				}
			}
		}
	}
}