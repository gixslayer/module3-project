package protocol;

import client.Client; 

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AnnouncePacket extends Packet {
	private final Client sourceClient;
	private final List<Client> knownClients;
	
	public AnnouncePacket() {
		super(Packet.TYPE_ANNOUNCE);
		
		this.sourceClient = new Client();
		this.knownClients = new ArrayList<Client>();
	}
	
	public AnnouncePacket(Client sourceClient, Client[] knownClients) {
		super(Packet.TYPE_ANNOUNCE);
		
		this.sourceClient = sourceClient;
		this.knownClients = new ArrayList<Client>();
		
		if(knownClients != null) {
			this.knownClients.addAll(Arrays.asList(knownClients));
		}
	}
	
	protected byte[] serializeContent() {
		byte[][] serializedClients = new byte[knownClients.size() + 1][];
		int totalLength = 0;
		
		serializedClients[0] = sourceClient.serialize(Client.SERIALIZE_LASTSEEN);
		totalLength += serializedClients[0].length;
		for(int i = 0; i < serializedClients.length - 1; i++) {
			serializedClients[i + 1] = knownClients.get(i).serialize(Client.SERIALIZE_ADDRESS | Client.SERIALIZE_LASTSEEN);
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

	public Client getSourceClient() {
		return sourceClient;
	}
	
	public List<Client> getKnownClients() {
		return knownClients;
	}
}
