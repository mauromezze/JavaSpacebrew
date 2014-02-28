package it.liuc.examplesJavaSpacebrew;

import java.awt.Color;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lab.spacebrew.Spacebrew;
import lab.spacebrew.SpacebrewClient;

/**
 * Simple subscriber example based on Spacebrew (pure) Java library.
 * <br>It shows the usage of both default and explicit subscription methods.
 * @author Luca Mari
 * @version 22 Feb 2014
 */
@SuppressWarnings("serial")
public class ExampleSub extends JFrame implements SpacebrewClient {
	private String hostname = "127.0.0.1"; // change it as required

	private JPanel pane = new JPanel();
	private JLabel label = new JLabel();
	private Spacebrew cl;

	public ExampleSub() {
		super("Subscriber Example");
		setBounds(100, 100, 300, 200);
		cl = new Spacebrew(this);
		cl.addSubscribe("a default Boolean subscriber", "boolean");
		cl.addSubscribe("a Boolean subscriber", "boolean", "onReceive");
		cl.connect(hostname, "mysubscriber", "A simple pure Java subscriber");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container con = this.getContentPane();
		con.add(pane);
		pane.add(label);
		setVisible(true);
	}

	public void onBooleanMessage(boolean value) {
		label.setText(value ? "click" : "");
	}

	public void onReceive(boolean value) {
		pane.setBackground(value ? Color.green : Color.lightGray);
	}

	public static void main(String[] args) { new ExampleSub(); }

}
