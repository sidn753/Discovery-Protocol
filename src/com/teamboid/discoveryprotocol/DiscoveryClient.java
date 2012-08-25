package com.teamboid.discoveryprotocol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

/**
 * @author Aidan Follestad
 */
public class DiscoveryClient {

	public DiscoveryClient(Context context) throws Exception {
		WifiManager wifi = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		broadcastAdr = getBroadcastAddress(wifi);
		myIP = getMyIP(wifi);
		socket = new DatagramSocket(NETWORK_PORT);
		socket.setBroadcast(true);
		socket.setReuseAddress(true);
		startReceiveThread();
	}

	private InetAddress getBroadcastAddress(WifiManager wifi) throws Exception {
		DhcpInfo dhcp = wifi.getDhcpInfo();
		if (dhcp == null) {
			throw new Exception(
					"DHCP info is null, check for a Wifi connection!");
		}
		int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
			quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
		return InetAddress.getByAddress(quads);
	}

	private InetAddress getMyIP(WifiManager wifi) {
		WifiInfo myWifiInfo = wifi.getConnectionInfo();
		int myIp = myWifiInfo.getIpAddress();
		int intMyIp3 = myIp / 0x1000000;
		int intMyIp3mod = myIp % 0x1000000;
		int intMyIp2 = intMyIp3mod / 0x10000;
		int intMyIp2mod = intMyIp3mod % 0x10000;
		int intMyIp1 = intMyIp2mod / 0x100;
		int intMyIp0 = intMyIp2mod % 0x100;
		try {
			return InetAddress.getByName(String.valueOf(intMyIp0) + "."
					+ String.valueOf(intMyIp1) + "." + String.valueOf(intMyIp2)
					+ "." + String.valueOf(intMyIp3));
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}

	private InetAddress myIP;
	private InetAddress broadcastAdr;
	private Thread receiveThread;
	private DatagramSocket socket;
	private DiscoveryListener events;
	private String _name;
	private String _status;
	private boolean _filterOwn = true;
	
	private final static int NETWORK_PORT = 2000;

	private void processPacket(DatagramPacket packet) throws Exception {
		if (events == null)
			return;
		InetAddress address = packet.getAddress();
		if (_filterOwn
				&& address.getHostAddress().equals(myIP.getHostAddress())) {
			// Filter out packet broadcasts that you sent.
			return;
		}
		JSONObject content = null;
		try {
			content = new JSONObject(new String(packet.getData(), "UTF8")
					.replace("\0", "").trim());
		} catch (Exception e) {
			/**
			 * Silently ignore packets that don't contain valid JSON. Could be
			 * other programs since this protocol intercepts UDP broadcasts too.
			 */
			return;
		}
		events.onReceive(content, address);
		DiscoveryEntity entity = new DiscoveryEntity(address, content);

		if (content.optString("type").equals("discovery")) {
			events.onDiscoveryRequest(entity);
		} else if (content.optString("type").equals("response")) {
			events.onDiscoveryResponse(entity);
		} else if (content.optString("type").equals("online")) {
			events.onOnline(entity);
		} else if (content.optString("type").equals("chat")) {
			events.onMessage(entity, content.optString("message"));
		} else if (content.optString("type").equals("broadcast")) {
			events.onBroadcast(entity, content.optString("message"));
		} else if (content.optString("type").equals("offline")) {
			events.onOffline(entity);
		} else if (content.optString("type").equals("nickname")) {
			events.onNameChange(entity);
		} else if (content.optString("type").equals("status")) {
			events.onStatus(entity);
		} else if (content.optString("type").equals("ping")) {
			events.onPing(entity);
		} else if (content.optString("type").equals("ping-back")) {
			events.onPingBack(entity);
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
					if (socket == null || socket.isClosed())
						break;
					byte[] receiveData = new byte[2000];
					DatagramPacket receivePacket = new DatagramPacket(
							receiveData, receiveData.length);
					try {
						socket.receive(receivePacket);
						processPacket(receivePacket);
					} catch (Exception e) {
						if (socket == null || socket.isClosed())
							break;
						e.printStackTrace();
						if (events != null) {
							events.onError(e.getMessage());
						}
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
				if (events != null) {
					events.onError(e.getMessage());
				}
			}
		}
		try {
			byte[] sendData = toSend.toString().getBytes("UTF8");
			socket.send(new DatagramPacket(sendData, sendData.length, to,
					NETWORK_PORT));
		} catch (Exception e) {
			e.printStackTrace();
			if (events != null) {
				events.onError("Failed to send a '" + atts.get(0)
						+ "' request! " + e.getMessage());
			}
		}
		if (events != null) {
			events.onSent(toSend, to);
		}
	}
	
	/**
	 * Gets whether or not there's an active Wifi connection.
	 */
	public boolean isWifiConnected(Context context) {
		ConnectivityManager connManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (mWifi != null && mWifi.isConnected()) {
			return true;
		} else return false;
	}
	
	/**
	 * Gets whether or not creating a Wifi connection is currently in progress. Also returns true if you're already connected.
	 */
	public boolean isWifiConnecting(Context context) {
		ConnectivityManager connManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (mWifi != null && mWifi.isConnectedOrConnecting()) {
			return true;
		} else return false;
	}
	
	/**
	 * Broadcasts a discovery request; when entities choose to respond to the
	 * request, you will receive a callback.
	 */
	public void discover() {
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "discovery" });
		toSend.add(new String[] { "id", Build.SERIAL });
		toSend.add(new String[] { "name", _name });
		toSend.add(new String[] { "status", _status });
		send(toSend, broadcastAdr);
	}

	/**
	 * Responds to a discovery request, making you visible to the requesting
	 * entity.
	 */
	public void respond(DiscoveryEntity request) {
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "response" });
		toSend.add(new String[] { "id", Build.SERIAL });
		toSend.add(new String[] { "name", _name });
		toSend.add(new String[] { "status", _status });
		send(toSend, request.getAddress());
	}

	/**
	 * Broadcasts an online notification, telling other entities you're online.
	 */
	public void online() {
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "online" });
		toSend.add(new String[] { "id", Build.SERIAL });
		toSend.add(new String[] { "name", _name });
		toSend.add(new String[] { "status", _status });
		send(toSend, broadcastAdr);
	}

	/**
	 * Sends a chat message to another entity.
	 */
	public void message(DiscoveryEntity to, String message) {
		if (message != null && message.trim().isEmpty()) {
			message = null;
		}
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "chat" });
		toSend.add(new String[] { "id", Build.SERIAL });
		toSend.add(new String[] { "name", _name });
		toSend.add(new String[] { "message", message });
		toSend.add(new String[] { "status", _status });
		send(toSend, to.getAddress());
	}

	/**
	 * Broadcasts a chat message to all other entities on the network.
	 */
	public void broadcast(String message) {
		if (message != null && message.trim().isEmpty()) {
			message = null;
		}
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "broadcast" });
		toSend.add(new String[] { "id", Build.SERIAL });
		toSend.add(new String[] { "name", _name });
		toSend.add(new String[] { "message", message });
		toSend.add(new String[] { "status", _status });
		send(toSend, broadcastAdr);
	}

	/**
	 * Updates your status message. When 'true' is passed in the second
	 * parameter, the status will be immediately broadcasted telling other
	 * entities of your new status. Otherwise it will be passed in future
	 * requests.
	 */
	public void status(String message, boolean broadcastUpdate) {
		if (message != null && message.trim().isEmpty()) {
			message = null;
		}
		_status = message;
		if (broadcastUpdate) {
			ArrayList<String[]> toSend = new ArrayList<String[]>();
			toSend.add(new String[] { "type", "status" });
			toSend.add(new String[] { "id", Build.SERIAL });
			toSend.add(new String[] { "name", _name });
			toSend.add(new String[] { "status", _status });
			send(toSend, broadcastAdr);
		}
	}

	/**
	 * Pings another entity, if they don't immediately ping back then it's likely they are no longer online.
	 */
	public void ping(DiscoveryEntity to) {
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "ping" });
		toSend.add(new String[] { "id", Build.SERIAL });
		toSend.add(new String[] { "name", _name });
		toSend.add(new String[] { "status", _status });
		send(toSend, to.getAddress());
	}
	
	/**
	 * Responds to a ping, indicating that you are online.
	 */
	public void pingBack(DiscoveryEntity to) {
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "ping-back" });
		toSend.add(new String[] { "id", Build.SERIAL });
		toSend.add(new String[] { "name", _name });
		toSend.add(new String[] { "status", _status });
		send(toSend, to.getAddress());
	}

	/**
	 * Broadcasts an offline notification, telling other entities you're going
	 * offline.
	 */
	public void offline() {
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "offline" });
		toSend.add(new String[] { "id", Build.SERIAL });
		toSend.add(new String[] { "name", _name });
		toSend.add(new String[] { "status", _status });
		send(toSend, broadcastAdr);
	}

	/**
	 * Sets your nickname that's displayed to other users. When 'true' is passed
	 * in the second parameter, the nickname will immediately be broadcasted
	 * telling other entities of your new nickname. Otherwise it will be passed
	 * in future requests.
	 */
	public void nickname(String name, boolean broadcastUpdate) {
		if (name != null && name.trim().isEmpty()) {
			name = null;
		}
		_name = name;
		if (broadcastUpdate) {
			ArrayList<String[]> toSend = new ArrayList<String[]>();
			toSend.add(new String[] { "type", "nickname" });
			toSend.add(new String[] { "id", Build.SERIAL });
			toSend.add(new String[] { "name", _name });
			toSend.add(new String[] { "status", _status });
			send(toSend, broadcastAdr);
		}
	}

	/**
	 * Sets the listener that will receive callbacks.
	 */
	public void setDiscoveryListener(DiscoveryListener listener) {
		events = listener;
	}

	/**
	 * Defaults to true; when set to false, you will receive your own
	 * broadcasts. This is good for testing your apps when you only have one
	 * device to test it (e.g., you will receive your own discovery and response
	 * broadcasts).
	 */
	public void setFilterSelf(boolean filter) {
		_filterOwn = filter;
	}

	@Override
	public void finalize() {
		socket.close();
		socket = null;
		receiveThread.interrupt();
	}
}