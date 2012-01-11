import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.io.DataOutputStream;
import java.util.Scanner;
import java.security.SecureRandom;
import java.math.BigInteger;
import java.security.MessageDigest;

public class Client {
	Socket clientConnection;
	BufferedReader incoming;
	DataOutputStream outgoing;
	AppInfo clientInfo;

	public Client() {}

	public Client(String ipadd, int port, String username) {
		try {
			this.connect(ipadd, port);
			this.setupStreams();
			clientInfo = new AppInfo(username);
		} catch(Exception e) {}
	}

	class MessageListenerThread extends Thread {
		public void run() {
			try {
				while(true) {
					String incomingMessage = incoming.readLine();
					String a = incomingMessage;
					if(incomingMessage == null) {
						break;
					}
					String[] delimitedMessage = incomingMessage.split("message:");

					if(incomingMessage.startsWith("cusername:")) {
						clientInfo.remoteUsername = incomingMessage.substring(10);
						System.out.println("Username changed to " + clientInfo.remoteUsername);
						System.out.print("\n" + clientInfo.username + ": ");
					} else if(a.contains("disconnect")) {
						System.out.println("\rClient has disconnected.");
						close();
						System.exit(0);
					} else {
						System.out.print("\r" + delimitedMessage[1]);
						System.out.print("\n" + clientInfo.username + ": ");
					}
				}

				System.out.print("break!\n");
				close();
				System.exit(0);
			} catch(Exception e) {}
		}
	}

	public static void main(String[] args) throws Exception {
		String ipadd = args[0];
		int port = 50000;
		Client c = new Client(ipadd, port, args[1]);

		String message;
		c.handshake();
		Scanner sc = new Scanner(System.in);
		Thread messageListener = c.new MessageListenerThread();
		messageListener.start();

		System.out.print(c.clientInfo.username + ": ");
		while(c.clientConnection.isConnected()) {
			message = sc.nextLine();
			while(message.contains("~") || message.length() == 0) {
				System.out.print(c.clientInfo.username + ": ");
				message = sc.nextLine();
				if(message == null)
					break;
			}

			if(message.contains("KThanksBye")) {
				c.sendHandshakeMessage("disconnect");
				c.close();
				System.exit(0);
			} else if(message.startsWith("@")) {
				c.clientInfo.username = message.substring(1);
				System.out.println("Username changed to " + message.substring(1));
				c.sendHandshakeMessage("cusername:" + c.clientInfo.username);
				System.out.print(c.clientInfo.username + ": ");
			} else {
				c.send(message);
				System.out.print(c.clientInfo.username + ": ");
			}
		}
	}

	public void connect(String ipadd, int port) throws Exception {
		System.out.println("Opening port " + port + "...");
		clientConnection = new Socket(ipadd, port);
		System.out.println(" Opened!");
	}

	public void setupStreams() throws Exception {
		System.out.print("Setting up streams... ");
		incoming = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
		outgoing = new DataOutputStream(clientConnection.getOutputStream());
		System.out.print(" done!\n");
	}

	public void send(String message) throws Exception {
		outgoing.writeBytes("message:" + clientInfo.username + ": " + message + "\n");
	}

	public void sendHandshakeMessage(String message) throws Exception {
		outgoing.writeBytes(message + "\n");
	}

	public String readLine() throws Exception {
		return incoming.readLine();
	}

	public void handshake() throws Exception {
		generateHash();
		sendHandshakeMessage("hash:" + clientInfo.hashID);
		sendHandshakeMessage("username:" + clientInfo.username);
		sendHandshakeMessage("ipaddress:" + clientConnection.getLocalAddress().toString());
		sendHandshakeMessage("port:" + Integer.toString(clientConnection.getLocalPort()));

		String raw_message = "";
		String[] token = new String[3];
		
		while(true) {
			while(incoming.ready()) {
				raw_message = incoming.readLine();
				System.out.println("server " + raw_message);
				token = raw_message.split(":");
				if(token[0].equals("hash")) {
					clientInfo.remoteHash = token[1];
				} else if(token[0].equals("username")) {
					clientInfo.remoteUsername = token[1];
				} else if(token[0].equals("ipaddress")) {
					clientInfo.remoteIP = token[1];
				} else if(token[0].equals("port")) {
					clientInfo.remotePort = token[1];
				} else {
					break;
				}
			}
			if(clientInfo.remoteUsername.length() != 0 && clientInfo.remoteIP.length() != 0 && clientInfo.remoteHash.length() != 0) {
				break;
			}
		}
	}

	public void close() throws Exception {
		System.out.print("Disconnecting... ");
		clientConnection.close();
		System.out.println("Disconnected!");
	}

	public void generateHash() throws Exception {
		SecureRandom rnd = new SecureRandom();
		String rndString = new BigInteger(130, rnd).toString(32);
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.reset();
		md.update(rndString.getBytes());
		byte[] hash = md.digest();
		BigInteger bigInt = new BigInteger(1, hash);
		String hashText = bigInt.toString(16);
		while(hashText.length() < 32) {
			hashText = "0" + hashText;
		}

		clientInfo.hashID = hashText;
	}
}
