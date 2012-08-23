package com.teamboid.discoveryprotocol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import org.json.JSONObject;

/**
 * @author Aidan Follestad
 */
public class DiscoveryClient {

	public DiscoveryClient() throws Exception {
		receiveAdr = InetAddress.getByName("0.0.0.0");
		broadcastAdr = InetAddress.getByName("255.255.255.255");

		socket = new DatagramSocket();
		socket.setBroadcast(true);
		socket.setReuseAddress(true);

		startReceiveThread();
	}

	private InetAddress receiveAdr;
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
		final DatagramSocket recvSock = new DatagramSocket();
		recvSock.setBroadcast(true);
		recvSock.setReuseAddress(true);
		recvSock.bind(new InetSocketAddress(receiveAdr, NETWORK_PORT));
		receiveThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					byte[] receiveData = new byte[1024];
					DatagramPacket receivePacket = new DatagramPacket(
							receiveData, receiveData.length);
					try {
						recvSock.receive(receivePacket);
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

	private void send(ArrayList<String[]> atts, InetAddress to)
			throws Exception {
		JSONObject toSend = new JSONObject();
		for (String[] a : atts) {
			toSend.put(a[0], a[1]);
		}
		byte[] sendData = toSend.toString().getBytes("UTF8");
		socket.send(new DatagramPacket(sendData, sendData.length, to,
				NETWORK_PORT));
	}

	
	/**
	 * Broadcasts a discovery request; when entities choose to respond to the
	 * request, you will receive a callback.
	 */
	public void discover() throws Exception {
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "discovery" });
		toSend.add(new String[] { "name", _name });
		send(toSend, broadcastAdr);
	}

	/**
	 * Responds to a discovery request, making you visible to the requesting
	 * entity.
	 */
	public void respond(DiscoveryEntity request) throws Exception {
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "response" });
		toSend.add(new String[] { "name", _name });
		send(toSend, request.getIP());
	}

	/**
	 * Broadcasts an online notification, telling other entities you're online.
	 */
	public void online() throws Exception {
		ArrayList<String[]> toSend = new ArrayList<String[]>();
		toSend.add(new String[] { "type", "online" });
		toSend.add(new String[] { "name", _name });
		send(toSend, broadcastAdr);
	}

	/**
	 * Sends a chat message to another entity.
	 */
	public void message(DiscoveryEntity to, String message) throws Exception {
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
	public void offline() throws Exception {
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
	public void setName(String name, boolean broadcastUpdate) throws Exception {
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
		receiveThread.interrupt();
		receiveThread = null;
	}
}