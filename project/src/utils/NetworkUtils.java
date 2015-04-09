package utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Utility class for network related code.
 * @author ciske
 *
 */
public final class NetworkUtils {
	private static final String NAME_REGEX = "(eth\\d+)|(wlan\\d+)";
	//private static final String ADDRESS_REGEX = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
	private static final String ADDRESS_REGEX = "192\\.168\\.5\\.\\d{1,3}";
	
	/**
	 * Returns the IPv4 address of the wlan network interface.
	 * @return the IPv4 address of the wlan network interface.
	 */
	public static InetAddress getLocalAddress() {
		// This method works in our setup, but still seems a bit messy and potentially problematic.
		// Perhaps send a specific multicast packet on startup and grab the proper IP from that before
		// doing anything else network related?
		try {
			Enumeration<NetworkInterface> netEnum = NetworkInterface.getNetworkInterfaces();
			
			while(netEnum.hasMoreElements()) {
				NetworkInterface ni = netEnum.nextElement();
				
				if(ni.isLoopback() || !ni.isUp() || !ni.supportsMulticast()) {
					continue;
				}
				if(!ni.getName().matches(NAME_REGEX)) {
					continue;
				}
				
				Enumeration<InetAddress> addressEnum = ni.getInetAddresses();
				
				while(addressEnum.hasMoreElements()) {
					InetAddress address = addressEnum.nextElement();
					String hostAddress = address.getHostAddress();
					
					if(hostAddress.matches(ADDRESS_REGEX)) {
						return address;
					}
				}
			}
			
			throw new RuntimeException("Failed to grab local address: Could not find any addresses matching criteria");
		} catch(SocketException e) {
			throw new RuntimeException(String.format("Failed to grab local address: %s", e.getMessage()));
		}
	}
}
