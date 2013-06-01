package message;

import java.io.Serializable;

import tvp.Node;

public class Ack implements Serializable{

	private static final long serialVersionUID = 1L;
	public Node node = null;
	public Ack(Node node) {
		this.node = node;
	}

}
