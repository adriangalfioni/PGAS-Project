package modelPGAS;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;


/**
 * Class that provides facilities to send and receive messages between processes
 * and delivers read or write requests from distributed data structure.
 * It encapsulates used transport protocol.
 * 
 * @author adrian
 *
 */
public class MiddlewareUDP extends Thread{

	// List with connected IPAddresses
	private LinkedList<IPAddress> connectedAddresses = new LinkedList<>();
	private DistributedArray distributedArray;
	
	private final Object monitor = new Object();
	
	boolean finishSortValue = true;
	boolean barrierReached = false;
	boolean initMessageReceived = false;
	boolean continueMessageReceived = false;
	boolean readMessageReceived = false;
	boolean andReduceMessageReceived = false;
	private int messagesReceived = 0;
	private int readedValue;
		
    private DatagramSocket listenerSocket;
	
    private Sorter sorter;
	private int currentProcessId;
	private int currentProcessPort;
	private IPAddress leaderAddress;
	private int totalProcessesCount;
	
	
	/**
	 * Middleware constructor that obtains socket to listen
	 * to incoming messages and sets class required fields
	 * @param sorterProcces thats instantiate middleware 
	 * @throws SocketException
	 */
    public MiddlewareUDP(Sorter sorterProcces) throws SocketException {      
    	
    	totalProcessesCount = sorterProcces.getTotalProcesses();
    	
    	// Leader process info
    	leaderAddress = sorterProcces.getLeaderAddress();
    	InetAddress leaderIp = leaderAddress.getIp();
    	int leaderPort = leaderAddress.getPortUDP();
    	
    	// Current process info
    	currentProcessId = sorterProcces.getLocalProcessId();
    	currentProcessPort = leaderPort + currentProcessId;
    	
    	// Obtain socket to receive incoming messages
    	listenerSocket = new DatagramSocket(leaderPort + currentProcessId, leaderIp);
    	
    	// Add leader address to connected IPAddresses list
    	connectedAddresses.add(leaderAddress);
    	
    	sorter = sorterProcces;
    }
    
    /**
     * Global communication operation to set all connected processes
     * @throws SocketException
     * @throws IOException
     */
    public void setAllAddresses() throws SocketException, IOException {
    	// If i am leader process
    	if(currentProcessId == 0) {
    		// Leader process wait until receive a message by
    		// all others processes with their IP and PORT
    		waitAllProcessesMessages();
    		
    		String msg = "";
    		for(int i = 1; i < connectedAddresses.size(); i++) {
        		msg += connectedAddresses.get(i).getPortUDP() + ",";
        	}
    		broadcast(Messages.MSG_INIT + "," + msg);
    	}else {
    		// Notify to leader process new connection
    		String msg = Messages.MSG_NEW_CONECTION+","+currentProcessPort+",";
    		sendToLeaderProcess(msg);
        	
        	// Wait until init message received from leader process
        	waitInitMsg();
    	}
    }
    
    /**
     * Global communication operation to synchronize all processes in barrier step
     * @throws IOException
     * @throws SocketException
     */
    public void barrier() throws IOException, SocketException {
    	// If i am leader process
    	if(currentProcessId == 0) {
    		// Leader process wait until receive a message by
    		// all others processes when they reach the barrier
    		waitAllProcessesMessages();
    		
    		broadcast(Messages.MSG_CONTINUE + ",");
    	}else {
    		// Notify to leader process that barrier is reached
    		String msg = Messages.MSG_BARRIER_REACHED+","+currentProcessPort+",";
    		sendToLeaderProcess(msg);
    		
    		// Wait until continue message received from leader process
    		waitContinueMsg();
    	}
    }
    
    
    /**
     * Global communication operation to synchronize all processes in andReduce step
     * @param finish value that is true iff swap operation
     * was not necessary because local array was sorted
     * @return value obtained by reduce all values applying 'and' operator
     * @throws IOException
     * @throws SocketException
     */
    public boolean andReduce(boolean finish) throws IOException, SocketException {
    	// If i am leader process
    	if(currentProcessId == 0) {
    		// Leader process wait until receive a message by
    		// all others processes when they reach the 'and reduce'
    		waitAllProcessesMessages();
    		
    		// Reduce all values applying and operator
    		boolean andReduceValue = finishSortValue && finish;
    		
    		int finished = andReduceValue ? 1 : 0;
    		broadcast(Messages.MSG_REDUCE_CONTINUE + "," + finished + ",");
    		
    		// Restart value
    		finishSortValue = true;

    		return andReduceValue;
    	}else {
    		// Notify to leader process that 'and reduce' is reached with local reduce value
    		int finished = finish ? 1 : 0;
    		String msg = Messages.MSG_AND_REDUCE_REACHED+","+currentProcessPort+","+finished+",";
    		sendToLeaderProcess(msg);
    		
    		// Wait until 'continue and reduce' message received from leader process
    		waitAndReduceContinueMsg();
    		
    		// Obtain global 'andReduce' value to return it
    		boolean valueToReturn = finishSortValue;
    		// Restart value
    		finishSortValue = true;
    		
    		return valueToReturn;
    	}
	}
    
    
    /**
     * Method to send message to leader process
     * @param msg to send
     * @throws SocketException
     * @throws IOException
     */
    private void sendToLeaderProcess(String msg) throws SocketException, IOException {
    	DatagramSocket senderSocket = new DatagramSocket();
    	
    	byte[] buf = new byte[512];
    	buf = msg.getBytes();
    	DatagramPacket packet = new DatagramPacket (buf, buf.length,
    			leaderAddress.getIp(), leaderAddress.getPortUDP());
    	
    	senderSocket.send(packet);
		
    	senderSocket.close();
    }
    
    
    /**
     * Method to send message to all NOT leader processes
     * @param message to send
     * @throws SocketException
     * @throws IOException
     */
    private void broadcast(String message) throws SocketException, IOException {
    	DatagramSocket senderSocket = new DatagramSocket();
    	
    	byte[] buf = new byte[512];
    	buf = message.getBytes();
    	
    	for(int i = 1; i < connectedAddresses.size(); i++) {
    		DatagramPacket packet = new DatagramPacket (buf, buf.length,
    				connectedAddresses.get(i).getIp(), connectedAddresses.get(i).getPortUDP());
        	
        	senderSocket.send(packet);
    	}
    	
    	// Close socket
    	senderSocket.close();
    }
    
    /**
     * Method to send message to process
     * @param processId from process to send a message
     * @param msg to send
     * @throws IOException
     * @throws SocketException
     */
    public void sendTo(int processId, String msg) throws IOException, SocketException {
    	
    	InetAddress mainPeerIp = leaderAddress.getIp();
    	int destinationPeerPort = leaderAddress.getPortUDP() + processId;
    	
    	DatagramSocket senderSocket = new DatagramSocket();
    	
    	byte[] buf = new byte[512];
    	buf = msg.getBytes();
    	
    	for(int i = 1; i < connectedAddresses.size(); i++) {
    		DatagramPacket packet = new DatagramPacket (buf, buf.length,
    				mainPeerIp, destinationPeerPort);
        	
        	senderSocket.send(packet);
    	}
    	// Close socket
    	senderSocket.close();
    }
    

	@Override
    public void run() {
        while (true) {
            try {
                //Listens to messages that comes through the UDP port
                byte[] receiveData = new byte[512];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                listenerSocket.receive(receivePacket);
                byte[] data = receivePacket.getData();
                String receivedMessage = new String(data);
                
                // Split message and obtain action code
                String[] parts = receivedMessage.split(",");
                String action = parts[0];
                
                switch (action) {
                
		            	/* 
		    			 * Not leader process sends their IP address info
		    			 */
		    		case Messages.MSG_NEW_CONECTION: // Message received ONLY by leader process.
	    			
		            	// Obtain sender address data
		            	int senderPort = Integer.parseInt(parts[1]);
		            	// Add sender address to connected list
		            	IPAddress senderAddress = new IPAddress(leaderAddress.getIp().getHostAddress(), senderPort);
		            	connectedAddresses.add(senderAddress);
		            	
		            	messagesReceived++;
		            	notifyLock();
		            	
		    			break;
        		
		    			/* 
	        			 * Leader process notify that all processes address was set
	        			 */
	                case Messages.MSG_INIT: // Message received by NOT leader processes.
	        			
	        			// Obtain not leader ports and add it to connected Addresses list
	        			for(int i = 1; i < parts.length-1; i++) {
		                	int receivedPort = Integer.parseInt(parts[i]);
	        				connectedAddresses.add(new IPAddress(leaderAddress.getIp().getHostAddress(), receivedPort));
	        	    	}
	        			
	        			initMessageReceived = true;
	        			notifyLock();
	        			
	        			break;
        		
	        			
	        			/* 
	        			 * Not leader process notify that the barrier is reached
	        			 */
	        		case Messages.MSG_BARRIER_REACHED: // Message received ONLY by leader process.
	        			
	        			messagesReceived++;
	        			notifyLock();
	        			
	        			break;
	        			
	        			/* 
	        			 * Leader process notify that all processes reached the barrier
	        			 */
	        		case Messages.MSG_CONTINUE: // Message received by NOT leader processes.
	        			
	        			continueMessageReceived = true;
	        			notifyLock();
	        			
	        			break;
	        			
	        			/* 
	        			 * Require to read value in local array and answer message with it
	        			 */
	        		case Messages.MSG_READ_VALUE: // Message received by ALL processes
	        			
	        			sorter.getDistributedArray().treatMessage(receivedMessage);
	        			break;
	        			
	        			/* 
	        			 * Response from MSG_READ_VALUE sent message with array read value
	        			 */
	        		case Messages.MSG_READ_VALUE_RESPONSE: // Message received by ALL processes
	        			
	        			sorter.getDistributedArray().treatMessage(receivedMessage);
	        			
	        			break;
	        			
	        			/* 
	        			 * Require to write received value in array position
	        			 */
	        		case Messages.MSG_WRITE_VALUE: // Message received by ALL processes
	        			
	        			sorter.getDistributedArray().treatMessage(receivedMessage);
	        			
	        			break;
	        			
	        			/* 
	        			 * Not leader process notify that the 'and reduce' is reached
	        			 */
	        		case Messages.MSG_AND_REDUCE_REACHED: // Message received ONLY by leader process.
	        			
	        			int finishValue = Integer.parseInt(parts[2]);
	        			finishSortValue = finishSortValue && finishValue == 1;
	        			
	        			messagesReceived++;
	        			notifyLock();
	        			
	        			break;
	        			
	        			/* 
	        			 * Leader process notify that all processes reached the 'and reduce'
	        			 */
	        		case Messages.MSG_REDUCE_CONTINUE: // Message received by NOT leader processes.
	        			
	        			int finishReduceValue = Integer.parseInt(parts[1]);
	        			finishSortValue = finishSortValue && finishReduceValue == 1;
	        			
	        			andReduceMessageReceived = true;
	        			notifyLock();
	        			
	        			break;
	        		
	        		default:
	        			System.out.println("INVALID ACTION CODE");
	        		}

            } catch (IOException ex) {
            	System.out.print(ex.getMessage());
            }
        }
	}
	
	
	/**
     * Blocks current thread until receive a message by every not leader process
     */
    private void waitAllProcessesMessages() {
    	synchronized (monitor) {
    		while (messagesReceived < totalProcessesCount - 1) {
    			try {
    				monitor.wait(); // wait until notified
    			} catch (Exception e) {}
    	    }
    	}
    	// Restart messageReceived count to 0
    	messagesReceived = 0;
    }
    
    
    /**
     * Blocks current thread until receive 'init' message from leader process
     */
    private void waitInitMsg() {
    	synchronized (monitor) {
    		while (!initMessageReceived) {
    			try {
    				monitor.wait(); // wait until notified
    			} catch (InterruptedException e) {break;}
    	    }
    		initMessageReceived = false;
    	}
    }
    
    /**
     * Blocks current thread until receive 'continue' message from leader process
     */
    private void waitContinueMsg() {
    	synchronized (monitor) {
    		while (!continueMessageReceived) {
    			try {
    				monitor.wait(); // wait until notified
    			} catch (InterruptedException e) {break;}
    	    }
    		continueMessageReceived = false;
    	}
    }
    
    /**
     * Blocks current thread until receive 'andReduce' message from leader process
     */
    private void waitAndReduceContinueMsg() {
    	synchronized (monitor) {
    		while (!andReduceMessageReceived) {
    			try {
    				monitor.wait(); // wait until notified
    			} catch (InterruptedException e) {break;}
    	    }
    		andReduceMessageReceived = false;
    	}
	}
    
    /**
     * Blocks current thread until receive 'read' message
     */
    private void waitValueReadedMsg() {
    	synchronized (monitor) {
    		while (!readMessageReceived) {
    			try {
    				monitor.wait(); // wait until notified
    			} catch (InterruptedException e) {break;}
    	    }
    		readMessageReceived = false;
    	}
    }
    
    
    /**
     * Wakes up thread that is waiting on this object's monitor
     */
    private void notifyLock() {
    	synchronized (monitor) {
			monitor.notify();
        }
    }

}
