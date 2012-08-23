package com.teamboid.discoveryprotocol;

import java.net.InetAddress;

/**
 * Represents another device using the discovery protocol.
 * @author Aidan Follestad
 */
public class DiscoveryEntity {

	public DiscoveryEntity(String name, InetAddress ip) {
		_name = name;
		_ip = ip;
	}
	
	private String _name;
	private InetAddress _ip;
	
	/**
	 * Gets the display name of the entity.
	 * @return
	 */
	public String getName() {
		return _name;
	}
	
	/**
	 * Gets the IP address of the entity.
	 * @return
	 */
	public InetAddress getIP() {
		return _ip;
	}
	
	@Override
	public String toString() {
		return "DiscoveryRequest [" + _name + ", " + _ip.getHostAddress() + "]";
	}
}
