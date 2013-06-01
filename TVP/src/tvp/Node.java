package tvp;

import java.io.Serializable;

public class Node implements Serializable{

	private static final long serialVersionUID = 1L;
	String host = null;
	int port = 0;
	public int id = 0;
	boolean me = false;
	boolean server = false;
	boolean client = false;
	Client clientIns = null;
	public Node(String host, int port, int id) {
		this.host = host;
		this.port = port;
		this.id = id;
	}
	
	public boolean isClient() {
		return client;
	}
	
	public boolean isServer() {
		return server;
	}
}
