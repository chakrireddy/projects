package message;

import java.io.Serializable;

import tvp.Node;

public class Commit implements Serializable{

	private static final long serialVersionUID = 1L;
	public Node node = null;
	public Commit(Node node) {
		this.node = node;
	}
}
