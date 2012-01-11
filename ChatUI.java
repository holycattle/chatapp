import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.io.*;

public class ChatUI extends JFrame implements KeyListener {
	public TextArea messageBox;
	public TextArea incomingMessageBox;
	public JButton sendButton;
	public boolean buttonClicked;

	public ChatUI() {
		buttonClicked = false;
		messageBox = new TextArea(5, 35);
		incomingMessageBox = new TextArea(10, 35);
		incomingMessageBox.textArea.setEditable(false);
		sendButton = new JButton("Send");
		setResizable(false); //make size fixed

		messageBox.textArea.addKeyListener(this);
		buildPanes();

		this.pack();
	}

	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == e.VK_ENTER)
			sendButton.doClick(); //fix "enter" problem
	}

	public void keyTyped(KeyEvent e) {}

	public void keyReleased(KeyEvent e) {
		if(e.getKeyCode() == e.VK_ENTER)
			messageBox.textArea.setText("");
	}

	public void buildPanes() {
		JPanel mainContentPane = new JPanel();
		mainContentPane.setLayout(new BorderLayout());
		mainContentPane.add(incomingMessageBox.scrollingArea, BorderLayout.CENTER);

		JPanel bottomPane = new JPanel();
		bottomPane.setLayout(new BorderLayout());
		bottomPane.add(messageBox.scrollingArea, BorderLayout.WEST);
		bottomPane.add(sendButton, BorderLayout.EAST);

		mainContentPane.add(bottomPane, BorderLayout.SOUTH);
		this.setContentPane(mainContentPane);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public static void main(String[] args) {
		
	}

}
