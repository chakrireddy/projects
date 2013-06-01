package message;

import java.io.Serializable;

import tvp.Node;

public class Grant implements Serializable{

	private static final long serialVersionUID = 1L;
	public Node node = null;	
	public Grant(Node node) {
		this.node = node;
	}
}
