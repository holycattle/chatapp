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

public class serverClientInstance {
	ChatUI userInterface;
	Socket clientConnection;
	BufferedReader incoming;
	DataOutputStream outgoing;
	AppInfo serverInfo;

	public serverClientInstance(Socket clientConnection) {
		this.clientConnection = clientConnection;
		userInterface = new ChatUI();
		userInterface.setVisible(true);
		makeEventListeners();
	}

	class MessageListenerThread extends Thread {
		public void run() {
			try {
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
		String raw_message = "";
		String[] token = new String[3];
		generateHash();

		while(true) {
			while(incoming.ready()) {
				raw_message = incoming.readLine();
				token = raw_message.split(":");
				if(token[0].equals("hash")) {
					sendHandshakeMessage("hash:" + serverInfo.hashID);
					serverInfo.remoteHash = token[1];
				} else if(token[0].equals("username")) {
					sendHandshakeMessage("username:" + serverInfo.username);
					serverInfo.remoteUsername = token[1];
				} else if(token[0].equals("ipaddress")) {
					sendHandshakeMessage("ipaddress:" + serverConnection.getInetAddress().toString());
					serverInfo.remoteIP = token[1];
				} else if(token[0].equals("port")) {
					sendHandshakeMessage("port:" + Integer.toString(serverConnection.getLocalPort()));
					serverInfo.remotePort = token[1];
				} else break;
			}
			
			if(serverInfo.remoteUsername.length() != 0 && serverInfo.remoteIP.length() != 0 && serverInfo.remoteHash.length() != 0) {
				break;
			}
		}
	}

	public void setupStreams() throws Exception {
		userInterface.incomingMessageBox.textArea.append("Setting up streams... ");
		incoming = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
		outgoing = new DataOutputStream(clientConnection.getOutputStream());
		userInterface.incomingMessageBox.textArea.append(" done!\nConnected!\n\n\n\n");
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

		serverInfo.hashID = hashText;
	}

	public void send(String message) throws Exception {
		outgoing.writeBytes("message:" + serverInfo.username + ": " + message + "\n");
	}

	public void sendHandshakeMessage(String message) throws Exception {
		outgoing.writeBytes(message + "\n");
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
