package it.liuc.examplesJavaSpacebrew;

import java.awt.Color;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lab.spacebrew.Spacebrew;
import lab.spacebrew.SpacebrewClient;

/**
 * Subscriber example based on Spacebrew (pure) Java library.
 * <br>It shows the usage of boolean, range, string, and custom publication methods.
 * @author Luca Mari
 * @version 22 Feb 2014
 */
@SuppressWarnings("serial")
public class FullExampleSub extends JFrame implements SpacebrewClient {
	private String hostname = "127.0.0.1"; // change it as required

	private JPanel pane = new JPanel();
	private JLabel label1 = new JLabel();
	private JLabel label2 = new JLabel();
	private JLabel label3 = new JLabel();
	private Spacebrew cl;

	public FullExampleSub() {
		super("Subscriber Example");
		setBounds(100, 100, 300, 200);
		cl = new Spacebrew(this);
		cl.addSubscribe("a Boolean subscriber", "boolean", "onBooleanReceive");
		cl.addSubscribe("a range subscriber", "range", "onRangeReceive");
		cl.addSubscribe("a string subscriber", "string", "onStringReceive");
		cl.addSubscribe("a custom subscriber", "x,y", "onCustomReceive");
		cl.connect(hostname, "myfullsubscriber", "A pure Java subscriber");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container con = this.getContentPane();
		con.add(pane);
		pane.add(label1);
		pane.add(label2);
		pane.add(label3);
		setVisible(true);
	}

	public void onBooleanReceive(boolean value) {
		pane.setBackground(value ? Color.green : Color.lightGray);
	}

	public void onRangeReceive(int value) {
		label1.setText("" + value);
	}

	public void onStringReceive(String value) {
		label2.setText(value);
	}

	public void onCustomReceive(String value) {
		label3.setText(value);
	}

	public static void main(String[] args) { new FullExampleSub(); }

}
