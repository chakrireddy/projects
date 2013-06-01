package tvp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import message.Request;

public class TreeVotingProtocol {

	Logger logger = Logger.getLogger(TreeVotingProtocol.class.getName());
	FileHandler fileHandler = null;
	private static TreeVotingProtocol tvp = null;
	List<Node> nodeList = new ArrayList<Node>();
	List<Node> serverList = new ArrayList<Node>();
	Map<Integer,Node> serverConnections = new HashMap<Integer, Node>();
	Map<Integer,Node> clientConnections = new HashMap<Integer, Node>();
	HashMap<Integer,Boolean> repliesStatus = new HashMap<Integer,Boolean>();
	Map<Integer,Map> commitStatus = new HashMap<Integer,Map>();
	Map<Integer,Data[]> newDataLst = new HashMap<Integer,Data[]>();
	//HashMap<Integer,Boolean> commitStatus = new HashMap<Integer,Boolean>();
	Node myNode = null;
	String[] failingNodes = null;
	int numClients = 0;
	int numServers = 0;
	int numMessages = 0;
	int timeUnit = 100;
	int awaitingGrntTime = 0;
	int holdTime = 100;
	Data[] dataObj = new Data[4];
	//File dataFile = null;
	boolean active = true;
	BufferedWriter fileWriter= null;
	String[] args = null;
	List<Queue<Request>> reqQueues = new ArrayList<Queue<Request>>();
	Map<Integer,Semaphore> mutexLst = new HashMap<Integer,Semaphore>();
	Stats stats = new Stats();
	Tree tree = null;
	TreeNode rootNode = null;
	public static synchronized TreeVotingProtocol getInstance() {
		if (tvp == null) {
			tvp = new TreeVotingProtocol();
		}
		return tvp;
	}

	public static void main(String[] args) {
		TreeVotingProtocol treeVotingProtocol = TreeVotingProtocol
				.getInstance();
		treeVotingProtocol.args = args;
		treeVotingProtocol.init(args);
	}

	public void init(String[] args) {
		initializeLog(args);
		readConfigFile(args);
		//holdTime = 
		tree = new Tree();
		rootNode = tree.buildTree(1, null);
		setConnections();
		if(myNode.isClient()){
			runAlgo();
		}else if(myNode.isServer()){
			startDataListeners();
		}
	}
	
	//Initialize the log
	public void initializeLog(String[] args) {
		
		Formatter format = new SimpleFormatter();
		File logfile = new File("Log"+args[0]+".txt");
		//dataFile = new File("Data"+args[0]+".txt");
		/*try {
			fileWriter = new BufferedWriter(new FileWriter(dataFile));
		} catch (IOException e) {
			e.printStackTrace();
		}	*/	
		try {
			fileHandler = new FileHandler(logfile.getAbsolutePath());
			fileHandler.setFormatter(format);
			logger.addHandler(fileHandler);
		} catch (SecurityException e1) {
			logger.log(Level.INFO, e1.getMessage());
			e1.printStackTrace();
		} catch (IOException e1) {
			logger.log(Level.INFO, e1.getMessage());
			e1.printStackTrace();
		}
		
	}
	
	//Read the config file and populate
	public void readConfigFile(String[] args){
		logger.log(Level.INFO, "reading the config file");
		File configFile = new File("config.txt");
		BufferedReader buffFileReader = null;
		
		try {
			buffFileReader = new BufferedReader(new FileReader(configFile));
			String line = null;
			boolean socketinfo = false;
			boolean failingnodes = false;
			
			while ((line = buffFileReader.readLine()) != null) {
				if(line.charAt(0) != '#'){
					if(line.contains("SOCKINFO")){
						socketinfo = true;
					}else if(socketinfo){		
						//read nodes and add to list
						String[] nodeInfo = line.split("\\s+");
						//logger.info("nodeID: "+nodeInfo[0]+" host: "+nodeInfo[1]+" port: "+nodeInfo[2]);
						Node node = new Node(nodeInfo[1], Integer.parseInt(nodeInfo[2]), Integer.parseInt(nodeInfo[0]));
						
						if(Integer.parseInt(args[0])==node.id){
							myNode = node;				
							if(Integer.parseInt(args[1])==0){
								node.server = true;
								
							}else if(Integer.parseInt(args[1])==1){
								node.client = true;
							}
							/*Server server = new Server(node.port);
							server.start();*/
						}else if(node.id<7){
							//node.isServer()
							node.server = true;
							serverList.add(node);
							serverConnections.put(node.id, node);
							logger.info("server: "+node.id+" host: "+node.host);
							repliesStatus.put(node.id, false);
						}else {//if(node.isClient()){
							node.client = true;
							clientConnections.put(node.id, node);
						}
						nodeList.add(node);
					}else if(line.contains("FAILINGNODES")){
						failingnodes = true;
					}else if(failingnodes){
						//read failing nodes
						failingNodes = line.split("\\s+");						
					}else if(line.startsWith("NC")){
						numClients = Integer.parseInt(line.split("=")[1]);
					}else if(line.startsWith("NS")){
						numServers = Integer.parseInt(line.split("=")[1]);
					}else if(line.startsWith("M")){
						numMessages = Integer.parseInt(line.split("=")[1]);
					}else if(line.contains("TIME_UNIT")){
						timeUnit = Integer.parseInt(line.split("=")[1]);
						awaitingGrntTime = timeUnit * 20;
						/*if(Integer.parseInt(args[3]) != -1){
							holdTime  = timeUnit * Integer.parseInt(args[3]);
						}else{*/
							holdTime  = timeUnit;
						//}
					}else if(line.startsWith("HOLD_TIME")){
						holdTime = timeUnit * Integer.parseInt(line.split("=")[1]);
					}
					
				}else {
					//reset the boolean values
					socketinfo = false;
					failingnodes = false;
				}
			}
			initializeDataObj();
			Server server = new Server(myNode.port);
			server.start();
		} catch (FileNotFoundException e) {
			logger.log(Level.INFO, e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			logger.log(Level.INFO, e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void setConnections(){		
		//setconnections to server
		for (int key : serverConnections.keySet()) {
			Node serNode = serverConnections.get(key);
			Client client = new Client(serNode);
			client.connect();
			serNode.clientIns = client;
		}
		if(myNode.isServer()){
			//setconnections for clients
			for (int key : clientConnections.keySet()) {
				Node clientNode = clientConnections.get(key);
				Client client = new Client(clientNode);
				client.connect();
				clientNode.clientIns = client;
			}
		}
	}
	
	public void initializeDataObj(){
		for (int i = 0; i < 4; i++) {
			dataObj[i] = new Data(i);
			reqQueues.add(new ArrayBlockingQueue<Request>(10));
			//create a queue for the commit responses
			Map<Integer,Boolean> map = new HashMap<Integer,Boolean>();
			for (int key : clientConnections.keySet()) {
				Node clientNode = clientConnections.get(key);
				map.put(clientNode.id, false);
			}
			commitStatus.put(i, map);
			mutexLst.put(i,new Semaphore(1));
		}
	}
	
	public void runAlgo(){
		String clientZero = null;
		boolean c0 = false;
		try {
			clientZero = args[3];
			if(clientZero != null&& (Integer.parseInt(clientZero)==1)){
				c0 = true;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		int deactivateNum = (numMessages/5);
		int reactivateNum = ((2*numMessages)/5);
		for (int i = 0; i < numMessages; i++) {
			if(i == deactivateNum && c0){
				//Deactivate servers
				deActivateServers();
			}else if(i == reactivateNum && c0){
				//Reactivate servers
				reActivateServers();
			}else {
			//send request to servers
			sendRequests();
			long startTime = System.currentTimeMillis();
			//wait for replies within awaiting grant time
			waitFor(awaitingGrntTime);
			
			//check if acceptable replies are received
			if(isRepliesAcc()){
				stats.numSuccessfulAccess++;
				stats.timeMap.put(i, System.currentTimeMillis()-startTime);
				//if received then wait for holdtime
				waitFor(holdTime);
				
				//send commit message to all servers
				sendCommit();
				
			}else{
				stats.numUnSuccessfulAccess++;
				//else send withdraw message to all servers
				sendWithdraw();
				
			}
			resetReplies();
			
			//checkforAck();
		}
			//waitFor(holdTime);
			waitFor(genRandom(5, 10)*timeUnit);
		}
		stats.print();
	}
	
	private boolean isRepliesAcc(){
		/*List<int[]> intLst = new ArrayList<int[]>();
		int[] one = {0,1};
		int[] two = {0,2};
		int[] three = {1,2};
		intLst.add(one);
		intLst.add(two);
		intLst.add(three);
		for (int i = 0; i < intLst.size(); i++) {
			int[] tmp = intLst.get(i);
			int counter = 0;
			for (int j = 0; j < tmp.length; j++) {
				logger.info(" replies j: "+j+"  val: "+repliesStatus.get(j));
				if(repliesStatus.get(j)){
					counter++;
				}
			}
			if(counter==tmp.length){
				return true;
			}
		}
		return false;*/
		List<Integer> respoLst = new ArrayList<Integer>();
		for (int key : repliesStatus.keySet()) {
			if(repliesStatus.get(key)){
				respoLst.add(key);
			}
		}
		tree.response = respoLst;
		return tree.testTree(rootNode);
		
	}
	
	private void resetReplies(){
		for (int key : repliesStatus.keySet()) {
			repliesStatus.put(key, false);
		}
	}
	
	private void sendRequests(){
		//ramdomly select a datanode
		int data = genRandom(0, 3);
		//int data =  genRandom(0, 1);
		int val = genRandom(0,100);
		logger.info("sending requests with data: "+data+" val: "+val);
		for (int key : serverConnections.keySet()) {
			Node serNode = serverConnections.get(key);
			Client client = serNode.clientIns;
			client.sendRequest(myNode, data, val);
		}
	}
	private void sendWithdraw(){
		logger.info("send withdraw");
		for (int key : serverConnections.keySet()) {
			Node serNode = serverConnections.get(key);
			Client client = serNode.clientIns;
			client.sendWithdraw(myNode);
		}
	}
	
	private void sendCommit(){
		logger.info("send commit");
		for (int key : serverConnections.keySet()) {
			Node serNode = serverConnections.get(key);
			Client client = serNode.clientIns;
			client.sendCommit(myNode);
		}
	}
	
	private void waitFor(int time){
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private int genRandom(int min, int max){
		int val = min + (int)(Math.random() * ((max - min) + 1));
		return val;
	}
	
	public void startDataListeners(){
		for (int i = 0; i < dataObj.length; i++) {
//		for (int i = 0; i < 2; i++) {
			Data dataOb = dataObj[i];
			DataListener dataList = new DataListener(dataOb, reqQueues.get(i), i);
			Thread dataThrd = new Thread(dataList);
			dataThrd.start();
		}
		/*Data dataOb = dataObj[0];
		DataListener dataList = new DataListener(dataOb, reqQueues.get(0), 0);
		Thread dataThrd = new Thread(dataList);
		dataThrd.start();*/
	}
	
	class DataListener implements Runnable{
		
		Data data = null;
		Queue<Request> reqQueue = null;
		int index = 0;
		Semaphore mutex = null;
		public DataListener(Data data, Queue<Request> reqQueue, int index) {
			this.data = data;
			this.reqQueue = reqQueue;
			this.index = index;
			this.mutex = mutexLst.get(index);
		}
		
		@Override
		public void run() {
			//logger.info("Thread running for: "+index);
			while(true){
				try {
					mutex.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				//logger.info("index: "+commitStatus.get(index).get(request.node.id));
				if(reqQueue.peek()!=null && active){
					Request request = reqQueue.peek();
					Client client = clientConnections.get(request.node.id).clientIns;
					if(!data.blocked){
						logger.info("Grant to "+request.node.id+" on data object: "+index);
						client.sendGrant(myNode);
						data.blocked = true;
					}else if((Boolean)commitStatus.get(index).get(request.node.id)){
					//else if(commitStatus.get(index)) {
						//got commit,, update the data value and increment version
						//logger.info("Commit received from: "+request.node.id+" on object: "+index);
						logger.info("*********** update data value to: "+request.valToWrite);
						data.val = request.valToWrite;
						/*try {
							fileWriter.append(""+data.val);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}*/
						//fileWriter.append(data.val);
						data.version++;
						//remove from queue and send Ack
						reqQueue.poll();
						commitStatus.get(index).put(request.node.id, false);
						//commitStatus.put(index, false);
						client.sendAck(myNode);
						data.blocked = false;
					}
					//printStatus(reqQueue, data, index);
				}
				//printStatus(reqQueue, data, index);
				mutex.release();
					Thread.yield();	
			}
		}
		
		/*private void printStatus(Queue<Request> queue, Data data, int index){
			logger.info("--------------------------------");
			logger.info("index: "+index);
			logger.info("data value: "+data.val);
			for (Request request : queue) {
				logger.info(" queue: "+request.node.id+" value: "+request.valToWrite);
			}
			logger.info("////////////////////////////////");
		}*/
		
	}
	
	public synchronized void updateDataObj(Data[] dataArr){
		for (int i = 0; i < dataArr.length; i++) {
			Data newData = dataArr[i];
			Data curData = dataObj[i];
			if(newData.version >= curData.version){
				curData.val = newData.val;
				curData.version = newData.version;
			}
		}
	}
	
	public void deActivateServers(){
		for (int key : tvp.serverConnections.keySet()) {
			Node nd = tvp.serverConnections.get(key);
			boolean failing = false;
			for (String strng : tvp.failingNodes) {
				if(nd.id == Integer.parseInt(strng)){
					failing = true;
				}
			}
			if(failing){
			//TODO send the request for datasets
				logger.info("sending deactivate to: "+nd.id);
				nd.clientIns.sendDeactivate();
			}						
		}
	}
	
	public void reActivateServers(){
		for (int key : tvp.serverConnections.keySet()) {
			Node nd = tvp.serverConnections.get(key);
			boolean failing = false;
			for (String strng : tvp.failingNodes) {
				if(nd.id == Integer.parseInt(strng)){
					failing = true;
				}
			}
			if(failing){
			//TODO send the request for datasets
				logger.info("sending reactivate to: "+nd.id);
				nd.clientIns.sendReactivate();
			}						
		}
	}
}
