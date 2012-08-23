package com.teamboid.discoveryprotocol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

/**
 * @author Aidan Follestad
 */
public class DiscoveryClient {

	public DiscoveryClient(Context context) throws Exception {
		broadcastAdr = getBroadcastAddress(context);
		socket = new DatagramSocket(NETWORK_PORT);
		socket.setBroadcast(true);
		socket.setReuseAddress(true);
		startReceiveThread();
	}

	private InetAddress getBroadcastAddress(Context mContext) throws Exception {
	    WifiManager wifi = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
	    DhcpInfo dhcp = wifi.getDhcpInfo();
	    if(dhcp == null) {
	    	throw new Exception("DHCP info is null, check for a Wifi connection!");
	    }
	    int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
	    byte[] quads = new byte[4];
	    for (int k = 0; k < 4; k++)
	      quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
	    return InetAddress.getByAddress(quads);
	}

	private InetAddress broadcastAdr;
	private Thread receiveThread;
	private DatagramSocket socket;
	private DiscoveryListener events;
	private String _name;

	private final static int NETWORK_PORT = 2000;

	private void processPacket(DatagramPacket packet) throws Exception {
		JSONObject content = new JSONObject(
				new String(packet.getData(), "UTF8"));
		InetAddress address = packet.getAddress();
		events.onReceive(content, address);
		DiscoveryEntity entity = new DiscoveryEntity(content.optString("name"),
				address);

		if (content.optString("type").equals("discovery")) {
			events.onDiscoveryRequest(entity);
		} else if (content.optString("type").equals("response")) {
			events.onDiscoveryResponse(entity);
		} else if (content.optString("type").equals("online")) {
			events.onOnline(entity);
		} else if (content.optString("type").equals("chat")) {
			events.onMessage(entity, content.optString("message"));
		} else if (content.optString("type").equals("offline")) {
			events.onOffline(entity);
		} else if (content.optString("type").equals("nickname")) {
			events.onNameChange(null, content.optString("name"));
		} else {
			events.onError("Received unknown request from "
					+ address.getHostAddress() + " (type: "
					+ content.optString("type") + ")");
		}
	}

	private void startReceiveThread() throws Exception {
		receiveThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					if(socket == null || socket.isClosed()) break;
					byte[] receiveData = new byte[1024];
					DatagramPacket receivePacket = new DatagramPacket(
							receiveData, receiveData.length);
					try {
						socket.receive(receivePacket);
						processPacket(receivePacket);
					} catch (Exception e) {
						e.printStackTrace();
						events.onError(e.getMessage());
					}
				}
			}
		});
		receiveThread.start();
	}

	private void send(ArrayList<String[]> atts, InetAddress to) {
		JSONObject toSend = new JSONObject();
		for (String[] a : atts) {
			try {
				toSend.put(a[0], a[1]);
			} catch (JSONException e) {
				e.printStackTrace();
				events.onError(e.getMessage());
			}
		}
		try {
			byte[] sendData = toSend.toString().getBytes("UTF8");
			socket.send(new DatagramPacket(sendData, sendData.length, to,
				NETWORK_PORT));
		} catch(Exception e) {
			e.printStackTrace();
			events.onError("Failed to send a '" + atts.get(0) + "' request! " + e.getMessage());
		}
		events.onSent(toSend, to);
	}

	
	/**
	 * Broadcasts a discovery request; when entities choose to respond to the
	 * request, you will receive a callback.
	 */
	public void discover() {
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "discovery" });
		toSend.add(new String[] { "name", _name });
		send(toSend, broadcastAdr);
	}

	/**
	 * Responds to a discovery request, making you visible to the requesting
	 * entity.
	 */
	public void respond(DiscoveryEntity request) {
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "response" });
		toSend.add(new String[] { "name", _name });
		send(toSend, request.getIP());
	}

	/**
	 * Broadcasts an online notification, telling other entities you're online.
	 */
	public void online() {
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "online" });
		toSend.add(new String[] { "name", _name });
		send(toSend, broadcastAdr);
	}

	/**
	 * Sends a chat message to another entity.
	 */
	public void message(DiscoveryEntity to, String message) {
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "chat" });
		toSend.add(new String[] { "name", _name });
		toSend.add(new String[] { "message", message });
		send(toSend, to.getIP());
	}

	/**
	 * Broadcasts an offline notification, telling other entities you're going
	 * offline.
	 */
	public void offline() {
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "offline" });
		toSend.add(new String[] { "name", _name });
		send(toSend, broadcastAdr);
	}

	/**
	 * Sets your nickname that's displayed to other users. When 'true' is passed
	 * in the second parameter, a notification will be broadcasted telling other
	 * entities of your new nickname. Otherwise it will be passed in future
	 * requests.
	 */
	public void nickname(String name, boolean broadcastUpdate) {
		_name = name;
		if (broadcastUpdate) {
			ArrayList<String[]> toSend = new ArrayList<String[]>();
			toSend.add(new String[] { "type", "nickname" });
			toSend.add(new String[] { "name", _name });
			send(toSend, broadcastAdr);
		}
	}

	/**
	 * Sets the listener that will receive callbacks.
	 */
	public void setDiscoveryListener(DiscoveryListener listener) {
		events = listener;
	}

	@Override
	public void finalize() {
		socket.close();
		socket = null;
		receiveThread.interrupt();
	}
}