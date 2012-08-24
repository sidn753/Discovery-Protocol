package com.teamboid.discoveryprotocol;

import java.net.InetAddress;

import org.json.JSONObject;

/**
 * @author Aidan Follestad
 */
public interface DiscoveryListener {

	/**
	 * Called when another entity broadcasts a message telling other entities they're now online.
	 * Your list of online users should now add this user. 
	 */
	public void onOnline(DiscoveryEntity entity);
	
	/**
	 * Called when a discovery request that was broadcasted by another entity is received.
	 * Responding to this request will tell the requesting entity that you're online, making you visible to them.
	 * <br/>
	 * You should check your list of online users to see if this user is already there, and add them if they aren't. 
	 */
	public void onDiscoveryRequest(DiscoveryEntity request);
	
	/**
	 * Called when another entity responds to your previous discovery request. 
	 * Your list of online users should now add this user. 
	 */
	public void onDiscoveryResponse(DiscoveryEntity response);
	
	/**
	 * Called when another entity sends you a chat message. 
	 */
	public void onMessage(DiscoveryEntity from, String message);
	
	/**
	 * Called when another entity broadcasts a message to all other entities on the network.
	 */
	public void onBroadcast(DiscoveryEntity from, String message);
	
	/**
	 * Called when another entity updates their status (contained in the DiscoveryEntity object). 
	 */
	public void onStatus(DiscoveryEntity from);
	
	/**
	 * Called when another entity updates their display name (contained in the DiscoveryEntity object), 
	 */
	public void onNameChange(DiscoveryEntity from);
	
	/**
	 * Called when another entity broadcasts a message telling other entities they're going offline.
	 * Your list of online users should now remove this user.
	 */
	public void onOffline(DiscoveryEntity entity);
	
	/**
	 * Called when an error occurs.
	 */
	public void onError(String message);
	
	/** 
	 * Called when another entity pings you, this is much like poking on Facebook.
	 */
	public void onPing(DiscoveryEntity entity);
	
	/**
	 * Called when a message is sent to other entities, good for debugging.
	 */
	public void onSent(JSONObject json, InetAddress to);
	
	/**
	 * Called when a message is received from another entity, good for debugging.
	 */
	public void onReceive(JSONObject json, InetAddress from);
}