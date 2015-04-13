package gui;

import client.Client;

public interface ChatLine {
	public String getTime();
	public void setTime(String time);
	public Client getClient();
	public void setClient(Client client);
	public String getLine();
	public void setLine(String line);
}
