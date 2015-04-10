package protocol;

import client.Client;
import utils.ByteUtils;

public class PokePacket extends Packet {
    private Client client;

    public PokePacket() {
        this(new Client());
    }

    public PokePacket(Client client) {
        super(TYPE_POKE);

        this.client = client;
    }

    @Override
    protected byte[] serializeContent() {
        byte[] clientBytes = client.serialize(Client.SERIALIZE_ADDRESS);
        byte[] buffer = new byte[clientBytes.length + 8];

        ByteUtils.getIntBytes(clientBytes.length, buffer, 0);
        System.arraycopy(clientBytes, 0, buffer, 8, clientBytes.length);

        return buffer;
    }

    @Override
    protected void deserializeContent(byte[] buffer, int offset, int length) {
        client.deserialize(buffer, offset + 8);
    }

    public Client getClient() {
        return client;
    }
}
