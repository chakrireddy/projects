package tvp;

import java.io.Serializable;

public class Data implements Serializable{
	
	private static final long serialVersionUID = 1L;
	boolean blocked = false;
	int val = 0;
	int version = 1;
	int index = 0;
	public Data(int i) {
		index = i;
		val = 3-i;
	}
	
}
