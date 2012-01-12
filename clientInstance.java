import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataOutputStream;
import java.util.Scanner;
import java.security.SecureRandom;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.awt.event.*;
import javax.swing.event.*;

public class clientInstance extends Thread {
	ChatUI userInterface;
	ServerSocket serverConnection;
	Socket clientConnection;
	BufferedReader incoming;
	DataOutputStream outgoing;
	AppInfo serverInfo;

	public clientInstance(String username, ServerSocket serverConnection, Socket clientConnection) {
		this.clientConnection = clientConnection;
		this.serverConnection = serverConnection;
		serverInfo = new AppInfo(username);
	}

	public void run() {
		try {
			userInterface = new ChatUI();
			makeEventListeners();
			userInterface.setVisible(true);
			
		} catch(Exception e) {}
		(new MessageListenerThread()).start();
	}

	class MessageListenerThread extends Thread {
		public void run() {
			try {
				generateHash();			
				setupStreams();
				handshake();
				System.out.println("done setting up");
				while(true) {
					String incomingMessage = incoming.readLine();
					if(incomingMessage == null) {
						break;
					}
					String[] delimitedMessage = incomingMessage.split("message:");

					if(incomingMessage.startsWith("cusername:")) {
						serverInfo.remoteUsername = incomingMessage.substring(10);
						userInterface.incomingMessageBox.textArea.append("Username changed to " + serverInfo.remoteUsername + "\n");
					} else if(incomingMessage.contains("disconnect")) {
						userInterface.incomingMessageBox.textArea.append("\rClient has disconnected.\n");
						close();
						System.exit(0);
					} else {
						userInterface.incomingMessageBox.textArea.append("\r" + delimitedMessage[1] + "\n");
						//this line used to print the server's username
					}
					userInterface.incomingMessageBox.textArea.setCaretPosition(userInterface.incomingMessageBox.textArea.getDocument().getLength());
				}

				close();
				System.exit(0);
			} catch(Exception e) {}
		}
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

	public void sendMessage() {
		String message = "";
		message = userInterface.messageBox.textArea.getText();
		userInterface.messageBox.textArea.setText("");
		try {
			if(message.contains("~") || message.length() == 0) {
				userInterface.messageBox.textArea.setText("");
			} else {
				if(serverConnection.isBound()) {
					if(message.contains("KThanksBye")) {
						sendHandshakeMessage("disconnect:");
						close();
						System.exit(0);
					} else if(message.startsWith("@")) {
						serverInfo.username = message.substring(1);
						userInterface.incomingMessageBox.textArea.append("Username changed to " + serverInfo.username + "\n");
						sendHandshakeMessage("cusername:" + serverInfo.username);
					} else {
						userInterface.incomingMessageBox.textArea.append(serverInfo.username + ": " + message + "\n");
						userInterface.incomingMessageBox.textArea.setCaretPosition(userInterface.incomingMessageBox.textArea.getDocument().getLength()); 
						send(message);						
					}
				}
			}
		} catch(Exception e) {}
	}

	public void handshake() throws Exception {
		System.out.println("handshake");
		String raw_message = "";
		String[] token = new String[3];
		
		userInterface.incomingMessageBox.textArea.append("Getting info from client... ");
		
		while(true) {
			System.out.println(raw_message);
			while(incoming.ready()) {
				raw_message = incoming.readLine();
				System.out.println(raw_message);
				token = raw_message.split(":");
				
				if(token[0].equals("hash")) {
					System.out.println("hash token received");
					sendHandshakeMessage("hash:" + serverInfo.hashID);
					System.out.println("hash token sent");
					serverInfo.remoteHash = token[1];
				} else if(token[0].equals("username")) {
					System.out.println("username token received");
					sendHandshakeMessage("username:" + serverInfo.username);
					System.out.println("username token sent");
					serverInfo.remoteUsername = token[1];
				} else if(token[0].equals("ipaddress")) {
					System.out.println("ipaddress token received");
					sendHandshakeMessage("ipaddress:" + serverConnection.getInetAddress().toString());
					System.out.println("ipaddress token sent");
					serverInfo.remoteIP = token[1];
				} else if(token[0].equals("port")) {
					System.out.println("port token received");
					sendHandshakeMessage("port:" + Integer.toString(serverConnection.getLocalPort()));
					System.out.println("port token sent");
					serverInfo.remotePort = token[1];
				} else break;
			}
			System.out.println("waiting for message from client again...");
			if(serverInfo.remoteUsername.length() != 0 && serverInfo.remoteIP.length() != 0 && serverInfo.remoteHash.length() != 0) {
				System.out.println("handshake not completed");
				break;
			}
		}
		userInterface.incomingMessageBox.textArea.append("Done!\n");
	}

	public void setupStreams() throws Exception {
		userInterface.incomingMessageBox.textArea.append("Setting up streams... ");
		incoming = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
		outgoing = new DataOutputStream(clientConnection.getOutputStream());
		userInterface.incomingMessageBox.textArea.append(" done!\n");
	}

	public void generateHash() throws Exception {
		System.out.print("Generating hash... ");
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
		serverInfo.hashID = hashText;
		System.out.println("hash done!");
	}

	public void send(String message) throws Exception {
		System.out.print("sending message... ");
		outgoing.writeBytes("message:" + serverInfo.username + ": " + message + "\n");
		System.out.println("message sent!");
	}

	public void sendHandshakeMessage(String message) throws Exception {
		System.out.print("shaking hand... ");
		outgoing.writeBytes(message + "\n");
		System.out.println("handshake sent!");
	}

	public String readLine() throws Exception {
		return incoming.readLine();
	}

	public void close() throws Exception {
		userInterface.incomingMessageBox.textArea.append("Disconnecting... ");
		serverConnection.close();
		clientConnection.close(); //change such that it closes a specific client
		userInterface.incomingMessageBox.textArea.append("Disconnected!\n");
	}
}
