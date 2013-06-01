package message;

import java.io.Serializable;

import tvp.Node;

public class Request implements Serializable{

	private static final long serialVersionUID = 1L;
	public Node node = null;
	public int dataNum = 0;
	public int valToWrite = 0;
	public Request(Node node, int dataNum, int valToWrite) {
		this.node = node;
		this.dataNum = dataNum;
		this.valToWrite = valToWrite;
	}
}
