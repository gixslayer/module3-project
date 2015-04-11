package protocol;

import client.Client;

public final class PokePacket extends Packet {
    private final Client client;

    public PokePacket() {
        this(new Client());
    }

    public PokePacket(Client client) {
        super(TYPE_POKE);

        this.client = client;
    }

    @Override
    protected byte[] serializeContent() {
        return client.serialize(Client.SERIALIZE_ADDRESS);
    }

    @Override
    protected void deserializeContent(byte[] buffer, int offset, int length) {
        client.deserialize(buffer, offset);
    }

    public Client getClient() {
        return client;
    }
}
