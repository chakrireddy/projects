package message;

import java.io.Serializable;

import tvp.Data;

public class PollReply implements Serializable{

	private static final long serialVersionUID = 1L;
	public Data[] dataArr= null;
	public PollReply(Data[] dataArr) {
		this.dataArr = dataArr;
	}

}
