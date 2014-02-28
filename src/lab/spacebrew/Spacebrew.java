package lab.spacebrew;

import org.json.*; //https://github.com/agoransson/JSON-processing

import java.lang.reflect.Method;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Spacebrew (pure) Java library.
 * <br>Adapted from the Spacebrew Processing library
 * (see https://github.com/spacebrew/spacebrewP5/archive/master.zip)
 * (main changes are marked by slash-slash-star-slash-slash).
 * <p>Usage is simple: any client (publisher or subscriber) must implement
 * the SpacebrewClient interface and create an instance of this class, e.g.:
 * <br><code>Spacebrew cl = new Spacebrew(this);</code>
 * <br>Automatic reconnection and default subscription methods
 * (i.e., <code>onBooleanMessage</code>, <code>onRangeMessage</code>,
 * and <code>onStringMessage</code>) are supported.
 * @author Luca Mari
 * @version 22 Feb 2014
 */
public class Spacebrew {

	/**
	 * Name of client as it will appear in the Spacebrew admin
	 * @type {String}
	 */
	public String name;

	/**
	 * What does client do?
	 * @type {String}
	 */
	public String description;

	/**
	 * How loud to be (mute debug messages)
	 * @type {Boolean}
	 */
	public boolean verbose = false;

	private SpacebrewClient client; //*// added: expected to be implemented by all clients
	private String hostname = "sandbox.spacebrew.cc";
	private Integer	port = 9000;
	private Method onRangeMessageMethod, onStringMessageMethod, onBooleanMessageMethod, onOtherMessageMethod, onCustomMessageMethod;
	@SuppressWarnings("unused")	private Method onOpenMethod, onCloseMethod;
	private WsClient wsClient;
	private boolean connectionEstablished = false;
	//*//private boolean connectionRequested = false;
	//*//private Integer reconnectAttempt = 0;
	private Integer reconnectInterval = 5000;

	private JSONObject tConfig = new JSONObject();
	//*//private JSONObject nameConfig = new JSONObject(); // just unused
	private ArrayList<SpacebrewMessage> publishes, subscribes;
	private HashMap<String, HashMap<String, Method>> callbacks;
	private Timer timer = new Timer(); //*// added, to handle automatic reconnection 

	/**
	 * Setup Spacebrew and try to set up default helper functions.
	 * @param client {SpacebrewClient} reference to the Spacebrew client that implements this
	 */
	public Spacebrew(SpacebrewClient client) {
		this.client = client;
		publishes = new ArrayList<SpacebrewMessage>();
		subscribes = new ArrayList<SpacebrewMessage>();
		callbacks = new HashMap<String, HashMap<String, Method>>();
		//*//parent.registerMethod("pre", this); // substituted with the ReconnectTask below
		ReconnectTask rt = new ReconnectTask(this);
		timer.schedule(rt, 1000, reconnectInterval);
		setupMethods();   
	}

	//*// added: simple threaded task that tries reconnecting the client if required
	class ReconnectTask extends TimerTask {
		private Spacebrew sb;
		
		ReconnectTask(Spacebrew sb) { this.sb = sb; }
		
        @Override public void run() {
        	if(!connected()) { sb.connect(sb.hostname, sb.port, sb.name, sb.description); }
        }
    }

	/**
	 * Ensure that the client attempts to reconnect to Spacebrew if the connection is lost.
	 */
	//*//
	/*
	public void pre() {
		if(connectionRequested && !connectionEstablished) { // attempt to reconnect
			if(System.currentTimeMillis() - reconnectAttempt > reconnectInterval) {
				if(verbose) { System.out.println("[pre] attempting to reconnect to Spacebrew"); }
				this.connect(this.hostname, this.port, this.name, this.description);
				reconnectAttempt = (int)System.currentTimeMillis();
			}
		}
	}
	*/

	private void setupMethods() {
		try {
			onOpenMethod = getClass().getMethod("onSbOpen", new Class[]{});
		} catch(Exception e) {
			//let's not print these messages: they confuse ppl and make them think they are doing something wrong
			//System.out.println("no onSbOpen method implemented");
		}
		try {
			onCloseMethod = getClass().getMethod("onSbClose", new Class[]{});
		} catch(Exception e) {
			// System.out.println("no onSbClose method implemented");
		}    
		try {
			onRangeMessageMethod = getClass().getMethod("onRangeMessage", new Class[]{String.class, int.class});
		} catch(Exception e) {
			//System.out.println("no onRangeMessage method implemented");
		}
		try {
			onStringMessageMethod = getClass().getMethod("onStringMessage", new Class[]{String.class, String.class});
		} catch(Exception e) {
			//System.out.println("no onStringMessage method implemented");
		}
		try {
			onBooleanMessageMethod = getClass().getMethod("onBooleanMessage", new Class[]{String.class, boolean.class});
		} catch(Exception e) {
			//System.out.println("no onBooleanMessage method implemented");
		}
		try {
			onOtherMessageMethod = getClass().getMethod("onOtherMessage", new Class[]{String.class, String.class});
		} catch(Exception e) {
			//System.out.println("no onCustomMessage method implemented");
		}
		try {
			onCustomMessageMethod = getClass().getMethod("onCustomMessage", new Class[]{String.class, String.class, String.class});
		} catch(Exception e) {
			//System.out.println("no onCustomMessage method implemented");
		}
	}

	/**
	 * Setup a Boolean publisher.
	 * @param name {String}  name of route
	 * @param _default {Boolean} default value
	 */
	public void addPublish(String name, boolean _default) {
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name; 
		m.type = "boolean"; 
		if(_default) {
			m._default = "true";
		} else {
			m._default = "false";
		}
		publishes.add(m);
		if(connectionEstablished) { updatePubSub(); }
	}

	/**
	 * Setup a range publisher.
	 * @param name {String} name of route
	 * @param _default {Integer} default starting value
	 */
	public void addPublish(String name, Integer _default) {
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name; 
		m.type = "range"; 
		m._default = _default.toString();
		publishes.add(m);
		if(connectionEstablished) { updatePubSub(); }
	}

	/**
	 * Setup a string publisher.
	 * @param name {String} name of route
	 * @param _default {String} default starting value
	 */
	public void addPublish(String name, String _default) {
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name; 
		m.type = "string"; 
		m._default = _default;
		publishes.add(m);
		if(connectionEstablished) { updatePubSub(); }
	}

	/**
	 * Setup a custom or string publisher.
	 * @param name {String} name of route
	 * @param type {String} type of route ("range", "boolean", or "string")
	 * @param _default {String} default starting value
	 */
	public void addPublish(String name, String type, String _default) {
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name;
		m.type = type;
		m._default = _default;
		publishes.add(m);
		if(connectionEstablished) { updatePubSub(); }
	}

	/**
	 * Setup a custom or boolean publisher.
	 * @param name {String} name of route
	 * @param type {String} type of route ("range", "boolean", or "string")
	 * @param _default {Boolean} default starting value
	 */
	public void addPublish(String name, String type, boolean _default) {
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name; 
		m.type = type; 
		m._default = Boolean.toString(_default);
		publishes.add(m);
		if(connectionEstablished) { updatePubSub(); }
	}

	/**
	 * Setup a custom or integer-based publisher.
	 * @param name {String} name of route
	 * @param type {String}  type of route ("range", "boolean", or "string")
	 * @param _default {Boolean} default starting value
	 */
	public void addPublish(String name, String type, Integer _default) {
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name; 
		m.type = type; 
		m._default = _default.toString();
		publishes.add(m);
		if(connectionEstablished) { updatePubSub(); }
	}

	/**
	 * Add a subscriber of default name, i.e., "onBooleanMessage", "onRangeMessage",
	 * or "onStringMessage".
	 * @param name {String} name of route
	 * @param type {String} type of route ("range", "boolean", or "string")
	 */
	public void addSubscribe(String name, String type) {
		String methodName;
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name;
		m.type = type.toLowerCase();
		subscribes.add(m);

		//*// added handling
		Method method = null;
		if(type.equals("boolean")) {
			methodName = "onBooleanMessage";
			try {
				method = client.getClass().getMethod(methodName, new Class[]{boolean.class});
			} catch(Exception e) {
				System.err.println("method " + methodName + "(boolean) doesn't exist in your client.");
				System.err.println(e);
			}
		} else if(type.equals("range")) {
			methodName = "onRangeMessage";
			try {
				method = client.getClass().getMethod(methodName, new Class[]{int.class});
			} catch(Exception e) {
				System.err.println("method " + methodName + "(int) doesn't exist in your client.");
				System.err.println(e);
			}
		} else if(type.equals("string")) {
			methodName = "onStringMessage";
			try {
				method = client.getClass().getMethod(methodName, new Class[]{int.class});
			} catch(Exception e) {
				System.err.println("method " + methodName + "(String) doesn't exist in your client.");
				System.err.println(e);
			}
		}

		if(method != null) {
			if(!callbacks.containsKey(name)) { callbacks.put(name, new HashMap<String, Method>()); }
			callbacks.get(name).put(type, method);
		}

		if(connectionEstablished) { updatePubSub(); }
	}

	/**
	 * Add a subscriber and a specific callback for this route.
	 * <br>Note: routes with a specific callback don't call the default methods (e.g. onRangeMessage, etc)
	 * @param name {String} name of route
	 * @param type {String} type of route ("range", "boolean", or "string")
	 * @param methodName {String} name of method
	 */
	public void addSubscribe(String name, String type, String methodName) {
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name;
		m.type = type.toLowerCase();
		subscribes.add(m);

		//*// all client.getClass()... methods below were parent.client.getClass()
		Method method = null;
		if(type.equals("boolean")) {
			try {
				method = client.getClass().getMethod(methodName, new Class[]{boolean.class});
			} catch(Exception e) {
				System.err.println("method " + methodName + "(boolean) doesn't exist in your client.");
			}
		} else if(type.equals("range")) {
			try {
				method = client.getClass().getMethod(methodName, new Class[]{int.class});
			} catch(Exception e) {
				System.err.println("Error: method " + methodName + "(int) doesn't exist in your client.");
			}
		} else if(type.equals("string")) {
			try {
				method = client.getClass().getMethod(methodName, new Class[]{String.class});
			} catch(Exception e) {
				System.err.println("Error: method " + methodName + "(String) doesn't exist in your client.");
			}
		} else {
			try {
				method = client.getClass().getMethod(methodName, new Class[]{String.class});
			} catch(Exception e) {
				System.err.println("Error: method " + methodName + "(String) doesn't exist in your client.");
			}
		}

		if(method != null) {
			if(!callbacks.containsKey(name)) { callbacks.put(name, new HashMap<String, Method>()); }
			callbacks.get(name).put(type, method);
		}

		if(connectionEstablished) { updatePubSub(); }
	}

	/**
	 * Connect to Spacebrew admin.
	 * @param hostname {String} URL to Spacebrew host
	 * @param name {String} Name of client as it will appear in the Spacebrew admin
	 * @param description {String} What does client do?
	 */
	public void connect(String hostname, String name, String description) {
		Integer port = 9000;
		//*// commented just for simplicity now
		/*
		String[][] m = matchAll(hostname, "ws://((?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])(?:\\.(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]))*):([0-9]{1,5})");
	    if (m != null) {
			if (m[0].length == 3) {
				hostname = m[0][1];
				port = Integer.parseInt(m[0][2]);
				this.connect(hostname, port, _name, _description);
				System.err.println("Using a full websockets URL will be deprecated in future versions of the Spacebrew lib.");
				System.err.println("Pass just the host name or call the connect(host, port, name, description) instead");
			} else {
				System.err.println("Spacebrew server URL is not valid.");				
			}    
	    } else {
			this.connect(hostname, port, _name, _description);    	
	    }
		 */
		this.connect(hostname, port, name, description); //*//
	}

	/**
	 * Connect to Spacebrew admin.
	 * @param hostname {String} URL to Spacebrew host
	 * @param port {Integer} port to Spacebrew host
	 * @param name {String} Name of client as it will appear in the Spacebrew admin
	 * @param description {String} What does client do?
	 */
	public void connect(String hostname, Integer port, String name, String description) {
		this.name = name;
		this.description = description;
		this.hostname = hostname;
		this.port = port;
		//*//this.connectionRequested = true;
		try {
			if(verbose) { System.out.println("[connect] connecting to spacebrew "+ hostname); }
			wsClient = new WsClient(this, ("ws://" + hostname + ":" + Integer.toString(port)));    
			wsClient.connect();
			updatePubSub();
		}
		catch(Exception e) {
			connectionEstablished = false;
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Close the connection to Spacebrew.
	 */
	public void close() {
		if(connectionEstablished) { wsClient.close(); }
		//*//connectionRequested = false;
	}

	/**
	 * Update publishers and subscribers.
	 */
	private void updatePubSub() {
		JSONArray publishers = new JSONArray();
		for(int i = 0, len = publishes.size(); i < len; i++) {
			SpacebrewMessage m = publishes.get(i);
			JSONObject pub = new JSONObject();
			pub.put("name", m.name);
			pub.put("type", m.type);
			pub.put("default", m._default);		    
			publishers.put(pub);      
		}

		JSONArray subscribers = new JSONArray();
		for(int i = 0; i < subscribes.size(); i++) {
			SpacebrewMessage m = subscribes.get(i);
			JSONObject subs = new JSONObject();
			subs.put("name", m.name);
			subs.put("type", m.type);
			subscribers.put(subs);      
		}

		JSONObject mObj = new JSONObject();
		JSONObject tMs1 = new JSONObject();
		JSONObject tMs2 = new JSONObject();
		tMs1.put("messages", subscribers);
		tMs2.put("messages", publishers);
		mObj.put("name", name);
		mObj.put("description", description);
		mObj.put("subscribe", tMs1);
		mObj.put("publish", tMs2);
		tConfig.put("config", mObj);    

		if(connectionEstablished) { wsClient.send(tConfig.toString()); }
	}

	/**
	 * Send a message along a specified route.
	 * @param messageName {String} name of route
	 * @param type {String} type of route ("boolean", "range", "string")
	 * @param value {String} what you're sending
	 */
	public void send(String messageName, String type, String value) {
		JSONObject m = new JSONObject();
		m.put("clientName", name);
		m.put("name", messageName);
		m.put("type", type);
		m.put("value", value);

		JSONObject sM = new JSONObject();
		sM.put("message", m);

		if(connectionEstablished) { wsClient.send( sM.toString()); }
		else { System.err.println("[send] can't send message, not currently connected!"); }
	}

	/**
	 * Send a Range message along a specified route.
	 * @param messageName {String} name of route
	 * @param value {Integer} what you're sending
	 */
	public void send(String messageName, int value) {    
		String type = "range";
		for(int i = 0, len = publishes.size(); i < len; i++) {
			SpacebrewMessage m = publishes.get(i);
			if(m.name.equals(messageName)) { 
				type = m.type;
				break;
			}
		}
		this.send(messageName, type, Integer.toString(value));
	}

	/**
	 * Send a Boolean message along a specified route.
	 * @param messageName {String} Name of route
	 * @param value {boolean} What you're sending
	 */
	public void send(String messageName, boolean value) {
		String type = "boolean";
		for(int i = 0, len = publishes.size(); i < len; i++) {
			SpacebrewMessage m = publishes.get(i);
			if(m.name.equals(messageName)) { 
				type = m.type;
				break;
			}
		}
		this.send(messageName, type, Boolean.toString(value));
	}

	/**
	 * Send a String message along a specified route.
	 * @param messageName {String} Name of route
	 * @param value {String} What you're sending
	 */
	public void send(String messageName, String value) {
		String type = "string";
		for(int i = 0, len = publishes.size(); i < len; i++) {
			SpacebrewMessage m = publishes.get(i);
			if(m.name.equals(messageName)) { 
				type = m.type;
				break;
			}
		}
		this.send(messageName, type, value);
	}

	/**
	 * Get whether the client is connected.
	 * @return {boolean} is connected?
	 */
	public boolean connected() { return connectionEstablished; }

	/**
	 * Websocket callback (don't call this please!).
	 */
	public void onOpen() {
		connectionEstablished = true;
		if(verbose) { System.out.println("[onOpen] spacebrew connection open!"); }

		wsClient.send(tConfig.toString()); // send config

		//*//
		/*
		if ( onOpenMethod != null ){
			try {
				onOpenMethod.invoke( parent );
			} catch( Exception e ){
				System.err.println("[onOpen] invoke failed, disabling :(");
				onOpenMethod = null;
			}
		}
		 */
	}

	/**
	 * Websocket callback (don't call this please!).
	 */
	public void onClose() {
		//*//
		/*
		if ( onCloseMethod != null ){
			try {
				onCloseMethod.invoke( parent );
			} catch( Exception e ){
				System.err.println("[onClose] invoke failed, disabling :(");
				onCloseMethod = null;
			}
		}
		*/

		connectionEstablished = false;
		if(verbose) { System.out.println("[onClose] spacebrew connection closed."); }
	}

	/**
	 * Websocket callback (don't call this please!).
	 */
	public void onMessage(String message) {
		JSONObject m = new JSONObject(message).getJSONObject("message");

		String name = m.getString("name");
		String type = m.getString("type");
		Method method = null;

		if(callbacks.containsKey(name)) {
			if(callbacks.get(name).containsKey(type)) {
				try {
					method = callbacks.get(name).get(type);
				} catch(Exception e) {}
			}
		}

		//*// all method.invoke(client, ...) methods below were method.invoke(this, ...)  
		if(type.equals("string")) {
			if(method != null) {
				try {
					method.invoke(client, m.getString("value"));
				} catch(Exception e) {
					System.err.println("[" + method.getName() + "] invoke failed.");
				}
			} else if(onStringMessageMethod != null) {
				try {
					onStringMessageMethod.invoke(client, name, m.getString("value"));
				} catch(Exception e) {
					System.err.println("[onStringMessageMethod] invoke failed, disabling :(");
					onStringMessageMethod = null;
				}
			}
		} else if(type.equals("boolean")) {
			if(method != null) {
				try {
					method.invoke(client, m.getBoolean("value"));
				} catch(Exception e) {
					System.err.println("[" + method.getName() + "] invoke failed.");
				}
			} else if(onBooleanMessageMethod != null) {
				try {
					onBooleanMessageMethod.invoke(client, name, m.getBoolean("value"));
				} catch(Exception e) {
					System.err.println("[onBooleanMessageMethod] invoke failed, disabling :(");
					onBooleanMessageMethod = null;
				}
			}
		} else if(type.equals("range")) {
			if(method != null) {
				try {
					method.invoke(client, m.getInt("value"));
				} catch(Exception e) {
					System.err.println("[" + method.getName() + "] invoke failed.");
				}
			} else if(onRangeMessageMethod != null) {
				try {
					onRangeMessageMethod.invoke(client, name, m.getInt("value"));
				} catch(Exception e) {
					System.err.println("[onRangeMessageMethod] invoke failed, disabling :(");
					onRangeMessageMethod = null;
				}
			}
		} else {
			if(method != null) {
				try {
					method.invoke(client, m.getString("value"));
				} catch(Exception e) {
					System.err.println("[" + method.getName() + "] invoke failed.");
				}
			} else {
				if(onCustomMessageMethod != null) {
					try {
						onCustomMessageMethod.invoke(client, name, type, m.getString("value"));
					} catch(Exception e) {
						System.err.println("[onCustomMessageMethod] invoke failed, disabling :(");
						onCustomMessageMethod = null;
					}
				}
				if(onOtherMessageMethod != null) {
					try {
						onOtherMessageMethod.invoke(client, name, type, m.getString("value"));
						System.err.println("[onOtherMessageMethod] will be deprecated in future version of Spacebrew lib");
					} catch(Exception e) {
						System.err.println("[onOtherMessageMethod] invoke failed, disabling :(");
						onOtherMessageMethod = null;
					}
				}
			}
		}
	}
}
