package message;

import java.io.Serializable;

import tvp.Node;

public class PollRequest implements Serializable{

	private static final long serialVersionUID = 1L;
	public Node myNode = null;
	public PollRequest(Node myNode) {
		this.myNode = myNode;
	}

}
