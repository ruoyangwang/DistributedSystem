import java.io.Serializable;



public class zkPacket implements Serializable{

	public static final int ZK_NULL = 0;
	//message from client 
	public static final int CLIENT_LOOKUP = 100;
	public static final int CLIENT_REQUEST = 101;
	public static final int CLIENT_STATUS = 102;
	public static final int CLIENT_QUIT = 110;
	
	//message from JobTracker
	public static final int JT_SUBMITTED = 200;
	public static final int JT_QUIT = 210;
	
	//job status
	public static final int JOB_PROGRESS = 300;
	public static final int JOB_DONE = 301;
	public static final int JOB_FAIL = 300;

	public int type = zkPacket.ZK_NULL;
	public String symbol;
}
