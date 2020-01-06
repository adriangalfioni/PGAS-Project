package modelPGAS;

/**
 * Class that encapsulates messages passed between processes
 *  
 * @author adrian
 *
 */
public class Messages {

	public static final String MSG_NEW_CONECTION = "1";
	public static final String MSG_INIT = "2";
	public static final String MSG_BARRIER_REACHED = "3";
	public static final String MSG_CONTINUE = "4";
	public static final String MSG_READ_VALUE = "5";
	public static final String MSG_READ_VALUE_RESPONSE = "6";
	public static final String MSG_WRITE_VALUE = "7";
	public static final String MSG_AND_REDUCE_REACHED = "8";
	public static final String MSG_REDUCE_CONTINUE = "9";
	
}
