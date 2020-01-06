package modelPGAS;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;


/**
 * Class that order array elements in distributed form.
 * This program will run in parallel way operating with different data. 
 * 
 * @author adrian
 *
 */
public class Sorter {
	
	private MiddlewareUDP middlewareUDP;
	private DistributedArray distributedArray;
	
	private IPAddress leaderProcessAddress;
	private int localProcessId;
	private int totalProcesses;
	private int arrayTotalSize;
	
	
	/**
	 * Sorter constructor thats initialize leader and current process data and start Middleware
	 * @param leaderProcessIP
	 * @param leaderProcessPort
	 * @param processId
	 * @param arrayTotalSize
	 * @param processQuantity
	 * @throws IOException
	 */
	public Sorter(String leaderProcessIP, Integer leaderProcessPort, Integer processId, Integer arrayTotalSize, Integer processQuantity) throws IOException {

		leaderProcessAddress = new IPAddress(leaderProcessIP, leaderProcessPort);
		localProcessId = processId;
		totalProcesses = processQuantity;
		this.arrayTotalSize = arrayTotalSize;
		
		// Instantiate Middleware and begin thread execution
		middlewareUDP = new MiddlewareUDP(this);
		middlewareUDP.start();
	}
	
    /**
     * Initialize system
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        if (args.length != 5) {
            System.err.println("Wrong arguments");
            System.exit(1);
        }
        
        String ppalPeerIP = String.valueOf(args[0]);
        int ppalPeerPort = Integer.valueOf(args[1]);
        int peerId = Integer.valueOf(args[2]);
        int arrayTotalSize = Integer.valueOf(args[3]);
        int peersQuantity = Integer.valueOf(args[4]);
        
        new Sorter(ppalPeerIP, ppalPeerPort, peerId, arrayTotalSize, peersQuantity).init();
    }
    

    /**
     * Initialize order actions
     * @throws IOException 
     * @throws SocketException 
     */
    public void init() throws SocketException, IOException {
        
        distributedArray = new DistributedArray(middlewareUDP, localProcessId, totalProcesses, arrayTotalSize);
        
        middlewareUDP.setAllAddresses();

        boolean finish = false;
        
        distributedArray.writeArrayInFile(String.valueOf(localProcessId), "Initial array = ", false);
        
		while (!finish ) {
			finish = true;

			// Sort local array block
			distributedArray.sort();
			
			middlewareUDP.barrier();
			
			// If i am not the last process 
			if(localProcessId != totalProcesses - 1) {
				finish = distributedArray.swap();
			}
			
			// Reduce finish by and
			finish = middlewareUDP.andReduce(finish);
		}
		
		distributedArray.writeArrayInFile(String.valueOf(localProcessId), "Final array = ", true);
        System.exit(0);
    }
    

    /**
     * Get leader address
     * @return leader address
     */
	public IPAddress getLeaderAddress() {
		return leaderProcessAddress;
	}

	/**
	 * Get Distributed Array
	 * @return Distributed Array
	 */
	public DistributedArray getDistributedArray() {
		return distributedArray;
	}

	/**
	 * Get local process ID
	 * @return local process ID
	 */
	public int getLocalProcessId() {
		return localProcessId;
	}

	/**
	 * Get total processes count
	 * @return total processes count
	 */
	public int getTotalProcesses() {
		return totalProcesses;
	}    
    
}
