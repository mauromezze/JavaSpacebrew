package it.liuc.examplesJavaSpacebrew;

import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import lab.spacebrew.Spacebrew;
import lab.spacebrew.SpacebrewClient;

/**
 * Simple publisher example based on Spacebrew (pure) Java library.
 * @author Luca Mari
 * @version 22 Feb 2014
 */
@SuppressWarnings("serial")
public class ExamplePub extends JFrame implements SpacebrewClient, MouseListener {
	private String hostname = "127.0.0.1"; // change it as required

	JPanel pane = new JPanel();
	JButton button = new JButton("Click Me");
	Spacebrew cl;

	public ExamplePub() {
		super("Publisher Example");
		setBounds(100, 100, 300, 200);
		cl = new Spacebrew(this);
		cl.addPublish("a Boolean publisher", false);
		cl.connect(hostname, "mypublisher", "A simple pure Java publisher");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container con = this.getContentPane();
		con.add(pane);
		button.addMouseListener(this);
		pane.add(button);
		setVisible(true);
	}

	@Override public void mousePressed(MouseEvent e) { cl.send("a Boolean publisher", true); }

	@Override public void mouseReleased(MouseEvent e) { cl.send("a Boolean publisher", false); }

	@Override public void mouseClicked(MouseEvent e) {}
	@Override public void mouseEntered(MouseEvent e) {}
	@Override public void mouseExited(MouseEvent e) {}

	public static void main(String[] args) {
		new ExamplePub();
	}

}
