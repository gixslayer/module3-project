package gui;

public class Alice {
	//private MainGUI main;
	private String name;
	
	private String[] triggers = {".*(hey).*", ".*(like)\\s(bob).*", ".*(how).*(are).*(you).*", ".*(i).*(fine).*", ".*(i).*(bad).*",
			".*(i)\\slike\\syou.*", ".*(i)\\slike.*", ".*(do)\\s(you).*", ".*(why)\\s(do).*"};
	private String[] responses = {"Hey! How are you?", "I guess I do..", "I'm fine. How are you?", "Good to hear!", "Oh, why's that?", 
			"Oh.. Ehm.. I.. Oh, look! A bird!", "Do you? I like them too!", "Yes, I do!", 
			"Hmm.. I don't know. Maybe you should ask Bob. He's a lot smarter than me. He's never around, though..."};
	
	public Alice(MainGUI main, String name) {
		//this.main = main;
		this.name = name;
	}
	
	public String getResponse(String txt) {
		for(int i=0; i<triggers.length; i++) {
			if(txt.toLowerCase().matches(triggers[i])) {
				return responses[i];
			}
		}
		return "Hey! I'm " + name + "!";
	}
}
