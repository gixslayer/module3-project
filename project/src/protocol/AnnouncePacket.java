package protocol;

import client.Client; 

import java.util.ArrayList;
import java.util.List;

public class AnnouncePacket extends Packet {
	private Client sourceClient;
	private List<Client> knownClients;
	
	public AnnouncePacket() {
		super(Packet.TYPE_ANNOUNCE);
		
		this.sourceClient = new Client();
		this.knownClients = new ArrayList<Client>();
	}
	
	public AnnouncePacket(Client sourceClient, List<Client> knownClients) {
		super(Packet.TYPE_ANNOUNCE);
		
		this.sourceClient = sourceClient;
		this.knownClients = knownClients;
	}
	
	protected byte[] serializeContent() {
		byte[][] serializedClients = new byte[knownClients.size() + 1][];
		int totalLength = 0;
		
		serializedClients[0] = sourceClient.serialize();
		totalLength += serializedClients[0].length;
		for(int i = 0; i < serializedClients.length - 1; i++) {
			serializedClients[i + 1] = knownClients.get(i).serialize();
			totalLength += serializedClients[i + 1].length;
		}
		
		byte[] buffer = new byte[totalLength];
		int offset = 0;
		
		for(int i = 0; i < serializedClients.length; i++) {
			System.arraycopy(serializedClients[i], 0, buffer, offset, serializedClients[i].length);
			offset += serializedClients[i].length;
		}
		
		return buffer;
	}
	
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		offset += sourceClient.deserialize(buffer, offset);
		knownClients.clear();
		
		while(offset < length) {
			Client knownClient = new Client();
			offset += knownClient.deserialize(buffer, offset);
			
			knownClients.add(knownClient);
		}
	}
	
	public void setSourceClient(Client client) {
		this.sourceClient = client;
	}
	
	public Client getSourceClient() {
		return sourceClient;
	}
	
	public List<Client> getKnownClients() {
		return knownClients;
	}
}
