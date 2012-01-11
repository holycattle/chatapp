import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.io.DataOutputStream;
import java.util.Scanner;
import java.security.SecureRandom;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.awt.event.*;
import javax.swing.event.*;

public class Client {
	Socket clientConnection;
	BufferedReader incoming;
	DataOutputStream outgoing;
	AppInfo clientInfo;
	ChatUI userInterface;

	public Client() {}

	public Client(String ipadd, int port, String username) {
		try {
			userInterface = new ChatUI();
			makeEventListeners();
			userInterface.setVisible(true);
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
						userInterface.incomingMessageBox.textArea.append("Username changed to " + clientInfo.remoteUsername + "\n");
						//used to print username
					} else if(a.contains("disconnect")) {
						userInterface.incomingMessageBox.textArea.append("\rClient has disconnected.\n");
						close();
						System.exit(0);
					} else {
						userInterface.incomingMessageBox.textArea.append("\r" + delimitedMessage[1] + "\n");
						//used to print username
					}
					userInterface.incomingMessageBox.textArea.setCaretPosition(userInterface.incomingMessageBox.textArea.getDocument().getLength());
				}

				close();
				System.exit(0);
			} catch(Exception e) {}
		}
	}

	public void connect(String ipadd, int port) throws Exception {
		userInterface.incomingMessageBox.textArea.append("Opening port " + port + "...");
		clientConnection = new Socket(ipadd, port);
		userInterface.incomingMessageBox.textArea.append(" Opened!\n");
	}

	public void setupStreams() throws Exception {
		userInterface.incomingMessageBox.textArea.append("Setting up streams... ");
		incoming = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
		outgoing = new DataOutputStream(clientConnection.getOutputStream());
		userInterface.incomingMessageBox.textArea.append(" done!\nConnected!\n\n\n\n");
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
		userInterface.incomingMessageBox.textArea.append("Disconnecting... ");
		clientConnection.close();
		userInterface.incomingMessageBox.textArea.append("Disconnected!\n");
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

	public void sendMessage() {
		String message = "";
		message = userInterface.messageBox.textArea.getText();
		userInterface.messageBox.textArea.setText("");
		try {
			if(message.contains("~") || message.length() == 0) {
				userInterface.messageBox.textArea.setText("");
			} else {
				if(clientConnection.isConnected()) {
					if(message.contains("KThanksBye")) {
						sendHandshakeMessage("disconnect:");
						close();
						System.exit(0);
					} else if(message.startsWith("@")) {
						clientInfo.username = message.substring(1);
						userInterface.incomingMessageBox.textArea.append("Username changed to " + clientInfo.username + "\n");
						sendHandshakeMessage("cusername:" + clientInfo.username);
					} else {
						userInterface.incomingMessageBox.textArea.append(clientInfo.username + ": " + message + "\n");
						userInterface.incomingMessageBox.textArea.setCaretPosition(userInterface.incomingMessageBox.textArea.getDocument().getLength()); 
						send(message);
					}
				}
			}
		} catch(Exception e) {}
	}

	public void makeEventListeners() {
		ActionListener SendListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					sendMessage();
				} catch(Exception ex) {}
			}
		};
		userInterface.sendButton.addActionListener(SendListener);		
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
	}
}
