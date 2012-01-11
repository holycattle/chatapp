import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class TextArea {
	public JTextArea textArea;
	public JScrollPane scrollingArea;

	public TextArea(int rows, int columns) {
		textArea = new JTextArea(rows, columns);
		scrollingArea = new JScrollPane(textArea);
	}

}
