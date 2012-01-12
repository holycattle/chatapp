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

public class Server {
	ServerSocket serverConnection;
	String username;

	public Server(String username) {
		this.username = username;
	}

	public void start(int port) throws Exception {
		serverConnection = new ServerSocket(port);
	}

	public void listen() throws Exception {		
		clientConnection = serverConnection.accept();
	}

	public static void main(String[] args) throws Exception {
		Server s = new Server(args[0]);
		s.start(50000);
		
		while(true) {
			s.listen();
		}
	}
}
