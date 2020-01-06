package modelPGAS;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Random;

/**
 * Class that represents Distributed Array. 
 * @author adrian
 *
 */
public class DistributedArray {
	
	private int[] arr;
	private int processId;
	private int localArraySize;
	private Random rand = new Random();
	
	private MiddlewareUDP middlewareUDP;
	
	private final Object monitor = new Object();
	boolean readMessageReceived = false;
	int readValue;
	
	/**
	 * DistributedArray constructor that initializes local array and obtain required references 
	 * @param middlewareUDP to send read and write requests
	 * @param processId
	 * @param processQuantity
	 * @param totalElements
	 */
	public DistributedArray(MiddlewareUDP middlewareUDP, int processId, int processQuantity, int totalElements) {
		
		this.middlewareUDP = middlewareUDP;
		
		this.processId = processId;
		
		localArraySize = totalElements / processQuantity; 
		arr = new int[localArraySize];
		fillWithRandomValues();
	}
	
	/**
	 * Given input process id obtain array lower index
	 * @param pid process id
	 * @return array lower index
	 */
	private int lowerIndex(int pid) {
		return localArraySize * pid;
	}
	
	/**
	 * Given input process id obtain array upper index
	 * @param pid process id
	 * @return array upper index
	 */
	private int upperIndex(int pid) {
		return localArraySize * pid + localArraySize -1;
	}
	
	
	/**
	 * Swap values between processes if are not sorted
	 * @return false iff was required to swap values
	 * @throws IOException
	 */
	public boolean swap() throws IOException {
		// Obtain required index
		int upperIndexCurrentPeer = upperIndex(processId);
		int lowerIndexNextPeer = lowerIndex(processId + 1);
		
		// Obtain required values
		int upperValue = read(upperIndexCurrentPeer);
		int lowerValue = read(lowerIndexNextPeer);
		
		if(upperValue > lowerValue) {
			write(upperIndexCurrentPeer, lowerValue);
			write(lowerIndexNextPeer, upperValue);
			return false; // Local array updated
		}
		
		return true;
	}
	
	
	/**
	 * Method to read global array value, if position index is out of
	 * local array range must require value to another process
	 * @param position (global)
	 * @return read value
	 * @throws SocketException
	 * @throws IOException
	 */
	private int read(int position) throws SocketException, IOException {
		
		int processIdWithPosition = position % (localArraySize - 1);
		
		// Position in local array
		if(processIdWithPosition == processId) {
			int localPosition = position - localArraySize * processId; 
			return arr[localPosition];
		}else {
			int localArrayPosition = position - localArraySize * processIdWithPosition;
			
			String msg = Messages.MSG_READ_VALUE+","+localArrayPosition+","+processId+",";
	    	middlewareUDP.sendTo(processIdWithPosition, msg);
	    	
	    	waitValueReadedMsg();
	    	
	    	return readValue;
		}
	}
	
	/**
	 * Method to write global array, if position index is out of
	 * local array range then must to send value to another process
	 * @param position where write value
	 * @param value to write
	 * @throws SocketException
	 * @throws IOException
	 */
	private void write(int position, int value) throws SocketException, IOException {
		
		int processIdWithPosition = position % (localArraySize - 1);
		
		// Position in local array
		if(processIdWithPosition == processId) {
			int localPosition = position - localArraySize * processId; 
			arr[localPosition] = value;
		}else {
			int localArrayPosition = position - localArraySize * processIdWithPosition;
			
			String msg = Messages.MSG_WRITE_VALUE+","+localArrayPosition+","+value+",";
			middlewareUDP.sendTo(processIdWithPosition, msg);
		}
	}
	
	
	/**
	 * Treat message received from Middleware
	 * @param msg to treat
	 * @throws SocketException
	 * @throws IOException
	 */
	public void treatMessage(String msg) throws SocketException, IOException {
		// Split message and obtain action code
        String[] parts = msg.split(",");
        String action = parts[0];
        
        switch (action) {
        	
	        /* 
			 * Require to read value in local array and answer message with it
			 */
        	case Messages.MSG_READ_VALUE:
    			
    			int position = Integer.parseInt(parts[1]);
    			int responseTo = Integer.parseInt(parts[2]);
    			
    			int value = valueAt(position);
    				
    			String readValueMsg = Messages.MSG_READ_VALUE_RESPONSE+","+processId+","+value+",";
    			middlewareUDP.sendTo(responseTo, readValueMsg);
    			
    			break;
    			
    		/* 
    		* Response from MSG_READ_VALUE sent message with array read value
    		*/
        	case Messages.MSG_READ_VALUE_RESPONSE:
    			
    			readValue = Integer.parseInt(parts[2]);
    			
    			readMessageReceived = true;
    			notifyLock();
    			
    			break;
    			
    		/* 
    		* Require to write received value in array position
    		*/
            case Messages.MSG_WRITE_VALUE:
        		int writePosition = Integer.parseInt(parts[1]);
        		int valueToWrite = Integer.parseInt(parts[2]);
        			
        		writeValueAt(writePosition, valueToWrite);
        			
            	break;    		
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
	
	/**
	 * Returns local value at input position
	 * @param position to get value
	 * @return local array value at position
	 */
	private int valueAt(int position) {
		// Invalid position
		if(position > localArraySize || position < 0) {
			return -1; 
		}else {
			return arr[position];
		}
	}
	
	/**
	 * Write value in local array
	 * @param position
	 * @param value
	 */
	private void writeValueAt(int position, int value) {
		// Valid position
		if(position <= localArraySize || position >= 0) {
			arr[position] = value; 
		}
	}

	/**
	 * Sort local array
	 */
	public void sort() {
		Arrays.sort(arr);
	}
	
	/**
	 * Fill local array with random values
	 */
	private void fillWithRandomValues(){
		for (int i=0 ; i < arr.length ; i++ ) {
			arr[i] = rand.nextInt(5001);
		}
	}
	
	/**
	 * Write array in text file
	 * @param fileName
	 * @param msg to append in line start
	 * @param appendInFile if false delete file if already exists
	 * @throws IOException
	 */
	public void writeArrayInFile(String fileName, String msg, boolean appendInFile) throws IOException {		
		BufferedWriter outputWriter = null;
		outputWriter = new BufferedWriter(new FileWriter(fileName, appendInFile));
		  
		outputWriter.write(msg+" "+Arrays.toString(arr));
		outputWriter.newLine();
		
		outputWriter.flush();  
		outputWriter.close();
	}

}
