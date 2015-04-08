package gui;

import java.util.HashMap;

public class Alice {
	private MainGUI main;
	private String name;
	
	private HashMap<String, String> triggers = new HashMap<String, String>();
	
	public Alice(MainGUI main, String name) {
		this.main = main;
		this.name = name;
		triggers.put(".*(hey).*", "Hey! How are you?");
		triggers.put(".*(like)\\s(bob).*", "I guess I do..");
		triggers.put(".*(how).*(are).*(you).*", "I'm fine. How are you?");
		triggers.put(".*(i).*(fine).*", "Good to hear!");
		triggers.put(".*(i).*(bad).*", "Oh, why's that?");
		triggers.put(".*(i)\\slike\\syou.*", "Oh.. Ehm.. I.. Oh, look! A bird!");
		triggers.put(".*(i)\\slike.*", "Do you? I like them too!");
		triggers.put(".*(do)\\s(you).*", "Yes, I do!");
		triggers.put(".*(why)\\s(do).*", "Hmm.. I don't know. Maybe you should ask Bob. He's a lot smarter than me. He's never around, though...");
	}
	
	public String getResponse(String txt) {
		for(String regex : triggers.keySet()) {
			if(txt.toLowerCase().matches(regex)) {
				return triggers.get(regex);
			}
		}
		return "Hey! I'm " + name + "!";
	}
}
