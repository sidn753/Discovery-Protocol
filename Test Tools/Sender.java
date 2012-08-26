import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Sender {

	public static void main(String[] args) {
		if(args.length < 3) {
			System.out.println();
			System.out.println("Usage: udp [dest-host] [dest-port] [content]");
			System.out.println();
			return;
		}
		StringBuilder content = new StringBuilder();
		for(int i = 2; i < args.length; i++) {
			content.append(args[i] + " ");
		}
		
		DatagramSocket clientSocket = null;
		try {
			InetAddress to = InetAddress.getByName(args[0]);
			int port = Integer.parseInt(args[1]);
			clientSocket = new DatagramSocket();
			byte[] sendData = content.toString().getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, to, port);
			clientSocket.send(sendPacket);
			System.out.println();
			System.out.println("Sent: " + content.toString());
			System.out.println();
		}
		catch(Exception e) {
			System.out.println("Error: " + e.getLocalizedMessage());
		}
		if(clientSocket != null) {
			clientSocket.close();
		}
	}
}
