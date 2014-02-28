package it.liuc.examplesJavaSpacebrew;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import lab.spacebrew.Spacebrew;
import lab.spacebrew.SpacebrewClient;

/**
 * Publisher example based on Spacebrew (pure) Java library.
 * <br>It shows the usage of boolean, range, string, and custom publication methods.
 * @author Luca Mari
 * @version 22 Feb 2014
 */
@SuppressWarnings("serial")
public class FullExamplePub extends JFrame implements SpacebrewClient, MouseListener, MouseMotionListener, ChangeListener, KeyListener {
	private String hostname = "127.0.0.1"; // change it as required

	JPanel pane = new JPanel();
	JButton button = new JButton("Click Me");
	JSlider slider = new JSlider(SwingConstants.HORIZONTAL, 0, 1023, 0); 
	JTextField text = new JTextField("");

	Spacebrew cl;

	public FullExamplePub() {
		super("Publisher Example");
		setBounds(100, 100, 300, 200);
		cl = new Spacebrew(this);
		cl.addPublish("a Boolean publisher", false);
		cl.addPublish("a range publisher", 0);
		cl.addPublish("a string publisher", "");
		cl.addPublish("a custom publisher", "x,y", "");
		cl.connect(hostname, "myfullpublisher", "A pure Java publisher");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container con = this.getContentPane();
		con.add(pane);
		button.addMouseListener(this);
		slider.addChangeListener(this);
		text.addKeyListener(this);
		pane.addMouseMotionListener(this);
		text.setPreferredSize(new Dimension(200, 20));
		pane.add(button);
		pane.add(slider);
		pane.add(text);
		setVisible(true);
	}

	@Override public void mousePressed(MouseEvent e) { cl.send("a Boolean publisher", true); }

	@Override public void mouseReleased(MouseEvent e) { cl.send("a Boolean publisher", false); }

	@Override public void stateChanged(ChangeEvent e) { cl.send("a range publisher", slider.getValue()); }
	
	@Override public void keyTyped(KeyEvent e) { cl.send("a string publisher", text.getText()); }
	
	@Override public void mouseMoved(MouseEvent e) { cl.send("a custom publisher", e.getX() + "," + e.getY()); }

	public static void main(String[] args) {
		new FullExamplePub();
	}

	@Override public void mouseClicked(MouseEvent e) {}
	@Override public void mouseEntered(MouseEvent e) {}
	@Override public void mouseExited(MouseEvent e) {}
	@Override public void mouseDragged(MouseEvent e) {}
	@Override public void keyPressed(KeyEvent e) {}
	@Override public void keyReleased(KeyEvent e) {}

}
