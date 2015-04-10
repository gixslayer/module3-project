package gui;

import client.Client;

public interface ChatLine {
	public Client getClient();
	public void setClient(Client client);
	public String getLine();
	public void setLine(String line);
}
