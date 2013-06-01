package tvp;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.Ack;
import message.Commit;
import message.Deactivate;
import message.Grant;
import message.Info;
import message.PollReply;
import message.PollRequest;
import message.Reactivate;
import message.Request;
import message.Withdraw;

public class Client{
	
	Socket socket = null;
	Node node = null;
	TreeVotingProtocol tvp = null;
	Logger logger = Logger.getLogger(Client.class.getName());
	FileHandler handler = null;
	ObjectOutputStream oos = null;
	
	public Client(Node node) {
		this.node = node;
		this.tvp = TreeVotingProtocol.getInstance();
		logger.addHandler(tvp.fileHandler);
	}
	
	public synchronized void connect() {
		
		while (socket == null) {
			try {
				socket = new Socket(node.host, node.port);				
			} catch (UnknownHostException e) {
				// e.printStackTrace();
			} catch (IOException e) {
				// e.printStackTrace();
			}
		}
		//logger.log(Level.INFO, "connection established with node: "+node.id);
		try {
			oos = new ObjectOutputStream(socket.getOutputStream());
			oos.flush();
			logger.info("connected to: "+node.host);
			sendInfo();
		} catch (IOException e) {
			logger.log(Level.INFO, e.getMessage());
			e.printStackTrace();
		}
		return;
	}
	
	public void sendInfo(){
		try {
			oos.writeObject(new Info(tvp.myNode));
			oos.flush();
		} catch (IOException e) {
			logger.log(Level.INFO, e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void close(){
		try {
			socket.close();
		} catch (IOException e) {
			logger.log(Level.INFO, e.getMessage());
			e.printStackTrace();
		}
	}

	public void sendRequest(Node myNode, int data, int val) {
		try {
			tvp.stats.numofMessagesEx++;
			oos.writeObject(new Request(myNode, data, val));
			oos.flush();
		} catch (IOException e) {
			logger.log(Level.INFO, e.getMessage());
			e.printStackTrace();
		}
	}

	public void sendCommit(Node myNode) {
		try {
			tvp.stats.numofMessagesEx++;
			oos.writeObject(new Commit(myNode));
			oos.flush();
		} catch (IOException e) {
			logger.log(Level.INFO, e.getMessage());
			e.printStackTrace();
		}
	}

	public void sendWithdraw(Node myNode) {
		try {
			tvp.stats.numofMessagesEx++;
			oos.writeObject(new Withdraw(myNode));
			oos.flush();
		} catch (IOException e) {
			logger.log(Level.INFO, e.getMessage());
			e.printStackTrace();
		}
	}

	public void sendGrant(Node myNode) {
		try {
			tvp.stats.numofMessagesEx++;
			oos.writeObject(new Grant(myNode));
			oos.flush();
		} catch (IOException e) {
			logger.log(Level.INFO, e.getMessage());
			e.printStackTrace();
		}
	}

	public void sendAck(Node myNode) {
		try {
			tvp.stats.numofMessagesEx++;
			oos.writeObject(new Ack(myNode));
			oos.flush();
		} catch (IOException e) {
			logger.log(Level.INFO, e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void sendDataRequest(Node myNode){
		try {
			tvp.stats.numofMessagesEx++;
			oos.writeObject(new PollRequest(myNode));
			oos.flush();
		} catch (IOException e) {
			logger.log(Level.INFO, e.getMessage());
			e.printStackTrace();
		}	
	}

	public void sendDataResponse(Data[] dataObj) {
		try {
			tvp.stats.numofMessagesEx++;
			oos.writeObject(new PollReply(dataObj));
			oos.flush();
		} catch (IOException e) {
			logger.log(Level.INFO, e.getMessage());
			e.printStackTrace();
		}		
	}

	public void sendDeactivate() {
		try {
			tvp.stats.numofMessagesEx++;
			oos.writeObject(new Deactivate());
			oos.flush();
		} catch (IOException e) {
			logger.log(Level.INFO, e.getMessage());
			e.printStackTrace();
		}	
		
	}

	public void sendReactivate() {
		try {
			tvp.stats.numofMessagesEx++;
			oos.writeObject(new Reactivate());
			oos.flush();
		} catch (IOException e) {
			logger.log(Level.INFO, e.getMessage());
			e.printStackTrace();
		}			
	}
}
