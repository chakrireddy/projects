package tvp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.Commit;
import message.Deactivate;
import message.Grant;
import message.Info;
import message.PollReply;
import message.PollRequest;
import message.Reactivate;
import message.Request;
import message.Withdraw;

public class Server extends Thread {

	int port = 0;
	ServerSocket serverSocket = null;
	TreeVotingProtocol tvp = null;
	Logger logger = Logger.getLogger("Server.class");
	FileHandler handler = null;
	Listener sockListener = null;

	public Server(int port) {
		this.port = port;
		this.tvp = TreeVotingProtocol.getInstance();
		logger.addHandler(tvp.fileHandler);
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			logger.log(Level.INFO, ioe.getMessage());
			System.exit(1);
		}
		logger.log(Level.INFO, "server started on "+port);
		while (true) {
			try {
				Socket socket = serverSocket.accept();
				
				Listener listener = new Listener(socket, this);
				this.sockListener = listener;
				listener.start();				
			} catch (IOException e) {
				e.printStackTrace();
				logger.log(Level.INFO, e.getMessage());
			}
		}
	}
}

class Listener extends Thread {
	Server server = null;
	Socket socket = null;
	ObjectInputStream ois = null;
	Node node = null;
	Request request = null;
	TreeVotingProtocol tvp = TreeVotingProtocol.getInstance();

	public Listener(Socket socket, Server server) {
		this.socket = socket;
		this.server = server;
		try {
			ois = new ObjectInputStream(socket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
			server.logger.log(Level.INFO, e.getMessage());
			//TODO remove system exit code
			//System.exit(1);
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				Object obj = ois.readObject();
				if(obj instanceof Info){
					node = ((Info)obj).node;
				}else if((obj instanceof Grant)&& tvp.active){
					tvp.stats.numofMessagesEx++;
					Grant grnt = (Grant) obj;
					tvp.repliesStatus.put(grnt.node.id, true);
				}else if((obj instanceof Request)&& tvp.active){
					tvp.stats.numofMessagesEx++;
					request = (Request)obj;
					acquireLock(tvp.mutexLst.get(request.dataNum));				
					tvp.reqQueues.get(request.dataNum).add(request);
					releaseLock(tvp.mutexLst.get(request.dataNum));
					/*if(tvp.dataObj[request.dataNum].blocked){
						//data object is blocked
						
					}else {
						//send grnt to client
						tvp.dataObj[request.dataNum].blocked = true;
						Node clientNd = tvp.clientConnections.get(request.node.id);
						clientNd.clientIns.sendGrant(tvp.myNode);
						
					}*/
				}else if((obj instanceof Commit)&& tvp.active){
					tvp.stats.numofMessagesEx++;
					//Commit commit = (Commit)obj;
					//tvp.commitStatus.put(request.dataNum, true);
					tvp.commitStatus.get(request.dataNum).put(request.node.id, true);
					//update the datavalue with the data in request
					//remove from the queue
					//update the blocked status
					//send Ack to client
				}else if((obj instanceof Withdraw)&& tvp.active){
					tvp.stats.numofMessagesEx++;
					Withdraw withdraw = (Withdraw)obj;
					//withdraw.node.id
					acquireLock(tvp.mutexLst.get(request.dataNum));
					server.logger.info("received withdraw message from: "+request.node.id);
					Queue<Request> reqQueue = tvp.reqQueues.get(request.dataNum);
					reqQueue.remove(request);
					releaseLock(tvp.mutexLst.get(request.dataNum));
				}else if(obj instanceof Deactivate){
					tvp.stats.numofMessagesEx++;
					tvp.active = false;
					server.logger.info("System deactivated");
					// remove messages from all queues
					//for each mutex lock do remove req and reset commit queues
					for (int key : tvp.mutexLst.keySet()) {
						Semaphore sema = tvp.mutexLst.get(key);
						acquireLock(sema);
						Queue<Request> rq = tvp.reqQueues.get(key);
						rq.clear();
						Map<Integer, Boolean> commMap = tvp.commitStatus.get(key);
						for (int comKey : commMap.keySet()) {
							commMap.put(comKey,false);
						}
						Data data = tvp.dataObj[key];
						data.version = 1;
						data.val = 3-data.index;
						releaseLock(sema);
					}
					//Donothing
				}else if(obj instanceof Reactivate){
					tvp.stats.numofMessagesEx++;
					tvp.active = true;
					server.logger.info("System reactivated");
					//TODO request to nonfailing servers for datasets
					for (int key : tvp.serverConnections.keySet()) {
						Node nd = tvp.serverConnections.get(key);
						boolean failing = true;
						for (String strng : tvp.failingNodes) {
							if(nd.id == Integer.parseInt(strng)){
								failing = false;
							}
						}
						if(!failing){
						//TODO send the request for datasets
							nd.clientIns.sendDataRequest(tvp.myNode);
						}						
					}
				}else if(obj instanceof PollRequest){
					tvp.stats.numofMessagesEx++;
					//TODO send data sets
					server.logger.info("received poll request message from node.id: "+node.id);
					Node clientNd = tvp.clientConnections.get(node.id);
					server.logger.info("send data objects to  node.id: "+node.id);
					clientNd.clientIns.sendDataResponse(tvp.dataObj);
					
				}else if(obj instanceof PollReply){
					tvp.stats.numofMessagesEx++;
					//TODO populate datasets, compare datasets and update.
					PollReply pollReply = (PollReply) obj;
					tvp.updateDataObj(pollReply.dataArr);
					
				}
			} catch (IOException e) {
				//e.printStackTrace();
				//server.logger.log(Level.INFO, e.getMessage());
				closeConnection();
			} catch (ClassNotFoundException e) {
				//e.printStackTrace();
				//server.logger.log(Level.INFO, e.getMessage());
			}
			Thread.yield();
		}
	}
	
	private void acquireLock(Semaphore mutex){
		try {
			mutex.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void releaseLock(Semaphore mutex){
		mutex.release();
	}
	
	private void closeConnection() {
		try {
			ois.close();
		} catch (IOException e) {
		}
		try {
			socket.close();
		} catch (IOException e) {
		}
	}
	
	/*private void printState(){
		//Print the request queue
		server.logger.log(Level.INFO, "-------- Request Queue -------");
		for (Request request : ma.reqQueue) {
			server.logger.log(Level.INFO, request.toString());
		}
		//Print the granted node
		server.logger.log(Level.INFO, "-------- Request Granted -------");
		if(ma.reqGrnt != null){
			server.logger.log(Level.INFO, ma.reqGrnt.toString());
		}
		//Print the replies
		server.logger.log(Level.INFO, "-------- Replies Map -------");
		for (int key : ma.repliesStatus.keySet()) {
			server.logger.log(Level.INFO, "nodeid: "+key+" value: "+ma.repliesStatus.get(key));
		}
		//Print all other status message
		server.logger.log(Level.INFO, "-------- InCS: "+ma.inCS);
	}*/
}
