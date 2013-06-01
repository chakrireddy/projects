package message;

import java.io.Serializable;

import tvp.Node;

public class Withdraw implements Serializable{

	private static final long serialVersionUID = 1L;
	public Node node = null;
	public Withdraw(Node node) {
		this.node = node;
	}
}
